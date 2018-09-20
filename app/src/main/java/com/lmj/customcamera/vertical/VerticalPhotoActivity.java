package com.lmj.customcamera.vertical;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.lmj.customcamera.R;
import com.lmj.customcamera.base.BaseActivity;

/**
 * author: lmj
 * date  : 2018/3/19.
 */

public class VerticalPhotoActivity extends BaseActivity<VerticalPhotoPresenter> implements VerticalPhotoContract.View {

    public static String VIN_PHOTO_PATH = "VIN_PHOTO_PATH";
    //获取相机权限
    public static final int GET_CAMERA_STATE = 1002;

    SurfaceView mSurfaceView;
    //   照相按钮
    ImageView mPhotoBtn;
    //    返回按钮
    ImageView mBackBtn;
    //    闪关灯按钮
    ImageView mFlashBtn;
    //   遮罩层
    VerticalRectView mRectView;

    @Override
    public void setPresenter(VerticalPhotoPresenter presenter) {

    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_vertical_photo;
    }

    @Override
    protected void init() {
        mSurfaceView = findViewById(R.id.surface_view_camera);
        mPhotoBtn = findViewById(R.id.scan_photo);
        mBackBtn = findViewById(R.id.scan_back);
        mFlashBtn = findViewById(R.id.scan_flash);
        mRectView = findViewById(R.id.scan_rect_view);
        mPresenter = new VerticalPhotoPresenter(this, this);
        int checkCallPhonePermission = ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA);
        if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.CAMERA}, GET_CAMERA_STATE);
        } else {
            mPresenter.start();
        }
    }

    /**
     * Activity被暂停或收回cpu和其他资源时调用时调stopPreview释放资源
     */
    public void onPause() {
        super.onPause();
        mPresenter.pauseRelease();
    }

    @Override
    protected void setViews() {

    }

    @Override
    protected void setListeners() {
        mRectView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPresenter.cameraAutoFocus();
            }
        });
        mPhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPresenter.onTakePhotoClick(mPhotoBtn);
            }
        });
        mBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPresenter.onBackClick();
            }
        });

        mFlashBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    mPresenter.onFlashClick();
            }
        });
    }

    @Override
    public VerticalRectView getRectView() {
        return mRectView;
    }

    @Override
    public SurfaceHolder getSurfaceHolder() {
        return mSurfaceView.getHolder();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            //就像onActivityResult一样这个地方就是判断你是从哪来的。
            case GET_CAMERA_STATE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mPresenter.start();
                } else {
                    Toast.makeText(mActivity.getApplicationContext(), "很遗憾你把相机权限禁用了。请务必开启相机权限享受我们提供的服务吧。", Toast.LENGTH_SHORT)
                            .show();
                }
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
