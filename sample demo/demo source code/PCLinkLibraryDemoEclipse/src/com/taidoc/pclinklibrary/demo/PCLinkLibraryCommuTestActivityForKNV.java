/*
 * Copyright (C) 2012 TaiDoc Technology Corporation. All rights reserved.
 */

package com.taidoc.pclinklibrary.demo;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.taidoc.pclinklibrary.android.bluetooth.util.BluetoothUtil;
import com.taidoc.pclinklibrary.connection.AndroidBluetoothConnection;
import com.taidoc.pclinklibrary.connection.util.ConnectionManager;
import com.taidoc.pclinklibrary.constant.PCLinkLibraryConstant;
import com.taidoc.pclinklibrary.demo.constant.PCLinkLibraryDemoConstant;
import com.taidoc.pclinklibrary.demo.util.GuiUtils;
import com.taidoc.pclinklibrary.exceptions.CommunicationTimeoutException;
import com.taidoc.pclinklibrary.exceptions.ExceedRetryTimesException;
import com.taidoc.pclinklibrary.exceptions.NotConnectSerialPortException;
import com.taidoc.pclinklibrary.exceptions.NotSupportMeterException;
import com.taidoc.pclinklibrary.interfaces.getStorageDataRecordInterface;
import com.taidoc.pclinklibrary.meter.AbstractMeter;
import com.taidoc.pclinklibrary.meter.record.WeightScaleRecord;
import com.taidoc.pclinklibrary.meter.util.MeterManager;

/**
 * PCLinkLibrary command test activity
 * 
 * @author Jay Lee
 * 
 */
public class PCLinkLibraryCommuTestActivityForKNV extends FragmentActivity {
	// Message types sent from the meterCommuHandler Handler
    public static final int MESSAGE_STATE_CONNECTING = 1;
    public static final int MESSAGE_STATE_CONNECT_FAIL = 2;
    public static final int MESSAGE_STATE_CONNECT_DONE = 3;
    public static final int MESSAGE_STATE_CONNECT_NONE = 4;
    public static final int MESSAGE_STATE_CONNECT_METER_SUCCESS = 5;
    public static final int MESSAGE_STATE_CHECK_METER_INFORMATION = 6;
    public static final int MESSAGE_STATE_CHECK_METER_BT_DISTENCE = 7;
    public static final int MESSAGE_STATE_CHECK_METER_BT_DISTENCE_FAIL = 8;
    public static final int MESSAGE_STATE_NOT_SUPPORT_METER = 9;
    public static final int MESSAGE_STATE_NOT_CONNECT_SERIAL_PORT = 10;
    public static final int MESSAGE_STATE_SCANED_DEVICE = 11;
    
	// Tag and Debug flag
    private static final boolean DEBUG = true;
    private static final String TAG = "PCLinkLibraryCommuTestActivityForKNV";

    // Views
    private ProgressDialog mProcessDialog = null;
    
    private Button mBtnBack;
    private TextView mTVDate;
    private TextView mTVUser;
    private TextView mTVGender;
    private TextView mTVAge;
    private TextView mTVHeight;
    private TextView mTVWeight;
    private TextView mTVBfr;
    private TextView mTVBm;
    private TextView mTVMc;
    private TextView mTVMr;
    private TextView mTVSw;
    
    // Handlers
    // The Handler that gets information back from the android bluetooth connection
    private final Handler mBTConnectionHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case PCLinkLibraryConstant.MESSAGE_STATE_CHANGE:
                        if (DEBUG) {
                            Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                        } /* end of if */
                        switch (msg.arg1) {
                            case AndroidBluetoothConnection.STATE_CONNECTED_BY_LISTEN_MODE:
                            	try {
                            		mTaiDocMeter = MeterManager.detectConnectedMeter(mConnection);
                            	}
                                catch (Exception e) {
                                	throw new NotSupportMeterException();
                                }
                                dimissProcessDialog();                                
                                if (mTaiDocMeter == null) {
                                    throw new NotSupportMeterException();
                                }/* end of if */
                                break;
                            case AndroidBluetoothConnection.STATE_CONNECTING:
                                // 暫無需特別處理的事項
                                break;
                            case AndroidBluetoothConnection.STATE_SCANED_DEVICE:
                            	meterCommuHandler.sendEmptyMessage(MESSAGE_STATE_SCANED_DEVICE);
                            	break;
                            case AndroidBluetoothConnection.STATE_LISTEN:
                                // 暫無需特別處理的事項
                                break;
                            case AndroidBluetoothConnection.STATE_NONE:
                                // 暫無需特別處理的事項
                                break;
                        } /* end of switch */
                        break;
                    case PCLinkLibraryConstant.MESSAGE_TOAST:
                        // 暫無需特別處理的事項
                        break;
                    default:
                        break;
                } /* end of switch */
            } catch (NotSupportMeterException e) {
                Log.e(TAG, "not support meter", e);
                showAlertDialog(R.string.not_support_meter, true);
            } /* end of try-catch */
        }
    };

    private View.OnClickListener mBackOnClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			GuiUtils.goToPCLinkLibraryHomeActivity(PCLinkLibraryCommuTestActivityForKNV.this);
		}
	};
	
    /**
     * Android BT connection
     */
    private AndroidBluetoothConnection mConnection;
    
    private getStorageDataRecordInterface getKNVRecordHandler = new getStorageDataRecordInterface() {
		
		@Override
		public void onFinish(WeightScaleRecord record) {
			dimissProcessDialog();
			if (record != null) {
				initUI(record);
			}
			else {
				showAlertDialog(R.string.connect_meter_fail, true);
			}
		}
	};
	
    /**
     * 控制Meter連通時以UI互動的Handler
     */
    private final Handler meterCommuHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CONNECTING:
                    mProcessDialog = ProgressDialog.show(PCLinkLibraryCommuTestActivityForKNV.this, null,
                            getString(R.string.connection_meter_and_get_result), true);
                    mProcessDialog.setCancelable(false);
                    break;
                case MESSAGE_STATE_SCANED_DEVICE:
                	// 取得Bluetooth Device資訊
                    final BluetoothDevice device = BluetoothUtil.getPairedDevice(mConnection.getConnectedDeviceAddress());
                    // Attempt to connect to the device
                    mConnection.LeConnect(getApplicationContext(), device);
                    // 確認是否連接上meter，time out 為 10秒 
                    long startConnectTime = System.currentTimeMillis();
                    while (mConnection.getState() != AndroidBluetoothConnection.STATE_CONNECTED) {
                    	try {
                            Thread.sleep(500);
                        }
                        catch (Exception e) {
                        }
                        long conntectTime = System.currentTimeMillis();
                        if ((conntectTime - startConnectTime) > (AndroidBluetoothConnection.BT_CONNECT_TIMEOUT)) {
                        	mConnection.LeDisconnect();
                            //throw new CommunicationTimeoutException();
                        	showAlertDialog(R.string.not_support_meter, true);
                            return;
                        }
                        
                        try {
                            Thread.sleep(500);
                        }
                        catch (Exception e) {
                        	
                        }
                    }
                    
                    mConnection.LeConnected(device);
                    try {
                    	mTaiDocMeter = MeterManager.detectConnectedMeter(mConnection, getKNVRecordHandler);
                    }
                    catch (ExceedRetryTimesException e) {
                    	meterCommuHandler.sendEmptyMessage(MESSAGE_STATE_NOT_SUPPORT_METER);
                    }
                    break;
                case MESSAGE_STATE_CONNECT_DONE:
                    //dimissProcessDialog();
                    break;
                case MESSAGE_STATE_CONNECT_FAIL:
                    dimissProcessDialog();
                    showAlertDialog(R.string.connect_meter_fail, true);
                    break;
                case MESSAGE_STATE_CONNECT_NONE:
                	//showAlertDialog(PCLinkLibraryCommuTestActivity.this, "00000");
                    dimissProcessDialog();
                    GuiUtils.goToPCLinkLibraryHomeActivity(PCLinkLibraryCommuTestActivityForKNV.this);
                    break;
                case MESSAGE_STATE_CONNECT_METER_SUCCESS:
                	showAlertDialog(R.string.connect_meter_success, false);
                    break;
                case MESSAGE_STATE_CHECK_METER_BT_DISTENCE:
                    ProgressDialog baCmdDialog = new ProgressDialog(
                            PCLinkLibraryCommuTestActivityForKNV.this);
                    baCmdDialog.setCancelable(false);
                    baCmdDialog.setMessage("send ba command");
                    baCmdDialog.setButton(DialogInterface.BUTTON_POSITIVE, "cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // Use either finish() or return() to either close the activity
                                    // or just
                                    // the dialog
                                    dialog.dismiss();
                                    return;
                                }
                            });
                    baCmdDialog.show();
                    break;
                case MESSAGE_STATE_CHECK_METER_BT_DISTENCE_FAIL:
                	showAlertDialog(R.string.check_bt_fail, true);
                    break;
                case MESSAGE_STATE_NOT_SUPPORT_METER:
                	showAlertDialog(R.string.not_support_meter, true);
                    break;
                case MESSAGE_STATE_NOT_CONNECT_SERIAL_PORT:
                	showAlertDialog(R.string.not_connect_serial_port, true);
                    break;
            } /* end of switch */
        }
    };

    private String mMacAddress;
    private AbstractMeter mTaiDocMeter = null;
    private boolean mTurnOffMeterClick = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.knv_layout);
        getSharedPreferencesSettings();
        findViews();
        setListener();
    }

    @Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		//super.onBackPressed();
    	
    	if (mProcessDialog == null || !mProcessDialog.isShowing()) {
    		GuiUtils.goToPCLinkLibraryHomeActivity(PCLinkLibraryCommuTestActivityForKNV.this);
    	}
	}

	private void findViews() {
    	mBtnBack = (Button) findViewById(R.id.btn_back);
    	mTVDate = (TextView) findViewById(R.id.tv_date);
    	mTVUser = (TextView) findViewById(R.id.tv_user);
        mTVGender = (TextView) findViewById(R.id.tv_gender);
        mTVAge = (TextView) findViewById(R.id.tv_age);
        mTVHeight = (TextView) findViewById(R.id.tv_height);
        mTVWeight = (TextView) findViewById(R.id.tv_weight);
        mTVBfr = (TextView) findViewById(R.id.tv_bfr);
        mTVBm = (TextView) findViewById(R.id.tv_bm);
        mTVMc = (TextView) findViewById(R.id.tv_mc);
        mTVMr = (TextView) findViewById(R.id.tv_mr);
        mTVSw = (TextView) findViewById(R.id.tv_sw);
    }
    
    private void setListener() {
    	mBtnBack.setOnClickListener(mBackOnClickListener);
    }
    
    private void initUI(final WeightScaleRecord record) {
    		this.runOnUiThread(new Runnable() {
    	        @Override
    	        public void run() {
    	        	try {
	    	        	DecimalFormat df = new DecimalFormat("#.#");
	    		    	SimpleDateFormat formatterDate = new SimpleDateFormat("yyyy/MM/dd hh:mm aa");
	    		    	String measurementDate = formatterDate.format(record.getMeasureTime());
	    		    	mTVDate.setText(measurementDate);
	    		    	mTVUser.setText(String.valueOf(record.getUser()));
	    		        mTVGender.setText(record.getGender().getValue() == 0 ? "Female" : "Male");
	    		        mTVAge.setText(String.valueOf(record.getAge()));
	    		        mTVHeight.setText(String.valueOf(record.getHeight()));
	    		        mTVWeight.setText(df.format(record.getWeight()));
	    		        mTVBfr.setText(String.valueOf(record.getBf()));
	    		        mTVBm.setText(String.valueOf(record.getBmr()));
	    		        mTVMc.setText(String.valueOf(record.getMoistureContent()));
	    		        mTVMr.setText(String.valueOf(record.getMuscleRatio()));
	    		        mTVSw.setText(String.valueOf(record.getSkeletonWeight()));
    	        	} catch (Exception e) {
    	        		int ccc = 3;
    	        		ccc = 4;
    	        	}
    	        }//public void run() {
    		});
    }
    
    private void updatePairedList() {
    	Map<String, String> addrs = new HashMap<String, String>();
    	String addrKey = PCLinkLibraryDemoConstant.BLE_PAIRED_METER_ADDR_ + String.valueOf(0);
		addrs.put(addrKey, mMacAddress);
		mConnection.updatePairedList(addrs, 1);
    }
    
    /**
     * Connect Meter
     */
    private void connectMeter() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                try {
                	meterCommuHandler.sendEmptyMessage(MESSAGE_STATE_CONNECTING);
                    
            		updatePairedList();
            		if (mConnection.getState() == AndroidBluetoothConnection.STATE_NONE) {
                        // Start the Android Bluetooth connection services to listen mode
                        mConnection.LeListen();
                        
                        if (DEBUG) {
                            Log.i(TAG, "into listen mode");
                        }
                    }
                } catch (CommunicationTimeoutException e) {
                    Log.e(TAG, e.getMessage(), e);
                    meterCommuHandler.sendEmptyMessage(MESSAGE_STATE_CONNECT_FAIL);
                } catch (NotSupportMeterException e) {
                    Log.e(TAG, "not support meter", e);
                    meterCommuHandler.sendEmptyMessage(MESSAGE_STATE_NOT_SUPPORT_METER);
                } catch (NotConnectSerialPortException e) {
                	meterCommuHandler.sendEmptyMessage(MESSAGE_STATE_NOT_CONNECT_SERIAL_PORT);
                } catch (ExceedRetryTimesException e) {
                	meterCommuHandler.sendEmptyMessage(MESSAGE_STATE_NOT_SUPPORT_METER);
                } finally {
                    meterCommuHandler.sendEmptyMessage(MESSAGE_STATE_CONNECT_DONE);
                }
                Looper.loop();
            }
        }).start();
    }

    /**
     * // 關閉Process dialog
     */
    private void dimissProcessDialog() {
        if (mProcessDialog != null) {
            mProcessDialog.dismiss();
            mProcessDialog = null;
        } /* end of if */
    }

    /**
     * 關閉Meter
     */
    private void disconnectMeter() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                try {
                    mConnection.LeDisconnect();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                } finally {
                }/* end of try-catch-finally */
                Looper.loop();
            }
        }).start();
    }

    /**
     * 取得共用設定值
     */
    private void getSharedPreferencesSettings() {
        SharedPreferences settings = getSharedPreferences(
                PCLinkLibraryDemoConstant.SHARED_PREFERENCES_NAME, 0);
        mMacAddress = settings.getString(PCLinkLibraryDemoConstant.PAIR_METER_MAC_ADDRESS, "");
    }

    /**
     * 初始化 Android Bluetooth Connection
     */
    private void setupAndroidBluetoothConnection() {
        if (mConnection == null) {
            Log.d(TAG, "setupAndroidBluetoothConnection()");
            // 這裡一定要用一個try-catch, 因為在4.3以前是無法用ble的,會造成runtime error
    		try {
    			mConnection = ConnectionManager.createAndroidBluetoothConnection(mBTConnectionHandler);
    			mConnection.canScanV3KNV(false);
    		}
    		catch (Exception ee) {
    		}
    		
        } /* end of if */
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
        if ("".equals(mMacAddress)) {
        	// 如果是用listen且meter支援ble的話則進入
        	if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
        		setupAndroidBluetoothConnection();
	            connectMeter();
        	}
        	else {
        		showAlertDialog(R.string.pair_meter_first, true);
        	}
        }
        else if (mTaiDocMeter == null) {
            setupAndroidBluetoothConnection();
            connectMeter();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        disconnectMeter();
        dimissProcessDialog();
    }

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	
	/**
     * Show no title alert dialog
     * 
     * @param messageConntentRStringId
     *            the R string Id of message content
     */
    private void showAlertDialog(int messageConntentRStringId, final boolean hasOnClinkListener) {

        new AlertDialog.Builder(PCLinkLibraryCommuTestActivityForKNV.this)
                .setMessage(messageConntentRStringId)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (hasOnClinkListener) {
                            // Back to the home menu
                        	GuiUtils.goToPCLinkLibraryHomeActivity(PCLinkLibraryCommuTestActivityForKNV.this);
                        } /* end of if */
                    };
                }).show();
    }
}
