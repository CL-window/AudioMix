//
// Created by slack on 17/5/4.
//

#include "android/log.h"

#ifndef AUDIO1_LOGWRAPPER_H
#define AUDIO1_LOGWRAPPER_H


#define LOG_TAG "RECSDKAPI"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__);
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__);

#endif //AUDIO1_LOGWRAPPER_H


