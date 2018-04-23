package com.lmj.customcamera;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_CAMERA = 666;
    ImageView mImageView;
    Button mPhotoBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setViews();
        setListeners();
    }

    private void setViews() {
        mImageView = (ImageView) findViewById(R.id.imageView);
        mPhotoBtn = (Button) findViewById(R.id.start_photo);
    }

    private void setListeners() {
        mPhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, PendingScanActivity.class);
                startActivityForResult(intent, REQUEST_CAMERA);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && requestCode == REQUEST_CAMERA) {
            String photoPath = data.getStringExtra(PendingScanActivity.VIN_PHOTO_PATH);
            Glide.with(this).load(photoPath).into(mImageView);
        }
    }

}
