package com.example.glumeter;


import java.util.Iterator;
import java.util.Set;

import com.example.glumeter.DeviceListAdapter;
import com.example.glumeter.R;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

/**
 * BT2.0设备扫描Activity
 * @author lyl
 *
 */
public class DevScanActivity extends ListActivity {
	
	public final static String btDevice = "deviceObject";
	public final static String  btAdapter = "bluetoothAdapter";
	private final static String TAG = DevScanActivity.class.getSimpleName();
	
	public static BluetoothAdapter mBluetoothAdapter;
	//判断是否停止扫描
	private boolean mScanning;
	
	//发现蓝牙设备后的回调方法
	private BluetoothReceiver receiver; 
	
	//绑定在主UI线程中的Handler
    private Handler mHandler;
    
    //发现的蓝牙设备列表
    private DeviceListAdapter mDeviceListAdapter;
   
    //扫描开始后10s后自动停止
    private static final long SCAN_PERIOD = 10000;
	
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG,"onCreate");
		getActionBar().setTitle(R.string.title_devices);
		
		mHandler = new Handler();
		
		 final BluetoothManager bluetoothManager 
		 				= (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		 mBluetoothAdapter = bluetoothManager.getAdapter();
		
		 Log.i(TAG,"GetBluetoothAdapter");
		// 检查设备是否支持蓝牙
	    if (mBluetoothAdapter == null) {
	    	Log.i(TAG,"mBluetoothAdapter == null");
	        Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
	        finish();
	        return;
	    }
	    //开启蓝牙，此处使用最简单的方法开启蓝牙，即在不通知用户的情况下直接开启了蓝牙
	    //如果你想要打开 Bluetooth 创建一个无线连接，你应当使用 ACTION_REQUEST_ENABLE Intent，这样会弹出一个提示框提示用户是否开启 
	   //Bluetooth，enable() 方法仅提供给有 UI 、更改系统设置的应用来使用，例如“电源管理”应用。
        mBluetoothAdapter.enable();
       
        mDeviceListAdapter = new DeviceListAdapter(this);
        setListAdapter(mDeviceListAdapter);
      
        //开始扫描蓝牙设备
        scanDevice(true);
        
        //配置意图过滤器、注册广播接收器
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);  
        receiver = new BluetoothReceiver();  
        registerReceiver(receiver, filter);  
    }     
			
 
    @Override 
    protected void onResume(){
    	Log.i(TAG, "onResume");
    	super.onResume();
	    //从后台回到前台，重新加载view
    	 mDeviceListAdapter = new DeviceListAdapter(this);
         setListAdapter(mDeviceListAdapter);
         //重新开始扫描设备
         scanDevice(true);
    	
    }
    
    @Override 
    protected void onPause(){
    	super.onPause();
    	Log.i(TAG, "onPause");
    	//停止扫描设备
    	scanDevice(false);
    }
    
    @Override
    protected void onStop(){
    	super.onStop();
    	mDeviceListAdapter.clear();
    	Log.i(TAG, "onStop");
    }
    
    //列表中的蓝牙设备点击事件
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mDeviceListAdapter.getDevice(position);
        if (device == null) return;
        if (mScanning) {
            mBluetoothAdapter.cancelDiscovery();
            mScanning = false;
        }
        //通过意图跳转到新的activity
        Intent mIntent = new Intent(this,DataDispActivity.class);  
        Bundle mBundle = new Bundle();  
        mBundle.putParcelable(btDevice, device); 
        mIntent.putExtras(mBundle);  
        startActivity(mIntent);  
    }
    
    /**
     * 使用mBluetoothAdapter.startDiscovery()和mBluetoothAdapter.cancelDiscovery()
     * 来打开或者关闭蓝牙扫描操作
     * 
     * @param enable
     */
    private void scanDevice(final boolean enable) {
        if (enable) {
        	//获得已配对的远程蓝牙设备的集合  
            Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();  
            if(bondedDevices.size()>0){  
                for(Iterator<BluetoothDevice> it = bondedDevices.iterator();it.hasNext();){  
                   final BluetoothDevice device = (BluetoothDevice)it.next();  
                    //打印出远程蓝牙设备的物理地址  
                    System.out.println(device.getAddress());  
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDeviceListAdapter.addDevice(device);
                            mDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }  
            }else{  
                System.out.println("还没有已配对的远程蓝牙设备！");  
            }  
        	 Log.i(TAG, "onScan");
            
        	 // 在预设时间之后停止扫描操作
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.cancelDiscovery();
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startDiscovery();
        } else {
            mScanning = false;
            mBluetoothAdapter.cancelDiscovery();
        }
        invalidateOptionsMenu();
    }
    
    
    private class BluetoothReceiver extends BroadcastReceiver { 
    	
        @Override  
        public void onReceive(Context context, Intent intent) {  
            String action = intent.getAction();  
            
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {  
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);  
                mDeviceListAdapter.addDevice(device);
                mDeviceListAdapter.notifyDataSetChanged();
            }  
           
        }  
    } 
   
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.dev_scan, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
