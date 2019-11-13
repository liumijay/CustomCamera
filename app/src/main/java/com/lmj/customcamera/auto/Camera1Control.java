package com.lmj.customcamera.auto;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.support.v4.app.ActivityCompat;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 5.0以下相机API的封装。
 */
@SuppressWarnings("deprecation")
public class Camera1Control implements ICameraControl {

    private static final int MIN_PREVIEW_PIXELS = 640 * 480;
    private static final int MAX_PREVIEW_PIXELS = 1280 * 720;

    private int displayOrientation = 0;
    private int cameraId = 0;
    private int flashMode;
    private AtomicBoolean takingPicture = new AtomicBoolean(false);
    private AtomicBoolean abortingScan = new AtomicBoolean(false);
    private Context context;
    private Camera camera;

    private Camera.Parameters parameters;
    private PermissionCallback permissionCallback;
    private Rect previewFrame = new Rect();

    private PreviewView previewView;
    private View displayView;
    private int rotation = 0;
    private Camera.Size optSize;

    public int getCameraRotation() {
        return rotation;
    }

    public AtomicBoolean getAbortingScan() {
        return abortingScan;
    }


    @Override
    public void setDisplayOrientation(@CameraView.Orientation int displayOrientation) {
        this.displayOrientation = displayOrientation;
        switch (displayOrientation) {
            case CameraView.ORIENTATION_PORTRAIT:
                rotation = 90;
                break;
            case CameraView.ORIENTATION_HORIZONTAL:
                rotation = 0;
                break;
            case CameraView.ORIENTATION_INVERT:
                rotation = 180;
                break;
            default:
                rotation = 0;
        }
        previewView.requestLayout();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refreshPermission() {
        startPreview(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFlashMode(@FlashMode int flashMode) {
        if (this.flashMode == flashMode) {
            return;
        }
        this.flashMode = flashMode;
        updateFlashMode(flashMode);
    }

    @Override
    public int getFlashMode() {
        return flashMode;
    }

    @Override
    public void start() {
        startPreview(false);
    }

    @Override
    public void stop() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            stopPreview();
            // 避免同步代码，为了先设置null后release
            Camera tempC = camera;
            camera = null;
            tempC.release();
            camera = null;
            buffer = null;
        }
    }

    private void stopPreview() {
        if (camera != null) {
            CameraThreadPool.cancelAutoFocusTimer();
            camera.stopPreview();
        }
    }

    @Override
    public void pause() {
        if (camera != null) {
            stopPreview();
        }
        setFlashMode(FLASH_MODE_OFF);
    }

    @Override
    public void resume() {
        takingPicture.set(false);
        if (camera == null) {
            openCamera();
        } else {
            previewView.textureView.setSurfaceTextureListener(surfaceTextureListener);
            if (previewView.textureView.isAvailable()) {
                startPreview(false);
            }
        }
    }

    @Override
    public View getDisplayView() {
        return displayView;
    }

    @Override
    public void takePicture(final OnTakePictureCallback onTakePictureCallback) {
        if (takingPicture.get()) {
            return;
        }
        switch (displayOrientation) {
            case CameraView.ORIENTATION_PORTRAIT:
                parameters.setRotation(90);
                break;
            case CameraView.ORIENTATION_HORIZONTAL:
                parameters.setRotation(0);
                break;
            case CameraView.ORIENTATION_INVERT:
                parameters.setRotation(180);
                break;
        }
        try {
            Camera.Size picSize = getOptimalSize(camera.getParameters().getSupportedPictureSizes());
            parameters.setPictureSize(picSize.width, picSize.height);
            camera.setParameters(parameters);
            takingPicture.set(true);

            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    startPreview(false);
                    takingPicture.set(false);
                    if (onTakePictureCallback != null) {
                        onTakePictureCallback.onPictureTaken(data);
                    }
                }
            });
        } catch (RuntimeException e) {
            e.printStackTrace();
            startPreview(false);
            takingPicture.set(false);
        }
    }

    @Override
    public void setPermissionCallback(PermissionCallback callback) {
        this.permissionCallback = callback;
    }

    public Camera1Control(Context context) {
        this.context = context;
        previewView = new PreviewView(context);
        openCamera();
    }

    private void openCamera() {
        setupDisplayView();
    }

    private void setupDisplayView() {
        final TextureView textureView = new TextureView(context);
        previewView.textureView = textureView;
        previewView.setTextureView(textureView);
        displayView = previewView;
        textureView.setSurfaceTextureListener(surfaceTextureListener);
    }

    private SurfaceTexture surfaceCache;

    private byte[] buffer = null;

    private void setPreviewCallbackImpl() {
        if (buffer == null) {
            buffer = new byte[displayView.getWidth()
                    * displayView.getHeight() * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8];
        }

    }

    private void initCamera() {
        try {
            if (camera == null) {
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                    Camera.getCameraInfo(i, cameraInfo);
                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                        cameraId = i;
                    }
                }
                try {
                    camera = Camera.open(cameraId);
                } catch (Throwable e) {
                    e.printStackTrace();
                    startPreview(true);
                    return;
                }

            }
            if (parameters == null) {
                parameters = camera.getParameters();
                parameters.setPreviewFormat(ImageFormat.NV21);
            }
            opPreviewSize(previewView.getWidth(), previewView.getHeight());
            camera.setPreviewTexture(surfaceCache);
            setPreviewCallbackImpl();
            startPreview(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            surfaceCache = surface;
            initCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            opPreviewSize(previewView.getWidth(), previewView.getHeight());
            startPreview(false);
            setPreviewCallbackImpl();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            setPreviewCallbackImpl();
        }
    };

    // 开启预览
    private void startPreview(boolean checkPermission) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if (checkPermission && permissionCallback != null) {
                permissionCallback.onRequestPermission();
            }
            return;
        }
        if (camera == null) {
            initCamera();
        } else {
            camera.startPreview();
            CameraThreadPool.createAutoFocusTimerTask(new Runnable() {
                @Override
                public void run() {
                    synchronized (Camera1Control.this) {
                        if (camera != null && !takingPicture.get()) {
                            try {
                                camera.autoFocus(new Camera.AutoFocusCallback() {
                                    @Override
                                    public void onAutoFocus(boolean success, Camera camera) {
                                    }
                                });
                            } catch (Throwable e) {
                                // startPreview是异步实现，可能在某些机器上前几次调用会autofocus failß
                            }
                        }
                    }
                }
            });
        }
    }

    private void opPreviewSize(int width, @SuppressWarnings("unused") int height) {

        if (parameters != null && camera != null &&camera.getParameters()!=null&& width > 0) {
            optSize = getOptimalSize(camera.getParameters().getSupportedPreviewSizes());
            parameters.setPreviewSize(optSize.width, optSize.height);
            previewView.setRatio(1.0f * optSize.width / optSize.height);

            camera.setDisplayOrientation(getSurfaceOrientation());
            stopPreview();
            try {
                camera.setParameters(parameters);
            } catch (RuntimeException e) {
                e.printStackTrace();

            }
        }
    }

    private Camera.Size getOptimalSize(List<Camera.Size> sizes) {
        int width = previewView.textureView.getWidth();
        int height = previewView.textureView.getHeight();
        final double screenAspectRatio = (width > height)
                ? ((double) width / (double) height) :
                ((double) height / (double) width);

        Camera.Size selectedSize = null;
        double selectedMinus = -1;
        List<Camera.Size> supportedPictureSizes = new ArrayList<>(sizes);
        Iterator<Camera.Size> it = supportedPictureSizes.iterator();
        while (it.hasNext()) {
            Camera.Size supportedPreviewSize = it.next();
            int realWidth = supportedPreviewSize.width;
            int realHeight = supportedPreviewSize.height;
            if (realWidth * realHeight < MIN_PREVIEW_PIXELS) {
                it.remove();
            } else if (realWidth * realHeight > MAX_PREVIEW_PIXELS) {
                it.remove();
            } else {
                double aRatio = (supportedPreviewSize.width > supportedPreviewSize.height)
                        ? ((double) supportedPreviewSize.width / (double) supportedPreviewSize.height) :
                        ((double) supportedPreviewSize.height / (double) supportedPreviewSize.width);
                double minus = Math.abs(aRatio - screenAspectRatio);

                boolean selectedFlag = false;
                if ((selectedMinus == -1 && minus <= 0.25f)
                        || (selectedMinus >= minus && minus <= 0.25f)) {
                    selectedFlag = true;
                }
                if (selectedFlag) {
                    selectedMinus = minus;
                    selectedSize = supportedPreviewSize;
                }
            }
        }

        if (selectedSize != null) {
            return selectedSize;
        } else {
            return parameters.getPreviewSize();
        }
    }

    private void updateFlashMode(int flashMode) {
        switch (flashMode) {
            case FLASH_MODE_TORCH:
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                break;
            case FLASH_MODE_OFF:
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                break;
            case ICameraControl.FLASH_MODE_AUTO:
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                break;
            default:
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                break;
        }
        camera.setParameters(parameters);
    }

    private int getSurfaceOrientation() {
        @CameraView.Orientation
        int orientation = displayOrientation;
        switch (orientation) {
            case CameraView.ORIENTATION_PORTRAIT:
                return 90;
            case CameraView.ORIENTATION_HORIZONTAL:
                return 0;
            case CameraView.ORIENTATION_INVERT:
                return 180;
            default:
                return 90;
        }
    }

    /**
     * 有些相机匹配不到完美的比例。比如。我们的layout是4:3的。预览只有16:9
     * 的，如果直接显示图片会拉伸，变形。缩放的话，又有黑边。所以我们采取的策略
     * 是，等比例放大。这样预览的有一部分会超出屏幕。拍照后再进行裁剪处理。
     */
    private class PreviewView extends FrameLayout {

        private TextureView textureView;

        private float ratio = 0.75f;

        void setTextureView(TextureView textureView) {
            this.textureView = textureView;
            removeAllViews();
            addView(textureView);
        }

        void setRatio(float ratio) {
            this.ratio = ratio;
            requestLayout();
            relayout(getWidth(), getHeight());
        }

        public PreviewView(Context context) {
            super(context);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            relayout(w, h);
        }

        private void relayout(int w, int h) {
            int width = w;
            int height = h;
            if (w < h) {
                // 垂直模式，高度固定。
                height = (int) (width * ratio);
            } else {
                // 水平模式，宽度固定。
                width = (int) (height * ratio);
            }

            int l = (getWidth() - width) / 2;
            int t = (getHeight() - height) / 2;

            previewFrame.left = l;
            previewFrame.top = t;
            previewFrame.right = l + width;
            previewFrame.bottom = t + height;
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            textureView.layout(previewFrame.left, previewFrame.top, previewFrame.right, previewFrame.bottom);
        }
    }

    @Override
    public Rect getPreviewFrame() {
        return previewFrame;
    }
}
