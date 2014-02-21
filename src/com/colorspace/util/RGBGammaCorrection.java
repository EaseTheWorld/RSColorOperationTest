package com.colorspace.util;

import android.os.Build;
import android.util.FloatMath;


// http://en.wikipedia.org/wiki/SRGB#Specification_of_the_transformation
public enum RGBGammaCorrection {
    EXACT {
        private static final float GAMMA = 2.4f;
        private static final float TRANSITION = 0.04045f;
        private static final float OFFSET = 0.055f;
        private static final float SLOPE = 12.92f;

        @Override
        protected float convertFromSRGBToLinearRGB(float v) {
            if (v > TRANSITION) {
                return powf((v + OFFSET) / (1f + OFFSET), GAMMA);
            } else {
                return v / SLOPE;
            }
        }

        @Override
        protected float convertFromLinearRGBToSRGB(float v) {
            if (v > TRANSITION / SLOPE) {
                return (1f + OFFSET) * powf(v, 1f / GAMMA) - OFFSET;
            } else {
                return v * SLOPE;
            }
        }
    },
    SIMPLE {
        private static final float GAMMA = 2.2f;

        @Override
        protected float convertFromSRGBToLinearRGB(float v) {
            return powf(v, GAMMA);
        }

        @Override
        protected float convertFromLinearRGBToSRGB(float v) {
            return powf(v, 1f / GAMMA);
        }
    };

    abstract float convertFromSRGBToLinearRGB(float v);

    abstract float convertFromLinearRGBToSRGB(float v);

    private static float powf(float x, float y) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return (float) Math.pow(x, y);
        } else {
            return FloatMath.pow(x, y);
        }
    }
}
