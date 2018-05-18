package com.lmj.customcamera.vertical;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.widget.ImageView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * author: lmj
 * date  : 2018/3/19.
 */

public class VerticalPhotoPresenter implements VerticalPhotoContract.Presenter {

    public static String TEMP_IMAGE_PATH = Environment.getExternalStorageDirectory() + "/CustomCamera/";
    private VerticalPhotoContract.View mView;

    private Activity mActivity;
    private Camera mCamera;
    // 定义传感器管理器
    private SensorManager sensorMag = null;
    // 闪光灯默认关闭
    private boolean flashlight = false;
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

    VerticalPhotoPresenter(VerticalPhotoContract.View view, Activity activity) {
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



    public String saveJpeg(Bitmap bm) {
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
        return TEMP_IMAGE_PATH + pngName;
    }

    @Override
    public void pauseRelease() {
        stopPreview();
    }

    @Override
    public void onFlashClick() {
        if (mCamera == null) {
            mCamera = Camera.open();
        }
        if (mCamera!=null){
            Camera.Parameters params = mCamera.getParameters();

            if (flashlight) {
                // 关闭闪光灯
                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                flashlight = false;
            } else {
                // 打开闪光灯
                params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                flashlight = true;
            }
            mCamera.setParameters(params);
        }
    }

    @Override
    public void cameraAutoFocus() {
        if (mCamera != null) {
            mCamera.autoFocus(autoFocusCB);
        }
    }

    @Override
    public void onTakePhotoClick(ImageView view) {
        view.setEnabled(false);
        takePhoto();
        view.setEnabled(true);
    }

    @Override
    public void onBackClick() {
        mActivity.finish();
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
            VerticalRectView rectView = mView.getRectView();
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
        Bitmap dealBm;
        VerticalRectView rectView = mView.getRectView();
        //旋转90度
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.postRotate(90);
        dealBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, false);
        float scale = dealBm.getWidth() / (float) rectView.getWidth();
        int width = (int) (rectView.rectWidth * scale);
        int height = (int) (rectView.rectHeight * scale);
        int left = (int) ( dealBm.getWidth()*rectView.leftRatio);
        int top = (int) (dealBm.getHeight()*rectView.topRatio);
        dealBm = Bitmap.createBitmap(dealBm,left,top , width, height);
        return dealBm;
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
                VerticalRectView rectView = mView.getRectView();
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
                // 旋转镜头
                mCamera.setDisplayOrientation(90);
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
            if (flashlight) {
                if (mCamera != null) {
                    Camera.Parameters params = mCamera.getParameters();
                    // 关闭闪光灯
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                }
                flashlight = false;
            }
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
            sensorMag = (SensorManager) mActivity.getSystemService(VerticalPhotoActivity.SENSOR_SERVICE);
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
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);//data是字节数据，将其解析成位图
                mCamera.stopPreview();
                String imgPath = saveJpeg(bitmap);
                Intent intent = new Intent();
                intent.putExtra(VerticalPhotoActivity.VIN_PHOTO_PATH, imgPath);
                mActivity.setResult(VerticalPhotoActivity.RESULT_OK, intent);
                mActivity.finish();
            }
        }
    }


}
