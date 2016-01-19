/*
 * Copyright (C) 2012 TaiDoc Technology Corporation. All rights reserved.
 */

package com.taidoc.pclinklibrary.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.taidoc.pclinklibrary.android.bluetooth.util.BluetoothUtil;
import com.taidoc.pclinklibrary.demo.constant.PCLinkLibraryDemoConstant;
import com.taidoc.pclinklibrary.demo.preference.MeterPreferenceDialog;
import com.taidoc.pclinklibrary.demo.util.GuiUtils;
import com.taidoc.pclinklibrary.util.LogUtil;

public class PCLinkLibraryDemoActivity extends FragmentActivity {
    // Views
	private LinearLayout mPL2303Layout;
	private LinearLayout mBTLayout;
	private Spinner mBaudRate;
	private Button mPL2303Connect;
	
    private Spinner mSelectedMeter;
    private Button mConnect;
    private Button mBLEPair;
    private Button mAudio;
    private RadioGroup mMeterTransferType;
    
    private MeterPreferenceDialog meterDialog;
    
    private BroadcastReceiver detachReceiver;
    
    private boolean mBLEMode;
    private boolean mKNVMode;
    private int mPairedMeterCount;
    private Map<String, String> mPairedMeterNames;
    private Map<String, String> mPairedMeterAddrs;
    
    private static final String ACTION_USB_PERMISSION = "com.taidoc.pclinklibrary.demo.USB_PERMISSION";
    
    // Listeners
    private OnItemSelectedListener mSelectedMeterOnItemSelectedListener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position,
				long id) {
			
			TextView view = (TextView)selectedItemView;
			if (view != null) {
				int indexOfBLE = isBLEMeter(view.getText().toString());
				if (indexOfBLE != -1) {
					mMeterTransferType.check(R.id.meterBtType2RadioButton);
					for (View v : mMeterTransferType.getTouchables()) {
						v.setEnabled(false);
					}
					
					mBLEMode = true;
					mKNVMode = view.getText().toString().toLowerCase().contains("knv v125");
				}
				else {
					for (int i=0; i<mMeterTransferType.getChildCount(); i++) {
						View v = mMeterTransferType.getChildAt(i);
						v.setEnabled(true);
					}
					
					mBLEMode = false;
					mKNVMode = false;
				}
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> parentView) {
		}
	};
	
    private Button.OnClickListener mConnectOnClickListener = new Button.OnClickListener() {

		@Override
		public void onClick(View v) {
			// 設定選擇裝置
            if (mSelectedMeter.getSelectedItem() != null) {
                String deviceMac = mSelectedMeter.getSelectedItem().toString().split("/")[1];
                if (!"".equals(deviceMac)) {
                    // Set pair device mac address
                    SharedPreferences settings = getSharedPreferences(
                            PCLinkLibraryDemoConstant.SHARED_PREFERENCES_NAME, 0);

                    settings.edit()
                            .putString(PCLinkLibraryDemoConstant.PAIR_METER_MAC_ADDRESS,
                                    deviceMac).commit();

                    // 設定BT Transfer Type
                    if (mMeterTransferType.getCheckedRadioButtonId() == R.id.meterBtType1RadioButton) {
                        // Type One
                    	//bleMode
                    	settings.edit()
                                .putString(PCLinkLibraryDemoConstant.BT_TRANSFER_TYPE,
                                        PCLinkLibraryDemoConstant.BT_TRANSFER_TYPE_ONE)
                                .putBoolean(PCLinkLibraryDemoConstant.BLE_MODE, false)
                                .putBoolean(PCLinkLibraryDemoConstant.KNV_MODE, false)
                                .commit();
                    } else {
                        // Type Two
                        settings.edit()
                                .putString(PCLinkLibraryDemoConstant.BT_TRANSFER_TYPE,
                                        PCLinkLibraryDemoConstant.BT_TRANSFER_TYPE_TWO)
                                .putBoolean(PCLinkLibraryDemoConstant.BLE_MODE, mBLEMode)
                                .putBoolean(PCLinkLibraryDemoConstant.KNV_MODE, mKNVMode)
                                
                                .commit();
                    } /* end of if */

                    // Go to the test command activity
                    if (mKNVMode) {
                    	GuiUtils.goToSpecifiedActivity(PCLinkLibraryDemoActivity.this,
                    			PCLinkLibraryCommuTestActivityForKNV.class);
                    }
                    else {
                    	GuiUtils.goToSpecifiedActivity(PCLinkLibraryDemoActivity.this,
                    			PCLinkLibraryCommuTestActivity.class);
                    }
                } /* end of if */
            } else {
                new AlertDialog.Builder(PCLinkLibraryDemoActivity.this)
                        .setMessage(R.string.bluetooth_need_to_pair)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Go to the setting page
                                startActivityForResult(new Intent(
                                        android.provider.Settings.ACTION_BLUETOOTH_SETTINGS), 0);
                            };
                        }).show();
            } /* end of if */
		}
    };
    
    private Button.OnClickListener mBLEPairOnClickListener = new Button.OnClickListener() {

		@Override
		public void onClick(View v) {
			ShowMeterDialog();
		}
    };
    
    private Button.OnClickListener mAudioOnClickListener = new Button.OnClickListener() {

		@Override
		public void onClick(View v) {
			GuiUtils.goToSpecifiedActivity(PCLinkLibraryDemoActivity.this,
					AudioMonitorActivity.class);
		}
    };
    
    
    
    private Button.OnClickListener mPL2303ConnectOnClickListener = new Button.OnClickListener() {

		@Override
		public void onClick(View v) {			
			SharedPreferences settings = getSharedPreferences(
                    PCLinkLibraryDemoConstant.SHARED_PREFERENCES_NAME, 0);
			
			settings.edit()
            .putString(PCLinkLibraryDemoConstant.BT_TRANSFER_TYPE,
                    PCLinkLibraryDemoConstant.PL2303_TRANSFER_TYPE)
            .commit();
			
			settings.edit()
            .putString(PCLinkLibraryDemoConstant.BAUDRATE,
                    mBaudRate.getSelectedItem().toString())
            .commit();
			
			GuiUtils.goToSpecifiedActivity(PCLinkLibraryDemoActivity.this,
                    PCLinkLibraryCommuTestActivity.class);
		}
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_paired_device_and_type);
        
        findViews();
        setListener();
        
        clearSharePreferences();                
    }
    
    /**
     * 清除配對設定
     */
    private void clearSharePreferences() {
        // set pair device mac address
        SharedPreferences settings = getSharedPreferences(
                PCLinkLibraryDemoConstant.SHARED_PREFERENCES_NAME, 0);
        settings.edit().remove(PCLinkLibraryDemoConstant.PAIR_METER_MAC_ADDRESS).commit();
    }

    /**
     * 設定此Activity會用到的View
     */
    private void findViews() {
    	
    	mPL2303Layout = (LinearLayout) findViewById(R.id.ll_top);
    	mBTLayout = (LinearLayout) findViewById(R.id.ll_body);
    	
    	mBaudRate = (Spinner) findViewById(R.id.spBaudRate);
    	mPL2303Connect = (Button) findViewById(R.id.btnPL2303ConnectMeter);
    	
        mSelectedMeter = (Spinner) findViewById(R.id.pairDeviceList);
        mConnect = (Button) findViewById(R.id.btnConnectMeter);
        mBLEPair = (Button) findViewById(R.id.btnPair);
        mAudio = (Button) findViewById(R.id.btnAudio);
        mMeterTransferType = (RadioGroup) findViewById(R.id.meterBtTransferTypeRadioGroup);
    }

    /**
     * 設定此Activity會用到的Listener
     */
    private void setListener() {
    	mConnect.setOnClickListener(mConnectOnClickListener);
    	mBLEPair.setOnClickListener(mBLEPairOnClickListener);
    	mAudio.setOnClickListener(mAudioOnClickListener);    	
    	mSelectedMeter.setOnItemSelectedListener(mSelectedMeterOnItemSelectedListener);
    	mPL2303Connect.setOnClickListener(mPL2303ConnectOnClickListener);
    }
    
    /**
     * 搜尋已配對的Bluetooth裝置，並將找到的裝置顯示在UI上面
     * 
     */
    public void queryBluetoothDevicesAndSetToUi() {

        new Thread(new Runnable() {
            private Handler handler = new Handler();
            private ProgressDialog processDialog;

            @Override
            public void run() {
                Looper.prepare();
                try {
                    final List<String> remoteDeviceNameList = new ArrayList<String>();
                    final ArrayAdapter<String> selectedMeterSpinnerAdapter = new ArrayAdapter<String>(
                            PCLinkLibraryDemoActivity.this, android.R.layout.simple_spinner_item,
                            remoteDeviceNameList);
                    selectedMeterSpinnerAdapter
                            .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            processDialog = ProgressDialog.show(PCLinkLibraryDemoActivity.this,
                                    null, getString(R.string.start_search_meter), true);
                            mSelectedMeter.setAdapter(selectedMeterSpinnerAdapter);
                        }
                    });

                    List<BluetoothDevice> remoteDeviceList = BluetoothUtil.getPairedDevices();

                    for (int i = 0; i < remoteDeviceList.size(); i++) {
                        String remoteDeviceName = remoteDeviceList.get(i).getName() + "/"
                                + remoteDeviceList.get(i).getAddress();
                        remoteDeviceNameList.add(remoteDeviceName);
                    } /* end of for */
                    
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                        	int adapterCount = initBLEMeters();
                        	
                        	adapterCount += selectedMeterSpinnerAdapter.getCount();
                        	
                        	if (adapterCount > 0) {
                                selectedMeterSpinnerAdapter.notifyDataSetChanged();
                                mSelectedMeter.setSelection(0);
                                
                                mSelectedMeterOnItemSelectedListener.onItemSelected(mSelectedMeter,
                                		mSelectedMeter.getChildAt(0), 0, 0);
                            } else {
                            	// 如果手機不支援ble, 則要求user一定要在設定頁先設定
                            	if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                            		showNeeToPairMessageDialog();
                            	}
                            } /* end of if */
                        }
                    }, 1000);
                } catch (Exception e) {
                    LogUtil.error(PCLinkLibraryDemoActivity.class, e.getMessage(), e);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            showErrorMessageDialog();
                        }
                    });
                } finally {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (processDialog != null) {
                                processDialog.dismiss();
                            } /* end of if */
                        }
                    });
                }
                Looper.loop();
            }

            /**
             * 顯示錯誤訊息
             */
            private void showErrorMessageDialog() {
                new AlertDialog.Builder(PCLinkLibraryDemoActivity.this)
                        .setMessage(R.string.bluetooth_occur_error)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Back to the home menu
                                GuiUtils.goToSpecifiedActivity(PCLinkLibraryDemoActivity.this,
                                        PCLinkLibraryDemoActivity.class);
                            };
                        }).show();
            }

            /**
             * 顯示錯誤訊息
             */
            private void showNeeToPairMessageDialog() {
                new AlertDialog.Builder(PCLinkLibraryDemoActivity.this)
                        .setMessage(R.string.bluetooth_need_to_pair)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Go to the setting page
                                startActivityForResult(new Intent(
                                        android.provider.Settings.ACTION_BLUETOOTH_SETTINGS), 0);
                            };
                        }).show();
            }
        }).start();
    }

    @Override
    protected void onStart() {
        super.onStart();        
    }

    @Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (detachReceiver != null) {
			unregisterReceiver(detachReceiver);
			detachReceiver = null;			
		}
	}

	/**
     * 當接收到usb時,會由onResume進入
     */
	@Override
	protected void onResume() {
		super.onResume();
				
		boolean fromPl2303 = false;
		Bundle bundle = getIntent().getExtras();
		if(bundle != null && bundle.containsKey(PCLinkLibraryDemoConstant.FromPL2303)) {
			fromPl2303 = bundle.getBoolean(PCLinkLibraryDemoConstant.FromPL2303); 
		}
		
		// 由usb attach呼叫
		if(getIntent().getAction() != null && getIntent().getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED) ||
				fromPl2303 == true) {
			initDetachedUsbListener();
	        initBaudRate();
			initUI(true);
		}
		else {
			initUI(false);
			this.queryBluetoothDevicesAndSetToUi();
		}
	}
	
	private void initDetachedUsbListener() {
		if (detachReceiver == null) {
	    	detachReceiver = new BroadcastReceiver() {
	
				@Override
				public void onReceive(Context context, Intent intent) {
					initUI(false);
					queryBluetoothDevicesAndSetToUi();
				}
	        };
	
	        IntentFilter filter = new IntentFilter();
	        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
	        registerReceiver(detachReceiver, filter);
		}
    }
	
	private void initBaudRate() {
		ArrayAdapter<CharSequence> adapter = 
				ArrayAdapter.createFromResource(this, R.array.BaudRate_Var, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mBaudRate.setAdapter(adapter);
		mBaudRate.setSelection(1);
	}
	
 	private void initUI(boolean isPL2303) {
		mPL2303Layout.setVisibility(isPL2303 ? View.VISIBLE : View.GONE);
    	mBTLayout.setVisibility(!isPL2303 ? View.VISIBLE : View.GONE);
    	
    	if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			//mBLEPair.setVisibility(View.GONE);
    		mBLEPair.setEnabled(false);
        }
    	else {
    		mBLEPair.setEnabled(true);
    	}
	}
 	
 	private int isBLEMeter(final String str) {
		String nameKey;
		String addrKey;
		String nameValue;
		String addrValue;
	    for (int i=0; i<mPairedMeterCount; i++) {
	    	nameKey = PCLinkLibraryDemoConstant.BLE_PAIRED_METER_NAME_ + String.valueOf(i);
			addrKey = PCLinkLibraryDemoConstant.BLE_PAIRED_METER_ADDR_ + String.valueOf(i);
			nameValue = mPairedMeterNames.get(nameKey);
			addrValue = mPairedMeterAddrs.get(addrKey);
		    String remoteDeviceName = nameValue + "/" + addrValue;
		    
		    if (str.equals(remoteDeviceName)) {
		    	return i;
		    }
	    }
	    
	    return -1;
 	}
 	
 	private int initBLEMeters() {
 		int adapterCount = 0;
 		if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
 			 
 		    mPairedMeterNames = new HashMap<String, String>();
 		    mPairedMeterAddrs = new HashMap<String, String>();
		    mPairedMeterCount = MeterPreferenceDialog.getPairedMeters(this, mPairedMeterNames, mPairedMeterAddrs);
		    
		    ArrayAdapter<String> selectedMeterSpinnerAdapter = (ArrayAdapter<String>)mSelectedMeter.getAdapter();
		    adapterCount = selectedMeterSpinnerAdapter.getCount();
 			String nameKey;
			String addrKey;
			String nameValue;
			String addrValue;
		    for (int i=0; i<mPairedMeterCount; i++) {
		    	nameKey = PCLinkLibraryDemoConstant.BLE_PAIRED_METER_NAME_ + String.valueOf(i);
				addrKey = PCLinkLibraryDemoConstant.BLE_PAIRED_METER_ADDR_ + String.valueOf(i);
				nameValue = mPairedMeterNames.get(nameKey);
				addrValue = mPairedMeterAddrs.get(addrKey);
			    String remoteDeviceName = nameValue + "/" + addrValue;
			    
			    if (selectedMeterSpinnerAdapter != null) {
			    	boolean find_flag = false;
				    for (int j=0; j<selectedMeterSpinnerAdapter.getCount(); j++) {
		 				String remoteDeviceName2 = selectedMeterSpinnerAdapter.getItem(j);
		 				if (remoteDeviceName.equals(remoteDeviceName2)) {
		 					find_flag = true;
		 					break;
		 				}
		 			}
				    if (!find_flag) {
					    adapterCount++;
	 					selectedMeterSpinnerAdapter.add(remoteDeviceName);
				    }
			    }
		    }
		}
 		
 		return adapterCount;
	}
 	
 	private void ShowMeterDialog() {
    	meterDialog = MeterPreferenceDialog.newInstance();
    	meterDialog.show(getSupportFragmentManager(), "FRAGMENT_SETTING_METER");
    }
}
