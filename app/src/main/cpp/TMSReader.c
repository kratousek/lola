#include "include/TMSReader.h"
#include <jni.h>

JNIEXPORT jstring JNICALL
Java_com_tomst_lolly_ui_home_HomeFragment_getExampleStringJNI(JNIEnv *env, jobject thiz) {
    libusb_context *ctx;
    char *hello = "Hello from JNI";
    return (*env)->NewStringUTF(env, hello);
}

JNIEXPORT jint JNICALL
Java_com_tomst_lolly_core_TMSReader_setNativeDescriptor(JNIEnv *env, jobject thiz,
                                                        jint fileDescriptor) {
    __android_log_write(ANDROID_LOG_ERROR, "|||DEBUG setnative|||", "start");
    libusb_context *ctx = NULL;
    libusb_device_handle *devh = NULL;
    int r = 0;
    r = libusb_set_option(NULL, LIBUSB_OPTION_NO_DEVICE_DISCOVERY, NULL);
    if (r != LIBUSB_SUCCESS) {
        return -1;
    }
    r = libusb_init(&ctx);
    if (r < 0) {
        return r;
    }
    r = libusb_wrap_sys_device(ctx, (intptr_t)fileDescriptor, &devh);
    if (r < 0) {
        return r;
    } else if (devh == NULL) {
        return r;
    }
    __android_log_write(ANDROID_LOG_ERROR, "|||DEBUG setnative|||", "end");

    return -26;
}

// get device count using libftdi
JNIEXPORT jint JNICALL
Java_com_tomst_lolly_core_TMSReader_getDeviceCount(JNIEnv *env, jobject thiz,
                                                   jint fileDescriptor) {
    __android_log_write(ANDROID_LOG_ERROR, "|||DEBUG|||", "start");
    int VID = 0x0403;
    int PID = 0xada1;

    jint devCount = 20;
    int openResult;
//    struct ftdi_context *ftdi;
//    struct ftdi_device_list *devList;
//
//    ftdi = ftdi_new();
//
//    if (ftdi == NULL) {
//        __android_log_write(ANDROID_LOG_ERROR, "TMSReader", "ftdi_new failed");
//
//        return -1;
//    }



    return -23;

//    __android_log_write(ANDROID_LOG_ERROR, "||| DEBUG |||", "ftdi_new passed, about to get devCount");
//
//    openResult = ftdi_usb_open(ftdi, VID, PID);
//
//    if (openResult == 0) {
//        devCount++;
//    }
//
//    __android_log_write(ANDROID_LOG_ERROR, "||| DEBUG |||", "devCount");
//
//    // are now able to iterate through the list of devices if desired
//
//    // cleanup
//    ftdi_list_free(&devList);
//    ftdi_free(ftdi);
//
//    return devCount;
}


int connectDevice() {

}

//int DoInitFTDI() {
//
//}
