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
		
	  //构造函数
	    public SaveDataToFile(Context context,ArrayList<String> data) throws IOException, NameNotFoundException { 
	    	dirPath =  context.getExternalFilesDir(null);
	    	this.mdata = data;
	    	SimpleDateFormat tempDate = new SimpleDateFormat("yyyy-MM-dd" + " "  
                    + "hh:mm:ss");  
	    	this.datetime = tempDate.format(new java.util.Date()).toString();
	    	this.filePath = dirPath + this.datetime +".txt";
//	    	Log.i(TAG,filePath );
	    	
	    }  
	    
	    //创建文件夹及文件  
	    public void createFile() {
//		File file = new File(dirPath);  
	    	File file = dirPath;
		if (!file.exists()) {  
		    try {  
		        //按照指定的路径创建文件夹  
		        file.mkdirs();  
		        Log.i(TAG,"   创建目录" );
		    } catch (Exception e) {  
		        // TODO: handle exception  
		    }  
		}  
		File dir = new File(filePath);  
		if (!dir.exists()) {  
		      try {  
		          //在指定的文件夹中创建文件  
		    	  Log.e(TAG,"   dir.createNewFile();" );
		          dir.createNewFile();  
		          print(mdata);
		    } catch (Exception e) {  
		    }  
		}
	  
	    	
	    }
	    //向已创建的文件中写入数据  
	    public void print(ArrayList<String> data) {  
	        FileWriter fw = null;  
	        BufferedWriter bw = null;  
	          
	        try {  
	             
	            fw = new FileWriter(filePath, true);//  
	            // 创建FileWriter对象，用来写入字符流  
	            bw = new BufferedWriter(fw); // 将缓冲对文件的输出  
	            bw.write(datetime + "\n");
	            
	            for(int i = 0;i < data.size();i++){
	            	Log.e(TAG,data.get(i) );
	            	bw.write(data.get(i)+" ");
	            }  
	            bw.flush(); // 刷新该流的缓冲  
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
