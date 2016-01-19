package com.taidoc.pclinklibrary.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.taidoc.pclinklibrary.connection.AudioConnection;
import com.taidoc.pclinklibrary.connection.AudioConnection.ReadDataListener;
import com.taidoc.pclinklibrary.demo.util.GuiUtils;
import com.taidoc.pclinklibrary.demo.view.AudioChart;

public class AudioMonitorActivity extends Activity {

	private Button mBtnStart;
	private Button mBtnStop;
	private Button mBtnClear;
	private AudioChart mAudioChart;
	
	private AudioConnection mAudioConn;
	
	private ReadDataListener mReadDataListener = new ReadDataListener() {
		
		@Override
		public void onReceive(short[] data, int len) {			
			mAudioChart.setData(data, len);			
		}

		@Override
		public void onPlugged() {
			int ccc = 3;
			ccc = 4;
		}

		@Override
		public void onUnPlugged() {
			int ccc = 3;
			ccc = 4;
		}
	}; 
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.monitor_audio);
		
		findViews();
		setListeners();
		
		init();
	}
	
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		
		mAudioConn.destroy();		
		GuiUtils.goToPCLinkLibraryHomeActivity(AudioMonitorActivity.this);
	}

	private View.OnClickListener mOnClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.btn_start:
				mBtnStart.setEnabled(false);
				mBtnStop.setEnabled(true);				
				//mAudioConn.startWrite();
				mAudioConn.startRead();
				break;
			case R.id.btn_stop:
				mBtnStart.setEnabled(true);
				mBtnStop.setEnabled(false);
				//mAudioConn.stopWrite();
				mAudioConn.stopRead();
				break;
			case R.id.btn_clear:
				mAudioChart.clearData();
				break;
			}
		}
	};
	
	private void findViews() {
		mAudioChart = (AudioChart) findViewById(R.id.chart);
		mBtnStart = (Button) findViewById(R.id.btn_start);
		mBtnStop = (Button) findViewById(R.id.btn_stop);
		mBtnClear = (Button) findViewById(R.id.btn_clear);
	}
	
	private  void setListeners() {
		mBtnStart.setOnClickListener(mOnClickListener);
		mBtnStop.setOnClickListener(mOnClickListener);
		mBtnClear.setOnClickListener(mOnClickListener);
	}
	
	private void init() {
		mAudioConn = new AudioConnection(AudioMonitorActivity.this, mReadDataListener);
	}
}
