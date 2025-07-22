package com.kbyai.alpr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FaceView extends View {

    private Context context;
    private Paint rectPaint;
    private Paint textPaint;
    private Paint textScorePaint;

    private Size frameSize;

    private ArrayList<HashMap<String, Object>> platesMap;

    public FaceView(Context context) {
        this(context, null);

        this.context = context;
        init();
    }

    public FaceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        init();
    }

    public void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        rectPaint = new Paint();
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(3);
        rectPaint.setColor(Color.GREEN);
        rectPaint.setAntiAlias(true);
        rectPaint.setTextSize(50);

        textPaint = new Paint();
        textPaint.setStyle(Paint.Style.STROKE);
        textPaint.setStrokeWidth(3);
        textPaint.setColor(Color.YELLOW);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(50);

        textScorePaint = new Paint();
        textScorePaint.setStyle(Paint.Style.STROKE);
        textScorePaint.setStrokeWidth(3);
        textScorePaint.setColor(Color.WHITE);
        textScorePaint.setAntiAlias(true);
        textScorePaint.setTextSize(25);
    }

    public void setFrameSize(Size frameSize)
    {
        this.frameSize = frameSize;
    }

    public void setFaceBoxes(ArrayList<HashMap<String, Object>> platesMap)
    {
        this.platesMap = platesMap;
        invalidate();
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (frameSize != null && platesMap != null) {
            int frameWidth = frameSize.getWidth();   // 1080
            int frameHeight = frameSize.getHeight(); // 1440
            int viewWidth = canvas.getWidth();       // 1080
            int viewHeight = canvas.getHeight();     // 2026

            // Fit-to-height scaling
            float scale = (float) viewHeight / frameHeight;

            // Scaled frame dimensions
            float scaledWidth = frameWidth * scale;

            // Horizontal offset (center crop)
            float offsetX = (viewWidth - scaledWidth) / 2;
            float offsetY = 0; // height fits perfectly, no vertical offset

            for (Map<String, Object> plate : platesMap) {
                String number = plate.containsKey("number") ? (String) plate.get("number") : "Unknown";
                String score = plate.containsKey("score") ? (String) plate.get("score") : "";
                float x1 = plate.containsKey("x1") ? ((Number) plate.get("x1")).floatValue() : 0.0f;
                float y1 = plate.containsKey("y1") ? ((Number) plate.get("y1")).floatValue() : 0.0f;
                float x2 = plate.containsKey("x2") ? ((Number) plate.get("x2")).floatValue() : 0.0f;
                float y2 = plate.containsKey("y2") ? ((Number) plate.get("y2")).floatValue() : 0.0f;

                // Apply scale and offset
                float drawX1 = x1 * scale + offsetX;
                float drawY1 = y1 * scale + offsetY;
                float drawX2 = x2 * scale + offsetX;
                float drawY2 = y2 * scale + offsetY;

                // Draw label
                textPaint.setStrokeWidth(2);
                textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                canvas.drawText(number, drawX1 + 10, drawY1 - 10, textPaint);

                textPaint.setStrokeWidth(1);
                textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                canvas.drawText(score, drawX1 + 10, drawY2 + 30, textScorePaint);

                // Draw rectangle
                rectPaint.setStrokeWidth(3);
                rectPaint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(drawX1, drawY1, drawX2, drawY2, rectPaint);
            }
        } else {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
    }
}
