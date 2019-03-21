package com.decawave.argomanager.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.decawave.argomanager.R;
import com.decawave.argomanager.components.struct.Anchor;

/*This class has been made possible by this spectacular tutorial site:
https://codelabs.developers.google.com/codelabs/advanced-android-training-draw-on-canvas/index.html?index=..%2F..advanced-android-training#2
 */

public class MyCanvasView extends View {
    private Paint mNewAnchorPaint;
    private Paint mExistingAnchorPaint;
    private int colorBlack;
    private int colorRed;
    private Canvas mExtraCanvas;
    private Bitmap mExtraBitmap;
    private float mX, mY;
    private float rectWidth = 15;
    private float rectHeight = 15;
    private static final float TOUCH_TOLERANCE = 4;
    private Rect mFrame = new Rect();
    public Anchor tempAnchor = new Anchor();

    MyCanvasView(Context context){
        this(context, null);
    }

    public MyCanvasView(Context context, AttributeSet attributeSet){
        super(context);
        tempAnchor.setName("tempAnchor");

        colorBlack = ResourcesCompat.getColor(getResources(),R.color.textFg,null);
        colorRed = ResourcesCompat.getColor(getResources(),R.color.anchor_color,null);

        mNewAnchorPaint = new Paint();
        mNewAnchorPaint.setColor(colorBlack);
        mNewAnchorPaint.setAntiAlias(true);
        mNewAnchorPaint.setDither(true);
        mNewAnchorPaint.setStyle(Paint.Style.STROKE);
        mNewAnchorPaint.setTextSize(30);
        mNewAnchorPaint.setStrokeWidth(3);

        mExistingAnchorPaint = new Paint();
        mExistingAnchorPaint.setColor(colorRed);
        mExistingAnchorPaint.setAntiAlias(true);
        mExistingAnchorPaint.setDither(true);
        mExistingAnchorPaint.setStyle(Paint.Style.STROKE);
        mExistingAnchorPaint.setTextSize(30);
        mExistingAnchorPaint.setStrokeWidth(3);

        setBackgroundResource(R.drawable.end301);

    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight){
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        mExtraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mExtraCanvas = new Canvas(mExtraBitmap);
        int inset = 40;
        mFrame = new Rect(inset, inset, width - inset, height - inset);
    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        canvas.drawBitmap(mExtraBitmap,0,0,null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                touchDown(x,y);
                invalidate();
                break;
            default:
                //do nothing
        }
        return true;
    }

    public void touchDown(float x, float y){
        mX = x;
        mY = y;
        String coords = "("+(int)mX+", "+(int)mY+")";
        float x_offset, y_offset;
        int bitmapWidth = mExtraBitmap.getWidth();

        if (mX > bitmapWidth-30) {
            x_offset = -140;
        } else if (mX > bitmapWidth-60) {
            x_offset = -110;
        }  else if (mX > bitmapWidth-90) {
            x_offset = -80;
        } else if (mX > bitmapWidth-120) {
            x_offset = -50;
        } else if (mX > bitmapWidth-150) {
            x_offset = -20;
        } else {
            x_offset = 0;
        }

        if (mY < 60){
            y_offset = 40;
        } else{
            y_offset = -20;
        }

        mExtraCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mExtraCanvas.drawRect(mX-(rectWidth/2),mY-(rectHeight/2),mX+(rectWidth/2),mY+(rectHeight/2),mNewAnchorPaint);
        mExtraCanvas.drawText(coords, mX + x_offset, mY + y_offset, mNewAnchorPaint);

        tempAnchor.setAnchorX(mX);
        tempAnchor.setAnchorY(mY);

        Log.i("TAG", "tempAnchor: \n" + tempAnchor.getName() + "\n" + tempAnchor.getAnchorX() + "\n" + tempAnchor.getAnchorY());

    }

}
