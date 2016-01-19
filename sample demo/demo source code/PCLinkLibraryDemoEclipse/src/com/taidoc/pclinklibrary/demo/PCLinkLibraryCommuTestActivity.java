/*
 * Copyright (C) 2012 TaiDoc Technology Corporation. All rights reserved.
 */

package com.taidoc.pclinklibrary.demo;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.taidoc.pclinklibrary.android.bluetooth.util.BluetoothUtil;
import com.taidoc.pclinklibrary.connection.AndroidBluetoothConnection;
import com.taidoc.pclinklibrary.connection.AndroidBluetoothConnection.LeConnectedListener;
import com.taidoc.pclinklibrary.connection.PL2303Connection;
import com.taidoc.pclinklibrary.connection.util.ConnectionManager;
import com.taidoc.pclinklibrary.constant.PCLinkLibraryConstant;
import com.taidoc.pclinklibrary.constant.PCLinkLibraryEnum.GenderType;
import com.taidoc.pclinklibrary.constant.PCLinkLibraryEnum.User;
import com.taidoc.pclinklibrary.demo.constant.PCLinkLibraryDemoConstant;
import com.taidoc.pclinklibrary.demo.util.GuiUtils;
import com.taidoc.pclinklibrary.exceptions.CommunicationTimeoutException;
import com.taidoc.pclinklibrary.exceptions.ExceedRetryTimesException;
import com.taidoc.pclinklibrary.exceptions.NotConnectSerialPortException;
import com.taidoc.pclinklibrary.exceptions.NotSupportMeterException;
import com.taidoc.pclinklibrary.meter.AbstractMeter;
import com.taidoc.pclinklibrary.meter.TD4283;
import com.taidoc.pclinklibrary.meter.record.AbstractRecord;
import com.taidoc.pclinklibrary.meter.record.BloodGlucoseRecord;
import com.taidoc.pclinklibrary.meter.record.BloodPressureRecord;
import com.taidoc.pclinklibrary.meter.record.SpO2Record;
import com.taidoc.pclinklibrary.meter.record.TemperatureRecord;
import com.taidoc.pclinklibrary.meter.record.WeightScaleRecord;
import com.taidoc.pclinklibrary.meter.util.MeterManager;

/**
 * PCLinkLibrary command test activity
 * 
 * @author Jay Lee
 * 
 */
public class PCLinkLibraryCommuTestActivity extends ListActivity {
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
    private static final String TAG = "PCLinkLibraryCommuTestActivity";

    // Views
    private ProgressDialog mProcessDialog = null;

    private volatile Thread mBACmdThread;

    private LeConnectedListener mLeConnectedListener = new LeConnectedListener() {

    	@Override
		public void onConnectionStateChange_Disconnect(BluetoothGatt gatt,
				int status, int newState) {
			mConnection.LeDisconnect();
        	showAlertDialog(R.string.not_support_meter, true);
		}

		@SuppressLint("NewApi")
		@Override
		public void onDescriptorWrite_Complete(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {
			mConnection.LeConnected(gatt.getDevice());
		}

		@Override
		public void onCharacteristicChanged_Notify(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    
                    try {
                    	mTaiDocMeter = MeterManager.detectConnectedMeter(mConnection);
                    }
                    catch (Exception e) {
                    	if (PCLinkLibraryDemoConstant.PL2303_TRANSFER_TYPE.equals(mBtTransferType)) {
                    		meterCommuHandler.sendEmptyMessage(MESSAGE_STATE_NOT_CONNECT_SERIAL_PORT);
                    	}
                    	else {
                    		meterCommuHandler.sendEmptyMessage(MESSAGE_STATE_NOT_SUPPORT_METER);
                    	}
                    }
                    
                    PCLinkLibraryCommuTestActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        	dimissProcessDialog();
                            if (mTaiDocMeter == null) {
                                //throw new NotSupportMeterException();
                                meterCommuHandler.sendEmptyMessage(MESSAGE_STATE_NOT_SUPPORT_METER);
                            }
                        }
                    });
                    
                    Looper.loop();
                }
            }).start();
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			// TODO Auto-generated method stub
			
		}
    };
    
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

    /**
     * BT Transfer type, Type I, Type II or PL2303
     */
    private String mBtTransferType;
    
    /**
     * PL2303 BaudRate
     */
    private String mBaudRate;

    /**
     * Command array in list view title
     */
    private String[] mCommandTitles = { "Get Project Code"/*"Get Cmd72"*/, "Get Meter Serial Number",
            "Get Meter Storage Count", "Get Latest Measurement Record", "Get Meter System Clock",
            "Set Meter System Clock", "Clear records", "Get Battery Status", "Turn(Power) Off Meter",
            "Send BA Command to Meter"/*, "Disable Always On"*/ };

    /**
     * Android BT connection
     */
    private AndroidBluetoothConnection mConnection;

    /**
     * PL2303 connection
     */
    private PL2303Connection mPL2303Connection;
    
    private boolean mBLEMode;
    private boolean mKNVMode;
    
    /**
     * 控制Meter連通時以UI互動的Handler
     */
    private final Handler meterCommuHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CONNECTING:
                    mProcessDialog = ProgressDialog.show(PCLinkLibraryCommuTestActivity.this, null,
                            getString(R.string.connection_meter_and_get_result), true);
                    mProcessDialog.setCancelable(false);
                    break;
                case MESSAGE_STATE_SCANED_DEVICE:
                	// 取得Bluetooth Device資訊
                    final BluetoothDevice device = BluetoothUtil.getPairedDevice(mConnection.getConnectedDeviceAddress());
                    // Attempt to connect to the device
                    mConnection.LeConnect(getApplicationContext(), device);
                    // 在mLeConnectedListener會收
                    break;
                case MESSAGE_STATE_CONNECT_DONE:
                    dimissProcessDialog();
                    break;
                case MESSAGE_STATE_CONNECT_FAIL:
                    dimissProcessDialog();
                    showAlertDialog(R.string.connect_meter_fail, true);
                    break;
                case MESSAGE_STATE_CONNECT_NONE:
                	//showAlertDialog(PCLinkLibraryCommuTestActivity.this, "00000");
                    dimissProcessDialog();
                    if (PCLinkLibraryDemoConstant.PL2303_TRANSFER_TYPE.equals(mBtTransferType)) {
                    	Bundle bundle = new Bundle();
                    	bundle.putBoolean(PCLinkLibraryDemoConstant.FromPL2303, true);
                    	GuiUtils.goToSpecifiedActivity(PCLinkLibraryCommuTestActivity.this,
                    			PCLinkLibraryDemoActivity.class, bundle);
                    }
                    else {                    	
                    	GuiUtils.goToPCLinkLibraryHomeActivity(PCLinkLibraryCommuTestActivity.this);
                    }
                    break;
                case MESSAGE_STATE_CONNECT_METER_SUCCESS:
                    showAlertDialog(R.string.connect_meter_success, false);
                    break;
                case MESSAGE_STATE_CHECK_METER_BT_DISTENCE:
                    ProgressDialog baCmdDialog = new ProgressDialog(
                            PCLinkLibraryCommuTestActivity.this);
                    baCmdDialog.setCancelable(false);
                    baCmdDialog.setMessage("send ba command");
                    baCmdDialog.setButton(DialogInterface.BUTTON_POSITIVE, "cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // Use either finish() or return() to either close the activity
                                    // or just
                                    // the dialog
                                    dialog.dismiss();
                                    mBACmdThread = null;
                                    return;
                                }
                            });
                    baCmdDialog.show();
                    break;
                case MESSAGE_STATE_CHECK_METER_BT_DISTENCE_FAIL:
                    mBACmdThread = null;
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
                
        setAdapter();
        getSharedPreferencesSettings();
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
                    if (PCLinkLibraryDemoConstant.PL2303_TRANSFER_TYPE.equals(mBtTransferType)) {
                    	// Attempt to connect to the device
                    	
                    	mPL2303Connection.connect();                    	
                        // 確認是否連接上meter，time out 為 10秒
                        long startConnectTime = System.currentTimeMillis();
                        while (mPL2303Connection.getState() != PL2303Connection.STATE_CONNECTED) {
                            long conntectTime = System.currentTimeMillis();
                            if ((conntectTime - startConnectTime) > (PL2303Connection.PL2303_CONNECT_TIMEOUT)) {
                                throw new CommunicationTimeoutException();
                            }
                        }
                          
                        try {
                        	mTaiDocMeter = MeterManager.detectConnectedMeter(mPL2303Connection);
                        }
                        catch (Exception e) {
                        	throw new NotSupportMeterException();
                        }
                        
                        if (mTaiDocMeter == null) {
                            throw new NotSupportMeterException();
                        }
                    }
                    else {
	                    // 判斷是以Type One還是Type Two連接Meter
	                    if (PCLinkLibraryDemoConstant.BT_TRANSFER_TYPE_ONE.equals(mBtTransferType)) {
	                        // 取得Bluetooth Device資訊
	                        BluetoothDevice device = BluetoothUtil.getPairedDevice(mMacAddress);
	                        // Attempt to connect to the device
	                        mConnection.connect(device);
	                        // 確認是否連接上meter，time out 為 10秒 
	                        long startConnectTime = System.currentTimeMillis();
	                        while (mConnection.getState() != AndroidBluetoothConnection.STATE_CONNECTED) {
	                        	try {
	                        		Thread.sleep(500);
	                        	}
	                        	catch (InterruptedException e) {
	                        	}
	                            long conntectTime = System.currentTimeMillis();
	                            if ((conntectTime - startConnectTime) > (AndroidBluetoothConnection.BT_CONNECT_TIMEOUT)) {
	                                throw new CommunicationTimeoutException();
	                            }
	                        }
	                        
	                        try {
	                        	mTaiDocMeter = MeterManager.detectConnectedMeter(mConnection);
	                        }
                            catch (Exception e) {
                            	throw new NotSupportMeterException();
                            }
	                        
	                        if (mTaiDocMeter == null) {
	                            throw new NotSupportMeterException();
	                        }
	                    } else {
	                    	if (mBLEMode) {
	                    		updatePairedList();
	                    		mConnection.setLeConnectedListener(mLeConnectedListener);
	                    		
	                    		if (mConnection.getState() == AndroidBluetoothConnection.STATE_NONE) {
		                            // Start the Android Bluetooth connection services to listen mode
		                            mConnection.LeListen();
		                            
		                            if (DEBUG) {
		                                Log.i(TAG, "into listen mode");
		                            }
		                        }
	                    	}
	                    	else {
		                        // Only if the state is STATE_NONE, do we know that we haven't started
		                        // already
		                        if (mConnection.getState() == AndroidBluetoothConnection.STATE_NONE) {
		                            // Start the Android Bluetooth connection services to listen mode
		                            mConnection.listen();
		                            
		                            if (DEBUG) {
		                                Log.i(TAG, "into listen mode");
		                            }
		                        }
	                    	}
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
                	if (PCLinkLibraryDemoConstant.PL2303_TRANSFER_TYPE.equals(mBtTransferType)) {
                		meterCommuHandler.sendEmptyMessage(MESSAGE_STATE_NOT_CONNECT_SERIAL_PORT);
                	}
                	else {
                		meterCommuHandler.sendEmptyMessage(MESSAGE_STATE_NOT_SUPPORT_METER);
                	}
                } finally {
                    if (PCLinkLibraryDemoConstant.BT_TRANSFER_TYPE_ONE.equals(mBtTransferType) ||
                    		PCLinkLibraryDemoConstant.PL2303_TRANSFER_TYPE.equals(mBtTransferType)) {
                        meterCommuHandler.sendEmptyMessage(MESSAGE_STATE_CONNECT_DONE);
                    }
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
    private void disconnectMeter(final boolean isNeedPowerOff) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                try {
                    if (isNeedPowerOff) {
                        mTaiDocMeter.turnOffMeterOrBluetooth(0);
                    } /* end of if */                    
                    if (mBLEMode) {
                    	mConnection.setLeConnectedListener(null);
                    	mConnection.LeDisconnect();
                    }
                    else {
                    	mConnection.disconnect();
                    	mConnection.LeDisconnect();
                    }
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
        mBtTransferType = settings.getString(PCLinkLibraryDemoConstant.BT_TRANSFER_TYPE, "");        
        mBaudRate = settings.getString(PCLinkLibraryDemoConstant.BAUDRATE, "19200");
        mBLEMode = settings.getBoolean(PCLinkLibraryDemoConstant.BLE_MODE, false);
        mKNVMode = settings.getBoolean(PCLinkLibraryDemoConstant.KNV_MODE, false);
    }

    /**
     * setting ListView title
     */
    private void setAdapter() {
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                mCommandTitles));
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
    
    /**
     * 初始化 PL2303 Connection
     */
    private void setupPL2303Connection() {
        if (mPL2303Connection == null) {
            Log.d(TAG, "setupPL2303Connection()");
            // Initialize the PL2303 connection to perform PL2303 connections
            try {
	            mPL2303Connection = ConnectionManager.createPL2303Connection(PCLinkLibraryCommuTestActivity.this, mBTConnectionHandler);            
	            mPL2303Connection.setBaudRate(mBaudRate);
            }
            catch (Exception e) {
            	
            }
            
        } /* end of if */
    }

    public static void showAlertDialog(Context context, String msg) {
	    new AlertDialog.Builder(context)
	    .setMessage(msg)
	    .setPositiveButton(R.string.ok, null).show();
    }
    
    /**
     * Show no title alert dialog
     * 
     * @param messageConntentRStringId
     *            the R string Id of message content
     */
    private void showAlertDialog(int messageConntentRStringId, final boolean hasOnClinkListener) {

        new AlertDialog.Builder(PCLinkLibraryCommuTestActivity.this)
                .setMessage(messageConntentRStringId)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (hasOnClinkListener) {
                            // Back to the home menu
                        	if (PCLinkLibraryDemoConstant.PL2303_TRANSFER_TYPE.equals(mBtTransferType)) {
                            	Bundle bundle = new Bundle();
                            	bundle.putBoolean(PCLinkLibraryDemoConstant.FromPL2303, false);
                            	GuiUtils.goToSpecifiedActivity(PCLinkLibraryCommuTestActivity.this,
                            			PCLinkLibraryDemoActivity.class, bundle);
                            }
                            else {                    	
                            	GuiUtils.goToPCLinkLibraryHomeActivity(PCLinkLibraryCommuTestActivity.this);
                            }
                        } /* end of if */
                    };
                }).show();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final int clickItem = position;
        if (clickItem != 9) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    try {
                        meterCommuHandler.sendEmptyMessage(MESSAGE_STATE_CONNECTING);

                        AlertDialog.Builder builder = null;
                        AlertDialog alertDialog = null;
                        LayoutInflater inflater = (LayoutInflater) PCLinkLibraryCommuTestActivity.this
                                .getSystemService(LAYOUT_INFLATER_SERVICE);
                        View layout = null;
                        // Date Format
                        SimpleDateFormat formatterDate = new SimpleDateFormat("yyyy/MM/dd hh:mm aa");
                        mTurnOffMeterClick = false;
                        switch (clickItem) {
                            case 0:
                                // Get Project Code
                            	/*GeneralWeightScaleMeter meter = (GeneralWeightScaleMeter)mTaiDocMeter;
                            	int rxCmd[] = meter.SetWeightScaleUserProfile(
                            			mConnection, 2, GenderType.Male, 0xa1, 15, 0);*/
                            	
                                String projectCode = mTaiDocMeter.getDeviceModel().getProjectCode();
                                layout = inflater.inflate(R.layout.get_project_code,
                                        (ViewGroup) findViewById(R.id.projectCodeLayout));
                                TextView viewProjectCode = (TextView) layout
                                        .findViewById(R.id.projectCode);
                                
                                viewProjectCode.setText(projectCode);
                                
                                /*if (rxCmd != null) {
                                	viewProjectCode.setText(String.format(
                                			"%02X, %02X, %02X, %02X, %02X, %02X, %02X, %02X, %02X, %02X, " +
                                			"%02X, %02X",
                                			rxCmd[0], rxCmd[1], rxCmd[2], rxCmd[3], rxCmd[4], rxCmd[5], rxCmd[6], rxCmd[7], rxCmd[8], rxCmd[9],
                                			rxCmd[10], rxCmd[11]));
                                }
                                else {
                                	viewProjectCode.setText("read fail");
                                }*/
                                break;
                            case 1:
                                // Get Meter Serial Number
                                String serialNumber = mTaiDocMeter.getSerialNumberRecord()
                                        .getSerialNumber();
                                layout = inflater.inflate(R.layout.get_serial_number,
                                        (ViewGroup) findViewById(R.id.serialNumberLayout));
                                TextView viewSerialNumber = (TextView) layout
                                        .findViewById(R.id.serialNumber);
                                viewSerialNumber.setText(serialNumber);
                                break;
                            case 2:
                                // Get Meter Storage Count
                                int storageCount = mTaiDocMeter.getStorageNumberAndNewestIndex(
                                        User.CurrentUser).getStorageNumber();
                                layout = inflater.inflate(R.layout.get_storage_count,
                                        (ViewGroup) findViewById(R.id.storageCountLayout));
                                TextView viewStorageCount = (TextView) layout
                                        .findViewById(R.id.storageCount);
                                viewStorageCount.setText(String.valueOf(storageCount));
                                break;
                            case 3:
                                // Get Latest Measurement Record
                                AbstractRecord record = mTaiDocMeter.getStorageDataRecord(0,
                                        User.CurrentUser);
                                layout = inflater
                                        .inflate(
                                                R.layout.get_latest_measurement_record,
                                                (ViewGroup) findViewById(R.id.getLastMeasurementRecordLayout));
                                // Views
                                LinearLayout bgLayout = (LinearLayout) layout
                                        .findViewById(R.id.bgLayout);
                                LinearLayout bpLayout = (LinearLayout) layout
                                        .findViewById(R.id.bpLayout);
                                LinearLayout thermometerLayout = (LinearLayout) layout
                                        .findViewById(R.id.thermometerLayout);                                
                                
                                TextView viewMeasurementDate = (TextView) layout
                                        .findViewById(R.id.measurementDate);
                                TextView viewMeasureType = (TextView) layout
                                        .findViewById(R.id.measurementType);
                                TextView viewBGValue = (TextView) layout.findViewById(R.id.bgValue);
                                TextView viewSysValue = (TextView) layout
                                        .findViewById(R.id.sysValue);
                                TextView viewDiaValue = (TextView) layout
                                        .findViewById(R.id.diaValue);
                                TextView viewPulseValue = (TextView) layout
                                        .findViewById(R.id.pulseValue);
                                TextView viewThermometerValue = (TextView) layout
                                        .findViewById(R.id.thermometerValue);
                                TextView viewIHBValue = (TextView) layout
                                        .findViewById(R.id.ihbValue);
                                
                                // spO2
                                LinearLayout spO2Layout = (LinearLayout) layout
                                        .findViewById(R.id.spO2Layout);
                                TextView viewSpO2Value = (TextView) layout
                                        .findViewById(R.id.spO2Value);
                                TextView viewSpO2PulseValue = (TextView) layout
                                        .findViewById(R.id.spO2PulseValue);
                                                                
                                // Weight
                                LinearLayout weightLayout = (LinearLayout) layout
                                        .findViewById(R.id.weightScaleLayout);
                                TextView viewAgeValue = (TextView) layout
                                        .findViewById(R.id.ageValue);
                                TextView viewGenderValue = (TextView) layout
                                        .findViewById(R.id.genderValue);
                                TextView viewHeightValue = (TextView) layout
                                        .findViewById(R.id.heightValue);
                                TextView viewWeightValue = (TextView) layout
                                        .findViewById(R.id.weightValue);
                                TextView viewBMIValue = (TextView) layout
                                        .findViewById(R.id.bmiValue);
                                TextView viewBMRValue = (TextView) layout
                                        .findViewById(R.id.bmrValue);
                                TextView viewBFValue = (TextView) layout.findViewById(R.id.bfValue);

                                // Convert value and set views
                                if (record instanceof BloodGlucoseRecord) {
                                    // convert value
                                    String measurementDate = formatterDate
                                            .format(((BloodGlucoseRecord) record).getMeasureTime());
                                    int bgValue = ((BloodGlucoseRecord) record).getGlucoseValue();
                                    // set views
                                    bgLayout.setVisibility(View.VISIBLE);
                                    bpLayout.setVisibility(View.GONE);
                                    thermometerLayout.setVisibility(View.GONE);
                                    weightLayout.setVisibility(View.GONE);
                                    viewMeasurementDate.setText(measurementDate);
                                    viewMeasureType.setText("BG");
                                    viewBGValue.setText(String.valueOf(bgValue));
                                } else if (record instanceof BloodPressureRecord) {
                                    // Convert value
                                    int sysValue = ((BloodPressureRecord) record)
                                            .getSystolicValue();
                                    int diaValue = ((BloodPressureRecord) record)
                                            .getDiastolicValue();
                                    int pulseValue = ((BloodPressureRecord) record).getPulseValue();
                                    int ihbValue = ((BloodPressureRecord) record).getIHB().getValue();
                                    
                                    String measurementDate = formatterDate
                                            .format(((BloodPressureRecord) record).getMeasureTime());
                                    // Set views
                                    bgLayout.setVisibility(View.GONE);
                                    bpLayout.setVisibility(View.VISIBLE);
                                    thermometerLayout.setVisibility(View.GONE);
                                    weightLayout.setVisibility(View.GONE);
                                    spO2Layout.setVisibility(View.GONE);
                                    viewMeasurementDate.setText(measurementDate);
                                    viewMeasureType.setText("BP");
                                    viewSysValue.setText(String.valueOf(sysValue));
                                    viewDiaValue.setText(String.valueOf(diaValue));
                                    viewPulseValue.setText(String.valueOf(pulseValue));
                                    viewIHBValue.setText(getResources().getStringArray(R.array.IHBValue)[ihbValue]);
                                } else if (record instanceof TemperatureRecord) {
                                    // Convert value
                                    double thermometerValue = ((TemperatureRecord) record)
                                            .getObjectTemperatureValue();
                                    String measurementDate = formatterDate
                                            .format(((TemperatureRecord) record).getMeasureTime());
                                    // Set views
                                    bgLayout.setVisibility(View.GONE);
                                    bpLayout.setVisibility(View.GONE);
                                    thermometerLayout.setVisibility(View.VISIBLE);
                                    spO2Layout.setVisibility(View.GONE);
                                    weightLayout.setVisibility(View.GONE);
                                    viewMeasurementDate.setText(measurementDate);
                                    viewMeasureType.setText("Thermometer");
                                    DecimalFormat df = new DecimalFormat("#.##");
                                    viewThermometerValue.setText(df.format(thermometerValue));
                                } else if (record instanceof SpO2Record) {
                                	int spO2Value = ((SpO2Record) record).getSpO2();
                                	int pulseValue = ((SpO2Record) record).getPulse();
                                	String measurementDate = formatterDate
                                            .format(((SpO2Record) record).getMeasureTime());
                                	spO2Layout.setVisibility(View.VISIBLE);
                                	bgLayout.setVisibility(View.GONE);
                                    bpLayout.setVisibility(View.GONE);
                                    thermometerLayout.setVisibility(View.GONE);
                                    weightLayout.setVisibility(View.GONE);
                                    viewMeasurementDate.setText(measurementDate);
                                    viewMeasureType.setText("spO2");
                                    viewSpO2Value.setText(String.valueOf(spO2Value));
                                    viewSpO2PulseValue.setText(String.valueOf(pulseValue));
                                } else if (record instanceof WeightScaleRecord) {
                                    // Convert value
                                    GenderType genderType = ((WeightScaleRecord) record)
                                            .getGender();
                                    int age = ((WeightScaleRecord) record).getAge();
                                    int height = ((WeightScaleRecord) record).getHeight();
                                    double weight = ((WeightScaleRecord) record).getWeight();
                                    double bmi = ((WeightScaleRecord) record).getBmi();
                                    int bmr = ((WeightScaleRecord) record).getBmr();
                                    double bf = ((WeightScaleRecord) record).getBf();
                                    String measurementDate = formatterDate
                                            .format(((WeightScaleRecord) record).getMeasureTime());
                                    // Set views
                                    bgLayout.setVisibility(View.GONE);
                                    bpLayout.setVisibility(View.GONE);
                                    thermometerLayout.setVisibility(View.GONE);
                                    spO2Layout.setVisibility(View.GONE);
                                    weightLayout.setVisibility(View.VISIBLE);
                                    viewMeasurementDate.setText(measurementDate);
                                    viewMeasureType.setText("Weight Scale");
                                    // DecimalFormat df = new DecimalFormat("#.##");
                                    viewAgeValue.setText("" + age);
                                    viewGenderValue.setText(genderType.toString());
                                    viewHeightValue.setText("" + height);
                                    viewWeightValue.setText("" + weight);
                                    viewBMIValue.setText("" + bmi);
                                    viewBMRValue.setText("" + bmr);
                                    viewBFValue.setText("" + bf);
                                }/* end of if */
                                break;
                            case 4:
                                // Get Meter System Clock
                                String meterTime = formatterDate.format(mTaiDocMeter
                                        .getSystemClock().getSystemClockTime());
                                layout = inflater.inflate(R.layout.get_meter_system_clock,
                                        (ViewGroup) findViewById(R.id.getMeterSystemClockLayout));
                                TextView viewMeterTimeClock = (TextView) layout
                                        .findViewById(R.id.meterSystemClock);
                                viewMeterTimeClock.setText(meterTime);
                                break;
                            case 5:
                                // Set Meter System Clock
                                // 取得Meter System Clock
                                String beforeMeterTime = formatterDate.format(mTaiDocMeter
                                        .getSystemClock().getSystemClockTime());
                                // 設定Meter System Clock
                                Date nowTime = new Date();
                                mTaiDocMeter.setSystemClock(nowTime);
                                String afterMeterTime = formatterDate.format(mTaiDocMeter
                                        .getSystemClock().getSystemClockTime());
                                layout = inflater.inflate(R.layout.set_meter_system_clock,
                                        (ViewGroup) findViewById(R.id.setMeterSystemClockLayout));
                                TextView viewBerforeMeterTimeClock = (TextView) layout
                                        .findViewById(R.id.beforeMeterSystemClock);
                                viewBerforeMeterTimeClock.setText(beforeMeterTime);
                                TextView viewAfterMeterTimeClock = (TextView) layout
                                        .findViewById(R.id.afterMeterSystemClock);
                                viewAfterMeterTimeClock.setText(afterMeterTime);
                                break;
                            case 6:
                                // Clear records
                                mTaiDocMeter.clearMeasureRecords(User.CurrentUser);
                                layout = inflater.inflate(R.layout.clear_records,
                                        (ViewGroup) findViewById(R.id.clearRecordsLayout));
                                break;
                            case 7:
                                // Get Battery Status
                            	String[] status = getResources().getStringArray(R.array.battery_status);
                                int batteryStatus = mTaiDocMeter.getBatteryStatus();                                
                                layout = inflater.inflate(R.layout.get_battery_status,
                                        (ViewGroup) findViewById(R.id.projectCodeLayout));
                                TextView viewBatteryStatus = (TextView) layout
                                        .findViewById(R.id.batteryStatus);
                                viewBatteryStatus.setText(String.format("%s (%d)", status[batteryStatus], batteryStatus));
                                break;
                            case 8:
                                // Turn(Power) Off Meter
                                layout = inflater.inflate(R.layout.turn_off_meter,
                                        (ViewGroup) findViewById(R.id.turnOffMeterLayout));
                                mTurnOffMeterClick = true;
                                mTaiDocMeter.turnOffMeterOrBluetooth(0);
                                
                                if (PCLinkLibraryDemoConstant.PL2303_TRANSFER_TYPE.equals(mBtTransferType)) {
                                	mPL2303Connection.disconnect();
                                }
                                else {                                	
                                	if (mBLEMode) {
                                		mConnection.LeDisconnect();
                                	}
                                	else {
                                		mConnection.disconnect();
                                		mConnection.LeDisconnect();
                                	}
                                }
                                break;
                            /*case 10:
                                // Disable Always On
                            	if (mBLEMode) {
                            		boolean result = mTaiDocMeter.disableBPAlwaysOn();
	                                layout = inflater.inflate(R.layout.disable_always_on,
	                                        (ViewGroup) findViewById(R.id.disableAlwaysonLayout));
	                                TextView tvResult = (TextView)layout.findViewById(R.id.tv_result);
	                                if (!result) {
	                                	tvResult.setText(getResources().getString(R.string.disable_alwayson_fail));
	                                }
                            	}
                                break;*/
                        } /* end of switch */

                        // 產生alert dialog

                        builder = new AlertDialog.Builder(PCLinkLibraryCommuTestActivity.this);
                        builder.setView(layout);
                        builder.setPositiveButton(getString(R.string.ok), new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mTurnOffMeterClick) {
                                	if (PCLinkLibraryDemoConstant.PL2303_TRANSFER_TYPE.equals(mBtTransferType)) {
                                    	Bundle bundle = new Bundle();
                                    	bundle.putBoolean(PCLinkLibraryDemoConstant.FromPL2303, true);
                                    	GuiUtils.goToSpecifiedActivity(PCLinkLibraryCommuTestActivity.this,
                                    			PCLinkLibraryDemoActivity.class, bundle);
                                    }
                                    else {                    	
                                    	GuiUtils.goToPCLinkLibraryHomeActivity(PCLinkLibraryCommuTestActivity.this);
                                    }
                                } /* end of if */
                            }
                        });
                        builder.setCancelable(false);
                        alertDialog = builder.create();
                        alertDialog.show();

                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage(), e);
                        meterCommuHandler.sendEmptyMessage(MESSAGE_STATE_CONNECT_FAIL);
                    } finally {
                        meterCommuHandler.sendEmptyMessage(MESSAGE_STATE_CONNECT_DONE);
                    } /* end of try-catch-finally */
                    Looper.loop();
                }
            }).start();
        } else {
            mBACmdThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    Looper.prepare();
                    meterCommuHandler.sendEmptyMessage(MESSAGE_STATE_CHECK_METER_BT_DISTENCE);

                    Thread thisThread = Thread.currentThread();
                    while (thisThread == mBACmdThread) {
                        try {
                            ((TD4283) mTaiDocMeter).sendBACommand();
                            Thread.sleep(500);
                        } catch (Exception e) {
                            if (e.getMessage().contains("Broken pipe")
                                    || e.getMessage().contains("Operation Canceled")) {
                                if (mBLEMode) {
                            		mConnection.LeDisconnect();
                            	}
                            	else {
                            		mConnection.disconnect();
                            		mConnection.LeDisconnect();
                            	}
                                
                                meterCommuHandler
                                        .sendEmptyMessage(MESSAGE_STATE_CHECK_METER_BT_DISTENCE_FAIL);
                            } /* end of if */
                        } /* end of try-catch */
                    } /* end of while */
                    Looper.loop();
                }
            });

            if (mBACmdThread != null) {
                mBACmdThread.start();
            } /* end of if */
        } /* end of if */

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (PCLinkLibraryDemoConstant.PL2303_TRANSFER_TYPE.equals(mBtTransferType)) {
        	setupPL2303Connection();
        	connectMeter();        	
        }
        else {
	        if ("".equals(mMacAddress)) {
	        	// 如果是用listen且meter支援ble的話則進入
	        	if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
	        		setupAndroidBluetoothConnection();
		            connectMeter();
	        	}
	        	else {
	        		showAlertDialog(R.string.pair_meter_first, true);
	        	}
	        } else if ("".equals(mBtTransferType)) {
	            showAlertDialog(R.string.meter_trans_type_fail, true);
	        } else if (mTaiDocMeter == null) {
	            setupAndroidBluetoothConnection();
	            connectMeter();
	        }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        disconnectMeter(true);
        dimissProcessDialog();
    }

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
}
