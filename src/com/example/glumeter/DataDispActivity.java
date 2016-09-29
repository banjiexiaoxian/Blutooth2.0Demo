package com.example.glumeter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.achartengine.GraphicalView;

import com.example.glumeter.ChartService;
import com.example.glumeter.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * 本Activity将蓝牙传输过来的数据在屏幕上实时画成波形图
 * 与蓝牙设备连接之后，跳转到本Activity
 * 
 * 
 * */
public class DataDispActivity extends Activity {


	private final static String TAG = DataDispActivity.class.getSimpleName();
	private static Context context ;
	
	private BluetoothDevice device;
    private BluetoothAdapter mBluetoothAdapter;
	private LinearLayout mCurveLayout;//存放图表的布局容器
	private GraphicalView mView;//左右图表
	private ChartService mChartService;
	private Button collectButton;
	private Button startDraw;
//	必须使用Android的SSP（协议栈默认）的UUID00001101-0000-1000-8000-00805F9B34FB才能正常和外部的，也是SSP串口的蓝牙设备去连接。
	private UUID uuid = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
	
	/**
	 * 用于控制某段数据是否需要存储以及存储多少秒内的数据，本例设置为10s
	 */
	private static final long COLLECT_PERIOD = 10000;
	private boolean mCollecting = false;
	
	/**
	 * 为了流畅画出实时的波形图，监听socket和接收蓝牙数据的线程、数据包解析的线程、以及存储数据到本地文件的线程分别运行在子线程中
	 * 只有画图的程序运行在UI主线程中
	 */
	private AcceptThread mAcceptThread;
	private DataAnalyseThread mdataAnalyseThread;
	private SaveDataToFileThread sdThread ;
	private GetBTDataThread mgetBTDataThread;
	
	private BluetoothSocket clientSocket;
   
	//用于缓存读取到的蓝牙数据的队列，待取出到mAnalyseQueue队列进行数据拼接和解析
    private static BlockingQueue<Integer> mReadBufferQueue;
    
    //用于缓存拼接好了的数据，待取出到主线程画图
    private static BlockingQueue<Integer> mAnalyseQueue;
    
    //用于缓存需要保存到本地文件中的数据，存满足够数据后被写入文件
    private static ArrayList<String> mCollectArray  ;
    
    /**
     * 用于缓存数据的队列和数组进行初始化
     */
    private void mQueuesInit() {
		if (mReadBufferQueue == null)
			mReadBufferQueue = new LinkedBlockingDeque<Integer>(32768);
		if (mAnalyseQueue == null)
			mAnalyseQueue = new LinkedBlockingDeque<Integer>(65536);
		if (mCollectArray == null)
			mCollectArray = new ArrayList<String>(4096);
	}
    
    /**
     * 数据存储完毕后，清空和销毁用于缓存数据的队列和数组
     */
    private void mQueuesClear() {
		if (mReadBufferQueue != null){
			mReadBufferQueue = null;
		}
			
		if (mAnalyseQueue != null){
			mAnalyseQueue= null;
		}
		if (mCollectArray != null){
			mCollectArray=null;
		}
    }
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_data_disp);
		
		
		
		//初始化图表
				mChartService = new ChartService(this);
				mChartService.setXYMultipleSeriesDataset("脉搏波");
				int color[] = {Color.RED,Color.BLUE,Color.YELLOW,Color.GREEN};
				mChartService.setXYMultipleSeriesRenderer(0,12000,0,500,"脉搏波", "时间", "电压值",
		                Color.RED, Color.RED, color, Color.BLACK);
		        mView = mChartService.getGraphicalView();
		//将图表添加到布局容器中
		mCurveLayout = (LinearLayout) findViewById(R.id.curve);
  		mCurveLayout.addView(mView, new LayoutParams(
                  LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		
  		
  		
  		//获取蓝牙适配器
		mBluetoothAdapter = DevScanActivity.mBluetoothAdapter;
		
		
		//初始化队列
        mQueuesInit();
	    Log.i(TAG, "队列已被初始化");
	    
	    //为开始画图按钮添加点击事件
  		startDraw = (Button) findViewById(R.id.startDraw);
	    startDraw.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i(TAG, "开始画图");
				mHandler.sendMessage(mHandler.obtainMessage());//开始画图	
			}
			});
        
        
	    //给开始采集按钮添加监听事件 
	    collectButton = (Button) findViewById(R.id.start_collect);
        collectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
			if (sdThread != null && sdThread.isAlive())
			sdThread.cancel();	
			mCollecting = true;
			collectButton.setText("正在采集...");
			//负责开始和停止收集波形的句柄
		    Handler mHandler2 = new Handler();
		    mHandler2.postDelayed(new Runnable() {
		        @Override
		        public void run() {
		        	//定时修改mCollecting标志位
		        	mCollecting = false;
		        	collectButton.setText("开始采集");
		        	if (sdThread == null || !sdThread.isAlive()){
		        		sdThread = new SaveDataToFileThread();
		        	}
		        	Log.e(TAG, "mHandler2.postDelayed.run");
		        	sdThread.start();	
		        }
		    },10000);
			
			}
		});
	    
			//初始化ServerSocket,开启监听线程
//        	mAcceptThread = new AcceptThread();
//        	mAcceptThread.start();
        	//初始化socket,开启蓝牙数据接收线程
        
        	//从跳转Activity的意图中获取当前试图连接的蓝牙设备
        	device = (BluetoothDevice)getIntent().getParcelableExtra(DevScanActivity.btDevice);
        	
        	//连接建立之前，需要先配对  
        	try {  
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {  
                    Method creMethod = BluetoothDevice.class  
                            .getMethod("createBond");  
                    Log.i(TAG, "开始配对");  
                    creMethod.invoke(device);  
                } else {  
                	Log.i(TAG, "已配对");
                }  
            } catch (Exception e) { 
            	Log.e(TAG, "无法配对");
                e.printStackTrace();  
            }  
        	
//        	当服务器端和客户端在同一个RFCOMM信道上都有一个BluetoothSocket时，则两端就建立了连接。此刻，每个设备都能获得一个输入输出流，进行数据传输。
//        	服务器端和客户端获得BluetoothSocket的方法是不同的，服务器端是在客户端的连接被接受时才产生一个BluetoothSocket，客户端是在打开一个到服务器端的RFCOMM信道时获得BluetoothSocket的。
        	try {
        		clientSocket = device.createRfcommSocketToServiceRecord(uuid);
            } catch (Exception e){
            	Log.e("","Error creating socket");
        		}
            try {
            	clientSocket.connect();
            } catch (IOException e) {
                Log.e("clientSocket.connect()",e.getMessage());
//                蓝牙串口连接可通过端口 （1-30）和UUID两种方法进行操作。
                //The problem is with the socket.mPort parameter. 
                //When you create your socket using socket = device.createRfcommSocketToServiceRecord(SERIAL_UUID); 
                //the mPort gets integer value "-1", and this value seems doesn't work for android >=4.2 
                //so you need to set it to "1". The bad news is that createRfcommSocketToServiceRecord only accepts UUID as parameter and not mPort so we have to use other aproach.
                //We need to use both socket attribs , the second one as a fallback. 
                try {
                	//
                    Log.i("clientSocket.connect()","trying fallback...");
                    clientSocket =(BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(device,1);
                    clientSocket.connect();
                    Log.i("clientSocket.connect()","Connected");
                }
             catch (Exception e2) {
                 Log.e("clientSocket.connect()", "Couldn't establish Bluetooth connection!");
              }
            }
            //clientSocket建立成功后，开启数据接收线程
            mgetBTDataThread = new GetBTDataThread(clientSocket);
	    	mgetBTDataThread.start();
	        
	    	//开启数据解析线程
		    mdataAnalyseThread = new DataAnalyseThread();
		    mdataAnalyseThread.start();
			
    }
   
   
    
    private class AcceptThread extends Thread{
		 private final BluetoothServerSocket mmServerSocket ;
		 
		 public AcceptThread(){
			 //final变量必须要被赋初值，所以不能把赋值语句放在try语句块里，有可能因为抛出异常而不被执行，这是Java编译器的语法规则
			 BluetoothServerSocket tmp = null;
			try{
				tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("myphone", uuid);
			}catch(IOException e){
				 try {
					 tmp =(BluetoothServerSocket) mBluetoothAdapter.getClass().getMethod("listenUsingRfcommOn", new Class[] {int.class}).invoke(mBluetoothAdapter,1); 
					
				} catch (IllegalAccessException e1) {
					e1.printStackTrace();
				} catch (InvocationTargetException e1) {
					e1.printStackTrace();
				} catch (NoSuchMethodException e1) {
					e1.printStackTrace();
				}
			}
			 mmServerSocket = tmp;
			}
		 
		 public void run(){
			 BluetoothSocket socket = null;
			 //保持监听，直到接收到客户端发起的请求，或者是发生异常
			 while(true){
				 try{
					 Log.i("AcceptThread","mmServerSocket.accept()阻塞");
					 socket = mmServerSocket.accept();
					 Log.i("AcceptThread", "mmServerSocket.accept()结束阻塞");
				 } catch (IOException e){
					 e.printStackTrace();
				 }
				 
				 //客户端发起的请求被接受，连接建立
				 if(socket != null){
					//需要在独立的线程里管理连接，因为服务器监听线程accept()方法必须保持活跃状态，同时每一个新建立的连接都应该运行在独立的线程里，互不影响。
					//开启蓝牙数据接收线程
					 Log.i("开启蓝牙数据接收线程", "success");
				    	mgetBTDataThread = new GetBTDataThread(socket);
				    	mgetBTDataThread.start();
				        //开启数据解析线程
					    mdataAnalyseThread = new DataAnalyseThread();
					    mdataAnalyseThread.start();
					 try {
						mmServerSocket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					 break;
				 }
			 }
		 }
		public void cancel() {
			try{
				mmServerSocket.close();
			}catch(IOException e){}
			}
	}	 
   
    
    
    
    
    
    /**
     * 接收蓝牙数据的线程
     * @param 构造函数的传入参数为socket 客户端BluetoothSocket
     *
     */
    private class GetBTDataThread extends Thread {
    
    	private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;
    	public GetBTDataThread(BluetoothSocket socket){
    		this.mmSocket = socket;
    		InputStream tmpIn = null;
			OutputStream tmpOut = null;
			
			try{
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			}catch(IOException e){}
			
			mmInStream = tmpIn;
			mmOutStream = tmpOut;
			
    	}
    	@Override
    	public void run(){
    		//读取socket数据并存入mReadBufferQueue中，按字节读取
            int bytes; // bytes returned from read()  
            // Keep listening to the InputStream until an exception occurs  
            while (true) {  
                try {  
                    // Read from the InputStream  
                	bytes = mmInStream.read();
                	if(bytes != -1){
                		 mReadBufferQueue.add(bytes);
                	}else{
                		Log.i(TAG, "mmInStream已空");
                	}
                } catch (IOException e) {  
                	e.printStackTrace();
                    break;  
                }  
            }  
		}
    	
    	//提供手动关闭线程接口
    	public void cancel() {
    		try {
				mmSocket.close();
				Log.i(TAG, "cancel GetBTDataThread");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
    }
    	
    
    /**
     * 运行在主线程上的画图程序
     */
    int i = 0;
    double[][] addY = new double[5][4];
    private Handler mHandler = new Handler() {
        @Override
        //每取出二十个点再画一次图（更新一次图表）
       public void handleMessage(Message msg) {
        	super.handleMessage(msg);
        	int i = 0,j = 0;
        	while(j<5){
        		while(i<4){
	        		Integer recvInt = null;
	        		try {
	    				recvInt = mAnalyseQueue.take();
//	    				Log.i("444",mAnalyseQueue.toString() );
	    				//0.0008056640625=3.3/4096
	    				//转化为电压值
	    				Log.i(TAG, "recvInt："+recvInt);
	    				//通过平移纵坐标将四路数据在屏幕上的位置分隔开，显示为四路波形图
	    				if(null != recvInt){
	    					addY[j][i] = recvInt + 3000*i;
	        				i++;
	    				}else{
	    					Log.i("mAnalyseQueue.poll", "no recv");
	    				}
	    			} catch (InterruptedException e) {
	    				e.printStackTrace();
	    			}
	        	}
        		j++;
        		i = 0;
        	}
	        	
        	if(j == 5){//四路数据的点都已装载
				mChartService.updateChart(addY//此处加入值
       	 			);
				i = 0;j = 0;
				//更新图表
	        	mHandler.sendMessage(mHandler.obtainMessage());
	        	
			}
        	}
};
  
    	

    
    
   /**
    * 数据存储线程
    *
    */
    private class SaveDataToFileThread extends Thread{
    	@Override
		public void run(){
			Log.i(TAG, "开始存储数据");
//			mCollectQueue写入文件
			try {
				context = getApplicationContext();
				SaveDataToFile stf = new SaveDataToFile(context,mCollectArray);
				stf.createFile();
				Log.e(TAG,"mCollectArray.clear()" );
				mCollectArray.clear();
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    	public void cancel() {
    		Log.e(TAG, "cancel sdThread");
    		mCollectArray.clear();
			interrupt();
		}
    }
    
    
    /**
     * 数据包的解析线程
     * 必须在数据接收线程运行之后才有数据用作解析，所以延时1s后开始运行
     * 某一数据包的格式：起始帧+序号帧+最小值+每一帧与最小值的差值的1/4(一共40个)
     * 解析方式：
    		//起始帧 (0x)00ffff,即0 255 255
    		//序号帧(0x)01,在起始帧之后,即1
    		//最小值(0x)05e1,在序号帧之后,即5 225,需要把两个数做处理，5*256+225=1505 得到真实值
    		//40个与最小值的差值(0x)01,在均值之后,即1,需要*4后与最小值做加，1505+1*4=1509 得到真实值
    		//结束帧(0x)eeee,在所有数据帧之后,即238,238
     */
    private class DataAnalyseThread extends Thread{
    	private boolean mShouldRunning;
    	public DataAnalyseThread() {
    		mShouldRunning = true;
    	}
    	public void run() {
    		
    		try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
    		Log.i("DataAnalyseThread", "开启了数据解析线程");
    		
    			Integer recvData = null;
    			int sum = 0,recvSum;
    			int tmp;
    			int i = 0;//判断收到的差值个数
    			int theTypeOfPrev = 0;//表明前一帧的类型，可能为不确定帧0、起始帧1、最小值帧2、差值帧3、结束帧4
    			int[] recvDataCache = {-1,-1,-1};
    			int k = 0;//用来指示recvDataCache的下标
    			int flag1 = 0;//标志位，用来指示此时是双字节帧的高位还是低位，0表示低位，1表示高位
    			int flag2 = 0;//标志位
    			int minValue = 0;
    			ArrayList<Integer> al = new ArrayList<Integer>();
    		while(mShouldRunning){
    			try {
					recvData = mReadBufferQueue.poll(10,
							TimeUnit.MILLISECONDS);
					
					Log.i("DataAnalyseThread.mReadBufferQueue.poll", ""+recvData);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
    			
    			if (recvData != null) {
    				Log.i("判断上一帧类型", theTypeOfPrev+"");
    				switch(theTypeOfPrev) {
    				case 0:
    					//此时应该等待起始帧
    					if(recvData == 0){
    						recvDataCache[0] = recvData;
    						recvDataCache[1] = -1;
    						recvDataCache[2] = -1;
    					}else if(recvData == 255 && recvDataCache[0] == 0 && recvDataCache[1] == -1){
    						recvDataCache[1] = recvData;}
    					else if(recvData == 255 && recvDataCache[0] == 0 && recvDataCache[1] == 255 && recvDataCache[2] == -1){
    						recvDataCache[2]= recvData;
    						theTypeOfPrev = 1;
    					}else{
    						recvDataCache[0] = -1;
    						continue;
    					}
    					break;
    				
    				case 1:
    					//此帧应该是序号帧
    					Log.i("DataAnalyseThread", "此帧是序号帧:"+recvData);
    					theTypeOfPrev = 2;
    					break;
    				case 2:
    					//此帧应该是最小值帧，可能是高位或者是低位
    					//此帧应该是最小值帧
    					Log.i("DataAnalyseThread", "此帧是最小值帧:"+recvData);
    					if(flag1 == 0){
    						//此时是最小值帧的低位
    						recvDataCache[0] = recvData;
    						flag1 = 1;
//    						Log.i("进入flag1=0", ""+theTypeOfPrev);
    					}else if(flag1 == 1){
//    						Log.i("进入flag1=1", ""+theTypeOfPrev);
    						//此时是最小值帧的高位
    						recvDataCache[1] = recvData;
    						minValue = recvDataCache[1]*256 + recvDataCache[0];
    						Log.i("minValue=", minValue+"");
    						flag1 = 0;//flag重置
    						theTypeOfPrev = 3;
    					}
    					
    					break;
    				case 3:
    					//此帧是差值帧
    					if(i == 39){
    						al.add(minValue + recvData*8);
    						//已经收到40个差值帧，下一帧应该是结束位
    						theTypeOfPrev = 4;
//    						continue;
    					}else{
    						al.add(minValue + recvData*8);
        					i++;
    					}
    					break;
    				case 4:
    					//此帧应该是结束位帧，可能是高位或者是低位
    					if(recvData == 238){
//    						Log.i("判断结束位", flag2+"");
    						if(flag2 == 0){
    							//此时是结束位帧的低位
        						flag2 = 1;
        						
    						}else if(flag2 == 1){
    							//此时是结束位帧的低位，该帧圆满接收，重新开始循环判断
    							Object[] msg = al.toArray();
    							al.clear();
    							theTypeOfPrev = 0;
    							i = 0;
    							flag2 = 0;
    							for(Object eachMsg : msg){
        							//暂时将四路数据放在一起，在画图时再分开
        							if (eachMsg instanceof Integer) {
        								   Integer eachMsg_Integer = Integer.parseInt(eachMsg.toString());
        								   try {
											mAnalyseQueue.put(eachMsg_Integer);
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
        								   //如果需要存储
        								   if(mCollecting){
        									   Log.i("DataAnalyseThread", "mCollectArray.add"+eachMsg_Integer);
        	    								mCollectArray.add(eachMsg.toString());
        	    							}
        								   
        							} 
        							
    							}
    							
    						}
    						
    					}else{
    						Log.i("DataAnalyseThread", "结束位异常，丢掉此帧");
    						al.clear();
    						i = 0;
    						theTypeOfPrev = 0;
    					}
    					break;
    				default:break;
    				}
    			}//end recv==null
    			else{
    				Log.i("mReadBufferQueue.poll", "no recv");
    			}
    		}
    			
    	}
    	public void cancel() {
			interrupt();
			mShouldRunning = false;
		}
    }
    
    
    
    
    //String转byte
    public byte[] getHexBytes(String message) {  
        int len = message.length() / 2;  
        char[] chars = message.toCharArray();  
        String[] hexStr = new String[len];  
        byte[] bytes = new byte[len];  
        for (int i = 0, j = 0; j < len; i += 2, j++) {  
            hexStr[j] = "" + chars[i] + chars[i + 1];  
            bytes[j] = (byte) Integer.parseInt(hexStr[j], 16);  
        }  
        return bytes;  
    } 
    
    //byte转String
    public static String bytesToHexString(byte[] bytes) {
        String result = "";
        for (int i = 0; i < bytes.length; i++) {
            String hexString = Integer.toHexString(bytes[i] & 0xFF);
            if (hexString.length() == 1) {
                hexString = '0' + hexString;
            }
            result += hexString.toUpperCase();
        }
        return result;
    }
    /**
	  * 将十六进制字符串转换为十进制数
	  */
	 public static int HexToInteger(String hexString){
	 	
	 	int len = hexString.length();
	 	int i,ch;
	 	int[] tmp = new int[3];
	 	for(i = 0;i < len;i++){
	 		ch = (int)hexString.charAt(i);
	 		if(ch <= 'F' && ch >= 'A') ch = ch - 'A' + 10;
	 		else if(ch <= 'f' && ch >= 'a') ch = ch - 'a' + 10;
	 		else if(ch == ' ') continue;
	 		else ch = ch - '0';
	 		tmp[i] = ch; 
	 	}
	 	return tmp[0]*256+tmp[1]*16+tmp[2];
	 }
	
		
	 
    @Override 
    protected void onResume(){
    	super.onResume();
    	Log.i(TAG, "onResume");
    	
    }
    
    @Override 
    protected void onPause(){
    	super.onPause();
    	Log.i(TAG, "onPause");
    	//关闭蓝牙连接
//    	mBLE.disconnect();
    }
    
    @Override
    protected void onStop(){
    	super.onStop();
    	mQueuesClear();
		if (mdataAnalyseThread != null && mdataAnalyseThread.isAlive()){
			mdataAnalyseThread.cancel();
			mdataAnalyseThread = null;
		}
		if (sdThread != null && sdThread.isAlive()){
			sdThread.cancel();
			sdThread = null;
		}
		if (mgetBTDataThread != null && mgetBTDataThread.isAlive()){
			mgetBTDataThread.cancel();
			mgetBTDataThread = null;
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
