package com.taidoc.pclinklibrary.demo.view;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.taidoc.pclinklibrary.demo.R;
import com.taidoc.pclinklibrary.demo.interfaces.OnSearchBLEClickListener;

public class SearchMeterDialog extends Dialog {

	private ListView mListView = null;
	private Button mCancel;
	
	/**
     * 是否成功找到paire meter
     */
    private List<BluetoothDevice> mSearchedDevices;
    private OnSearchBLEClickListener mHandler;
  
    // Listeners
    /*private ListView.OnItemClickListener mListViewOnItemClickListener = new ListView.OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parentView, View view, int position,
				long id) {
			// 進行Import流程
			CheckedTextView v = (CheckedTextView)view.findViewById(R.id.ctv_item);
			
			BluetoothDevice searchedDevice = mSearchedDevices.get(position);
			String macAddress = searchedDevice.getAddress();
    		if (!(macAddress.equals("N/A") || TextUtils.isEmpty(macAddress))) {
    			if (mHandler != null) {
    				mHandler.OnSearchBLEClicked(searchedDevice);
    			}
            }
            
            dismiss();
		}
    };*/
    
    private Button.OnClickListener mCancelOnClickListener = new Button.OnClickListener() {

		@Override
		public void onClick(View view) {
			if (mHandler != null) {
				mHandler.OnSearchBLEClicked(null);
			}
			dismiss();
		}
    };
    
	public SearchMeterDialog(Context context) {
		super(context);
	}
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
    }	
	
	@Override
	public void show() {
		this.setContentView(R.layout.search_meter);
		super.show();
		
		findViews();
        setListeners();
	}

	private void findViews() {
		mListView = (ListView) findViewById(R.id.lv_content);
		mCancel = (Button) findViewById(R.id.btn_cancel);
	}
	
	/**
     * 設定相關View的Listener
     */
	private void setListeners() {
		//mListView.setOnItemClickListener(mListViewOnItemClickListener);
		mCancel.setOnClickListener(mCancelOnClickListener);
	}
	
	public void setOnSearchBLEClickListener(OnSearchBLEClickListener handler) {
    	this.mHandler = handler;
    }
	
	public void updateSearchedDevices(List<BluetoothDevice> devices) {
		if (mSearchedDevices == null) {
			mSearchedDevices = new ArrayList<BluetoothDevice>();
		}
		else {
			mSearchedDevices.clear();
		}
		
		mSearchedDevices.addAll(devices);
		
		List<String> array = new ArrayList<String>();
		for (int i=0; i<mSearchedDevices.size(); i++) {
    		BluetoothDevice pairedDevice = mSearchedDevices.get(i);
    		String tmpStr = pairedDevice.getName();
    		array.add(tmpStr);
    	}
    	
		ListAdapter adapter = new ListAdapter(getContext(), array);
		mListView.setAdapter(adapter);
	}
	
	private class ListAdapter extends BaseAdapter {
    	private List<String> mList;
    	private LayoutInflater inflater = null;
    	
    	public ListAdapter(Context context, List<String> list) {
    		mList = list;
    		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	}
    	
    	public int getCount() {
    		return mList.size();
    	}
    	
    	public Object getItem(int position) {
    		return position;
    	}
    	
    	public long getItemId(int position) {
    		return position;
    	}
    	
    	public View getView(final int position, View convertView, ViewGroup parent) {
    		
    		ViewHolder viewHolder = null;
    		View vi = convertView;
    		if(convertView == null) {
    			vi = inflater.inflate(R.layout.listview_content_search, null);
    			viewHolder = new ViewHolder();
    			viewHolder.tvTitle = (TextView) vi.findViewById(R.id.tv_title);
    			viewHolder.btnDel = (Button) vi.findViewById(R.id.btn_del);
    			vi.setTag(viewHolder);
    		}
    		else {
    			viewHolder = (ViewHolder) convertView.getTag();
    		}
    		
    		viewHolder.tvTitle.setText(mList.get(position).toString());
    		viewHolder.btnDel.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					BluetoothDevice searchedDevice = mSearchedDevices.get(position);
					String macAddress = searchedDevice.getAddress();
		    		if (!(macAddress.equals("N/A") || TextUtils.isEmpty(macAddress))) {
		    			if (mHandler != null) {
		    				mHandler.OnSearchBLEClicked(searchedDevice);
		    			}
		            }
		            
		            dismiss();
				}
			});
    		
    		return vi;
    	}
    }
	
	private class ViewHolder {
		TextView tvTitle;
		Button btnDel;
	}
}