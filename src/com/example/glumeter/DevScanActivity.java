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
 * BT2.0�豸ɨ��Activity
 * @author lyl
 *
 */
public class DevScanActivity extends ListActivity {
	
	public final static String btDevice = "deviceObject";
	public final static String  btAdapter = "bluetoothAdapter";
	private final static String TAG = DevScanActivity.class.getSimpleName();
	
	public static BluetoothAdapter mBluetoothAdapter;
	//�ж��Ƿ�ֹͣɨ��
	private boolean mScanning;
	
	//���������豸��Ļص�����
	private BluetoothReceiver receiver; 
	
	//������UI�߳��е�Handler
    private Handler mHandler;
    
    //���ֵ������豸�б�
    private DeviceListAdapter mDeviceListAdapter;
   
    //ɨ�迪ʼ��10s���Զ�ֹͣ
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
		// ����豸�Ƿ�֧������
	    if (mBluetoothAdapter == null) {
	    	Log.i(TAG,"mBluetoothAdapter == null");
	        Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
	        finish();
	        return;
	    }
	    //�����������˴�ʹ����򵥵ķ����������������ڲ�֪ͨ�û��������ֱ�ӿ���������
	    //�������Ҫ�� Bluetooth ����һ���������ӣ���Ӧ��ʹ�� ACTION_REQUEST_ENABLE Intent�������ᵯ��һ����ʾ����ʾ�û��Ƿ��� 
	   //Bluetooth��enable() �������ṩ���� UI ������ϵͳ���õ�Ӧ����ʹ�ã����硰��Դ����Ӧ�á�
        mBluetoothAdapter.enable();
       
        mDeviceListAdapter = new DeviceListAdapter(this);
        setListAdapter(mDeviceListAdapter);
      
        //��ʼɨ�������豸
        scanDevice(true);
        
        //������ͼ��������ע��㲥������
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);  
        receiver = new BluetoothReceiver();  
        registerReceiver(receiver, filter);  
    }     
			
 
    @Override 
    protected void onResume(){
    	Log.i(TAG, "onResume");
    	super.onResume();
	    //�Ӻ�̨�ص�ǰ̨�����¼���view
    	 mDeviceListAdapter = new DeviceListAdapter(this);
         setListAdapter(mDeviceListAdapter);
         //���¿�ʼɨ���豸
         scanDevice(true);
    	
    }
    
    @Override 
    protected void onPause(){
    	super.onPause();
    	Log.i(TAG, "onPause");
    	//ֹͣɨ���豸
    	scanDevice(false);
    }
    
    @Override
    protected void onStop(){
    	super.onStop();
    	mDeviceListAdapter.clear();
    	Log.i(TAG, "onStop");
    }
    
    //�б��е������豸����¼�
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mDeviceListAdapter.getDevice(position);
        if (device == null) return;
        if (mScanning) {
            mBluetoothAdapter.cancelDiscovery();
            mScanning = false;
        }
        //ͨ����ͼ��ת���µ�activity
        Intent mIntent = new Intent(this,DataDispActivity.class);  
        Bundle mBundle = new Bundle();  
        mBundle.putParcelable(btDevice, device); 
        mIntent.putExtras(mBundle);  
        startActivity(mIntent);  
    }
    
    /**
     * ʹ��mBluetoothAdapter.startDiscovery()��mBluetoothAdapter.cancelDiscovery()
     * ���򿪻��߹ر�����ɨ�����
     * 
     * @param enable
     */
    private void scanDevice(final boolean enable) {
        if (enable) {
        	//�������Ե�Զ�������豸�ļ���  
            Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();  
            if(bondedDevices.size()>0){  
                for(Iterator<BluetoothDevice> it = bondedDevices.iterator();it.hasNext();){  
                   final BluetoothDevice device = (BluetoothDevice)it.next();  
                    //��ӡ��Զ�������豸�������ַ  
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
                System.out.println("��û������Ե�Զ�������豸��");  
            }  
        	 Log.i(TAG, "onScan");
            
        	 // ��Ԥ��ʱ��֮��ֹͣɨ�����
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
