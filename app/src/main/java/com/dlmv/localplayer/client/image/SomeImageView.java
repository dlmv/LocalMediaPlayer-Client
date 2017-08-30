package com.dlmv.localplayer.client.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class SomeImageView extends View {
	private boolean myInit = false;
	private int myWidth;
	private int myHeight;
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		myInit = true;
		float xsize = ((float)(getWidth())) / myBitmap.getWidth() ;
		float ysize = ((float)(getHeight())) / myBitmap.getHeight();
		myZoomCoeff = Math.min(xsize, ysize);
		myWidth = (int)(myBitmap.getWidth() * myZoomCoeff);
		myHeight = (int)(myBitmap.getHeight() * myZoomCoeff);
	}

	private final Paint myPaint = new Paint();

	private Bitmap myBitmap;

	public SomeImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	public void setBitmap(Bitmap b) {
		myBitmap = b;
	}
	
	
	public SomeImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public SomeImageView(Context context) {
		super(context);
	}

	private android.graphics.Matrix myMatrix = new android.graphics.Matrix();

	@Override
	protected void onDraw(final Canvas canvas) {
		if (!myInit) {
			return;
		}
		myPaint.setColor(Color.DKGRAY);
		final int w = getWidth();
		final int h = getHeight();
		canvas.drawRect(0, 0, w, h, myPaint);
		if (myBitmap == null || myBitmap.isRecycled()) {
			return;
		}
		Log.e("image", "x: " + Integer.toString(myFixedX));
		Log.e("image", "y: " + Integer.toString(myFixedY));
		float realZoom = myZoomFactor * myZoomCoeff;
		myMatrix.reset();
		myMatrix.setScale(realZoom, realZoom);
		myMatrix.postTranslate(-myFixedX * (myZoomFactor - 1), -myFixedY * (myZoomFactor - 1));
		myMatrix.postTranslate(getShiftX(), getShiftY());
		canvas.drawBitmap(myBitmap,  myMatrix, null);
	}

	private void shift(int x, int y) {
		if (myCanScrollZoomedPage) {
			if (myStartPressedX != -1) {
				myFixedX = (int)((myStartPressedX - x) / (myZoomFactor - 1)) + myStartFixedX;
				myFixedY = (int)((myStartPressedY - y) / (myZoomFactor - 1)) + myStartFixedY;
				if (myFixedX < 0) myFixedX = 0;
				if (myFixedY < 0) myFixedY = 0;
				if (myFixedX >= myWidth) myFixedX = myWidth;
				if (myFixedY >= myHeight) myFixedY = myHeight;
				postInvalidate();
			} else {
				myStartPressedX = x;
				myStartPressedY = y;	
				myStartFixedX = myFixedX;
				myStartFixedY = myFixedY;
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!myInit) {
			return false;
		}
		if (myBitmap == null || myBitmap.isRecycled()) {
			return false;
		}
		switch (event.getPointerCount()) {
			case 1:
				return onSingleTouchEvent(event);
			case 2:
				return onDoubleTouchEvent(event);
			default:
				return false;
		}
	}

	private boolean onSingleTouchEvent(MotionEvent event) {
		int x = (int)event.getX();
		int y = (int)event.getY();

		switch (event.getAction()) {
			case MotionEvent.ACTION_UP:
				myStartPressedX = -1;
				myStartPressedY = -1;	
				myStartFixedX = -1;
				myStartFixedY = -1;
				myCanScrollZoomedPage = true;
				break;
			case MotionEvent.ACTION_DOWN:
				if (myCanScrollZoomedPage) {
					myStartPressedX = x;
					myStartPressedY = y;	
					myStartFixedX = myFixedX;
					myStartFixedY = myFixedY;
				}
				break;
			case MotionEvent.ACTION_MOVE:
				shift(x, y);
				break;
		}
		return true;
	}

	private int getShiftX() {
		return (getWidth() - myWidth) / 2;
	}
	
	private int getShiftY() {
		return (getHeight() - myHeight) / 2;
	}
	
	private volatile float myZoomFactor = 1.0f;
	private volatile float myZoomCoeff = 1.0f;
	private volatile int myFixedX = 0;
	private volatile int myFixedY = 0;
	private int myStartFixedX = 0;
	private int myStartFixedY = 0;
	private int myPinchX = 0;
	private int myPinchY = 0;
	private int myStartPressedX = 0;
	private int myStartPressedY = 0;
	private float myStartPinchDistance2 = -1;
	private float myStartZoomFactor;
	private volatile boolean myCanScrollZoomedPage = true;
	
	private void startZoom(int pinchx, int pinchy, float dist) {
		myPinchX = pinchx;
		myPinchY = pinchy;
		myStartPinchDistance2 = dist;
		float x1 = (myFixedX * (myZoomFactor - 1) / myZoomFactor);
		float realPinchX = (myPinchX - getShiftX()) / myZoomFactor + x1;
		float y1 = (myFixedY * (myZoomFactor - 1) / myZoomFactor);
		float realPinchY = (myPinchY - getShiftY()) / myZoomFactor + y1;
		if ((realPinchX < 0 || realPinchX > myWidth) ||
				(realPinchY < 0 || realPinchY > myHeight)) {
			return;
		}
		myStartZoomFactor = myZoomFactor;
		myStartFixedX = myFixedX;
		myStartFixedY = myFixedY;
		myCanScrollZoomedPage = false;
	}
	
	private void zoom(float factor) {
		myZoomFactor = myStartZoomFactor * factor;
		if (myZoomFactor < 1) {
			myZoomFactor = 1;
		}
		float maxZoom = 8;
		if (myZoomFactor > maxZoom) {
			myZoomFactor = maxZoom;
		}
		float x1 = (myStartFixedX * (myStartZoomFactor - 1) / myStartZoomFactor);
		float realPinchX = (myPinchX - getShiftX()) / myStartZoomFactor + x1;
		float x2 = ((realPinchX * (myZoomFactor - myStartZoomFactor) + myStartZoomFactor * x1) / myZoomFactor);
		myFixedX = (int) (x2 / (myZoomFactor - 1) * myZoomFactor);
		float y1 = (myStartFixedY * (myStartZoomFactor - 1) / myStartZoomFactor);
		float realPinchY = (myPinchY - getShiftY()) / myStartZoomFactor + y1;
		float y2 = ((realPinchY * (myZoomFactor - myStartZoomFactor) + myStartZoomFactor * y1) / myZoomFactor);
		myFixedY = (int) (y2 / (myZoomFactor - 1) * myZoomFactor);
		if (myFixedX < 0) myFixedX = 0;
		if (myFixedY < 0) myFixedY = 0;
		if (myFixedX >= myWidth) myFixedX = myWidth;
		if (myFixedY >= myHeight) myFixedY = myHeight;
		postInvalidate();
	}
	
	private void stopZoom() {
		myStartPinchDistance2 = -1;
		invalidate();
	}
	
	private boolean onDoubleTouchEvent(MotionEvent event) {
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_POINTER_UP:
				stopZoom();
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
			{
				final int pinchx = (int)((event.getX(0) + event.getX(1)) / 2);
				final int pinchy = (int)((event.getY(0) + event.getY(1)) / 2);
				final float diffX = event.getX(0) - event.getX(1);
				final float diffY = event.getY(0) - event.getY(1);
				final float dist = Math.max(diffX * diffX + diffY * diffY, 10f);
				startZoom(pinchx, pinchy, dist);
				break;
			}
			case MotionEvent.ACTION_MOVE:
			{
				final float diffX = event.getX(0) - event.getX(1);
				final float diffY = event.getY(0) - event.getY(1);
				final float dist = Math.max(diffX * diffX + diffY * diffY, 10f);
				if (myStartPinchDistance2 < 0) {
					final int pinchx = (int)((event.getX(0) + event.getX(1)) / 2);
					final int pinchy = (int)((event.getY(0) + event.getY(1)) / 2);
					startZoom(pinchx, pinchy, dist);
				} else {
					zoom(dist / myStartPinchDistance2);
				}
			}
			break;
		}
		return true;
	}

}