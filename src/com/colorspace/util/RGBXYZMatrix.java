package com.colorspace.util;

import java.util.Arrays;

import android.graphics.Matrix;

// http://en.wikipedia.org/wiki/RGB_color_space#Specifications
public enum RGBXYZMatrix {
    SRGB(0.64f, 0.33f, 0.30f, 0.60f, 0.15f, 0.06f, WhitePoint.D65),
    ADOBE_RGB(0.64f, 0.33f, 0.21f, 0.71f, 0.15f, 0.06f, WhitePoint.D65);

    private final float[] matrixLinearRGB2XYZ;
    private final float[] matrixXYZ2LinearRGB;
    final WhitePoint whitePoint;
    private final Matrix matrixLinearRGB2XYZ2 = new Matrix();
    private final Matrix matrixXYZ2LinearRGB2 = new Matrix();

    private RGBXYZMatrix(float xr, float yr, float xg, float yg, float xb, float yb, WhitePoint wp) {
        this.whitePoint = wp;

        // x + y + z = 1
        float zr = 1f - xr - yr;
        float zg = 1f - xg - yg;
        float zb = 1f - xb - yb;

        // RGBPrimaryMatrix x S = XYZWhite.
        // So S = InverseRGBPrimaryMatrix x XYZWhite.
        float[] m1 = new float[] { xr, xg, xb, yr, yg, yb, zr, zg, zb };
        float[] m1i = inverseMatrix(m1);
        // y is 1
        float sr = m1i[0] * wp.x + m1i[1] + m1i[2] * wp.z;
        float sg = m1i[3] * wp.x + m1i[4] + m1i[5] * wp.z;
        float sb = m1i[6] * wp.x + m1i[7] + m1i[8] * wp.z;

        float[] m2 = new float[9];
        m2[0] = sr * m1[0];
        m2[1] = sg * m1[1];
        m2[2] = sb * m1[2];
        m2[3] = sr * m1[3];
        m2[4] = sg * m1[4];
        m2[5] = sb * m1[5];
        m2[6] = sr * m1[6];
        m2[7] = sg * m1[7];
        m2[8] = sb * m1[8];
        float[] m2i = inverseMatrix(m2);

        matrixLinearRGB2XYZ = m2;
        matrixXYZ2LinearRGB = m2i;
        matrixLinearRGB2XYZ2.setValues(m2);
        matrixXYZ2LinearRGB2.setValues(m2i);
    }

    @Override
    public String toString() {
        return "RGB2XYZ : " + Arrays.toString(matrixLinearRGB2XYZ) + " XYZ2RGB : "
                + Arrays.toString(matrixXYZ2LinearRGB);
    }

    // invert 3x3 matrix
    public static float[] inverseMatrix(float[] m) {
        float[] inv = new float[9];
        Matrix m0 = new Matrix();
        Matrix m1 = new Matrix();
        m0.setValues(m);
        m0.invert(m1);
        m1.getValues(inv);
        return inv;
    }

    public void applyMatrixFromLinearRGBToXYZ(float[] rgb, float[] xyz) {
        applyMatrix(rgb, xyz, matrixLinearRGB2XYZ);
    }

    public void applyMatrixYOnlyFromLinearRGBToXYZ(float[] rgb, float[] xyz) {
        float[] m = matrixLinearRGB2XYZ;
        xyz[1] = rgb[0] * m[3] + rgb[1] * m[4] + rgb[2] * m[5];
    }

    public void applyMatrixFromXYZToLinearRGB(float[] xyz, float[] rgb) {
        applyMatrix(xyz, rgb, matrixXYZ2LinearRGB);
    }

    private static void applyMatrix(float[] src, float[] dst, float[] m) {
        dst[0] = src[0] * m[0] + src[1] * m[1] + src[2] * m[2];
        dst[1] = src[0] * m[3] + src[1] * m[4] + src[2] * m[5];
        dst[2] = src[0] * m[6] + src[1] * m[7] + src[2] * m[8];
    }
}
