package com.lmj.customcamera;

/**
 * 基础类按google 蓝皮案例走
 * @param <T> presenter 类型
 */
public interface BaseView<T> {

    /**
     * view中注入Presenter.
     * @param presenter Presenter
     */
    void setPresenter(T presenter);
}