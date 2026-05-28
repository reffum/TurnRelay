package main

/*
  #include "turnrelay.h"
*/
import "C"


//
// Exported to C JNI
//

//export main_go_start
func main_go_start(
	turn_server_address *C.char,
	turn_server_port C.int,
	turn_username *C.char,
	turn_password *C.char,
	server_address *C.char,
	server_port C.int,
	tun_fd C.int) C.int {

	turnServerAddress := C.GoString(turn_server_address)
	turnServerPort := int(turn_server_port)
	turnUsername := C.GoString(turn_username)
	turnPassword := C.GoString(turn_password)
	serverAddress := C.GoString(server_address)
	serverPort := int(server_port)
	tunFd := int(tun_fd)

	err := TunnelStart(
		turnServerAddress,
		turnServerPort,
		turnUsername,
		turnPassword,
		serverAddress,
		serverPort,
		tunFd)

	if err != nil {
		LogError("Failed to start: ", err);
		return 1
	}

	return 0
}

//export main_go_stop
func main_go_stop() C.int {
	err := TunnelStop()
	if err != nil {
		LogError("Failed to stop: ", err)
		return 1
	}

	return 0
}

func main() {}

