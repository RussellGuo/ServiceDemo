#include <jni.h>
#include <string>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
extern "C"
JNIEXPORT jstring

JNICALL
Java_com_huaqin_russell_ServiceDemo_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
extern "C"
JNIEXPORT void JNICALL
Java_com_huaqin_russell_ServiceDemo_MainActivity_RunIt(JNIEnv *env, jobject instance) {

    while(true) {
        int r = system("input swipe 200 200 60 60");
        sleep(r ? 10 : 11);
    }

}