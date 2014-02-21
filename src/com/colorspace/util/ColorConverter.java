package com.colorspace.util;

import java.util.Arrays;

import android.graphics.Color;

public enum ColorConverter {
    REPLACE_H_IN_HSV {
        public float[] getPrecomputedData(int replaceColor) {
            return getHsv(replaceColor);
        }

        @Override
        public int colorReplace(int color, float[] replaceData) {
            float[] hsv = getHsv(color);
            hsv[0] = replaceData[0];
            int dstColor = Color.HSVToColor(hsv);
            return dstColor;
        }

        private float[] getHsv(int color) {
            float[] hsv = new float[3];
            int red = Color.red(color);
            int green = Color.green(color);
            int blue = Color.blue(color);
            Color.RGBToHSV(red, green, blue, hsv);
            return hsv;
        }
    },
    REPLACE_HS_IN_HSL {
        @Override
        public float[] getPrecomputedData(int replaceColor) {
            return HSL.getHslFromColor(replaceColor);
        }

        @Override
        public int colorReplace(int color, float[] replaceData) {
            float[] hsl = new float[4];
            hsl[0] = replaceData[0];
            hsl[1] = replaceData[1];
            hsl[2] = HSL.getLightness(color);
            hsl[3] = replaceData[3]; // for performance
            return HSL.getColorFromHsl(hsl);
        }
    },
    REPLACE_AB_IN_LAB {
        private final RGBGammaCorrection GAMMA_CORRECTION = RGBGammaCorrection.EXACT;
        private final RGBXYZMatrix RGB_XYZ_MATRIX = RGBXYZMatrix.SRGB;
        @Override
        public float[] getPrecomputedData(int color) {
            float[] rgb = getScaledRGBFromIntColor(color);
            // srgb -> linear rgb
            rgb[0] = GAMMA_CORRECTION.convertFromSRGBToLinearRGB(rgb[0]);
            rgb[1] = GAMMA_CORRECTION.convertFromSRGBToLinearRGB(rgb[1]);
            rgb[2] = GAMMA_CORRECTION.convertFromSRGBToLinearRGB(rgb[2]);

            // linear rgb -> xyz
            float[] xyz = new float[3];
            RGB_XYZ_MATRIX.applyMatrixFromLinearRGBToXYZ(rgb, xyz);

            float fx = xyz2labFunc(xyz[0] / RGB_XYZ_MATRIX.whitePoint.x);
            float fy = xyz2labFunc(xyz[1]);
            float fz = xyz2labFunc(xyz[2] / RGB_XYZ_MATRIX.whitePoint.z);
            return new float[] { fx - fy, fy - fz };
        }

        @Override
        public int colorReplace(int color, float[] replaceData) {
            float[] rgb = getScaledRGBFromIntColor(color);
            // srgb -> linear rgb
            rgb[0] = GAMMA_CORRECTION.convertFromSRGBToLinearRGB(rgb[0]);
            rgb[1] = GAMMA_CORRECTION.convertFromSRGBToLinearRGB(rgb[1]);
            rgb[2] = GAMMA_CORRECTION.convertFromSRGBToLinearRGB(rgb[2]);

            // linear rgb -> xyz
            float[] xyz = new float[3];
            RGB_XYZ_MATRIX.applyMatrixYOnlyFromLinearRGBToXYZ(rgb, xyz);

            // replace x, z
            float fy = xyz2labFunc(xyz[1]);
            xyz[0] = RGB_XYZ_MATRIX.whitePoint.x * xyz2labInverseFunc(fy + replaceData[0]);
            xyz[2] = RGB_XYZ_MATRIX.whitePoint.z * xyz2labInverseFunc(fy - replaceData[1]);

            // xyz -> linear rgb
            RGB_XYZ_MATRIX.applyMatrixFromXYZToLinearRGB(xyz, rgb);

            // linear rgb -> srgb
            rgb[0] = GAMMA_CORRECTION.convertFromLinearRGBToSRGB(rgb[0]);
            rgb[1] = GAMMA_CORRECTION.convertFromLinearRGBToSRGB(rgb[1]);
            rgb[2] = GAMMA_CORRECTION.convertFromLinearRGBToSRGB(rgb[2]);

            return getIntColorFromScaledRGB(rgb);
        }

        public int colorReplaceInLab2(int color, float[] replaceLab) {
            float[] rgb = getScaledRGBFromIntColor(color);
            // srgb -> linear rgb
            rgb[0] = GAMMA_CORRECTION.convertFromSRGBToLinearRGB(rgb[0]);
            rgb[1] = GAMMA_CORRECTION.convertFromSRGBToLinearRGB(rgb[1]);
            rgb[2] = GAMMA_CORRECTION.convertFromSRGBToLinearRGB(rgb[2]);

            // linear rgb -> xyz
            float[] xyz = new float[3];
            RGB_XYZ_MATRIX.applyMatrixFromLinearRGBToXYZ(rgb, xyz);

            float[] lab = new float[3];
            xyz2lab(xyz, lab);

            lab[1] = replaceLab[1];
            lab[2] = replaceLab[2];

            lab2xyz(lab, xyz);

            // xyz -> linear rgb
            RGB_XYZ_MATRIX.applyMatrixFromXYZToLinearRGB(xyz, rgb);

            // linear rgb -> srgb
            rgb[0] = GAMMA_CORRECTION.convertFromLinearRGBToSRGB(rgb[0]);
            rgb[1] = GAMMA_CORRECTION.convertFromLinearRGBToSRGB(rgb[1]);
            rgb[2] = GAMMA_CORRECTION.convertFromLinearRGBToSRGB(rgb[2]);

            return getIntColorFromScaledRGB(rgb);
        }

        private int getIntColorFromScaledRGB(float[] rgb) {
            int r = scaleRGB(rgb[0]);
            int g = scaleRGB(rgb[1]);
            int b = scaleRGB(rgb[2]);
            return Color.rgb(r, g, b);
        }

        private static final float XYZ_LAB_TRANSITION = 6f / 29f;
        private static final float XYZ_LAB_CONSTANT1 = 29f * 29f / 6f / 6f / 3f;
        private static final float XYZ_LAB_CONSTANT2 = 4f / 29f;

        private float xyz2labFunc(float v) {
            if (v > XYZ_LAB_TRANSITION * XYZ_LAB_TRANSITION * XYZ_LAB_TRANSITION) {
                return (float) Math.cbrt(v);
            } else {
                return XYZ_LAB_CONSTANT1 * v + XYZ_LAB_CONSTANT2;
            }
        }

        private float xyz2labInverseFunc(float v) {
            if (v > XYZ_LAB_TRANSITION) {
                return v * v * v;
            } else {
                return (v - XYZ_LAB_CONSTANT2) / XYZ_LAB_CONSTANT1;
            }
        }

        private void xyz2lab(float[] xyz, float[] lab) {
            float fy = xyz2labFunc(xyz[1]);
            lab[0] = 116f * fy - 16f;
            lab[1] = 500f * (xyz2labFunc(xyz[0] / RGB_XYZ_MATRIX.whitePoint.x) - fy);
            lab[2] = 200f * (fy - xyz2labFunc(xyz[2] / RGB_XYZ_MATRIX.whitePoint.z));
        }

        private void lab2xyz(float[] lab, float[] xyz) {
            float ll = (lab[0] + 16f) / 116f;
            xyz[0] = RGB_XYZ_MATRIX.whitePoint.x * xyz2labInverseFunc(ll + lab[1] / 500f);
            xyz[1] = xyz2labInverseFunc(ll);
            xyz[2] = RGB_XYZ_MATRIX.whitePoint.z * xyz2labInverseFunc(ll - lab[2] / 200f);
        }

        public float[] getLabFromIntColor(int color) {
            float[] rgb = getScaledRGBFromIntColor(color);
            // srgb -> linear rgb
            rgb[0] = GAMMA_CORRECTION.convertFromSRGBToLinearRGB(rgb[0]);
            rgb[1] = GAMMA_CORRECTION.convertFromSRGBToLinearRGB(rgb[1]);
            rgb[2] = GAMMA_CORRECTION.convertFromSRGBToLinearRGB(rgb[2]);

            // linear rgb -> xyz
            float[] xyz = new float[3];
            RGB_XYZ_MATRIX.applyMatrixFromLinearRGBToXYZ(rgb, xyz);

            float[] lab = new float[3];
            xyz2lab(xyz, lab);
            return lab;
        }

        public void test(float[] rgb) {
            android.util.Log.i(TAG, "sRGB=" + Arrays.toString(rgb));
            // srgb -> linear rgb
            rgb[0] = GAMMA_CORRECTION.convertFromSRGBToLinearRGB(rgb[0]);
            rgb[1] = GAMMA_CORRECTION.convertFromSRGBToLinearRGB(rgb[1]);
            rgb[2] = GAMMA_CORRECTION.convertFromSRGBToLinearRGB(rgb[2]);
            android.util.Log.i(TAG,
                    "linear RGB=" + Arrays.toString(rgb) + " " + GAMMA_CORRECTION.convertFromLinearRGBToSRGB(1f));

            // linear rgb -> xyz
            float[] xyz = new float[3];
            RGB_XYZ_MATRIX.applyMatrixFromLinearRGBToXYZ(rgb, xyz);
            android.util.Log.i(TAG, "XYZ=" + Arrays.toString(xyz));

            float[] lab = new float[3];
            xyz2lab(xyz, lab);
            android.util.Log.i(TAG, "Lab=" + Arrays.toString(lab));

            lab2xyz(lab, xyz);
            android.util.Log.i(TAG, "XYZ=" + Arrays.toString(xyz));

            // xyz -> linear rgb
            RGB_XYZ_MATRIX.applyMatrixFromXYZToLinearRGB(xyz, rgb);
            android.util.Log.i(TAG, "linear RGB=" + Arrays.toString(rgb));

            // linear rgb -> srgb
            rgb[0] = GAMMA_CORRECTION.convertFromLinearRGBToSRGB(rgb[0]);
            rgb[1] = GAMMA_CORRECTION.convertFromLinearRGBToSRGB(rgb[1]);
            rgb[2] = GAMMA_CORRECTION.convertFromLinearRGBToSRGB(rgb[2]);
            android.util.Log.i(TAG, "sRGB=" + Arrays.toString(rgb));
        }
    };

    public abstract float[] getPrecomputedData(int replaceColor);

    public abstract int colorReplace(int color, float[] replaceData);

    private static final float RGB_MAX = 255f;

    public static int scaleRGB(float f) {
        if (f > 1f) {
            return 255;
        } else if (f < 0f) {
            return 0;
        } else {
            return (int) (f * RGB_MAX);
        }
    }

    public static float[] getScaledRGBFromIntColor(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        return new float[] { (float) r / RGB_MAX, (float) g / RGB_MAX, (float) b / RGB_MAX };
    }

    private static final String TAG = "ColorConverter";
}
