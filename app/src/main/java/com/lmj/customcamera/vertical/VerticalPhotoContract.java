package com.lmj.customcamera.vertical;

import android.view.SurfaceHolder;
import android.widget.ImageView;

import com.lmj.customcamera.base.BasePresenter;
import com.lmj.customcamera.base.BaseView;

/**
 * author: lmj
 * date  : 2018/3/19.
 */

public class VerticalPhotoContract {

    /**
     * 主界面view接口
     */
    interface View extends BaseView<VerticalPhotoPresenter> {
        VerticalRectView getRectView();

        SurfaceHolder getSurfaceHolder();
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
        void onFlashClick();

        /**
         * 相机自动对焦
         */
        void cameraAutoFocus();

        /**
         * 照相点击监听
         */
        void onTakePhotoClick(ImageView view);

        /**
         * 回退按钮点击监听
         */
        void onBackClick();

    }
}
