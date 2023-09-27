#include <jni.h>
#include <string>
#include <android/log.h>

#include <cmath>
#include <iostream>
#include <opencv2/videoio.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/highgui.hpp>

#include "pure.hpp"
#include "pure.cpp"

using namespace std;
using namespace cv;

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_pupillometry_1v1_ProcessDataActivity_processImage(JNIEnv *env, jobject thiz, jstring imagePath, jstring resultPath) {
    const char *imagePathStr = env->GetStringUTFChars(imagePath, nullptr);
    const char *resultPathStr = env->GetStringUTFChars(resultPath, nullptr);

    Mat img, gray, debug;

    // Read image in both color and grayscale
    img = imread(imagePathStr, IMREAD_COLOR);
    if (img.empty()) {
        __android_log_print(ANDROID_LOG_DEBUG, "C++", "Image empty :(");
        exit(-1);
    } else {
        __android_log_print(ANDROID_LOG_DEBUG, "C++", "Image loaded :)");
    }
    cvtColor(img, gray, COLOR_BGR2GRAY);

    // Run the detector
    pure::Detector detector;
    auto result = detector.detect(gray, &debug);

    // Draw result on color image
    ellipse(img, Point(result.center), Size(result.axes), result.angle, 0, 360, Scalar(0, 0, 255));

    // Save image
    imwrite(resultPathStr, img);

    env->ReleaseStringUTFChars(imagePath, imagePathStr);
    env->ReleaseStringUTFChars(resultPath, resultPathStr);

    std::string center_x = std::to_string(result.center.x);
    std::string center_y = std::to_string(result.center.y);
    std::string width = std::to_string(result.axes.width);
    std::string height = std::to_string(result.axes.height);
    std::string angle = std::to_string(result.angle);
    std::string confidence_value = std::to_string(result.confidence.value);
    std::string confidence_aspect_ratio = std::to_string(result.confidence.aspect_ratio);
    std::string confidence_angular_spread = std::to_string(result.confidence.angular_spread);
    std::string confidence_outline_contrast = std::to_string(result.confidence.outline_contrast);

    std::string res = center_x + "#" + center_y + "#" + width + "#" + height + "#" + angle
            + "#" + confidence_value + "#" + confidence_aspect_ratio + "#"
            + confidence_angular_spread + "#" + confidence_outline_contrast;

    return env->NewStringUTF(res.c_str());
}