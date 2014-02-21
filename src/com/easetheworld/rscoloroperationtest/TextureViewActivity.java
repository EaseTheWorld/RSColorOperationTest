package com.easetheworld.rscoloroperationtest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;
import com.easetheworld.rscoloroperationtest.R;

public class TextureViewActivity extends Activity {

    private FilteredCameraView mFilteredCameraView;
    private SeekBar mRedSeekBar;
    private SeekBar mGreenSeekBar;
    private SeekBar mBlueSeekBar;
    private SeekBar mOpacitySeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texture);

        mFilteredCameraView = (FilteredCameraView) findViewById(R.id.textureView);

        mRedSeekBar = (SeekBar) findViewById(R.id.redSeekBar);
        mGreenSeekBar = (SeekBar) findViewById(R.id.greenSeekBar);
        mBlueSeekBar = (SeekBar) findViewById(R.id.blueSeekBar);
        mOpacitySeekBar = (SeekBar) findViewById(R.id.alphaSeekBar);
        mRedSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mGreenSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mBlueSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mOpacitySeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO Auto-generated method stub
        menu.add("Still Image");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        startActivity(new Intent(this, MainActivity.class));
        return super.onOptionsItemSelected(item);
    }

    private SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        private float getProgress(SeekBar seekBar) {
            return (float) seekBar.getProgress() / seekBar.getMax();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        }
    };
}
