#include <jni.h>
#include <android/log.h>
#include "PJHavenDSP.h"

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_haven_music_PJHavenAudioProcessor_nativeCreate(JNIEnv* env, jobject thiz) {
    return reinterpret_cast<jlong>(new PJHavenDSP());
}

JNIEXPORT void JNICALL
Java_com_haven_music_PJHavenAudioProcessor_nativeRelease(JNIEnv* env, jobject thiz, jlong handle) {
    delete reinterpret_cast<PJHavenDSP*>(handle);
}

JNIEXPORT void JNICALL
Java_com_haven_music_PJHavenAudioProcessor_nativeSetSampleRate(JNIEnv* env, jobject thiz, jlong handle, jfloat sr) {
    auto dsp = reinterpret_cast<PJHavenDSP*>(handle);
    if (dsp) dsp->setSampleRate(sr);
}

JNIEXPORT void JNICALL
Java_com_haven_music_PJHavenAudioProcessor_nativeSetEnabled(JNIEnv* env, jobject thiz, jlong handle, jboolean enabled) {
    auto dsp = reinterpret_cast<PJHavenDSP*>(handle);
    if (dsp) dsp->setEnabled(enabled);
}

JNIEXPORT void JNICALL
Java_com_haven_music_PJHavenAudioProcessor_nativeSetProfile(JNIEnv* env, jobject thiz, jlong handle, jint profile) {
    auto dsp = reinterpret_cast<PJHavenDSP*>(handle);
    if (dsp) dsp->setProfile(profile);
}

JNIEXPORT void JNICALL
Java_com_haven_music_PJHavenAudioProcessor_nativeSetPunchIntensity(JNIEnv* env, jobject thiz, jlong handle, jfloat intensity) {
    auto dsp = reinterpret_cast<PJHavenDSP*>(handle);
    if (dsp) dsp->setPunchIntensity(intensity);
}

JNIEXPORT void JNICALL
Java_com_haven_music_PJHavenAudioProcessor_nativeSetImmerseIntensity(JNIEnv* env, jobject thiz, jlong handle, jfloat intensity) {
    auto dsp = reinterpret_cast<PJHavenDSP*>(handle);
    if (dsp) dsp->setImmerseIntensity(intensity);
}

JNIEXPORT void JNICALL
Java_com_haven_music_PJHavenAudioProcessor_nativeSetAuraIntensity(JNIEnv* env, jobject thiz, jlong handle, jfloat intensity) {
    auto dsp = reinterpret_cast<PJHavenDSP*>(handle);
    if (dsp) dsp->setAuraIntensity(intensity);
}

JNIEXPORT void JNICALL
Java_com_haven_music_PJHavenAudioProcessor_nativeSetSpaceIntensity(JNIEnv* env, jobject thiz, jlong handle, jfloat intensity) {
    auto dsp = reinterpret_cast<PJHavenDSP*>(handle);
    if (dsp) dsp->setSpaceIntensity(intensity);
}

JNIEXPORT void JNICALL
Java_com_haven_music_PJHavenAudioProcessor_nativeSetEQBand(JNIEnv* env, jobject thiz, jlong handle, jint band, jfloat gainDb) {
    auto dsp = reinterpret_cast<PJHavenDSP*>(handle);
    if (dsp) dsp->setEQBand(band, gainDb);
}

JNIEXPORT void JNICALL
Java_com_haven_music_PJHavenAudioProcessor_nativeProcess(
    JNIEnv* env, jobject thiz, jlong handle,
    jobject input, jobject output,
    jint numFrames, jint channels, jboolean is16Bit) {

    auto dsp = reinterpret_cast<PJHavenDSP*>(handle);
    if (!dsp) return;

    void* inPtr = env->GetDirectBufferAddress(input);
    auto* outPtr = static_cast<float*>(env->GetDirectBufferAddress(output));

    if (inPtr && outPtr) {
        dsp->process(outPtr, inPtr, numFrames, channels, is16Bit);
    }
}

} // extern "C"
