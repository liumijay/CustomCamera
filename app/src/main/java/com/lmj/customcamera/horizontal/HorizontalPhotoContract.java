package com.lmj.customcamera.horizontal;

import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.lmj.customcamera.base.BasePresenter;
import com.lmj.customcamera.base.BaseView;


/**
 * author: lmj
 * date  : 2018/3/19.
 */

public class HorizontalPhotoContract {

    /**
     * 主界面view接口
     */
    interface View extends BaseView<HorizontalPhotoPresenter> {
        HorizontalRectView getRectView();

        SurfaceView getCameraView();

        void setTakeBtn(boolean isTook);
    }

    /**
     * 主界面Presenter接口
     */
    interface Presenter extends BasePresenter {

        /**
         * 页面不可见时释放内存
         */
        void pauseRelease();

        /**
         * 闪关灯点击监听
         */
        void onCloseClick();

        /**
         * 相机自动对焦
         */
        void cameraAutoFocus();

        /**
         * 照相点击监听
         */
        void onTakePhotoClick(android.view.View view);

        /**
         * 重拍
         */
        void onRestartClick();

    }
}
