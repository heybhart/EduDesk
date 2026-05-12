package com.example.myapplication;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class DonutChartView extends View {

    private Paint paintActive, paintResolved, paintBackground;
    private RectF ovalRect;
    private float activeCount = 0, resolvedCount = 0;
    private float sweepActive = 0, sweepResolved = 0;

    public DonutChartView(Context ctx) { super(ctx); init(); }
    public DonutChartView(Context ctx, AttributeSet attrs) { super(ctx, attrs); init(); }
    public DonutChartView(Context ctx, AttributeSet attrs, int defStyle) { super(ctx, attrs, defStyle); init(); }

    private void init() {
        paintActive = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintActive.setStyle(Paint.Style.STROKE);
        paintActive.setColor(0xFF6B7599); // Mid Blue

        paintResolved = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintResolved.setStyle(Paint.Style.STROKE);
        paintResolved.setColor(0xFFD0D4E0); // Light Gray

        paintBackground = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBackground.setStyle(Paint.Style.STROKE);
        paintBackground.setColor(0xFFF0F2F5); // Background color
    }

    public void setData(float active, float resolved) {
        this.activeCount = active;
        this.resolvedCount = resolved;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f;
        float strokeWidth = w * 0.15f;
        float radius = (Math.min(w, h) / 2f) - strokeWidth;

        ovalRect = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);

        paintActive.setStrokeWidth(strokeWidth);
        paintResolved.setStrokeWidth(strokeWidth);
        paintBackground.setStrokeWidth(strokeWidth);
        
        paintActive.setStrokeCap(Paint.Cap.ROUND);
        paintResolved.setStrokeCap(Paint.Cap.ROUND);

        float total = activeCount + resolvedCount;
        
        // Draw background circle
        canvas.drawCircle(cx, cy, radius, paintBackground);

        if (total == 0) return;

        float activeSweep   = (activeCount / total) * 360f;
        float resolvedSweep = (resolvedCount / total) * 360f;

        // Draw Resolved part
        canvas.drawArc(ovalRect, -90f, resolvedSweep, false, paintResolved);
        
        // Draw Active part
        canvas.drawArc(ovalRect, -90f + resolvedSweep, activeSweep, false, paintActive);
    }
}