package com.colorspace.util;


// http://en.wikipedia.org/wiki/Standard_illuminant#Illuminant_series_D
// CIE 1931 2 degrees
public enum WhitePoint {
    // This is xyY space. Y is 1.
    D50(0.34567f, 0.35850f),
    D65(0.31271f, 0.32902f);

    // This is XYZ space. Y is 1.
    final float x;
    final float z;

    private WhitePoint(float x, float y) {
        this.x = x / y;
        this.z = (1f - x - y) / y;
    }
}
