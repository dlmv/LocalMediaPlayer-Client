package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class VerticalSeekBar extends SeekBar {

    public VerticalSeekBar(Context context) {
        super(context);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldh, oldw);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    protected void onDraw(Canvas c) {
        c.rotate(-90);
        c.translate(-getHeight(), 0);
        super.onDraw(c);
    }

    private SeekBar.OnSeekBarChangeListener myListener;

    @Override
    public void setOnSeekBarChangeListener(SeekBar.OnSeekBarChangeListener listener) {
        super.setOnSeekBarChangeListener(listener);
        myListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                int progress = getMax() - (int) (getMax() * event.getY() / getHeight());
                setProgress(progress);
                if (myListener != null) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        myListener.onStartTrackingTouch(this);
                    }
                    myListener.onProgressChanged(this, progress, true);
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        myListener.onStopTrackingTouch(this);
                    }
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                if (myListener != null) {
                    myListener.onStopTrackingTouch(this);
                }
                break;
        }
        return true;
    }

    @Override
    public synchronized void setProgress(int progress){
        super.setProgress(progress);
        onSizeChanged(getWidth(), getHeight(), 0, 0);
    }
}