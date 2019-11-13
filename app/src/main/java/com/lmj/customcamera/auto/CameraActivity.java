package com.lmj.customcamera.auto;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Surface;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.lmj.customcamera.R;
import com.lmj.customcamera.crop.CropView;
import com.lmj.customcamera.crop.FrameOverlayView;
import com.lmj.customcamera.util.CameraParamUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraActivity extends AppCompatActivity {

    public static final String KEY_OUTPUT_FILE_PATH = "outputFilePath";
    public static final String KEY_CONTENT_TYPE = "contentType";

    public static final String CONTENT_TYPE_GENERAL = "general";
    public static final String CONTENT_TYPE_RECT = "rect";

    private static final int REQUEST_CODE_PICK_IMAGE = 100;
    private static final int PERMISSIONS_REQUEST_CAMERA = 800;
    private static final int PERMISSIONS_EXTERNAL_STORAGE = 801;

    private File outputFile;
    private String contentType;
    private Handler handler = new Handler();

    private CameraLayout takePictureContainer;
    private CameraLayout cropContainer;
    private CameraLayout confirmResultContainer;
    private ImageView lightButton;
    private CameraView cameraView;
    private ImageView displayImageView;
    private CropView cropView;
    private FrameOverlayView overlayView;
    private MaskView cropMaskView;
    private ImageView takePhotoBtn;
    private PermissionCallback permissionCallback = new PermissionCallback() {
        @Override
        public boolean onRequestPermission() {
            ActivityCompat.requestPermissions(CameraActivity.this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSIONS_REQUEST_CAMERA);
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        takePictureContainer = findViewById(R.id.take_picture_container);
        confirmResultContainer = findViewById(R.id.confirm_result_container);

        cameraView = findViewById(R.id.camera_view);
        cameraView.getCameraControl().setPermissionCallback(permissionCallback);
        lightButton = findViewById(R.id.light_button);
        lightButton.setOnClickListener(lightButtonOnClickListener);
        takePhotoBtn = findViewById(R.id.take_photo_button);
        findViewById(R.id.album_button).setOnClickListener(albumButtonOnClickListener);
        takePhotoBtn.setOnClickListener(takeButtonOnClickListener);
        findViewById(R.id.close_button).setOnClickListener(closeButtonOnClickListener);

        // confirm result;
        displayImageView = findViewById(R.id.display_image_view);
        confirmResultContainer.findViewById(R.id.confirm_button).setOnClickListener(confirmButtonOnClickListener);
        confirmResultContainer.findViewById(R.id.cancel_button).setOnClickListener(confirmCancelButtonOnClickListener);
        findViewById(R.id.rotate_button).setOnClickListener(rotateButtonOnClickListener);

        cropView = findViewById(R.id.crop_view);
        cropContainer = findViewById(R.id.crop_container);
        overlayView = findViewById(R.id.overlay_view);
        cropContainer.findViewById(R.id.confirm_button).setOnClickListener(cropConfirmButtonListener);
        cropMaskView = cropContainer.findViewById(R.id.crop_mask_view);
        cropContainer.findViewById(R.id.cancel_button).setOnClickListener(cropCancelButtonListener);

        setOrientation(getResources().getConfiguration());
        initParams();
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    private void initParams() {
        String outputPath = getIntent().getStringExtra(KEY_OUTPUT_FILE_PATH);

        if (!TextUtils.isEmpty(outputPath)) {
            outputFile = new File(outputPath);
        } else {
            outputFile = new File(CameraParamUtil.TEMP_IMAGE_PATH + System.currentTimeMillis() + ".png");
        }

        contentType = getIntent().getStringExtra(KEY_CONTENT_TYPE);
        if (contentType == null) {
            contentType = CONTENT_TYPE_GENERAL;
        }
        int maskType;

        if (contentType.equals(CONTENT_TYPE_GENERAL)) {
            maskType = MaskView.MASK_TYPE_NONE;
            cropMaskView.setVisibility(View.INVISIBLE);
        } else {
            maskType = MaskView.MASK_TYPE_RECT;
            overlayView.setVisibility(View.INVISIBLE);
        }
        cameraView.setMaskType(maskType);
        cropMaskView.setMaskType(maskType);
    }


    private void showTakePicture() {
        cameraView.getCameraControl().resume();
        updateFlashMode();
        takePictureContainer.setVisibility(View.VISIBLE);
        confirmResultContainer.setVisibility(View.INVISIBLE);
        cropContainer.setVisibility(View.INVISIBLE);
    }

    private void showCrop() {
        cameraView.getCameraControl().pause();
        updateFlashMode();
        takePictureContainer.setVisibility(View.INVISIBLE);
        confirmResultContainer.setVisibility(View.INVISIBLE);
        cropContainer.setVisibility(View.VISIBLE);
    }

    private void showResultConfirm() {
        cameraView.getCameraControl().pause();
        updateFlashMode();
        takePictureContainer.setVisibility(View.INVISIBLE);
        confirmResultContainer.setVisibility(View.VISIBLE);
        cropContainer.setVisibility(View.INVISIBLE);
    }

    // take photo;
    private void updateFlashMode() {
        int flashMode = cameraView.getCameraControl().getFlashMode();
        if (flashMode == ICameraControl.FLASH_MODE_TORCH) {
            lightButton.setImageResource(R.mipmap.bd_light_on);
        } else {
            lightButton.setImageResource(R.mipmap.bd_light_off);
        }
    }

    private View.OnClickListener albumButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(CameraActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSIONS_EXTERNAL_STORAGE);
                return;
            }
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
        }
    };

    private View.OnClickListener lightButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (cameraView.getCameraControl().getFlashMode() == ICameraControl.FLASH_MODE_OFF) {
                cameraView.getCameraControl().setFlashMode(ICameraControl.FLASH_MODE_TORCH);
            } else {
                cameraView.getCameraControl().setFlashMode(ICameraControl.FLASH_MODE_OFF);
            }
            updateFlashMode();
        }
    };

    private View.OnClickListener takeButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            cameraView.takePicture(outputFile, takePictureCallback);
        }
    };

    private CameraView.OnTakePictureCallback takePictureCallback = new CameraView.OnTakePictureCallback() {
        @Override
        public void onPictureTaken(final Bitmap bitmap) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    takePictureContainer.setVisibility(View.INVISIBLE);
                    if (cropMaskView.getMaskType() == MaskView.MASK_TYPE_NONE) {
                        cropView.setFilePath(outputFile.getAbsolutePath());
                        showCrop();
                    } else {
                        displayImageView.setImageBitmap(bitmap);
                        showResultConfirm();
                    }
                }
            });
        }
    };

    private View.OnClickListener cropCancelButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // 释放 cropView中的bitmap;
            cropView.setFilePath(null);
            showTakePicture();
        }
    };

    private View.OnClickListener cropConfirmButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int maskType = cropMaskView.getMaskType();
            Rect rect;
            if (maskType==MaskView.MASK_TYPE_NONE){
                rect = overlayView.getFrameRect();
            }else {
                rect = cropMaskView.getFrameRect();
            }
            Bitmap cropped = cropView.crop(rect);
            displayImageView.setImageBitmap(cropped);
            cropAndConfirm();
        }
    };

    private void cropAndConfirm() {
        cameraView.getCameraControl().pause();
        updateFlashMode();
        doConfirmResult();
    }

    // confirm result;
    private View.OnClickListener closeButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    };

    private void doConfirmResult() {
        CameraThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                    Bitmap bitmap = ((BitmapDrawable) displayImageView.getDrawable()).getBitmap();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent();
                intent.putExtra(CameraParamUtil.IMAGE_PATH, outputFile.getAbsolutePath());
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
    }

    private View.OnClickListener confirmButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            doConfirmResult();
        }
    };

    private View.OnClickListener confirmCancelButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            displayImageView.setImageBitmap(null);
            showTakePicture();
        }
    };

    private View.OnClickListener rotateButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            cropView.rotate(90);
        }
    };

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setOrientation(newConfig);
    }

    private void setOrientation(Configuration newConfig) {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int orientation;
        int cameraViewOrientation = CameraView.ORIENTATION_PORTRAIT;
        switch (newConfig.orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                cameraViewOrientation = CameraView.ORIENTATION_PORTRAIT;
                orientation = CameraLayout.ORIENTATION_PORTRAIT;
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                orientation = CameraLayout.ORIENTATION_HORIZONTAL;
                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
                    cameraViewOrientation = CameraView.ORIENTATION_HORIZONTAL;
                } else {
                    cameraViewOrientation = CameraView.ORIENTATION_INVERT;
                }
                break;
            default:
                orientation = CameraLayout.ORIENTATION_PORTRAIT;
                cameraView.setOrientation(CameraView.ORIENTATION_PORTRAIT);
                break;
        }
        takePictureContainer.setOrientation(orientation);
        cameraView.setOrientation(cameraViewOrientation);
        cropContainer.setOrientation(orientation);
        confirmResultContainer.setOrientation(orientation);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data.getData();
                cropView.setFilePath(getRealPathFromURI(uri));
                showCrop();
            } else {
                cameraView.getCameraControl().resume();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraView.getCameraControl().refreshPermission();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.camera_permission_required, Toast.LENGTH_LONG)
                            .show();
                }
                break;
            }
            case PERMISSIONS_EXTERNAL_STORAGE:
            default:
                break;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        CameraThreadPool.cancelAutoFocusTimer();
    }
}
