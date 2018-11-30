package com.lmj.customcamera.base;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

/**
 * 基础Activity 类.
 *
 * @param <T> 基础Presenter
 */
public abstract class BaseActivity<T extends BasePresenter> extends AppCompatActivity {
    /** Presenter. */
    protected T mPresenter;


    /** Activity 实例. */
    protected Activity mActivity;

    /** ButterKnife绑定视图 */
    protected View mContentView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        if (provideContentViewId()!= 0){
            mContentView = this.getLayoutInflater().inflate(provideContentViewId(), null);
            setContentView(mContentView);
        }
        getExtraDatas();
        init();
        initToolbar();
        setViews();
        setListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //全屏显示
        if (Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(option);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        release();

    }

    /**
     * 获取 Extra 数据.
     */
    protected void getExtraDatas() {
    }

    /**
     * 初始化Toolbar.
     */
    protected void initToolbar() {
    }

    /**
     * 布局文件layout的id.
     *
     * @return layout 对应的id.
     */
    protected abstract int provideContentViewId();

    /**
     * 初始化操作.
     */
    protected abstract void init();

    /**
     * 视图的设置操作.
     */
    protected abstract void setViews();

    /**
     * 涉及到监听器的设置操作.
     */
    protected abstract void setListeners();

    /**
     * 释放相关操作.
     */
    protected void release(){};

}
