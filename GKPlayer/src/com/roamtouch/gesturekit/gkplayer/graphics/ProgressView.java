package com.roamtouch.gesturekit.gkplayer.graphics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PointF;
import android.graphics.RectF;
import android.text.TextPaint;
import android.widget.ImageView;

public class ProgressView extends ImageView {
	private Paint paint = new Paint();
	private TextPaint textPaint = new TextPaint();
	private long progressAngle = 0;
	private String text = "";
	private PointF textPoint = new PointF();
	private RectF innerRect = new RectF();
	private RectF outerRect = new RectF();
	private float innerStroke = 0;
	private float outerStroke = 0;

	public ProgressView(Context context) {
		super(context);
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(0x77FFFFFF);
		
		textPaint.setColor(0xFFFFFFFF);
		textPaint.setFakeBoldText(false);
		textPaint.setTextAlign(Align.CENTER);
	}
	
	@Override
	public void onDraw(Canvas canvas){
		super.onDraw(canvas);
		paint.setColor(0x77FFFFFF);
		paint.setStrokeWidth(innerStroke);
		canvas.drawArc(innerRect, 0, 360, false, paint);
		paint.setStrokeWidth(outerStroke);
		paint.setColor(0x77000000);
		canvas.drawArc(outerRect, 270, progressAngle, false, paint);
		paint.setColor(0x44FFFFFF);
		canvas.drawArc(outerRect, (270+progressAngle)%360, 360 - progressAngle, false, paint);
		
		canvas.drawText(text, textPoint.x, textPoint.y, textPaint);
	}
	
	@Override
	public void onLayout(boolean changed, int left, int top, int right, int bottom){
		super.onLayout(changed, left, top, right, bottom);
		
		outerStroke = (right - left) * 0.18f;
		float padding = outerStroke*1.01f;
		outerRect.set(padding,  padding, right - left - padding, bottom - top - padding);
		textPaint.setTextSize((int)(outerStroke / 1.2f));
		textPoint.set(outerRect.left + outerRect.width() / 2, outerRect.top + outerRect.bottom - textPaint.getTextSize());
		
		innerStroke = (right - left) * 0.08f;
		
		float size = outerRect.width() - outerStroke * 2;
		innerRect.left = outerRect.left + (outerRect.width()-size)/2;
		innerRect.top = outerRect.top + (outerRect.height()-size)/2;
		innerRect.right = innerRect.left + size;
		innerRect.bottom = innerRect.top + size;
	}
	
	public void setProgress(long currentTime, long totalTime){
		this.progressAngle = currentTime * 360 / totalTime;
		text = milliSecondsToTimer(currentTime);
		postInvalidate();
	}
	
	private String milliSecondsToTimer(long milliseconds) {
		String finalTimerString = "";
		String secondsString = "";

		int hours = (int) (milliseconds / (1000 * 60 * 60));
		int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
		int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);

		if (hours > 0) {
			finalTimerString = hours + ":";
		}

		if (seconds < 10) 
			secondsString = "0" + seconds;
		else
			secondsString = "" + seconds;
		
		finalTimerString = finalTimerString + minutes + ":" + secondsString;

		return finalTimerString;
	}

}
