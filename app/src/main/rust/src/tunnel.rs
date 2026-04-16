use nix::errno::Errno;
use nix::fcntl::{fcntl, FcntlArg, OFlag};
use nix::unistd::{read, write};
use std::io::ErrorKind;
use std::net::ToSocketAddrs;
use std::os::fd::{AsFd, BorrowedFd};
use std::sync::Arc;
use std::thread::{spawn, JoinHandle};
use tokio::io::unix::AsyncFd;
use tokio::io::{Error, Result};
use tokio::net::UdpSocket;
use tokio_util::sync::CancellationToken;
use webrtc::turn::client::{Client, ClientConfig};
use webrtc::util::Conn;


const BUFFER_SIZE: usize = 4096;

pub enum TunnelState {
    Idle,
    Connected,
    Error,
}

pub struct Tunnel {
    turn_server: String,
    turn_port: u16,
    turn_user: String,
    turn_pass: String,

    remote_server: String,
    remote_port: u16,

    thread_handle: Option<JoinHandle<Result<()>>>,
    tun_fd: BorrowedFd<'static>,

    state: TunnelState,

    on_state_changed: fn(TunnelState),
    cancellation_token: CancellationToken,
}

impl Tunnel {
    pub(crate) fn new(
        turn_server: String,
        turn_port: u16,
        turn_user: String,
        turn_pass: String,

        remote_server: String,
        remote_port: u16,

        tun_fd: BorrowedFd<'static>,
        on_state_changed: fn(TunnelState)
    ) -> Self {
        Self {
            turn_server,
            turn_port,
            turn_user,
            turn_pass,
            remote_server,
            remote_port,
            tun_fd,
            thread_handle: None,
            state: TunnelState::Idle,
            on_state_changed,
            cancellation_token: CancellationToken::default(),
        }
    }

    /**
     * The main function of the tunnel server thread.
     */
    async fn tunnel_work_func(
        remote_server: String,
        remote_port: u16,
        turn_server: String,
        turn_port: u16,
        turn_user: String,
        turn_pass: String,
        tun_fd: BorrowedFd<'_>,
        cancellation_token: CancellationToken,
        on_state_changed: fn(TunnelState)
    ) -> Result<()> {
        let remote_server_addr_str = format!("{}:{}", remote_server, remote_port);
        let remote_server_addr = remote_server_addr_str
            .to_socket_addrs()?
            .next()
            .expect("remote server address not resolved");

        let turn_server_addr_str = format!("{}:{}", turn_server, turn_port);

        // The TURN client can't create a local UDP socket by itself
        let turn_udp_conn = UdpSocket::bind("0.0.0.0:0").await?;

        // Create the TURN client
        let turn_config = ClientConfig {
            stun_serv_addr: turn_server_addr_str.clone(),
            turn_serv_addr: turn_server_addr_str.clone(),
            username: turn_user.clone(),
            password: turn_pass.clone(),
            conn: Arc::new(turn_udp_conn),
            realm: String::new(),
            software: String::new(),
            rto_in_ms: 0,
            vnet: None
        };

        info!("Connecting to TURN server...");
        let turn_client = Client::new(turn_config).await.unwrap();
        match turn_client.listen().await {
            Ok(_) => {},
            Err(e) => {
                error!("TURN client listening error: {:?}", e);
                on_state_changed(TunnelState::Error);
                return Err(Error::new(ErrorKind::Other, e))
            }
        };

        let relay_conn = match turn_client.allocate().await {
            Ok(relay_conn) => relay_conn,
            Err(e) => {
                error!("TURN client allocate error: {:?}", e);
                on_state_changed(TunnelState::Error);
                return Err(Error::new(ErrorKind::Other, e))
            }
        };

        info!("Connected to TURN server successfully.!");

        let turn_conn = Arc::new(relay_conn);

        let mut tun_buffer = [0u8; BUFFER_SIZE];
        let mut net_buffer = [0u8; BUFFER_SIZE];

        set_nonblocking(tun_fd)
            .expect("Failed to set TUN fd to non-blocking");

        let tun_async_fd = AsyncFd::new(tun_fd.as_fd())
            .expect("Failed to register TUN fd with tokio");

        let result: Result<()>;
        on_state_changed(TunnelState::Connected);

        loop {
            tokio::select! {
                // Handle TUN read. Read from TUN and send to the remote server via TURN
                guard = tun_async_fd.readable() => {
                    let mut mut_guard = guard.expect("IO error");

                    let readed = match read(tun_async_fd.get_ref(), &mut tun_buffer) {
                        Ok(0) => {
                            error!("TUN closed");
                            result = Err(Error::new(ErrorKind::UnexpectedEof, "TUN closed"));
                            break;
                        },
                        Ok(n) => n,
                        Err(Errno::EAGAIN) | Err(Errno::EINTR) => {
                            mut_guard.clear_ready();
                            continue;
                        }
                        Err(e) => {
                            error!("TUN read error: {:?}", e);
                            result = Err(Error::new(ErrorKind::Other, e));
                            break;
                        }
                    };

                    let send_result = turn_conn.send_to(
                        &tun_buffer[..readed],
                        remote_server_addr
                    ).await;

                    if send_result.is_err() {
                        error!("Send data to remote server error: {:?}", send_result.err());
                        result = Err(Error::new(ErrorKind::Other, "Failed to send data to remote server"));
                        break;
                    }
                }

                // Handle TURN read. Read from TURN and send to the TUN
                r = turn_conn.recv_from(&mut net_buffer) => {
                    let (size, remote_addr) = match r {
                        Ok((size, addr)) => (size, addr),
                        Err(e) => {
                            eprint!("TURN read error: {:?}", e);
                            result = Err(Error::new(ErrorKind::Other, "TURN read error"));
                            break;
                        }
                    };

                    if remote_addr == remote_server_addr {
                        match write(tun_async_fd.get_ref(), &net_buffer[..size]) {
                            Ok(_) => {},
                            Err(e) => {
                                error!("TUN write error: {:?}", e);
                                result = Err(Error::new(ErrorKind::Other, "TUN write error"));
                                break;
                            }
                        };
                    } else {
                        error!("Received data from unexpected address: {}", remote_addr);
                    }
                }

                _ = cancellation_token.cancelled() => {
                    error!("Server thread cancelled");
                    result = Ok(());
                    break;
                }
            }
        }

        if result.is_ok() {
            on_state_changed(TunnelState::Idle);
        } else {
            on_state_changed(TunnelState::Error);
        }

        result
    }

    /**
     * Start the tunnel server thread.
     * It runs the tunnel server in a separate thread.
     */
    pub fn start(&mut self) -> Result<()> {
        if self.thread_handle.is_some() {
            return Err(Error::new(ErrorKind::AlreadyExists, "Server thread already exists"));
        }

        self.state = TunnelState::Idle;

        error!("Starting Tunnel worker.");

        let remote_server = self.remote_server.clone();
        let remote_port = self.remote_port;
        let turn_server = self.turn_server.clone();
        let turn_port = self.turn_port;
        let turn_pass = self.turn_pass.clone();
        let turn_user = self.turn_user.clone();
        let tun_fd = self.tun_fd;
        let cancellation_token = self.cancellation_token.clone();
        let on_state_changed = self.on_state_changed;

        let handle = spawn(move || {
            let rt = tokio::runtime::Runtime::new().unwrap();
           rt.block_on(Tunnel::tunnel_work_func(
                remote_server,
                remote_port,
                turn_server,
                turn_port,
                turn_user,
                turn_pass,
                tun_fd,
                cancellation_token,
                on_state_changed
            ))
        });

        self.thread_handle = Some(handle);

        error!("Tunnel worker started successfully.");

        Ok(())
    }

    pub fn stop(&mut self) -> Result<()> {
        if self.thread_handle.is_none() {
            return Err(Error::new(ErrorKind::NotFound, "Tunnel thread does not exist"));
        }

        error!("Stopping Tunnel worker.");

        let handle = self.thread_handle.take().unwrap();
        let result = handle.join().unwrap();

        if result.is_err() {
            return Ok(());
        } else {
            error!("Tunnel thread exiting with error: {}.", result.unwrap_err());
        }

        Ok(())
    }
}

/**
 * Set the given file descriptor to non-blocking mode.
 */
fn set_nonblocking(fd: BorrowedFd) -> Result<()> {
    // 1. Get current flags
    let flags = fcntl(fd, FcntlArg::F_GETFL)?;
    let mut new_flags = OFlag::from_bits_truncate(flags);

    // 2. Add the non-blocking flag
    new_flags.insert(OFlag::O_NONBLOCK);

    // 3. Set the updated flags back to the FD
    fcntl(fd, FcntlArg::F_SETFL(new_flags))?;

    Ok(())
}
