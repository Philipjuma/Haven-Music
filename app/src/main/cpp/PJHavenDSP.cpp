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
    punchDb(0.0f), auraDb(0.0f), spaceIntensity(0.0f),
    width(1.0f), envelope(0.0f) {

    for (int i = 0; i < 9; ++i) eqGains[i] = 0.0f;

    // Initialize delay buffers for Space effect
    for (int i = 0; i < numDelays; ++i) {
        delayLengths[i] = (int)(sampleRate * (0.03f + i * 0.015f));
        delayBuffers[i] = new float[delayLengths[i]];
        std::fill(delayBuffers[i], delayBuffers[i] + delayLengths[i], 0.0f);
        delayWritePtrs[i] = 0;
    }

    updateFilters();
}

PJHavenDSP::~PJHavenDSP() {
    for (int i = 0; i < numDelays; ++i) {
        delete[] delayBuffers[i];
    }
}

void PJHavenDSP::setSampleRate(float sr) {
    if (sampleRate != sr) {
        sampleRate = sr;
        // Re-init delay buffers for new sample rate
        for (int i = 0; i < numDelays; ++i) {
            delete[] delayBuffers[i];
            delayLengths[i] = (int)(sampleRate * (0.03f + i * 0.015f));
            delayBuffers[i] = new float[delayLengths[i]];
            std::fill(delayBuffers[i], delayBuffers[i] + delayLengths[i], 0.0f);
            delayWritePtrs[i] = 0;
        }
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
    punchDb = (intensity / 1000.0f) * 8.0f; // Max +8dB Punch
    updateFilters();
}

void PJHavenDSP::setImmerseIntensity(float intensity) {
    // Width already handles extraWidth in process, but let's map it clearly
    // intensity is 0..1000
    width = (profile == 1) ? 1.18f : 1.0f;
    width += (intensity / 1000.0f) * 0.5f; // Add up to 0.5 additional width
}

void PJHavenDSP::setAuraIntensity(float intensity) {
    auraDb = (intensity / 100.0f) * 5.0f; // Max +5dB Aura/Air
    updateFilters();
}

void PJHavenDSP::setSpaceIntensity(float intensity) {
    spaceIntensity = (intensity / 5.0f) * 0.35f; // Map preset 0..5 to wet level
}

void PJHavenDSP::setEQBand(int band, float gainDb) {
    if (band >= 0 && band < 9) {
        eqGains[band] = gainDb / 100.0f; // Input usually in millibel
        updateFilters();
    }
}

void PJHavenDSP::updateFilters() {
    profileFiltersL.clear();
    profileFiltersR.clear();

    float attackMs = 10.0f;
    float releaseMs = 120.0f;
    attackCoeff = 1.0f - exp(-1.0f / (attackMs * 0.001f * sampleRate));
    releaseCoeff = 1.0f - exp(-1.0f / (releaseMs * 0.001f * sampleRate));

    // 1. User 9-Band EQ Frequencies
    float freqs[9] = {60.0f, 150.0f, 250.0f, 500.0f, 1000.0f, 2000.0f, 4000.0f, 8000.0f, 16000.0f};
    for (int i = 0; i < 9; ++i) {
        userEqL[i].setPeaking(sampleRate, freqs[i], 1.2f, eqGains[i]);
        userEqR[i].setPeaking(sampleRate, freqs[i], 1.2f, eqGains[i]);
    }

    // 2. Punch & Aura
    punchFilterL.setLowShelf(sampleRate, 100.0f, 0.7f, punchDb);
    punchFilterR.setLowShelf(sampleRate, 100.0f, 0.7f, punchDb);
    auraFilterL.setPeaking(sampleRate, 12000.0f, 0.7f, auraDb);
    auraFilterR.setPeaking(sampleRate, 12000.0f, 0.7f, auraDb);

    // 3. Profile Specific Tuning
    if (profile == 1) { // Headphones: Warmth & Air
        BiquadFilter f;
        f.setLowShelf(sampleRate, 60.0f, 0.7f, 3.0f);
        profileFiltersL.push_back(f); profileFiltersR.push_back(f);
        f.setPeaking(sampleRate, 8000.0f, 1.0f, 2.0f);
        profileFiltersL.push_back(f); profileFiltersR.push_back(f);
    } else { // Speaker: Body & Presence
        BiquadFilter f;
        f.setPeaking(sampleRate, 250.0f, 1.0f, 5.0f);
        profileFiltersL.push_back(f); profileFiltersR.push_back(f);
        f.setPeaking(sampleRate, 4000.0f, 1.0f, 2.5f);
        profileFiltersL.push_back(f); profileFiltersR.push_back(f);
    }

    crossoverL.setLowPass(sampleRate, 120.0f, 0.707f);
    crossoverR.setLowPass(sampleRate, 120.0f, 0.707f);
}

void PJHavenDSP::process(float* output, const void* input, int numFrames, int channels, bool is16Bit) {
    if (!isEnabled) return;

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

        // --- DSP CHAIN ---

        // 1. Initial Headroom
        l *= 0.70f; r *= 0.70f;

        // 2. Punch & Aura (Physical Bass/Clarity)
        l = punchFilterL.process(l); r = punchFilterR.process(r);
        l = auraFilterL.process(l); r = auraFilterR.process(r);

        // 3. 9-Band User EQ
        for (int b = 0; b < 9; ++b) {
            l = userEqL[b].process(l);
            r = userEqR[b].process(r);
        }

        // 4. Profile Overlays
        for (auto& f : profileFiltersL) l = f.process(l);
        for (auto& f : profileFiltersR) r = f.process(r);

        // 5. Immerse (Stereo 3D Expansion)
        float lowL = crossoverL.process(l);
        float lowR = crossoverR.process(r);
        float highL = l - lowL;
        float highR = r - lowR;
        float mid = (highL + highR) * 0.5f;
        float side = (highL - highR) * 0.5f * width;
        l = lowL + (mid + side);
        r = lowR + (mid - side);

        // 6. Space (Native Reverb Engine)
        if (spaceIntensity > 0.0f) {
            float reverbSignal = 0.0f;
            for (int d = 0; d < numDelays; ++d) {
                reverbSignal += delayBuffers[d][delayWritePtrs[d]] * 0.4f;
                delayBuffers[d][delayWritePtrs[d]] = (l + r) * 0.5f + (reverbSignal * 0.3f);
                delayWritePtrs[d] = (delayWritePtrs[d] + 1) % delayLengths[d];
            }
            l += reverbSignal * spaceIntensity;
            r += reverbSignal * spaceIntensity;
        }

        // 7. Dynamics & Soft Limiting
        float absSample = std::max(std::abs(l), std::abs(r));
        if (absSample > envelope) envelope += attackCoeff * (absSample - envelope);
        else envelope += releaseCoeff * (absSample - envelope);

        auto softLimit = [](float x) {
            float threshold = 0.75f;
            if (std::abs(x) < threshold) return x;
            float sign = (x > 0) ? 1.0f : -1.0f;
            return sign * (threshold + (0.95f - threshold) * std::tanh((std::abs(x) - threshold) / (0.95f - threshold)));
        };

        output[i * 2] = softLimit(l);
        output[i * 2 + 1] = softLimit(r);
    }
}
