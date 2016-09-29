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
 * ��Activity�����������������������Ļ��ʵʱ���ɲ���ͼ
 * �������豸����֮����ת����Activity
 * 
 * 
 * */
public class DataDispActivity extends Activity {


	private final static String TAG = DataDispActivity.class.getSimpleName();
	private static Context context ;
	
	private BluetoothDevice device;
    private BluetoothAdapter mBluetoothAdapter;
	private LinearLayout mCurveLayout;//���ͼ��Ĳ�������
	private GraphicalView mView;//����ͼ��
	private ChartService mChartService;
	private Button collectButton;
	private Button startDraw;
//	����ʹ��Android��SSP��Э��ջĬ�ϣ���UUID00001101-0000-1000-8000-00805F9B34FB�����������ⲿ�ģ�Ҳ��SSP���ڵ������豸ȥ���ӡ�
	private UUID uuid = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
	
	/**
	 * ���ڿ���ĳ�������Ƿ���Ҫ�洢�Լ��洢�������ڵ����ݣ���������Ϊ10s
	 */
	private static final long COLLECT_PERIOD = 10000;
	private boolean mCollecting = false;
	
	/**
	 * Ϊ����������ʵʱ�Ĳ���ͼ������socket�ͽ����������ݵ��̡߳����ݰ��������̡߳��Լ��洢���ݵ������ļ����̷ֱ߳����������߳���
	 * ֻ�л�ͼ�ĳ���������UI���߳���
	 */
	private AcceptThread mAcceptThread;
	private DataAnalyseThread mdataAnalyseThread;
	private SaveDataToFileThread sdThread ;
	private GetBTDataThread mgetBTDataThread;
	
	private BluetoothSocket clientSocket;
   
	//���ڻ����ȡ�����������ݵĶ��У���ȡ����mAnalyseQueue���н�������ƴ�Ӻͽ���
    private static BlockingQueue<Integer> mReadBufferQueue;
    
    //���ڻ���ƴ�Ӻ��˵����ݣ���ȡ�������̻߳�ͼ
    private static BlockingQueue<Integer> mAnalyseQueue;
    
    //���ڻ�����Ҫ���浽�����ļ��е����ݣ������㹻���ݺ�д���ļ�
    private static ArrayList<String> mCollectArray  ;
    
    /**
     * ���ڻ������ݵĶ��к�������г�ʼ��
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
     * ���ݴ洢��Ϻ���պ��������ڻ������ݵĶ��к�����
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
		
		
		
		//��ʼ��ͼ��
				mChartService = new ChartService(this);
				mChartService.setXYMultipleSeriesDataset("������");
				int color[] = {Color.RED,Color.BLUE,Color.YELLOW,Color.GREEN};
				mChartService.setXYMultipleSeriesRenderer(0,12000,0,500,"������", "ʱ��", "��ѹֵ",
		                Color.RED, Color.RED, color, Color.BLACK);
		        mView = mChartService.getGraphicalView();
		//��ͼ����ӵ�����������
		mCurveLayout = (LinearLayout) findViewById(R.id.curve);
  		mCurveLayout.addView(mView, new LayoutParams(
                  LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		
  		
  		
  		//��ȡ����������
		mBluetoothAdapter = DevScanActivity.mBluetoothAdapter;
		
		
		//��ʼ������
        mQueuesInit();
	    Log.i(TAG, "�����ѱ���ʼ��");
	    
	    //Ϊ��ʼ��ͼ��ť��ӵ���¼�
  		startDraw = (Button) findViewById(R.id.startDraw);
	    startDraw.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i(TAG, "��ʼ��ͼ");
				mHandler.sendMessage(mHandler.obtainMessage());//��ʼ��ͼ	
			}
			});
        
        
	    //����ʼ�ɼ���ť��Ӽ����¼� 
	    collectButton = (Button) findViewById(R.id.start_collect);
        collectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
			if (sdThread != null && sdThread.isAlive())
			sdThread.cancel();	
			mCollecting = true;
			collectButton.setText("���ڲɼ�...");
			//����ʼ��ֹͣ�ռ����εľ��
		    Handler mHandler2 = new Handler();
		    mHandler2.postDelayed(new Runnable() {
		        @Override
		        public void run() {
		        	//��ʱ�޸�mCollecting��־λ
		        	mCollecting = false;
		        	collectButton.setText("��ʼ�ɼ�");
		        	if (sdThread == null || !sdThread.isAlive()){
		        		sdThread = new SaveDataToFileThread();
		        	}
		        	Log.e(TAG, "mHandler2.postDelayed.run");
		        	sdThread.start();	
		        }
		    },10000);
			
			}
		});
	    
			//��ʼ��ServerSocket,���������߳�
//        	mAcceptThread = new AcceptThread();
//        	mAcceptThread.start();
        	//��ʼ��socket,�����������ݽ����߳�
        
        	//����תActivity����ͼ�л�ȡ��ǰ��ͼ���ӵ������豸
        	device = (BluetoothDevice)getIntent().getParcelableExtra(DevScanActivity.btDevice);
        	
        	//���ӽ���֮ǰ����Ҫ�����  
        	try {  
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {  
                    Method creMethod = BluetoothDevice.class  
                            .getMethod("createBond");  
                    Log.i(TAG, "��ʼ���");  
                    creMethod.invoke(device);  
                } else {  
                	Log.i(TAG, "�����");
                }  
            } catch (Exception e) { 
            	Log.e(TAG, "�޷����");
                e.printStackTrace();  
            }  
        	
//        	���������˺Ϳͻ�����ͬһ��RFCOMM�ŵ��϶���һ��BluetoothSocketʱ�������˾ͽ��������ӡ��˿̣�ÿ���豸���ܻ��һ��������������������ݴ��䡣
//        	�������˺Ϳͻ��˻��BluetoothSocket�ķ����ǲ�ͬ�ģ������������ڿͻ��˵����ӱ�����ʱ�Ų���һ��BluetoothSocket���ͻ������ڴ�һ�����������˵�RFCOMM�ŵ�ʱ���BluetoothSocket�ġ�
        	try {
        		clientSocket = device.createRfcommSocketToServiceRecord(uuid);
            } catch (Exception e){
            	Log.e("","Error creating socket");
        		}
            try {
            	clientSocket.connect();
            } catch (IOException e) {
                Log.e("clientSocket.connect()",e.getMessage());
//                �����������ӿ�ͨ���˿� ��1-30����UUID���ַ������в�����
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
            //clientSocket�����ɹ��󣬿������ݽ����߳�
            mgetBTDataThread = new GetBTDataThread(clientSocket);
	    	mgetBTDataThread.start();
	        
	    	//�������ݽ����߳�
		    mdataAnalyseThread = new DataAnalyseThread();
		    mdataAnalyseThread.start();
			
    }
   
   
    
    private class AcceptThread extends Thread{
		 private final BluetoothServerSocket mmServerSocket ;
		 
		 public AcceptThread(){
			 //final��������Ҫ������ֵ�����Բ��ܰѸ�ֵ������try������п�����Ϊ�׳��쳣������ִ�У�����Java���������﷨����
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
			 //���ּ�����ֱ�����յ��ͻ��˷�������󣬻����Ƿ����쳣
			 while(true){
				 try{
					 Log.i("AcceptThread","mmServerSocket.accept()����");
					 socket = mmServerSocket.accept();
					 Log.i("AcceptThread", "mmServerSocket.accept()��������");
				 } catch (IOException e){
					 e.printStackTrace();
				 }
				 
				 //�ͻ��˷�������󱻽��ܣ����ӽ���
				 if(socket != null){
					//��Ҫ�ڶ������߳���������ӣ���Ϊ�����������߳�accept()�������뱣�ֻ�Ծ״̬��ͬʱÿһ���½��������Ӷ�Ӧ�������ڶ������߳������Ӱ�졣
					//�����������ݽ����߳�
					 Log.i("�����������ݽ����߳�", "success");
				    	mgetBTDataThread = new GetBTDataThread(socket);
				    	mgetBTDataThread.start();
				        //�������ݽ����߳�
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
     * �����������ݵ��߳�
     * @param ���캯���Ĵ������Ϊsocket �ͻ���BluetoothSocket
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
    		//��ȡsocket���ݲ�����mReadBufferQueue�У����ֽڶ�ȡ
            int bytes; // bytes returned from read()  
            // Keep listening to the InputStream until an exception occurs  
            while (true) {  
                try {  
                    // Read from the InputStream  
                	bytes = mmInStream.read();
                	if(bytes != -1){
                		 mReadBufferQueue.add(bytes);
                	}else{
                		Log.i(TAG, "mmInStream�ѿ�");
                	}
                } catch (IOException e) {  
                	e.printStackTrace();
                    break;  
                }  
            }  
		}
    	
    	//�ṩ�ֶ��ر��߳̽ӿ�
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
     * ���������߳��ϵĻ�ͼ����
     */
    int i = 0;
    double[][] addY = new double[5][4];
    private Handler mHandler = new Handler() {
        @Override
        //ÿȡ����ʮ�����ٻ�һ��ͼ������һ��ͼ��
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
	    				//ת��Ϊ��ѹֵ
	    				Log.i(TAG, "recvInt��"+recvInt);
	    				//ͨ��ƽ�������꽫��·��������Ļ�ϵ�λ�÷ָ�������ʾΪ��·����ͼ
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
	        	
        	if(j == 5){//��·���ݵĵ㶼��װ��
				mChartService.updateChart(addY//�˴�����ֵ
       	 			);
				i = 0;j = 0;
				//����ͼ��
	        	mHandler.sendMessage(mHandler.obtainMessage());
	        	
			}
        	}
};
  
    	

    
    
   /**
    * ���ݴ洢�߳�
    *
    */
    private class SaveDataToFileThread extends Thread{
    	@Override
		public void run(){
			Log.i(TAG, "��ʼ�洢����");
//			mCollectQueueд���ļ�
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
     * ���ݰ��Ľ����߳�
     * ���������ݽ����߳�����֮�������������������������ʱ1s��ʼ����
     * ĳһ���ݰ��ĸ�ʽ����ʼ֡+���֡+��Сֵ+ÿһ֡����Сֵ�Ĳ�ֵ��1/4(һ��40��)
     * ������ʽ��
    		//��ʼ֡ (0x)00ffff,��0 255 255
    		//���֡(0x)01,����ʼ֮֡��,��1
    		//��Сֵ(0x)05e1,�����֮֡��,��5 225,��Ҫ��������������5*256+225=1505 �õ���ʵֵ
    		//40������Сֵ�Ĳ�ֵ(0x)01,�ھ�ֵ֮��,��1,��Ҫ*4������Сֵ���ӣ�1505+1*4=1509 �õ���ʵֵ
    		//����֡(0x)eeee,����������֮֡��,��238,238
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
    		Log.i("DataAnalyseThread", "���������ݽ����߳�");
    		
    			Integer recvData = null;
    			int sum = 0,recvSum;
    			int tmp;
    			int i = 0;//�ж��յ��Ĳ�ֵ����
    			int theTypeOfPrev = 0;//����ǰһ֡�����ͣ�����Ϊ��ȷ��֡0����ʼ֡1����Сֵ֡2����ֵ֡3������֡4
    			int[] recvDataCache = {-1,-1,-1};
    			int k = 0;//����ָʾrecvDataCache���±�
    			int flag1 = 0;//��־λ������ָʾ��ʱ��˫�ֽ�֡�ĸ�λ���ǵ�λ��0��ʾ��λ��1��ʾ��λ
    			int flag2 = 0;//��־λ
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
    				Log.i("�ж���һ֡����", theTypeOfPrev+"");
    				switch(theTypeOfPrev) {
    				case 0:
    					//��ʱӦ�õȴ���ʼ֡
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
    					//��֡Ӧ�������֡
    					Log.i("DataAnalyseThread", "��֡�����֡:"+recvData);
    					theTypeOfPrev = 2;
    					break;
    				case 2:
    					//��֡Ӧ������Сֵ֡�������Ǹ�λ�����ǵ�λ
    					//��֡Ӧ������Сֵ֡
    					Log.i("DataAnalyseThread", "��֡����Сֵ֡:"+recvData);
    					if(flag1 == 0){
    						//��ʱ����Сֵ֡�ĵ�λ
    						recvDataCache[0] = recvData;
    						flag1 = 1;
//    						Log.i("����flag1=0", ""+theTypeOfPrev);
    					}else if(flag1 == 1){
//    						Log.i("����flag1=1", ""+theTypeOfPrev);
    						//��ʱ����Сֵ֡�ĸ�λ
    						recvDataCache[1] = recvData;
    						minValue = recvDataCache[1]*256 + recvDataCache[0];
    						Log.i("minValue=", minValue+"");
    						flag1 = 0;//flag����
    						theTypeOfPrev = 3;
    					}
    					
    					break;
    				case 3:
    					//��֡�ǲ�ֵ֡
    					if(i == 39){
    						al.add(minValue + recvData*8);
    						//�Ѿ��յ�40����ֵ֡����һ֡Ӧ���ǽ���λ
    						theTypeOfPrev = 4;
//    						continue;
    					}else{
    						al.add(minValue + recvData*8);
        					i++;
    					}
    					break;
    				case 4:
    					//��֡Ӧ���ǽ���λ֡�������Ǹ�λ�����ǵ�λ
    					if(recvData == 238){
//    						Log.i("�жϽ���λ", flag2+"");
    						if(flag2 == 0){
    							//��ʱ�ǽ���λ֡�ĵ�λ
        						flag2 = 1;
        						
    						}else if(flag2 == 1){
    							//��ʱ�ǽ���λ֡�ĵ�λ����֡Բ�����գ����¿�ʼѭ���ж�
    							Object[] msg = al.toArray();
    							al.clear();
    							theTypeOfPrev = 0;
    							i = 0;
    							flag2 = 0;
    							for(Object eachMsg : msg){
        							//��ʱ����·���ݷ���һ���ڻ�ͼʱ�ٷֿ�
        							if (eachMsg instanceof Integer) {
        								   Integer eachMsg_Integer = Integer.parseInt(eachMsg.toString());
        								   try {
											mAnalyseQueue.put(eachMsg_Integer);
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
        								   //�����Ҫ�洢
        								   if(mCollecting){
        									   Log.i("DataAnalyseThread", "mCollectArray.add"+eachMsg_Integer);
        	    								mCollectArray.add(eachMsg.toString());
        	    							}
        								   
        							} 
        							
    							}
    							
    						}
    						
    					}else{
    						Log.i("DataAnalyseThread", "����λ�쳣��������֡");
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
    
    
    
    
    //Stringתbyte
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
    
    //byteתString
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
	  * ��ʮ�������ַ���ת��Ϊʮ������
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
    	//�ر���������
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
