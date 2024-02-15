#include "include/TMSReader.h"
#include <jni.h>

JNIEXPORT jstring JNICALL
Java_com_tomst_lolly_ui_home_HomeFragment_getExampleStringJNI(JNIEnv *env, jobject this) {
    char *hello = "Hello from JNI";
    return (*env)->NewStringUTF(env, hello);
}

//int DoInitFTDI() {
//
//}
