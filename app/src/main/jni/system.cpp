#define LOG_TAG "Shadowsocks"

#include "jni.h"
#include <android/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <cpu-features.h>

#include <sys/un.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <ancillary.h>
#include <cstring>
#define LOGI(...) do { __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__); } while(0)
#define LOGW(...) do { __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__); } while(0)
#define LOGE(...) do { __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__); } while(0)

jstring Java_net_typeblog_socks_system_getabi(JNIEnv *env, jobject thiz) {
  AndroidCpuFamily family = android_getCpuFamily();
  uint64_t features = android_getCpuFeatures();
  const char *abi;

  if (family == ANDROID_CPU_FAMILY_X86) {
    abi = "x86";
  } else if (family == ANDROID_CPU_FAMILY_MIPS) {
    abi = "mips";
  } else if (family == ANDROID_CPU_FAMILY_ARM) {
    // if (features & ANDROID_CPU_ARM_FEATURE_ARMv7) {
    abi = "armeabi-v7a";
    // } else {
    //   abi = "armeabi";
    // }
  } else if (family == ANDROID_CPU_FAMILY_ARM64) {
  	abi = "arm64-v8a";
  }
  return env->NewStringUTF(abi);
}

void Java_net_typeblog_socks_system_exec(JNIEnv *env, jobject thiz, jstring cmd) {
    const char *str  = env->GetStringUTFChars(cmd, 0);
    system(str);
    env->ReleaseStringUTFChars(cmd, str);
}

void Java_net_typeblog_socks_system_jniclose(JNIEnv *env, jobject thiz, jint fd) {
    close(fd);
}

jint Java_net_typeblog_socks_system_sendfd(JNIEnv *env, jobject thiz, jint tun_fd) {
    int fd;
    struct sockaddr_un addr;

    if ( (fd = socket(AF_UNIX, SOCK_STREAM, 0)) == -1) {
        LOGE("socket() failed: %s (socket fd = %d)\n", strerror(errno), fd);
        return (jint)-1;
    }

    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    strncpy(addr.sun_path, "/data/data/net.typeblog.socks/sock_path", sizeof(addr.sun_path)-1);

    if (connect(fd, (struct sockaddr*)&addr, sizeof(addr)) == -1) {
        LOGE("connect() failed: %s (fd = %d)\n", strerror(errno), fd);
        close(fd);
        return (jint)-1;
    }

    if (ancil_send_fd(fd, tun_fd)) {
        LOGE("ancil_send_fd: %s", strerror(errno));
        close(fd);
        return (jint)-1;
    }

    close(fd);
    return 0;
}

static const char *classPathName = "net/typeblog/socks/System";

static JNINativeMethod method_table[] = {
    { "jniclose", "(I)V",
        (void*) Java_net_typeblog_socks_system_jniclose },
    { "sendfd", "(I)I",
        (void*) Java_net_typeblog_socks_system_sendfd },
    { "exec", "(Ljava/lang/String;)V",
        (void*) Java_net_typeblog_socks_system_exec },
    { "getABI", "()Ljava/lang/String;",
        (void*) Java_net_typeblog_socks_system_getabi }
};



/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv* env, const char* className,
    JNINativeMethod* gMethods, int numMethods)
{
    jclass clazz;

    clazz = env->FindClass(className);
    if (clazz == NULL) {
        LOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        LOGE("RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
 * Register native methods for all classes we know about.
 *
 * returns JNI_TRUE on success.
 */
static int registerNatives(JNIEnv* env)
{
  if (!registerNativeMethods(env, classPathName, method_table,
                 sizeof(method_table) / sizeof(method_table[0]))) {
    return JNI_FALSE;
  }

  return JNI_TRUE;
}

/*
 * This is called by the VM when the shared library is first loaded.
 */

typedef union {
    JNIEnv* env;
    void* venv;
} UnionJNIEnvToVoid;

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    UnionJNIEnvToVoid uenv;
    uenv.venv = NULL;
    jint result = -1;
    JNIEnv* env = NULL;

    LOGI("JNI_OnLoad");

    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
        LOGE("ERROR: GetEnv failed");
        goto bail;
    }
    env = uenv.env;

    if (registerNatives(env) != JNI_TRUE) {
        LOGE("ERROR: registerNatives failed");
        goto bail;
    }

    result = JNI_VERSION_1_4;

bail:
    return result;
}
