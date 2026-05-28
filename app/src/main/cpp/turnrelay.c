#include <stdlib.h>
#include <stdbool.h>
#include <jni.h>
#include <android/log.h>

#include "main_go_export.h"

// Log macros
#pragma clang diagnostic push
#pragma ide diagnostic ignored "OCUnusedGlobalDeclarationInspection"
#define LOG_TAG "TurnRelayNDK"

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)


// Global variables shared across threads
static JavaVM *g_vm = NULL;
static jobject g_nativeWrapperGlobalRef = NULL;
static jmethodID g_updateStateMethodId = NULL;
static jmethodID g_protectMethodId = NULL;

//
// Functions exported to Go
//

// JNI call of TunnelProcess function. Threadsafe
void TunnelProcess_updateState(int stateId)
{
    JNIEnv *env = NULL;
    bool is_attached = false;
    jint res = (*g_vm)->GetEnv(g_vm, (void **) &env, JNI_VERSION_1_6);

    // Check if thread is detached.
    if (res == JNI_EDETACHED)
    {
        // 2. Attach the raw native thread to the JVM execution context
        if ((*g_vm)->AttachCurrentThread(g_vm, &env, NULL) != JNI_OK)
        {
            LOGE("Failed to attach native thread to JVM");
            return;
        }
        is_attached = true;
    }
    else
    {
        LOGW("TunnelProcess_updateState: Thread is already attached");
    }

    (*env)->CallVoidMethod(env, g_nativeWrapperGlobalRef, g_updateStateMethodId, stateId);

    if(is_attached)
        (*g_vm)->DetachCurrentThread(g_vm);
}

bool TunnelProcess_protect(int fd)
{
    JNIEnv *env = NULL;
    bool is_attached = false;
    jint res = (*g_vm)->GetEnv(g_vm, (void**)&env, JNI_VERSION_1_6);

    // Check if thread is detached.
    if (res == JNI_EDETACHED)
    {
        // 2. Attach the raw native thread to the JVM execution context
        if ((*g_vm)->AttachCurrentThread(g_vm, &env, NULL) != JNI_OK)
        {
            LOGE("Failed to attach native thread to JVM");
            return false;
        }
        is_attached = true;
    }

    bool r = (*env)->CallBooleanMethod(env, g_nativeWrapperGlobalRef, g_protectMethodId, fd);

    if(is_attached)
        (*g_vm)->DetachCurrentThread(g_vm);

    return r;
}

void write_log_debug(const char* message)
{
    LOGD("%s", message);
}

void write_log_info(const char *message)
{
    LOGI("%s", message);
}

void write_log_warning(const char *message)
{
    LOGW("%s", message);
}

void write_log_error(const char *message)
{
    LOGE("%s", message);
}

//
// JNI functions
//

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void* reserved)
{
    LOGI("JNI_OnLoad is called");

    g_vm = vm;

    JNIEnv *env = NULL;

    // Get the JNIEnv pointer and check the version
    if ((*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_6) != JNI_OK)
    {
        LOGE("GetEnv failed");
        return JNI_ERR;
    }

    // Find the TunnelProcess class that contains your native methods
    jclass clazz = (*env)->FindClass(env, "com/ogro/turnrelay/net/TunnelProcess");
    if (clazz == NULL)
    {
        LOGE("Failed to find MainActivity class");
        return JNI_ERR;
    }

    LOGI("JNI_OnLoad is successfully completed");

    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL
Java_com_ogro_turnrelay_net_TunnelProcess_start(
        JNIEnv *env,
        jobject thiz,
        jstring turnServerAddress,
        jint turnSererPort,
        jstring turnUsername,
        jstring turnPassword,
        jstring serverAddress,
        jint serverPort,
        jint tunFd
) {
    const char * turn_server_address = (*env)->GetStringUTFChars(env, turnServerAddress, NULL);
    const int turn_server_port = turnSererPort;
    const char * turn_username = (*env)->GetStringUTFChars(env, turnUsername, NULL);
    const char * turn_password = (*env)->GetStringUTFChars(env, turnPassword, NULL);
    const char * server_address = (*env)->GetStringUTFChars(env, serverAddress, NULL);
    const int server_port = serverPort;
    const int tun_fd = tunFd;

    // If we haven't cached the global references yet, do it now
    if (g_nativeWrapperGlobalRef == NULL) {
        // Convert the temporary local object reference to a permanent Global Reference
        g_nativeWrapperGlobalRef = (*env)->NewGlobalRef(env, thiz);

        // Find class and cache Method ID
        jclass clazz = (*env)->GetObjectClass(env, thiz);
        g_updateStateMethodId = (*env)->GetMethodID(env, clazz, "updateState", "(I)V");
        g_protectMethodId = (*env)->GetMethodID(env, clazz, "protect", "(I)Z");
    }

    int result = main_go_start(
            turn_server_address,
            turn_server_port,
            turn_username,
            turn_password,
            server_address,
            server_port,
            tun_fd
    );

    if(result != 0)
    {
        jclass exClass = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exClass == NULL)
        {
            LOGE("Failed to find RuntimeException class");
            return;
        }
        (*env)->ThrowNew(env, exClass, "Start failed");
    }

    (*env)->ReleaseStringUTFChars(env, turnServerAddress, turn_server_address);
    (*env)->ReleaseStringUTFChars(env, turnUsername, turn_username);
    (*env)->ReleaseStringUTFChars(env, turnPassword, turn_password);
    (*env)->ReleaseStringUTFChars(env, serverAddress, server_address);
}

JNIEXPORT void JNICALL
Java_com_ogro_turnrelay_net_TunnelProcess_stop(JNIEnv *env, jobject thiz)
{
    LOGD("JNI stop is called");
    main_go_stop();
}
#pragma clang diagnostic pop