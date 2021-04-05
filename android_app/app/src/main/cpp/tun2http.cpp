
#include "tun2http.h"

JavaVM *jvm = nullptr;
int pipefds[2];
pthread_t thread_id = 0;
pthread_mutex_t lock;
int loglevel = ANDROID_LOG_WARN;

extern int max_tun_msg;
extern struct ng_session *ng_session;

// JNI

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    log_android(ANDROID_LOG_INFO, "JNI load");

    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        log_android(ANDROID_LOG_ERROR, "JNI load GetEnv failed");
        return -1;
    }

    // Raise file number limit to maximum
    struct rlimit rlim;
    if (getrlimit(RLIMIT_NOFILE, &rlim))
        log_android(ANDROID_LOG_DEBUG, "getrlimit error %d: %s", errno, strerror(errno));
    else {
        rlim_t soft = rlim.rlim_cur;
        rlim.rlim_cur = rlim.rlim_max;
        if (setrlimit(RLIMIT_NOFILE, &rlim))
            log_android(ANDROID_LOG_DEBUG, "setrlimit error %d: %s", errno, strerror(errno));
        else
            log_android(ANDROID_LOG_VERBOSE, "raised file limit from %d to %d", soft, rlim.rlim_cur);
    }

    return JNI_VERSION_1_6;
}

void JNI_OnUnload(JavaVM *vm, void *reserved) {
    log_android(ANDROID_LOG_INFO, "JNI unload");

    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK)
        log_android(ANDROID_LOG_ERROR, "JNI load GetEnv failed");
}

// JNI ServiceSinkhole

extern "C" JNIEXPORT void JNICALL
Java_tun_proxy_service_Tun2HttpVpnService_jni_1init(JNIEnv *env, jobject instance) {
    loglevel = ANDROID_LOG_WARN;

    struct arguments args;
    args.env = env;
    args.instance = instance;
    init(&args);

    if (pthread_mutex_init(&lock, nullptr))
        log_android(ANDROID_LOG_ERROR, "pthread_mutex_init failed");

    // Create signal pipe
    if (pipe(pipefds))
        log_android(ANDROID_LOG_ERROR, "Create pipe error %d: %s", errno, strerror(errno));
    else
        for (int i = 0; i < 2; i++) {
            int flags = fcntl(pipefds[i], F_GETFL, 0);
            if (flags < 0 || fcntl(pipefds[i], F_SETFL, flags | O_NONBLOCK) < 0)
                log_android(ANDROID_LOG_ERROR, "fcntl pipefds[%d] O_NONBLOCK error %d: %s",
                            i, errno, strerror(errno));
        }
}

extern "C" JNIEXPORT void JNICALL
Java_tun_proxy_service_Tun2HttpVpnService_jni_1start(
        JNIEnv *env, jobject instance, jint tun, jboolean fwd53, jint rcode, jstring proxyIp, jint proxyPort) {

    const char *proxy_ip = env->GetStringUTFChars(proxyIp, nullptr);

    max_tun_msg = 0;

    // Set blocking
    int flags = fcntl(tun, F_GETFL, 0);
    if (flags < 0 || fcntl(tun, F_SETFL, flags & ~O_NONBLOCK) < 0)
        log_android(ANDROID_LOG_ERROR, "fcntl tun ~O_NONBLOCK error %d: %s",
                    errno, strerror(errno));

    if (thread_id && pthread_kill(thread_id, 0) == 0)
        log_android(ANDROID_LOG_ERROR, "Already running thread %x", thread_id);
    else {
        jint rs = env->GetJavaVM(&jvm);
        if (rs != JNI_OK)
            log_android(ANDROID_LOG_ERROR, "GetJavaVM failed");

        // Get arguments
        struct arguments *args = static_cast<arguments *>(malloc(sizeof(struct arguments)));
        // args->env = will be set in thread
        args->instance = env->NewGlobalRef(instance);
        args->tun = tun;
        args->fwd53 = fwd53;
        args->rcode = rcode;
        strcpy(args->proxyIp, proxy_ip);
        args->proxyPort = proxyPort;


        // Start native thread
        int err = pthread_create(&thread_id, nullptr, handle_events, (void *) args);
        if (err == 0)
            log_android(ANDROID_LOG_DEBUG, "Started thread %x", thread_id);
        else
            log_android(ANDROID_LOG_ERROR, "pthread_create error %d: %s", err, strerror(err));


    }

    env->ReleaseStringUTFChars(proxyIp, proxy_ip);
}

extern "C" JNIEXPORT void JNICALL
Java_tun_proxy_service_Tun2HttpVpnService_jni_1stop(
        JNIEnv *env, jobject instance, jint tun) {
    pthread_t t = thread_id;
    log_android(ANDROID_LOG_DEBUG, "Stop tun %d  thread %x", tun, t);
    if (t && pthread_kill(t, 0) == 0) {
        log_android(ANDROID_LOG_VERBOSE, "Write pipe thread %x", t);
        if (write(pipefds[1], "x", 1) < 0)
            log_android(ANDROID_LOG_WARN, "Write pipe error %d: %s", errno, strerror(errno));
        else {
            log_android(ANDROID_LOG_VERBOSE, "Join thread %x", t);
            int err = pthread_join(t, nullptr);
            if (err != 0)
                log_android(ANDROID_LOG_WARN, "pthread_join error %d: %s", err, strerror(err));
        }

        clear();

        log_android(ANDROID_LOG_DEBUG, "Stopped thread %x", t);
    } else
        log_android(ANDROID_LOG_VERBOSE, "Not running thread %x", t);
}

extern "C" JNIEXPORT jint JNICALL
Java_tun_proxy_service_Tun2HttpVpnService_jni_1get_1mtu(JNIEnv *env, jobject instance) {
    return get_mtu();
}


extern "C" JNIEXPORT void JNICALL
Java_tun_proxy_service_Tun2HttpVpnService_jni_1done(JNIEnv *env, jobject instance) {
    log_android(ANDROID_LOG_INFO, "Done");

    clear();

    if (pthread_mutex_destroy(&lock))
        log_android(ANDROID_LOG_ERROR, "pthread_mutex_destroy failed");

    for (int i = 0; i < 2; i++)
        if (close(pipefds[i]))
            log_android(ANDROID_LOG_ERROR, "Close pipe error %d: %s", errno, strerror(errno));
}

// JNI Util

extern "C" JNIEXPORT jstring JNICALL
Java_tun_utils_Util_jni_1getprop(JNIEnv *env, jclass type, jstring name_) {
    const char *name = env->GetStringUTFChars(name_, 0);

    char value[PROP_VALUE_MAX + 1] = "";
    __system_property_get(name, value);

    env->ReleaseStringUTFChars(name_, name);

    return env->NewStringUTF( value);
}

static jmethodID midProtect = nullptr;


int protect_socket(const struct arguments *args, int socket) {
    jclass cls = args->env->GetObjectClass(args->instance);
    if (midProtect == nullptr)
        midProtect = jniGetMethodID(args->env, cls, "protect", "(I)Z");

    jboolean isProtected = args->env->CallBooleanMethod(
            args->instance, midProtect, socket);
    jniCheckException(args->env);

    if (!isProtected) {
        log_android(ANDROID_LOG_ERROR, "protect socket failed");
        return -1;
    }

    args->env->DeleteLocalRef(cls);

    return 0;
}


jmethodID jniGetMethodID(JNIEnv *env, jclass cls, const char *name, const char *signature) {
    jmethodID method = env->GetMethodID(cls, name, signature);
    if (method == nullptr) {
        log_android(ANDROID_LOG_ERROR, "Method %s %s not found", name, signature);
        jniCheckException(env);
    }
    return method;
}

int jniCheckException(JNIEnv *env) {
    jthrowable ex = env->ExceptionOccurred();
    if (ex) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        env->DeleteLocalRef(ex);
        return 1;
    }
    return 0;
}
