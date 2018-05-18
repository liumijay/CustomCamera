package com.lmj.customcamera;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.lmj.customcamera.horizontal.HorizontalPhotoActivity;
import com.lmj.customcamera.vertical.VerticalPhotoActivity;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_CAMERA = 666;
    ImageView mImageView;
    Button mVerticalPhotoBtn;
    Button mHorizontalPhotoBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setViews();
        setListeners();
    }

    private void setViews() {
        mImageView = (ImageView) findViewById(R.id.imageView);
        mVerticalPhotoBtn = (Button) findViewById(R.id.start_vertical_photo);
        mHorizontalPhotoBtn = (Button) findViewById(R.id.start_horizontal_photo);
    }

    private void setListeners() {
        mVerticalPhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, VerticalPhotoActivity.class);
                startActivityForResult(intent, REQUEST_CAMERA);
            }
        });
        mHorizontalPhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, HorizontalPhotoActivity.class);
                startActivityForResult(intent, REQUEST_CAMERA);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && requestCode == REQUEST_CAMERA) {
            String photoPath = data.getStringExtra(VerticalPhotoActivity.VIN_PHOTO_PATH);
            Glide.with(this).load(photoPath).into(mImageView);
        }
    }

}
