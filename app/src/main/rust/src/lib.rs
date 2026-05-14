use jni::errors::Error;
use jni::objects::{JObject, JString};
use jni::refs::Global;
use jni::sys::{jint, jlong, JNI_VERSION_1_6};
use jni::{jni_sig, jni_str, EnvUnowned, JValue, JavaVM};
use jni::jni_mangle;

use std::os::fd::{BorrowedFd, RawFd};
use std::sync::OnceLock;

#[macro_use]
extern crate log;
extern crate android_logger;

use log::LevelFilter;
use android_logger::Config;

mod tunnel;
pub use tunnel::{Tunnel, TunnelState};

// Global reference to the TunnelProcess object
static TUNNEL_PROCESS: OnceLock<Global<JObject>> = OnceLock::new();


fn on_state_change(state: TunnelState) {
    let jvm = JavaVM::singleton().unwrap();

    jvm.attach_current_thread(|env| -> Result<(), jni::errors::Error> {
        let g_class = TUNNEL_PROCESS.get().unwrap();

        env.call_method(
            &g_class,
            jni_str!("updateState"),
            jni_sig!("(I)V"),
            &[JValue::Int(state as i32)],
        ).unwrap();
        Ok(())
    }).unwrap();
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn JNI_OnLoad(
    _vm: * mut jni::sys::JavaVM,
    _reserved: *mut std::ffi::c_void) -> jint {

    info!("rust_lib. JNI_OnLoad called");

    android_logger::init_once(
        Config::default().with_max_level(LevelFilter::Debug),
    );

    JNI_VERSION_1_6
}


#[jni_mangle("com.ogro.turnrelay.net.TunnelProcess")]
pub extern "system" fn start<'caller>(
    mut unowned_env: EnvUnowned<'caller>,
    class: JObject<'caller>,

    turnServerAddress: JString<'caller>,
    turnServerPort: jint,
    turnUsername: JString<'caller>,
    turnPassword: JString<'caller>,
    serverAddress: JString<'caller>,
    serverPort: jint,
    tunFd: jint,
)  {
    let outcome =
        unowned_env.with_env(|env| -> Result<_, Error> {
            if turnServerPort > u16::MAX as i32 {
                return Err(Error::JavaException);
            }

            if serverPort > u16::MAX as i32 {
                return Err(Error::JavaException);
            }

            let turn_server = turnServerAddress.to_string();
            let turn_port = turnServerPort as u16;
            let turn_username = turnUsername.to_string();
            let turn_password = turnPassword.to_string();
            let server_address = serverAddress.to_string();
            let server_port = serverPort as u16;
            let tun_fd = unsafe { BorrowedFd::borrow_raw(tunFd as RawFd) };

            let mut tunnel = Box::new(Tunnel::new(
                turn_server,
                turn_port,
                turn_username,
                turn_password,
                server_address,
                server_port,
                tun_fd,
                on_state_change
            ));

            // Store the TunnelProcess global reference
            let g_clazz = env.new_global_ref(&class).unwrap();
            TUNNEL_PROCESS.get_or_init(|| {g_clazz});

            // Start the tunnel worker thread

            let r = tunnel.start();
            let ptr = Box::into_raw(tunnel) as jlong;

            let clazz = env.get_object_class(class).unwrap();

            env.set_static_field(
                &clazz,
                jni_str!("tunnelPtr"),
                jni_sig!("J"),
                JValue::Long(ptr)
            ).unwrap();

            if r.is_err() {
                return Err(Error::JavaException);
            } else {
                Ok(())
            }
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}


#[jni_mangle("com.ogro.turnrelay.net.TunnelProcess")]
pub extern "system" fn stop<'caller>(
    mut unowned_env: EnvUnowned<'caller>,
    class: JObject<'caller>,
) {
    let outcome = unowned_env.with_env(|env| -> Result<(), Error> {
        let clazz = env.get_object_class(class).unwrap();
        let tunnel_ptr = env.get_static_field(
            &clazz,
            jni_str!("tunnelPtr"),
            jni_sig!("J")).unwrap().j().unwrap();

        if tunnel_ptr == 0 { return Err(Error::JavaException); }

        let mut tunnel = unsafe {
            Box::from_raw(tunnel_ptr as *mut Tunnel)
        };

        info!("Stopping tunnel worker thread...");

        let r = tunnel.stop();

        if r.is_err() {
            return Err(Error::JavaException);
        } else {
            Ok(())
        }
    });

    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>();
}