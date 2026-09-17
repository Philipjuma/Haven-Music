#ifndef PJ_HAVEN_DSP_H
#define PJ_HAVEN_DSP_H

#include <vector>
#include <cmath>
#include <algorithm>

class BiquadFilter {
public:
    BiquadFilter() : a0(1.0f), a1(0.0f), a2(0.0f), b1(0.0f), b2(0.0f), z1(0.0f), z2(0.0f) {}

    void setLowShelf(float sampleRate, float freq, float q, float dbGain);
    void setPeaking(float sampleRate, float freq, float q, float dbGain);
    void setLowPass(float sampleRate, float freq, float q);

    inline float process(float x) {
        float y = a0 * x + z1;
        z1 = a1 * x - b1 * y + z2;
        z2 = a2 * x - b2 * y;
        return y;
    }

    void reset() { z1 = z2 = 0.0f; }

private:
    float a0, a1, a2, b1, b2;
    float z1, z2;
};

class PJHavenDSP {
public:
    PJHavenDSP();
    ~PJHavenDSP();

    void setSampleRate(float sr);
    void setEnabled(bool enabled);
    void setProfile(int profile); // 0: Speaker, 1: Headphones

    void setPunchIntensity(float intensity);
    void setImmerseIntensity(float intensity);
    void setAuraIntensity(float intensity);

    void process(float* output, const void* input, int numFrames, int channels, bool is16Bit);

private:
    void updateFilters();

    float sampleRate;
    bool isEnabled;
    int profile;

    float extraPunch;
    float extraWidth;
    float extraClarity;

    std::vector<BiquadFilter> filtersL;
    std::vector<BiquadFilter> filtersR;
    BiquadFilter crossoverL;
    BiquadFilter crossoverR;

    float width;
    float envelope;
    float attackCoeff;
    float releaseCoeff;

    static constexpr float limiterThreshold = 0.891f; // -1.0 dBFS
};

#endif // PJ_HAVEN_DSP_H
