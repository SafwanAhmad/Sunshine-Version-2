package com.example.android.sunshine.app;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

/**
 * Created by safwanx on 8/6/16.
 */
public class WindVane extends View {

    int centerX;
    int centerY;
    int radius;
    float vaneDirection;
    final DisplayMetrics metrics = getResources().getDisplayMetrics();


    public void setVaneDirection(float windDirection) {
        //We are getting direction from which wind is blowing, to find vane direction
        //we add 180 degrees. This is the direction measured with the y axis (North)
        //We convert it to angle measure w.r.t +X axis, i.e. pointing East.
        float vaneDirectionDegrees = 90 - (windDirection + 180);

        //Convert vane direction from degrees to radians
        vaneDirection = (float) ((vaneDirectionDegrees/360.0)*(2 * Math.PI));

        //Update the view that has been already drawn, because updated values are
        //available once the detail fragment is done with loading data.
        this.invalidate();

    }

    public WindVane(Context context) {
        super(context);
    }

    public WindVane(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WindVane(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public WindVane(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {



        //Create a 100x100 view, which is by default the width and height
        setMeasuredDimension((int)dpTopx(100), (int)dpTopx(100));
        centerX = (int)(dpTopx(100)/2.0);
        centerY = (int)(dpTopx(100)/2.0);
        radius = centerX - (int)dpTopx(4);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawVane(canvas);
    }


    private void drawVane(Canvas canvas){
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);

        paint.setStrokeWidth(dpTopx(4));

        canvas.drawCircle(centerX, centerY, radius, paint);


        // Draw thin Red line.
        paint.setStrokeWidth(dpTopx(2));
        paint.setColor(Color.RED);
        int lineLength = radius - (int)dpTopx(10);
        float lineAngle = vaneDirection;
        int endX = centerX + worldToViewX((int) polarToCartesianX(lineLength, lineAngle));
        int endY = centerY + worldToViewY((int) polarToCartesianY(lineLength, lineAngle));
        canvas.drawLine(centerX,centerY,endX,endY,paint);

        //Draw thick Black line.
        paint.setStrokeWidth(dpTopx(6));
        paint.setColor(Color.BLACK);
        lineLength = radius - (int)dpTopx(20);
        endX = centerX + worldToViewX((int) polarToCartesianX(lineLength, lineAngle));
        endY = centerY + worldToViewY((int)polarToCartesianY(lineLength, lineAngle));
        canvas.drawLine(centerX, centerY, endX, endY, paint);

    }


    private float dpTopx(float dp){
        return metrics.density*dp;
    }


    private int worldToViewX(int x){
        return x;
    }

    private int worldToViewY(int y){
        return -y;
    }

    private double polarToCartesianX(float length, float angle){
        return (length * Math.cos(angle));
    }

    private double polarToCartesianY(float length, float angle){
        return (length * Math.sin(angle));
    }

}
