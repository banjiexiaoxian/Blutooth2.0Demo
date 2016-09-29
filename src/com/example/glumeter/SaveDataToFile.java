package com.example.glumeter;
import java.io.BufferedWriter;  
import java.io.File;  
import java.io.FileWriter;  
import java.io.IOException;  
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;  

public class SaveDataToFile{
		private final static String TAG = "SaveDataToFile";
		private File dirPath;
		private String filePath;
		private ArrayList<String> mdata;
		private String datetime;
		
	  //���캯��
	    public SaveDataToFile(Context context,ArrayList<String> data) throws IOException, NameNotFoundException { 
	    	dirPath =  context.getExternalFilesDir(null);
	    	this.mdata = data;
	    	SimpleDateFormat tempDate = new SimpleDateFormat("yyyy-MM-dd" + " "  
                    + "hh:mm:ss");  
	    	this.datetime = tempDate.format(new java.util.Date()).toString();
	    	this.filePath = dirPath + this.datetime +".txt";
//	    	Log.i(TAG,filePath );
	    	
	    }  
	    
	    //�����ļ��м��ļ�  
	    public void createFile() {
//		File file = new File(dirPath);  
	    	File file = dirPath;
		if (!file.exists()) {  
		    try {  
		        //����ָ����·�������ļ���  
		        file.mkdirs();  
		        Log.i(TAG,"   ����Ŀ¼" );
		    } catch (Exception e) {  
		        // TODO: handle exception  
		    }  
		}  
		File dir = new File(filePath);  
		if (!dir.exists()) {  
		      try {  
		          //��ָ�����ļ����д����ļ�  
		    	  Log.e(TAG,"   dir.createNewFile();" );
		          dir.createNewFile();  
		          print(mdata);
		    } catch (Exception e) {  
		    }  
		}
	  
	    	
	    }
	    //���Ѵ������ļ���д������  
	    public void print(ArrayList<String> data) {  
	        FileWriter fw = null;  
	        BufferedWriter bw = null;  
	          
	        try {  
	             
	            fw = new FileWriter(filePath, true);//  
	            // ����FileWriter��������д���ַ���  
	            bw = new BufferedWriter(fw); // ��������ļ������  
	            bw.write(datetime + "\n");
	            
	            for(int i = 0;i < data.size();i++){
	            	Log.e(TAG,data.get(i) );
	            	bw.write(data.get(i)+" ");
	            }  
	            bw.flush(); // ˢ�¸����Ļ���  
	            bw.close();  
	            fw.close();  
	        } catch (IOException e) {  
	            // TODO Auto-generated catch block  
	            e.printStackTrace();  
	            try {  
	                bw.close();  
	                fw.close();  
	            } catch (IOException e1) {  
	                // TODO Auto-generated catch block  
	            }  
	        }  
	    }  
}
