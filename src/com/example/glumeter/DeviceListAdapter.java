package com.example.glumeter;

import java.util.ArrayList;

import com.example.glumeter.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class DeviceListAdapter extends BaseAdapter {

	// Adapter for holding devices found through scanning.

	private ArrayList<BluetoothDevice> mDevices;
	private LayoutInflater mInflator;
	private Activity mContext;

	public DeviceListAdapter(Activity c) {
		super();
		mContext = c;
		mDevices = new ArrayList<BluetoothDevice>();
		mInflator = mContext.getLayoutInflater();
	}

	public void addDevice(BluetoothDevice device) {
		Log.i("addDevice", "12321");
		if (!mDevices.contains(device)) {
			Log.i("addDevice", "in");
			mDevices.add(device);
		}
	}

	public BluetoothDevice getDevice(int position) {
		return mDevices.get(position);
	}

	public void clear() {
		mDevices.clear();
	}

	@Override
	public int getCount() {
		return mDevices.size();
	}

	@Override
	public Object getItem(int i) {
		return mDevices.get(i);
	}

	@Override
	public long getItemId(int i) {
		return i;
	}

	@Override
	public View getView(int i, View view, ViewGroup viewGroup) {
		Log.i("getView","getView");
		ViewHolder viewHolder;
		// General ListView optimization code.
		if (view == null) {
			view = mInflator.inflate(R.layout.listitem_device, null);
			viewHolder = new ViewHolder();
			viewHolder.deviceAddress = (TextView) view
					.findViewById(R.id.device_address);
			viewHolder.deviceName = (TextView) view
					.findViewById(R.id.device_name);
			view.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) view.getTag();
		}

		BluetoothDevice device = mDevices.get(i);
		final String deviceName = device.getName();
		if (deviceName != null && deviceName.length() > 0)
			viewHolder.deviceName.setText(deviceName);
		else
			viewHolder.deviceName.setText(R.string.unknown_device);
		viewHolder.deviceAddress.setText(device.getAddress());

		return view;
	}

	class ViewHolder {
		TextView deviceName;
		TextView deviceAddress;
	}
}
