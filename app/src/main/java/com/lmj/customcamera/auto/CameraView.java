
package com.lmj.customcamera.auto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.support.annotation.IntDef;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.lmj.customcamera.util.ImageUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 负责，相机的管理。同时提供，裁剪遮罩功能。
 */
public class CameraView extends FrameLayout {

    /**
     * 照相回调
     */
    interface OnTakePictureCallback {
        void onPictureTaken(Bitmap bitmap);
    }

    /**
     * 垂直方向 {@link #setOrientation(int)}
     */
    public static final int ORIENTATION_PORTRAIT = 0;
    /**
     * 水平方向 {@link #setOrientation(int)}
     */
    public static final int ORIENTATION_HORIZONTAL = 90;
    /**
     * 水平翻转方向 {@link #setOrientation(int)}
     */
    public static final int ORIENTATION_INVERT = 270;


    @IntDef({ORIENTATION_PORTRAIT, ORIENTATION_HORIZONTAL, ORIENTATION_INVERT})
    @interface Orientation {

    }

    private CameraViewTakePictureCallback cameraViewTakePictureCallback = new CameraViewTakePictureCallback();

    private ICameraControl cameraControl;

    /**
     * 相机预览View
     */
    private View displayView;
    /**
     * 身份证，银行卡，等裁剪用的遮罩
     */
    private MaskView maskView;



    public ICameraControl getCameraControl() {
        return cameraControl;
    }

    public void setOrientation(@Orientation int orientation) {
        cameraControl.setDisplayOrientation(orientation);
    }
    public CameraView(Context context) {
        super(context);
        init();
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void start() {
        cameraControl.start();
        setKeepScreenOn(true);
    }

    public void stop() {
        cameraControl.stop();
        setKeepScreenOn(false);
    }

    public void takePicture(final File file, final OnTakePictureCallback callback) {
        cameraViewTakePictureCallback.file = file;
        cameraViewTakePictureCallback.callback = callback;
        cameraControl.takePicture(cameraViewTakePictureCallback);
    }


    public void setMaskType(@MaskView.MaskType int maskType) {
        maskView.setMaskType(maskType);
        if (maskType==MaskView.MASK_TYPE_NONE){
            maskView.setVisibility(INVISIBLE);
        }else {
            maskView.setVisibility(VISIBLE);
        }
    }


    private void init() {

        cameraControl = new Camera1Control(getContext());

        displayView = cameraControl.getDisplayView();
        addView(displayView);

        maskView = new MaskView(getContext());
        addView(maskView);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        displayView.layout(left, 0, right, bottom - top);
        maskView.layout(left, 0, right, bottom - top);

    }

    /**
     * 拍摄后的照片。需要进行裁剪。有些手机（比如三星）不会对照片数据进行旋转，而是将旋转角度写入EXIF信息当中，
     * 所以需要做旋转处理。
     *
     * @param outputFile 写入照片的文件。
     * @param data  原始照片数据。
     * @param rotation   照片exif中的旋转角度。
     *
     * @return 裁剪好的bitmap。
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private Bitmap crop(File outputFile, byte[] data, int rotation) {
        try {
            Rect previewFrame = cameraControl.getPreviewFrame();

            if (maskView.getWidth() == 0 || maskView.getHeight() == 0
                    || previewFrame.width() == 0 || previewFrame.height() == 0) {
                return null;
            }

            // BitmapRegionDecoder不会将整个图片加载到内存。
            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(data, 0, data.length, true);



            int width = rotation % 180 == 0 ? decoder.getWidth() : decoder.getHeight();
            int height = rotation % 180 == 0 ? decoder.getHeight() : decoder.getWidth();

            Rect frameRect = maskView.getFrameRect();

            int left = width * frameRect.left / maskView.getWidth();
            int top = height * frameRect.top / maskView.getHeight();
            int right = width * frameRect.right / maskView.getWidth();
            int bottom = height * frameRect.bottom / maskView.getHeight();

            // 高度大于图片
            if (previewFrame.top < 0) {
                // 宽度对齐。
                int adjustedPreviewHeight = previewFrame.height() * getWidth() / previewFrame.width();
                int topInFrame = ((adjustedPreviewHeight - frameRect.height()) / 2)
                        * getWidth() / previewFrame.width();
                int bottomInFrame = ((adjustedPreviewHeight + frameRect.height()) / 2) * getWidth()
                        / previewFrame.width();

                // 等比例投射到照片当中。
                top = topInFrame * height / previewFrame.height();
                bottom = bottomInFrame * height / previewFrame.height();
            } else {
                // 宽度大于图片
                if (previewFrame.left < 0) {
                    // 高度对齐
                    int adjustedPreviewWidth = previewFrame.width() * getHeight() / previewFrame.height();
                    int leftInFrame = ((adjustedPreviewWidth - maskView.getFrameRect().width()) / 2) * getHeight()
                            / previewFrame.height();
                    int rightInFrame = ((adjustedPreviewWidth + maskView.getFrameRect().width()) / 2) * getHeight()
                            / previewFrame.height();

                    // 等比例投射到照片当中。
                    left = leftInFrame * width / previewFrame.width();
                    right = rightInFrame * width / previewFrame.width();
                }
            }

            Rect region = new Rect();
            region.left = left;
            region.top = top;
            region.right = right;
            region.bottom = bottom;

            // 90度或者270度旋转
            if (rotation % 180 == 90) {
                int x = decoder.getWidth() / 2;
                int y = decoder.getHeight() / 2;

                int rotatedWidth = region.height();
                int rotated = region.width();

                // 计算，裁剪框旋转后的坐标
                region.left = x - rotatedWidth / 2;
                region.top = y - rotated / 2;
                region.right = x + rotatedWidth / 2;
                region.bottom = y + rotated / 2;
                region.sort();
            }

            BitmapFactory.Options options = new BitmapFactory.Options();

            // 最大图片大小。
            int maxPreviewImageSize = 2560;
            int size = Math.min(decoder.getWidth(), decoder.getHeight());
            size = Math.min(size, maxPreviewImageSize);

            options.inSampleSize = ImageUtil.calculateInSampleSize(options, size, size);
            options.inScaled = true;
            options.inDensity = Math.max(options.outWidth, options.outHeight);
            options.inTargetDensity = size;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bitmap = decoder.decodeRegion(region, options);

            if (rotation != 0) {
                // 只能是裁剪完之后再旋转了。有没有别的更好的方案呢？
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                Bitmap rotatedBitmap = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                if (bitmap != rotatedBitmap) {
                    // 有时候 createBitmap会复用对象
                    bitmap.recycle();
                }
                bitmap = rotatedBitmap;
            }

            try {
                if (!outputFile.exists()) {
                    outputFile.createNewFile();
                }
                FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
                fileOutputStream.flush();
                fileOutputStream.close();
                return bitmap;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class CameraViewTakePictureCallback implements ICameraControl.OnTakePictureCallback {

        private File file;
        private OnTakePictureCallback callback;

        @Override
        public void onPictureTaken(final byte[] data) {
            CameraThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    final int rotation = ImageUtil.getOrientation(data);
                    Bitmap bitmap = crop(file, data, rotation);
                    callback.onPictureTaken(bitmap);
                }
            });
        }
    }
}
