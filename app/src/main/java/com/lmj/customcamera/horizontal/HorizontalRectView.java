package com.lmj.customcamera.horizontal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;

import com.lmj.customcamera.R;


/**
 * author: lmj
 * date  : 2018/3/19.
 */

public class HorizontalRectView extends AppCompatImageView {
    //    角标画笔
    private Paint mHornPaint;
    //    背景画笔
    private Paint mBgPaint;
    //    文本画笔
    private Paint mTextPaint;
    //     提示
    private String mTip;
    //    矩形宽度
    public int rectWidth;
    //    矩形高度
    public int rectHeight;
    //    顶部偏移量
    public int topOffset;
    //    左边偏移量
    public int leftOffset;
    //    右边偏移量
    public int rightOffset;
    //    底部偏移量
    public int bottomOffset;
    public float leftRatio = 0.16f;
    public float topRatio = 0.1f;
    public int widthScreen;
    public int heightScreen;
    private final DisplayMetrics mMetrics;


    public HorizontalRectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMetrics = context.getResources().getDisplayMetrics();
        widthScreen = mMetrics.widthPixels;
        heightScreen = mMetrics.heightPixels;

        initPaint();
    }

    private void initPaint() {
        mTip = "请放正凭证，并调整好光线";

        mBgPaint = new Paint();
        mBgPaint.setStyle(Paint.Style.FILL);
        mBgPaint.setColor(getResources().getColor(R.color.supply_scan_bg));
        mBgPaint.setAntiAlias(true);

        mHornPaint = new Paint();
        mHornPaint.setStyle(Paint.Style.FILL);
        mHornPaint.setColor(getResources().getColor(R.color.common_white));
        mHornPaint.setStrokeWidth(dip2px(3));
        mHornPaint.setAntiAlias(true);

        mTextPaint = new Paint();
        mTextPaint.setColor(getResources().getColor(R.color.common_white));
        mTextPaint.setTextSize(dip2px(15));
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        topOffset = (int) (heightScreen * topRatio);
        leftOffset = (int) (widthScreen * leftRatio);
        rectHeight = heightScreen - 2 * topOffset;
        rightOffset = (int) (widthScreen * (1 - leftRatio));
        bottomOffset = topOffset + rectHeight;
        rectWidth = widthScreen - 2 * leftOffset;
    }


    @Override
    protected void onDraw(Canvas canvas) {


        //画矩形
        canvas.drawRect(leftOffset, 0, rightOffset, topOffset, mBgPaint);//上矩形
        canvas.drawRect(leftOffset, bottomOffset, rightOffset, heightScreen, mBgPaint);//下矩形
        canvas.drawRect(0, 0, leftOffset, heightScreen, mBgPaint);//左矩形
        canvas.drawRect(rightOffset, 0, widthScreen, heightScreen, mBgPaint);//右矩形


        //画8个小角标

        canvas.drawLine(leftOffset, topOffset, leftOffset + dip2px(30), topOffset, mHornPaint);//左上横角标
        canvas.drawLine(leftOffset, topOffset, leftOffset, topOffset + dip2px(30), mHornPaint);//左上竖角标
        canvas.drawLine(rightOffset, topOffset, rightOffset - dip2px(30), topOffset, mHornPaint);//右上横角标
        canvas.drawLine(rightOffset, topOffset, rightOffset, dip2px(30) + topOffset, mHornPaint);//右上竖角标
        canvas.drawLine(leftOffset, bottomOffset, leftOffset + dip2px(30), bottomOffset, mHornPaint);//左下横角标
        canvas.drawLine(leftOffset, bottomOffset, leftOffset, bottomOffset - dip2px(30), mHornPaint);//左下竖角标
        canvas.drawLine(rightOffset, bottomOffset, rightOffset - dip2px(30), bottomOffset, mHornPaint);//右下横角标
        canvas.drawLine(rightOffset, bottomOffset, rightOffset, bottomOffset - dip2px(30), mHornPaint);//右下竖角标

        canvas.drawText(mTip, widthScreen / 2, topOffset - dip2px(10), mTextPaint);

    }

    public void setTip(String tip) {
        mTip = tip;
        invalidate();
    }

    int dip2px(float dipValue) {
        final float scale = mMetrics.density;
        return (int) (dipValue * scale + 0.5f);
    }
}