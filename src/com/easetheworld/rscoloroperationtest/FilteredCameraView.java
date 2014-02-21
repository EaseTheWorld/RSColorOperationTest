package com.easetheworld.rscoloroperationtest;

import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.TextureView;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.easetheworld.renderscript.blendmode.FilterScript;

public class FilteredCameraView extends FrameLayout implements TextureView.SurfaceTextureListener {

    private Camera mCamera;
    private Bitmap mSrcBitmap;
    private TextureView mTextureView;

    private Bitmap mDstBitmap;
    private ImageView mImageView;

    private FilterScript mFilterScript;

    public FilteredCameraView(Context context) {
        super(context);
        init();
    }

    public FilteredCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FilteredCameraView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public Bitmap getBitmap() {
        return mDstBitmap.copy(mDstBitmap.getConfig(), false);
    }

    private void init() {
        mTextureView = new TextureView(getContext());
        mTextureView.setSurfaceTextureListener(this);
        mImageView = new ImageView(getContext());
        addView(mTextureView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        addView(mImageView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mCamera = Camera.open();

        try {
            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();
        } catch (IOException ioe) {
            // Something bad happened
        }

        mSrcBitmap = mTextureView.getBitmap();
        mDstBitmap = mSrcBitmap.copy(Bitmap.Config.ARGB_8888, true);
        mFilterScript = new FilterScript(getContext(), mSrcBitmap);
        mImageView.setImageBitmap(mDstBitmap);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mCamera.stopPreview();
        mCamera.release();

        return false;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            mTextureView.getBitmap(mSrcBitmap);
            mFilterScript.setInputBitmap(mSrcBitmap);
            mImageView.invalidate();
    }
}
