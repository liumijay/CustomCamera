package com.lmj.customcamera.horizontal;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lmj.customcamera.R;
import com.lmj.customcamera.base.BaseActivity;

/**
 * author: lmj
 * date  : 2018/3/19.
 */

public class HorizontalPhotoActivity extends BaseActivity<HorizontalPhotoPresenter> implements HorizontalPhotoContract.View, View.OnClickListener {
    SurfaceView mSurfaceView;
    //   遮罩层
    HorizontalRectView mRectView;
    //拍照/完成
    TextView mTakeBtn;
    //重拍
    TextView mRestartBtn;

    public static String VIN_PHOTO_PATH = "VIN_PHOTO_PATH";
    //获取相机权限
    public static final int GET_CAMERA_STATE = 1002;
    //startActivityForResult的值
    public static final int REQUEST_CAMERA = 666;
    private ImageView mCloseBtn;

    @Override
    public void setPresenter(HorizontalPhotoPresenter presenter) {

    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_horizontal_photo;
    }

    @Override
    protected void init() {
        mSurfaceView = findViewById(R.id.surface_view_camera);
        //   遮罩层
        mRectView = findViewById(R.id.take_rect_view);
        //拍照/完成
        mTakeBtn = findViewById(R.id.take_photo);
        //重拍
        mRestartBtn = findViewById(R.id.take_restart);
        //     关闭
        mCloseBtn = findViewById(R.id.take_close);
        mPresenter = new HorizontalPhotoPresenter(this, this);
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

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                (int) (screenWidth * 0.12), (int) (screenWidth * 0.12));
        layoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
        layoutParams.rightMargin = (int) (screenWidth * 0.02);
        mTakeBtn.setLayoutParams(layoutParams);

        layoutParams = new FrameLayout.LayoutParams((int) (screenWidth * 0.16), FrameLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.END;
        layoutParams.topMargin = (int) (screenHeight * 0.7);
        mRestartBtn.setLayoutParams(layoutParams);
        setTakeBtn(false);
    }

    @Override
    protected void setListeners() {
        mRectView.setOnClickListener(this);
        mCloseBtn.setOnClickListener(this);
        mTakeBtn.setOnClickListener(this);
        mRestartBtn.setOnClickListener(this);
    }

    @Override
    public HorizontalRectView getRectView() {
        return mRectView;
    }

    @Override
    public SurfaceHolder getSurfaceHolder() {
        return mSurfaceView.getHolder();
    }

    @Override
    public void setTakeBtn(boolean isTook) {
        if (isTook) {
            mTakeBtn.setBackgroundResource(R.drawable.shape_circle_solid_green);
            mTakeBtn.setText(R.string.common_finish);
            mRectView.setTip(getString(R.string.my_car_photo_success));
            mRestartBtn.setVisibility(View.VISIBLE);
        } else {
            mTakeBtn.setBackgroundResource(R.drawable.shape_circle_stroke_white);
            mTakeBtn.setText(R.string.common_take_photo);
            mRectView.setTip(getString(R.string.my_car_photo_ready));
            mRestartBtn.setVisibility(View.GONE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            //就像onActivityResult一样这个地方就是判断你是从哪来的。
            case GET_CAMERA_STATE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mPresenter.start();
                } else {
                    Toast.makeText(mActivity.getApplicationContext(), "很遗憾你把相机权限禁用了。", Toast.LENGTH_SHORT).show();
                }
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.take_close:
                mPresenter.onCloseClick();
                break;
            case R.id.take_photo:
                mPresenter.onTakePhotoClick(view);
                break;
            case R.id.take_restart:
                mPresenter.onRestartClick();
                break;
            case R.id.take_rect_view:
                mPresenter.cameraAutoFocus();
                break;
            default:
                break;
        }
    }
}
