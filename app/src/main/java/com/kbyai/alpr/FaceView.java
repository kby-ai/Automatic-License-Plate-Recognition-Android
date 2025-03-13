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

        if (frameSize != null &&  platesMap != null) {
            float x_scale = this.frameSize.getWidth() / (float)canvas.getWidth();
            float y_scale = this.frameSize.getHeight() / (float)canvas.getHeight();

            for (Map<String, Object> plate : platesMap) {
                String number = plate.containsKey("number") ? (String) plate.get("number") : "Unknown";
                float x1 = plate.containsKey("x1") ? ((Number) plate.get("x1")).floatValue() : 0.0f;
                float y1 = plate.containsKey("y1") ? ((Number) plate.get("y1")).floatValue() : 0.0f;
                float x2 = plate.containsKey("x2") ? ((Number) plate.get("x2")).floatValue() : 0.0f;
                float y2 = plate.containsKey("y2") ? ((Number) plate.get("y2")).floatValue() : 0.0f;
//                int frameWidth = plate.containsKey("frameWidth") ? ((Number) plate.get("frameWidth")).intValue() : 0;
//                int frameHeight = plate.containsKey("frameHeight") ? ((Number) plate.get("frameHeight")).intValue() : 0;

//                Log.i("processPlates", "Plate Number: " + number);
//                Log.i("processPlates", "Bounding Box: (" + x1 + ", " + y1 + ") -> (" + x2 + ", " + y2 + ")");
//                Log.i("processPlates", "Frame Size: " + frameWidth + "x" + frameHeight);

                textPaint.setStrokeWidth(2);
                textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                assert number != null;
                canvas.drawText(number, (x1 / x_scale) + 10, (y1 / y_scale) - 10, textPaint);

                rectPaint.setStrokeWidth(3);
                rectPaint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(new Rect((int)(x1 / x_scale), (int)(y1 / y_scale), (int)(x2 / x_scale), (int)(y2 / y_scale)), rectPaint);
            }
        }
        else {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
    }
}
