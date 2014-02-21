package com.easetheworld.renderscript.blendmode;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.SystemClock;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.RenderScript.RSErrorHandler;
import android.support.v8.renderscript.RenderScript.RSMessageHandler;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.support.v8.renderscript.ScriptIntrinsicLUT;

public class FilterScript {

    private static final String TAG = "FilterScript";

    // order must be same as processFuncTable in filter.rs
    public enum FilterType {
        NORMAL,
        ERASER,

        LIGHTEN_ONLY,
        SCREEN,
        DODGE,
        ADDITION,

        DARKEN_ONLY,
        MULTIPLY,
        BURN,

        OVERLAY,
        SOFT_LIGHT,
        HARD_LIGHT,

        DIFFERENCE,
        SUBTRACT,
        GRAIN_EXTRACT,
        GRAIN_MERGE,
        DIVIDE,

        HUE,
        SATURATION,
        COLOR,
        VALUE,

        TONE_CURVE,
        SEPIA(1f, 0.5f, 0f),
        BRIGHTNESS_CONTRAST(0.5f, 0.75f, -1f),

        MOSAIC,
        NOISE,

        BLUR2
        ;

        public final float defaultR;
        public final float defaultG;
        public final float defaultB;

        private FilterType() {
            this(-1f, 0f, 0f);
        }

        private FilterType(float r, float g, float b) {
            defaultR = r;
            defaultG = g;
            defaultB = b;
        }

        protected void forEachRoot(FilterScript script, Allocation ain, Allocation aout, int left, int top, int right, int bottom) {
            if (left < 0 && right < 0) {
                script.totalFilterScript.invoke_filter(script.totalFilterScript, ordinal(), ain, aout);
            } else {
                script.totalFilterScript.invoke_filter(script.totalFilterScript, ordinal(), ain, aout, left, top, right, bottom);
            }
        }

        final void applyRect(FilterScript script, Bitmap out, int left, int top, int right, int bottom) {
            long t1 = SystemClock.elapsedRealtime();
            // forEachRoot returns immediately because it is asynchronous.
            // Instead copyTo is blocked until computation is done.
            forEachRoot(script, script.inAllocation, script.outAllocation, left, top, right, bottom);
            script.outAllocation.copyTo(out);
            long t2 = SystemClock.elapsedRealtime();
            android.util.Log.i(TAG, "mode=" + this + " time=" + (t2 - t1));
        }
    }

    ScriptIntrinsicBlur blurScript;

    final ScriptC_filter totalFilterScript;

    final Allocation inAllocation;
    final Allocation blendingAllocation;
    final Allocation drawingAllocation;
    final Allocation outAllocation;
    Allocation inBlurAllocation;

    private final RenderScript rs;

    public FilterScript(Context context, Bitmap in) {
        rs = RenderScript.create(context);
        rs.setMessageHandler(new RSMessageHandler() {

            @Override
            public void run() {
                super.run();
                android.util.Log.i(TAG, "Message run " + mID + " " + mData + " " + mLength);
            }
        });
        rs.setErrorHandler(new RSErrorHandler() {

            @Override
            public void run() {
                super.run();
                android.util.Log.i(TAG, "Error run " + mErrorMessage + " " + mErrorNum);
            }
        });
        inAllocation = Allocation.createFromBitmap(rs, in, Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT);
        drawingAllocation = Allocation.createTyped(rs, inAllocation.getType());
        blendingAllocation = Allocation.createTyped(rs, inAllocation.getType());
        outAllocation = Allocation.createTyped(rs, inAllocation.getType());
        outAllocation.copyFrom(inAllocation);

        totalFilterScript = new ScriptC_filter(rs);
        totalFilterScript.set_blendingAllocation(blendingAllocation);
        totalFilterScript.set_drawingAllocation(drawingAllocation);

        blurScript = ScriptIntrinsicBlur.create(rs, Element.RGBA_8888(rs));
        blurScript.setInput(inAllocation);
        inBlurAllocation = Allocation.createTyped(rs, inAllocation.getType());
        blurScript.setRadius(15f);
        blurScript.forEach(inBlurAllocation);
        totalFilterScript.set_blurAllocation(inBlurAllocation);
        // convolveScript = ScriptIntrinsicConvolve5x5.create(rs,
        // Element.RGBA_8888(rs));
        // convolveScript.setInput(inAllocation);
        // float[] coeff = new float[25];
        // for (int i = 0; i < coeff.length; i++) {
        // coeff[i] = 1f / 25f;
        // }
        // convolveScript.setCoefficients(coeff);

    }

    public RenderScript getRS() {
        return rs;
    }

    public void setInputBitmap(Bitmap bitmap) {
        inAllocation.copyFrom(bitmap);
    }

    public void setBlendingBitmap(Bitmap bitmap) {
        blendingAllocation.copyFrom(bitmap);
    }

    public void setDrawingBitmap(Bitmap bitmap) {
        drawingAllocation.copyFrom(bitmap);
    }

    public void apply(FilterType type, Bitmap out) {
        type.applyRect(this, out, -1, -1, -1, -1);
    }

    public void applyRect(FilterType type, Bitmap out,
            int left, int top, int right, int bottom) {
        type.applyRect(this, out, left, top, right, bottom);
    }

    public void setBlurRadius(float radius, Bitmap out) {
        blurScript.setRadius(radius);
        FilterType.BLUR2.applyRect(this, out, -1, -1, -1, -1);
    }

    public void testLUT(Bitmap out) {
        Allocation aout = Allocation.createTyped(rs, inAllocation.getType());

        ScriptIntrinsicLUT mLUT = ScriptIntrinsicLUT.create(rs, Element.RGBA_8888(rs));

        // a * x ^ b = 1 - a * (1 - x) ^ b , x = 1/2
        // then a = 2 ^ (b-1)
        // sepia (r = 3, g = 2, b = 1);
        for (int ct = 0; ct < 256; ct++) {
            float f = ((float) ct) / 255.f;

            float r = f;
            if (r < 0.5f) {
                r = 4.0f * r * r * r;
            } else {
                r = 1.0f - r;
                r = 1.0f - (4.0f * r * r * r);
            }
            r = f * f;
            mLUT.setRed(ct, (int) (r * 255.f + 0.5f));

            float g = f;
            if (g < 0.5f) {
                g = 2.0f * g * g;
            } else {
                g = 1.0f - g;
                g = 1.0f - (2.0f * g * g);
            }
            mLUT.setGreen(ct, ct);// (int) (g * 255.f + 0.5f));

            float b = f * 0.5f + 0.25f;
            mLUT.setBlue(ct, ct);// (int) (b * 255.f + 0.5f));
        }

        mLUT.forEach(inAllocation, aout);
        aout.copyTo(out);
    }

    public Bitmap testBitmapPremultiply(int w, int h) {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

            for (int i = 0; i < w; i++) {
            int color = Color.argb(i * 255 / w, 255, 0, 0);
                for (int j = 0; j < h; j++) {
                    bmp.setPixel(i, j, color);
                }
            }

            Allocation in = Allocation.createFromBitmap(rs, bmp);
            Allocation out = Allocation.createTyped(rs, in.getType());
            ScriptC_test script = new ScriptC_test(rs);
            script.set_gSize(w);
            script.forEach_root(in, out);
            byte[] iii = new byte[out.getBytesSize()];
            out.copyTo(iii);
            for (int i = 0; i < w; i++) {
                android.util.Log.i(
                    TAG, "getPixel byte " + i + " "
                                + String.format("%02x%02x%02x%02x", iii[i * 4 + 3], iii[i * 4], iii[i * 4 + 1],
                                        iii[i * 4 + 2]));
            }
            out.copyTo(bmp);
            script.forEach_dump(out);

            for (int i = 0; i < w; i++) {
            android.util.Log.i(TAG, "getPixel2 bitmap " + i + " " + Integer.toHexString(bmp.getPixel(i, 0)));
            }

        return bmp;
    }

    public void testEquation() {
        ScriptC_test script = new ScriptC_test(rs);
        script.invoke_solveThirdDegreeEquation(2, -4, -22, 24);
        script.invoke_solveThirdDegreeEquation(1, 3, 3, 1);
        script.invoke_solveThirdDegreeEquation(1, 0, -1, 0);
    }
}