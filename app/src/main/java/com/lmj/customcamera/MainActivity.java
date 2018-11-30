package com.lmj.customcamera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.lmj.customcamera.horizontal.HorizontalPhotoActivity;
import com.lmj.customcamera.vertical.VerticalPhotoActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_CAMERA = 666;

    public static final int REQUEST_PERMISS = 888;
    ImageView mImageView;
    Button mVerticalPhotoBtn;
    Button mHorizontalPhotoBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setViews();
        setListeners();
        requestPermission();
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

    private void requestPermission(){
        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA};
        //收集未授权或者拒绝过的权限
        ArrayList<String> deniedPermissionList = new ArrayList<>();
        for (String per : permissions) {
            int checkSelfPermission = ContextCompat.checkSelfPermission(this, per);
            if (checkSelfPermission == PackageManager.PERMISSION_DENIED) {
                deniedPermissionList.add(per);
            }
        }
        if (!deniedPermissionList.isEmpty()) {
            String[] permissionArray = deniedPermissionList.toArray(new String[deniedPermissionList.size()]);
            ActivityCompat.requestPermissions(this, permissionArray,REQUEST_PERMISS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode ==  REQUEST_PERMISS){
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "很遗憾你把多媒体权限禁用了", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }
}
