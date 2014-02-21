package com.colorspace.util;

import java.util.Arrays;

import android.graphics.Color;

public class HSL {
    public static float getLightness(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int max, min;
        if (r > g) {
            if (g > b) {
                max = r;
                min = b;
            } else {
                min = g;
                if (r > b) {
                    max = r;
                } else {
                    max = b;
                }
            }
        } else {
                if (r > b) {
                    max = g;
                    min = b;
                } else {
                    min = r;
                    if (g > b) {
                        max = g;
                    } else {
                        max = b;
                    }
                }
        }
        return (max + min) / 255f / 2f;
    }

    // http://en.wikipedia.org/wiki/HSL_and_HSV#Hue_and_chroma
    // h : 0 ~ 6
    // s : 0 ~ 1
    // l : 0 ~ 1
    public static float[] getHslFromColor(int color) {
        float[] rgb = ColorConverter.getScaledRGBFromIntColor(color);
        float r = rgb[0];
        float g = rgb[1];
        float b = rgb[2];
        Arrays.sort(rgb);
        float max = rgb[2];
        float min = rgb[0];
        float h, s, l;
        float c = max - min;
        l = (max + min) / 2f;
        float x;
        if (c == 0f) {
            s = 0f;
            h = 0f;
            x = 0f;
        } else {
            s = c / (1f - Math.abs(2f * l - 1f));
            if (r == max) { // -1 ~ 1
                h = (g - b) / c;
                if (h < 0f) {
                    h += 6f;
                }
                x = Math.abs(g - b) / c;
            } else if (g == max) { // 1 ~ 3
                h = 2f + (b - r) / c;
                x = Math.abs(b - r) / c;
            } else { // 3 ~ 5
                h = 4f + (r - g) / c;
                x = Math.abs(r - g) / c;
            }
        }
        return new float[] { h, s, l, x };
    }

    // http://en.wikipedia.org/wiki/HSL_and_HSV#From_HSL
    public static int getColorFromHsl(float[] hsl) {
        float r, g, b;
        float h = hsl[0]; // 0 ~ 6
        float s = hsl[1];
        float l = hsl[2];
        float c = (1f - Math.abs(2f * l - 1f)) * s; // max - min
        float min = l - c / 2f;
        float mid = c * hsl[3] + min;// c * (1f - Math.abs(h % 2f - 1f)) + min;
        float max = c + min;
        if (h < 1f) {
            r = max;
            g = mid;
            b = min;
        } else if (h < 2f) {
            r = mid;
            g = max;
            b = min;
        } else if (h < 3f) {
            r = min;
            g = max;
            b = mid;
        } else if (h < 4f) {
            r = min;
            g = mid;
            b = max;
        } else if (h < 5f) {
            r = mid;
            g = min;
            b = max;
        } else {
            r = max;
            g = min;
            b = mid;
        }

        return Color.rgb((int) (r * 255f), (int) (g * 255f), (int) (b * 255f));
        // return Color.rgb((int) r, (int) g, (int) b);
    }
}
