package com.easetheworld.rscoloroperationtest;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.easetheworld.renderscript.blendmode.FilterScript;
import com.easetheworld.renderscript.blendmode.FilterScript.FilterType;

public class MainActivity extends Activity {

    private ImageView iv;

    private Bitmap srcBitmap;
    private Bitmap drawingBitmap;
    private Canvas drawingCanvas;
    private Paint drawingPaint;
    private Bitmap resultBitmap;
    
    private SeekBar mRedSeekBar;
    private SeekBar mGreenSeekBar;
    private SeekBar mBlueSeekBar;
    private SeekBar mAlphaSeekBar;
    private float blurSize;

    private FilterScript mFilterScript;
    private FilterScript.FilterType[] FILTER_TYPES = FilterScript.FilterType.values();
    private FilterScript.FilterType mCurrentFilter = FILTER_TYPES[0];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        iv = (ImageView) findViewById(R.id.image2);
        iv.setOnTouchListener(new View.OnTouchListener() {
            private Path path = new Path();

            private float prevX1;
            private float prevY1;
            private float prevX2;
            private float prevY2;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getActionMasked();
                float x = event.getX();
                float y = event.getY();
                switch (action) {
                case MotionEvent.ACTION_DOWN:
                    drawingBitmap.eraseColor(0);
                    mFilterScript.setBlendingBitmap(resultBitmap);
                    path.reset();
                    path.moveTo(x, y);
                    prevX1 = x;
                    prevY1 = y;
                    prevX2 = x;
                    prevY2 = y;
                    break;
                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_UP:
                    float endX = (prevX1 + x) / 2f;
                    float endY = (prevY1 + y) / 2f;
                    path.quadTo(prevX1, prevY1, endX, endY);
                    float padding = drawingPaint.getStrokeWidth() / 2f + blurSize;
                    int left = (int) (Math.min(Math.min(prevX2, prevX1), endX) - padding);
                    int top = (int) (Math.min(Math.min(prevY2, prevY1), endY) - padding);
                    int right = (int) (Math.max(Math.max(prevX2, prevX1), endX) + padding + 0.5f);
                    int bottom = (int) (Math.max(Math.max(prevY2, prevY1), endY) + padding + 0.5f);
                    drawingCanvas.save();
                    drawingCanvas.clipRect(left, top, right, bottom);
                    drawingCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
                    drawingCanvas.drawPath(path, drawingPaint);
                    drawingCanvas.restore();
                    applyFilter(mCurrentFilter, left, top, right, bottom);
                    prevX2 = endX;
                    prevY2 = endY;
                    prevX1 = x;
                    prevY1 = y;
                    break;
                }
                return true;
            }
        });
        ListView filterModeList = (ListView) findViewById(R.id.filterModeList);
        filterModeList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        filterModeList.setAdapter(new ArrayAdapter<FilterScript.FilterType>(this,
                android.R.layout.simple_list_item_single_choice,
                FILTER_TYPES) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextSize(14f);
                return tv;
            }
        });
        filterModeList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FilterScript.FilterType type = FILTER_TYPES[position];
                if (type.defaultR != -1f) {
                    mRedSeekBar.setProgress((int) (type.defaultR * mRedSeekBar.getMax()));
                    mGreenSeekBar.setProgress((int) (type.defaultG * mGreenSeekBar.getMax()));
                    mBlueSeekBar.setProgress((int) (type.defaultB * mBlueSeekBar.getMax()));
                }
                if (type.defaultB == -1f) {
                    mBlueSeekBar.setVisibility(View.GONE);
                } else {
                    mBlueSeekBar.setVisibility(View.VISIBLE);
                }
                long t1 = SystemClock.elapsedRealtime();
                // applyFilter(type);
                long t2 = SystemClock.elapsedRealtime();
                // Toast.makeText(MainActivity.this, type.name() + " in " + (t2
                // - t1), Toast.LENGTH_SHORT).show();
                mCurrentFilter = type;
            }
        });
        filterModeList.setItemChecked(mCurrentFilter.ordinal(), true);

        mRedSeekBar = (SeekBar) findViewById(R.id.redSeekBar);
        mGreenSeekBar = (SeekBar) findViewById(R.id.greenSeekBar);
        mBlueSeekBar = (SeekBar) findViewById(R.id.blueSeekBar);
        mAlphaSeekBar = (SeekBar) findViewById(R.id.alphaSeekBar);
        mRedSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mGreenSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mBlueSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mAlphaSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        srcBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.yun, options);
        if (srcBitmap.getConfig() != Bitmap.Config.ARGB_8888) {
            srcBitmap = srcBitmap.copy(Bitmap.Config.ARGB_8888, false);
        }
        resultBitmap = srcBitmap.copy(srcBitmap.getConfig(), true);
        drawingBitmap = createLayerBitmap(srcBitmap);
        drawingCanvas = new Canvas(drawingBitmap);
        drawingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        drawingPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        drawingPaint.setStyle(Paint.Style.STROKE);
        drawingPaint.setStrokeWidth(getResources().getDisplayMetrics().density * 20f);
        drawingPaint.setStrokeCap(Paint.Cap.ROUND);
        drawingPaint.setStrokeJoin(Paint.Join.ROUND);
        blurSize = drawingPaint.getStrokeWidth() / 2f;
        drawingPaint.setMaskFilter(new BlurMaskFilter(blurSize, BlurMaskFilter.Blur.NORMAL));

        mFilterScript = new FilterScript(this, srcBitmap);

        mRedSeekBar.setProgress(mRedSeekBar.getMax());

        iv.setImageBitmap(resultBitmap);

        mFilterScript.testEquation();
    }

    private void applyFilter(FilterType type) {
        mFilterScript.setDrawingBitmap(drawingBitmap);
        mFilterScript.apply(type, resultBitmap);
        iv.invalidate();
    }

    private void applyFilter(FilterType type, int left, int top, int right, int bottom) {
        mFilterScript.setDrawingBitmap(drawingBitmap);
        mFilterScript.applyRect(type, resultBitmap, left, top, right, bottom);
        iv.invalidate(left, top, right, bottom);
    }

    private static Bitmap createLayerBitmap(Bitmap srcBitmap) {
        return Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), srcBitmap.getConfig());
    }

    private SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        private int getProgress(SeekBar seekBar) {
            return seekBar.getProgress() * 255 / seekBar.getMax();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int r = getProgress(mRedSeekBar);
            int g = getProgress(mGreenSeekBar);
            int b = getProgress(mBlueSeekBar);
            int a = getProgress(mAlphaSeekBar);
            int color = Color.argb(a, r, g, b);
            drawingPaint.setColor(color);

            // test
            // drawingBitmap.eraseColor(color);
            // applyFilter(mCurrentFilter);
            // iv.invalidate();
        }
    };

    public void clickHandler(View v) {
        switch (v.getId()) {
        case android.R.id.button1:
            iv.setImageBitmap(srcBitmap);
            break;
        case android.R.id.button2:
            iv.setImageBitmap(drawingBitmap);
            break;
        case android.R.id.button3:
            iv.setImageBitmap(resultBitmap);
            break;
        }
    }
}
