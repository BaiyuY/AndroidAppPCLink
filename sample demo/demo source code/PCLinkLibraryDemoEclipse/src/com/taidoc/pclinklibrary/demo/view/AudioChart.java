package com.taidoc.pclinklibrary.demo.view;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class AudioChart extends ViewGroup {

	private Paint paint;
	private Paint paint2;
	private ChartView chartView;
	
	private int len;
	private short[] data;
	private List<PointF> points;
	private float firstXPos;
	private float lastXPos;
	private float firstYPos;
	private float lastYPos;
	private float xInterval;
	private float yInterval;
	
	public AudioChart(Context context) {
		super(context);
		init();
	}
	
	public AudioChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context, attrs);
        init();
    }
	
	public AudioChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttrs(context, attrs);
        init();
    }
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub
	
	}
	
	@Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        // 設定各個
        setSubViewLayout(w, h);
    }
	
	private void initAttrs(Context context, AttributeSet attrs) {
    }
	
	private void initPaint() {
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setStyle(Paint.Style.FILL_AND_STROKE);
		paint.setColor(Color.BLACK);
		paint.setStrokeWidth(5);
	}
	
	private void initPaint2() {
		paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint2.setStyle(Paint.Style.FILL_AND_STROKE);
		paint2.setColor(Color.BLUE);
		paint2.setStrokeWidth(5);
	}
	
	private void initViews() {
		chartView = new ChartView(getContext());
        addView(chartView);
	}
	
	public void init() {
		initPaint();
		initPaint2();
		initViews();
		
	}
	
	private void setSubViewLayout(float width, float height) {
		firstXPos = 10f;		
		firstYPos = 10f;
		lastXPos = width - (10f * 2);
		lastYPos = height - (10f * 2);
		
		chartView.layout((int)firstXPos, (int)firstYPos, (int)width, (int)height);
		
		chartView.setBackgroundColor(Color.WHITE);
	}

	public void clearData() {
		setData(null, 0);
		chartView.invalidate();
	}
	
	public void setData(short[] data, int len) {
		this.len = len;
		this.data = data;
		
		xInterval = 0f;
		yInterval = 0f;
		
		if (points != null) {
			points.clear();
		}
		else {
			points = new ArrayList<PointF>();
		}
		
		if (data == null) {
			return;
		}
		
		// find min(max) value
		if (len > 0) {
			short min = data[0];
			short max = data[0];
			
			// 這裡其實只是要確認裡面的值不為0, 因為在audiorecord的read不管有沒有值,都會回,全部會填0
			for (int i=1; i<len; i++) {
				short b = data[i];
				if (b < min) {
					min = b;
				}
				else if (b > max) {
					max = b;
				}			
			}
			
			if (min != 0 && max != 0) {
				min = Short.MIN_VALUE;
				max = Short.MAX_VALUE;				
				
				xInterval = (lastXPos - firstXPos) / len;
				yInterval = (lastYPos - firstYPos) / (max - min);
								
				for (int i=0; i<len; i++) {
					//short b = data[i];
					int b = (max - min) - (data[i] + 32768);
					points.add(new PointF(
							(firstXPos + (xInterval * i)),
							(firstYPos + (yInterval * b))));
				}
				
				chartView.invalidate();
			}
		}
	}
	
	private class ChartView extends View {

        public ChartView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            if (points != null) {				
            	int centerX = (int)((lastYPos - firstYPos) / 2);
            	canvas.drawLine(0, centerX, getWidth(), centerX, paint2);
            	            	
            	for (int i=0; i<points.size() - 1; i++) {
            		PointF point = points.get(i);
            		PointF point2 = points.get(i + 1);
            		canvas.drawLine(point.x, point.y, point2.x, point2.y, paint);
            	}
            }
        }
    }
}
