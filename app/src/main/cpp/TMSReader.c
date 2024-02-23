#include "include/TMSReader.h"
#include <jni.h>

JNIEXPORT jstring JNICALL
Java_com_tomst_lolly_ui_home_HomeFragment_getExampleStringJNI(JNIEnv *env, jobject this) {
    libusb_context *ctx;
    char *hello = "Hello from JNI";
    return (*env)->NewStringUTF(env, hello);
}

struct ftdi_context* ftdi_ctx;

int close_device(void)
{
    signal(SIGINT, SIG_DFL);
    ftdi_free(ftdi_ctx);

    return ftdi_usb_close(ftdi_ctx);
}

//int DoInitFTDI() {
//
//}
