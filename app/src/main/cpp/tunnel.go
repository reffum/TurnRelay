package main

/*
   #include "turnrelay.h"
*/
import "C"
import "fmt"
import "os"
import "io"
import "net"
import "context"
import "sync"
import "syscall"

import (
	"github.com/pion/dtls/v3"
	"github.com/pion/dtls/v3/pkg/crypto/selfsign"
	"github.com/pion/transport/v3/vnet"
	"github.com/pion/turn/v3"
)

var (
	ctxMu  sync.Mutex
	cancel context.CancelFunc
)

func updateStateDisconnected() {
	C.TunnelProcess_updateState(C.STATE_DISCONNECT)
}

func updateStateConnected() {
	C.TunnelProcess_updateState(C.STATE_CONNECTED)
}

func updateStateError() {
	C.TunnelProcess_updateState(C.STATE_ERROR)
}

/**
  Protect the connection from sending data throw the TUN itself.
  It use Android VpnService.protect()
**/
func protectConnection(conn net.PacketConn) error {
	LogInfo("Protect connection")

	sysConn, ok := conn.(syscall.Conn)
	if !ok {
		LogError("conn doesn't support syscall.Conn")
		return fmt.Errorf("conn doesn't support syscall.Conn")
	}

	rawConn, err := sysConn.SyscallConn()
	if err != nil {
		LogError("sysConn.SyscallConn return error")
		return fmt.Errorf("sysConn.SyscallConn")
	}

	var protectResult bool
	
	err = rawConn.Control(func(fd uintptr) {
		LogDebug("Calling protect for connection")
		r := C.TunnelProcess_protect(C.int(fd))
		protectResult = bool(r)
	})
	
	if err != nil {
		LogError("Protect connection error. sysConn.Control returned error")
		return fmt.Errorf("Protect connection error. sysConn.Control returned error")
	}

	if !protectResult {
		LogError("TunnelProcess.protect() return false. ")
		return fmt.Errorf("TunnelProcess.protect() return error")
	}

	return nil
}


func TunnelStart(
	turnServerAddress string,
	turnServerPort int,
	turnUsername string,
	turnPassword string,
	serverAddress string,
	serverPort int,
 	tunFd int) error {

	LogInfo("Tunnel Start")
	LogInfo("TURN server address: ", turnServerAddress)
	LogInfo("TURN server port: ", turnServerPort)
	LogInfo("TURN username: ", turnUsername)
	LogInfo("TURN password: ", turnPassword)
	LogInfo("Remote server address: ", serverAddress)
	LogInfo("Remote server port: ", serverPort)

	updateStateDisconnected()

	fdFile := os.NewFile(uintptr(tunFd), "tun-device")

	serverAddressPort := fmt.Sprintf("%s:%d", serverAddress, serverPort)
	turnAddressPort := fmt.Sprintf("%s:%d", turnServerAddress, turnServerPort)
	
	turnRelayConnection, err := createTurnConnection(
		turnAddressPort,
		turnUsername,
		turnPassword)

	if err != nil {
		LogError("Create TURN connection error:", err)
		updateStateError()
		return fmt.Errorf("Create TURN connection error:", err)
	}

	LogInfo("Connecting to TURN successful")

	dtlsConn, err := createDtlsConnection(turnRelayConnection, serverAddressPort)
	if err != nil {
		LogError("Create DTLS connection error:", err)
		updateStateError()
		return fmt.Errorf("Create DTLS connection error:", err)
	}

	runProxy(fdFile, dtlsConn)
	updateStateConnected()
	
	return nil
}

func TunnelStop() error {
	LogInfo("Tunnel Stop")

	ctxMu.Lock()
	defer ctxMu.Unlock()

	if cancel != nil {
		cancel()
		cancel = nil
		LogInfo("Go: Cancel signal sent to Copy task.")
	} else {
		LogWarn("Tunnel isn't running yet")
	}

	updateStateDisconnected()
	return nil
}

func createTurnConnection(
	turnAddress string,
	username string,
	password string) (net.PacketConn, error) {

	// 1. Create a virtual network configuration
	vnetConfig := &vnet.NetConfig{
		// Leaving this empty creates a localized virtual network environment 
		// that doesn't attempt to scan Android's Netlink routing tables.
	}

	myVnet, err := vnet.NewNet(vnetConfig)
	if err != nil {
		return nil, err
	}	

	conn, err := net.ListenPacket("udp4", "0.0.0.0:0")
	if err != nil {
		return nil, fmt.Errorf("create TURN listener error: %v", err)
	}

	err = protectConnection(conn)
	if err != nil {
		return nil, err
	}

	config := &turn.ClientConfig{
		STUNServerAddr: turnAddress,
		TURNServerAddr: turnAddress,
		Conn:           conn,
		Net:			myVnet,
		Username:       username,
		Password:       password,
		Realm:          "internal.local",
	}

	client, err := turn.NewClient(config)
	if err != nil {
		return nil, fmt.Errorf("create new TURN client error: %v", err)
	}

	err = client.Listen()
	if err != nil {
		return nil, fmt.Errorf("create new TURN listen error: %v", err)
	}

	relayConn, err := client.Allocate()
	if err != nil {
		return nil, fmt.Errorf("TURN client allocate error: %v", err)
	}

	return relayConn, nil
}

func createDtlsConnection(
	relayConn net.PacketConn,
	addrString string) (*dtls.Conn, error) {
	
	// Generate a self-signed certificate
	certificate, err := selfsign.GenerateSelfSigned()
	if err != nil {
		return nil, fmt.Errorf("certificate generate failed: %w", err)
	}

	addr, err := net.ResolveUDPAddr("udp", addrString)
	if err != nil {
		return nil, fmt.Errorf("resolve udp address failed: %w", err)
	}

	dtlsConn, err := dtls.ClientWithOptions(
		relayConn,
		addr,
		dtls.WithCertificates(certificate),
		dtls.WithInsecureSkipVerify(true),
		dtls.WithExtendedMasterSecret(dtls.RequireExtendedMasterSecret),
	)

	if err != nil {
		return nil, fmt.Errorf("dtls dial failed: %w", err)
	}

	return dtlsConn, nil
}

func runProxy(tun *os.File, client net.Conn) {
	LogInfo("Run the tunnel threads")

	ctxMu.Lock()
	defer ctxMu.Unlock()

	// Stop any existing background task first
	if cancel != nil {
		cancel()
	}

	var ctx context.Context
	ctx, cancel = context.WithCancel(context.Background())

	go func() {
		LogInfo("Tunnel thread is initializing.")

		errChan := make(chan error, 2)
		
		// Tunnel -> Client (Encapsulate)
		go func() {
			_, err := io.Copy(client, tun)
			errChan <- err
		}()

		// Client -> Tunnel (Decapsulate)
		go func() {
			_, err := io.Copy(tun, client)
			errChan <- err
		}()

		select {
		case <-ctx.Done():
			LogInfo("Tunnel thread has stopped");
			client.Close()
			updateStateDisconnected()
			return
		case err := <-errChan:
			if err != nil {
				LogError("Go: Copy finished with error:", err)
				updateStateError()
			} else {
				LogInfo("Go: Copy finished successfully (EOF reached).")
				updateStateDisconnected()
			}
			return			
		}

		defer client.Close()
	} ()

	LogInfo("Connecton has completed")
}
