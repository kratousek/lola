#include "include/TMSReader.h"
#include <jni.h>

JNIEXPORT jstring JNICALL
Java_com_tomst_lolly_ui_home_HomeFragment_getExampleStringJNI(JNIEnv *env, jobject thiz) {
    libusb_context *ctx;
    char *hello = "Hello from JNI";
    return (*env)->NewStringUTF(env, hello);
}

JNIEXPORT void JNICALL
Java_com_tomst_lolly_core_TMSReader_setNativeDescriptor(JNIEnv *env, jobject thiz,
                                                        jint fileDescriptor) {
    libusb_context *ctx;
    libusb_device_handle *devh;
    libusb_set_option(&ctx, LIBUSB_OPTION_NO_DEVICE_DISCOVERY, NULL);
    libusb_init(&ctx);
    libusb_wrap_sys_device(NULL, (intptr_t)fileDescriptor, &devh);
}

// get device count using libftdi
JNIEXPORT jint JNICALL
Java_com_tomst_lolly_core_TMSReader_getDeviceCount(JNIEnv *env, jobject thiz,
                                                   jint fileDescriptor) {
    int VID = 0x0403;
    int PID = 0xada1;

    libusb_context *ctx = NULL;
    libusb_device_handle *devh;
    libusb_set_option(NULL, LIBUSB_OPTION_NO_DEVICE_DISCOVERY, NULL);
    libusb_init(&ctx);
    libusb_wrap_sys_device(ctx, (intptr_t)fileDescriptor, &devh);

    jint devCount;
    struct ftdi_context *ftdi;
    struct ftdi_device_list *devList;


    ftdi = ftdi_new();

    if (ftdi == NULL) {
        __android_log_write(ANDROID_LOG_ERROR, "TMSReader", "ftdi_new failed");

        return -1;
    }

    __android_log_write(ANDROID_LOG_ERROR, "||| DEBUG |||", "ftdi_new passed, about to get devCount");

    devCount = ftdi_usb_find_all(ftdi, &devList, VID, PID);

    __android_log_write(ANDROID_LOG_ERROR, "||| DEBUG |||", "devCount");

    // are now able to iterate through the list of devices if desired

    // cleanup
    ftdi_list_free(&devList);
    ftdi_free(ftdi);

    return devCount;
}


int connectDevice() {

}

//int DoInitFTDI() {
//
//}
