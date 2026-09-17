#include "PJHavenDSP.h"
#include <android/log.h>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

void BiquadFilter::setLowShelf(float sampleRate, float freq, float q, float dbGain) {
    float w0 = 2.0f * M_PI * freq / sampleRate;
    float alpha = sin(w0) / (2.0f * q);
    float a = pow(10.0f, dbGain / 40.0f);
    float cosW0 = cos(w0);
    float twoSqrtAAlpha = 2.0f * sqrt(a) * alpha;

    float norm = 1.0f / ((a + 1.0f) + (a - 1.0f) * cosW0 + twoSqrtAAlpha);
    a0 = a * ((a + 1.0f) - (a - 1.0f) * cosW0 + twoSqrtAAlpha) * norm;
    a1 = 2.0f * a * ((a - 1.0f) - (a + 1.0f) * cosW0) * norm;
    a2 = a * ((a + 1.0f) - (a - 1.0f) * cosW0 - twoSqrtAAlpha) * norm;
    b1 = -2.0f * ((a - 1.0f) + (a + 1.0f) * cosW0) * norm;
    b2 = ((a + 1.0f) + (a - 1.0f) * cosW0 - twoSqrtAAlpha) * norm;
}

void BiquadFilter::setPeaking(float sampleRate, float freq, float q, float dbGain) {
    float w0 = 2.0f * M_PI * freq / sampleRate;
    float alpha = sin(w0) / (2.0f * q);
    float a = pow(10.0f, dbGain / 40.0f);
    float cosW0 = cos(w0);

    float norm = 1.0f / (1.0f + alpha / a);
    a0 = (1.0f + alpha * a) * norm;
    a1 = -2.0f * cosW0 * norm;
    a2 = (1.0f - alpha * a) * norm;
    b1 = a1;
    b2 = (1.0f - alpha / a) * norm;
}

void BiquadFilter::setLowPass(float sampleRate, float freq, float q) {
    float w0 = 2.0f * M_PI * freq / sampleRate;
    float alpha = sin(w0) / (2.0f * q);
    float cosW0 = cos(w0);

    float norm = 1.0f / (1.0f + alpha);
    a0 = (1.0f - cosW0) * 0.5f * norm;
    a1 = (1.0f - cosW0) * norm;
    a2 = a0;
    b1 = -2.0f * cosW0 * norm;
    b2 = (1.0f - alpha) * norm;
}

PJHavenDSP::PJHavenDSP() :
    sampleRate(44100.0f), isEnabled(false), profile(1),
    extraPunch(0.0f), extraWidth(0.0f), extraClarity(0.0f),
    width(1.0f), envelope(0.0f) {
    updateFilters();
}

PJHavenDSP::~PJHavenDSP() {}

void PJHavenDSP::setSampleRate(float sr) {
    if (sampleRate != sr) {
        sampleRate = sr;
        updateFilters();
    }
}

void PJHavenDSP::setEnabled(bool enabled) {
    isEnabled = enabled;
    if (!enabled) envelope = 0.0f;
}

void PJHavenDSP::setProfile(int p) {
    if (profile != p) {
        profile = p;
        updateFilters();
    }
}

void PJHavenDSP::setPunchIntensity(float intensity) {
    extraPunch = (intensity / 1000.0f) * 4.0f;
}

void PJHavenDSP::setImmerseIntensity(float intensity) {
    extraWidth = (intensity / 1000.0f) * 0.25f;
}

void PJHavenDSP::setAuraIntensity(float intensity) {
    extraClarity = (intensity / 100.0f) * 3.0f;
}

void PJHavenDSP::updateFilters() {
    filtersL.clear();
    filtersR.clear();

    float attackMs = 10.0f;
    float releaseMs = 120.0f;
    attackCoeff = 1.0f - exp(-1.0f / (attackMs * 0.001f * sampleRate));
    releaseCoeff = 1.0f - exp(-1.0f / (releaseMs * 0.001f * sampleRate));

    if (profile == 1) { // Headphones
        BiquadFilter f;
        f.setLowShelf(sampleRate, 60.0f, 0.7f, 5.0f);
        filtersL.push_back(f); filtersR.push_back(f);

        f.setPeaking(sampleRate, 90.0f, 1.0f, 5.0f);
        filtersL.push_back(f); filtersR.push_back(f);

        f.setPeaking(sampleRate, 120.0f, 1.0f, 3.5f);
        filtersL.push_back(f); filtersR.push_back(f);

        f.setPeaking(sampleRate, 2000.0f, 1.0f, 0.8f);
        filtersL.push_back(f); filtersR.push_back(f);

        f.setPeaking(sampleRate, 4000.0f, 1.0f, 1.5f);
        filtersL.push_back(f); filtersR.push_back(f);

        f.setPeaking(sampleRate, 8000.0f, 1.0f, 2.5f);
        filtersL.push_back(f); filtersR.push_back(f);

        f.setPeaking(sampleRate, 16000.0f, 1.0f, 2.0f);
        filtersL.push_back(f); filtersR.push_back(f);

        width = 1.18f;
    } else { // Speaker
        BiquadFilter f;
        f.setPeaking(sampleRate, 150.0f, 1.0f, 4.5f);
        filtersL.push_back(f); filtersR.push_back(f);

        f.setPeaking(sampleRate, 200.0f, 1.0f, 3.5f);
        filtersL.push_back(f); filtersR.push_back(f);

        f.setPeaking(sampleRate, 300.0f, 1.0f, 1.5f);
        filtersL.push_back(f); filtersR.push_back(f);

        f.setPeaking(sampleRate, 4000.0f, 1.0f, 2.0f);
        filtersL.push_back(f); filtersR.push_back(f);

        f.setPeaking(sampleRate, 8000.0f, 1.0f, 2.0f);
        filtersL.push_back(f); filtersR.push_back(f);

        width = 1.0f;
    }

    crossoverL.setLowPass(sampleRate, 120.0f, 0.707f);
    crossoverR.setLowPass(sampleRate, 120.0f, 0.707f);
}

void PJHavenDSP::process(float* output, const void* input, int numFrames, int channels, bool is16Bit) {
    if (!isEnabled) {
        // Passthrough with conversion if needed (though usually handled in Kotlin for efficiency)
        return;
    }

    float currentWidth = width + extraWidth;
    float punchGain = pow(10.0f, extraPunch / 20.0f);
    float clarityGain = pow(10.0f, extraClarity / 20.0f);
    float heat = punchGain * 0.7f + clarityGain * 0.3f;

    const auto* in16 = static_cast<const int16_t*>(input);
    const float* inF = static_cast<const float*>(input);

    for (int i = 0; i < numFrames; ++i) {
        float l, r;
        if (channels == 2) {
            if (is16Bit) {
                l = (float)in16[i * 2] / 32768.0f;
                r = (float)in16[i * 2 + 1] / 32768.0f;
            } else {
                l = inF[i * 2];
                r = inF[i * 2 + 1];
            }
        } else {
            if (is16Bit) l = r = in16[i] / 32768.0f;
            else l = r = inF[i];
        }

        // 1. Headroom
        l *= 0.45f;
        r *= 0.45f;

        // 2. EQ
        for (auto& f : filtersL) l = f.process(l);
        for (auto& f : filtersR) r = f.process(r);

        // 3. Heat
        l *= heat;
        r *= heat;

        // 4. Stereo Widening (120Hz crossover)
        float lowL = crossoverL.process(l);
        float lowR = crossoverR.process(r);
        float highL = l - lowL;
        float highR = r - lowR;

        float mid = (highL + highR) * 0.5f;
        float side = (highL - highR) * 0.5f * currentWidth;
        float lowMid = (lowL + lowR) * 0.5f;

        l = lowMid + (mid + side);
        r = lowMid + (mid - side);

        // 5. Dynamics
        float absSample = std::max(std::abs(l), std::abs(r));
        if (absSample > envelope) envelope += attackCoeff * (absSample - envelope);
        else envelope += releaseCoeff * (absSample - envelope);

        float threshold = 0.251f;
        float ratio = 2.0f;
        if (envelope > threshold) {
            float reduction = 1.0f / (1.0f + (envelope - threshold) * (ratio - 1.0f));
            l *= reduction;
            r *= reduction;
        }

        // 6. Limiter
        output[i * 2] = std::max(-limiterThreshold, std::min(limiterThreshold, l));
        output[i * 2 + 1] = std::max(-limiterThreshold, std::min(limiterThreshold, r));
    }
}
