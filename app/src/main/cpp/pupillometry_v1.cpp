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
Java_com_example_pupillometry_1v1_ProcessDataActivity_processImage(JNIEnv *env, jobject thiz, jlong image) {
    Mat &img = *(Mat *) image;

    Mat gray, debug;

    // Read image in both color and grayscale
    cvtColor(img, gray, COLOR_BGR2GRAY);

    // Run the detector
    pure::Detector detector;
    auto result = detector.detect(gray, &debug);

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

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_pupillometry_1v1_ProcessDataActivity_processImageWithSave(JNIEnv *env, jobject thiz, jlong image, jstring resultPath) {
    Mat &img = *(Mat *) image;
    const char *resultPathStr = env->GetStringUTFChars(resultPath, nullptr);

    Mat gray, debug;

    // Read image in both color and grayscale
    cvtColor(img, gray, COLOR_BGR2GRAY);

    // Run the detector
    pure::Detector detector;
    auto result = detector.detect(gray, &debug);

    // Draw ellipse
    ellipse(img, Point(result.center), Size(result.axes), result.angle, 0, 360, Scalar(0, 0, 255));

    // Save image
    imwrite(resultPathStr, img);

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