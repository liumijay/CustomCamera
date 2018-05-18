package com.lmj.customcamera.horizontal;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.View;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * author: lmj
 * date  : 2018/3/19.
 */

public class HorizontalPhotoPresenter implements HorizontalPhotoContract.Presenter {
    public static String TEMP_IMAGE_PATH = Environment.getExternalStorageDirectory() + "/CustomCamera/";

    private HorizontalPhotoContract.View mView;

    private Activity mActivity;
    private Camera mCamera;
    // 定义传感器管理器
    private SensorManager sensorMag = null;
    //    是否自动对焦
    private boolean isFinished = false;
    private SurfaceHolder mHolder;
    // 输出结果的先后顺序
    /**
     * AutoFocusCallback自动对焦
     */
    private Camera.AutoFocusCallback autoFocusCB = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            if (!success) {
                camera.autoFocus(this);//如果失败，自动聚焦
            }
        }
    };
    private Bitmap mBitmap;

    HorizontalPhotoPresenter(HorizontalPhotoContract.View view, Activity activity) {
        mView = view;
        mActivity = activity;
    }

    @Override
    public void start() {
        mHolder = mView.getSurfaceHolder();
        mHolder.setFormat(PixelFormat.TRANSPARENT);//translucent半透明 transparent透明
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mHolder.addCallback(new SurfaceCallBack());
    }



    public void saveJpeg(Bitmap bm) {
        Bitmap dealBm = dealBitmap(bm);

        long dataTake = System.currentTimeMillis();
        String pngName = dataTake + ".png";

        File jpegFile = getFile(TEMP_IMAGE_PATH, pngName);
        try {
            FileOutputStream fos = new FileOutputStream(jpegFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            //			//如果需要改变大小(默认的是宽960×高1280),如改成宽600×高800
            //			Bitmap newBM = bm.createScaledBitmap(bm, 600, 800, false);

            dealBm.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        Intent intent = new Intent();
        intent.putExtra(HorizontalPhotoActivity.VIN_PHOTO_PATH, TEMP_IMAGE_PATH + pngName);
        mActivity.setResult(HorizontalPhotoActivity.RESULT_OK, intent);
        mActivity.finish();
    }

    @Override
    public void pauseRelease() {
        stopPreview();
    }

    @Override
    public void onCloseClick() {
       mActivity.finish();
    }

    @Override
    public void cameraAutoFocus() {
        if (mCamera != null) {
            mCamera.autoFocus(autoFocusCB);
        }
    }

    @Override
    public void onTakePhotoClick(View view) {
        if (!isFinished){
            view.setEnabled(false);
            takePhoto();
            isFinished = true;
            mView.setTakeBtn(true);
            view.setEnabled(true);
        }else {
           saveJpeg(mBitmap);
        }
    }

    @Override
    public void onRestartClick() {
        mCamera.startPreview();
        isFinished = false;
        mView.setTakeBtn(false);
    }

    private void takePhoto() {
        if (mCamera != null) {
            mCamera.takePicture(null, null, new MyPictureCallback());
        }
    }

    /**
     * 修改相机参数
     */
    private void updateCameraParameters() {
        if (mCamera != null) {
            HorizontalRectView rectView = mView.getRectView();
            Camera.Parameters parameters = mCamera.getParameters();
            //先找最合适的照片尺寸
            List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
            Camera.Size pictureSize = getOptimalPreviewSize(pictureSizes, rectView.heightScreen, rectView.widthScreen);
            parameters.setPictureSize(pictureSize.width, pictureSize.height);

            //再找最合适的预览尺寸
            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
            Camera.Size size = getOptimalPreviewSize(sizes, pictureSize.width, pictureSize.height);
            parameters.setPreviewSize(size.width, size.height);
            mCamera.setParameters(parameters);

        }
    }

    /**
     * 获取最适合屏幕的照片 尺寸
     */
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null)
            return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        // Try to find an mSize match aspect ratio and mSize
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - h);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - h);
                }
            }
        }
        return optimalSize;
    }


    /**
     * 对照片进行旋转,剪切处理
     *
     * @return 处理结束后的图片
     */
    private Bitmap dealBitmap(Bitmap bm) {
        HorizontalRectView rectView = mView.getRectView();
        //旋转90度

        float scale = bm.getWidth() / (float) rectView.getWidth();
        int width = (int) (rectView.rectWidth * scale);
        int height = (int) (rectView.rectHeight * scale);
        int left = (int) ( bm.getWidth()*rectView.leftRatio);
        int top = (int) (bm.getHeight()*rectView.topRatio);
        bm = Bitmap.createBitmap(bm,left,top , width, height);
        return bm;
    }

    /**
     * 释放资源
     */
    private void stopPreview() {
        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(null);
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class SurfaceCallBack implements SurfaceHolder.Callback {

        /**
         * 开启相机
         *
         * @param holder holder
         */
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                HorizontalRectView rectView = mView.getRectView();
                mCamera = Camera.open();
                updateCameraParameters();
                //再调整SurfaceView 宽高，注意 调整宽高得在SurfaceView能获取宽高的时候
                Camera.Size size = mCamera.getParameters().getPreviewSize();
                float scale = rectView.getWidth() / Float.parseFloat(size.height + "");
                rectView.getLayoutParams().height = (int) (size.width * scale);
            } catch (Exception e) {
                // 打开相机异常
                if (null != mCamera) {
                    mCamera.release();
                    mCamera = null;
                }
            }
        }

        /**
         * 相机预览数据
         */
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            try {
                mCamera.setPreviewDisplay(mHolder);
                updateCameraParameters();
                restartSensor();
                mCamera.startPreview();
                // 开始预览
                mCamera.autoFocus(autoFocusCB);
            } catch (Exception e) {
                // 设置相机参数异常
                e.printStackTrace();
            }
        }

        /**
         * 关闭相机
         */
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (mCamera != null) {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        }
    }

    /**
     * 传感器管理器
     */
    private void restartSensor() {

        // 获取系统传感器管理器
        if (sensorMag == null) {
            sensorMag = (SensorManager) mActivity.getSystemService(HorizontalPhotoActivity.SENSOR_SERVICE);
        }

    }
    public  File getFile(String dirname,String filename){
        File file = new File(dirname);
        if (!file.exists()){
            //要点！
            file.mkdirs();
        }
        File tempFile = new File(file,filename);
        return tempFile;

    }
    private class MyPictureCallback implements Camera.PictureCallback {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            if (null != data && data.length > 0) {
                //data是字节数据，将其解析成位图
                mBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                mCamera.stopPreview();
            }
        }
    }


}
