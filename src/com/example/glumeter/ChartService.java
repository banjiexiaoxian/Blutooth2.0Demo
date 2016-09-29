package com.example.glumeter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.util.Log;
 
public class ChartService {
 
     private GraphicalView mGraphicalView;
     private XYMultipleSeriesDataset multipleSeriesDataset;// ���ݼ�����
     private XYMultipleSeriesRenderer multipleSeriesRenderer;// ��Ⱦ������
     /**��������*/
     private static final int SERIES_NR=4;
     private static final String TAG = "message";
     private ArrayList<XYSeries> mSeries = new ArrayList<XYSeries>();
//     private XYSeries mSeries;// �������ݼ�����
     private ArrayList<XYSeriesRenderer> mRenderer = new ArrayList<XYSeriesRenderer>();// ������Ⱦ������
     private Context context;
     private double addY;
     private static  int addX;
 	 private Random random=new Random();
 	 /**ʱ������*/
//     long[] xcache = new long[20];
 	/**y������*/
//     int[] ycache = new int[20];
 
    public ChartService(Context context) {
         this.context = context;
     }
 
     /**
      * ��ȡͼ��
      *       
      * @return
      */
     public GraphicalView getGraphicalView() {
         mGraphicalView = ChartFactory.getCubeLineChartView(context,
                 multipleSeriesDataset, multipleSeriesRenderer, 0.1f);
         return mGraphicalView;
     }

     /**
      * ��ȡ���ݼ�����xy����ļ���
      * 
      * @param curveTitle
      */
     public void setXYMultipleSeriesDataset(String curveTitle) {
         multipleSeriesDataset = new XYMultipleSeriesDataset();
//         long value = new Date().getTime();
         String[] WaveLength = {"850","875","940","1050"};
         for (int i = 0; i < SERIES_NR; i++) {
        	 XYSeries ts = new XYSeries(WaveLength[i]);
        	 mSeries.add(i, ts) ; 
//        	 for (int k = 0; k < 300; k++) {
//        		 mSeries.get(i).add(k,k,i*3.3);
// 		      } 
//         mSeries = new XYSeries(curveTitle);
         multipleSeriesDataset.addSeries(mSeries.get(i));
         }
//         mSeries2 = new XYSeries(curveTitle);
//         multipleSeriesDataset.addSeries(mSeries2);
     }
 
     /**
      * ��ȡ��Ⱦ��
      * 
      * @param maxX
      *            x�����ֵ
      * @param maxY
      *            y�����ֵ
     * @param chartTitle
      *            ���ߵı���
      * @param xTitle
      *            x�����
      * @param yTitle
      *            y�����
      * @param axeColor
      *            ��������ɫ
      * @param labelColor
      *            ������ɫ
      * @param curveColor
      *            ������ɫ
      * @param gridColor
      *            ������ɫ
      */
     public void setXYMultipleSeriesRenderer(double minY,double maxY,double minX,double maxX,
             String chartTitle, String xTitle, String yTitle, int axeColor,
             int labelColor, int[] curveColor, int gridColor) {
         multipleSeriesRenderer = new XYMultipleSeriesRenderer();
         if (chartTitle != null) {
             multipleSeriesRenderer.setChartTitle(chartTitle);
         }
         multipleSeriesRenderer.setXTitle(xTitle);//x��˵��
         multipleSeriesRenderer.setYTitle(yTitle);//y��˵��
//         multipleSeriesRenderer.setRange(new double[] { 0, maxX, 0, maxY });//xy��ķ�Χ
         multipleSeriesRenderer.setMargins(new int[]{5,5,5,5});//���ÿհ�����С
         multipleSeriesRenderer.setMarginsColor(Color.TRANSPARENT);//���ÿհ�����ɫ
         multipleSeriesRenderer.setLabelsTextSize(15);//����������������С
         multipleSeriesRenderer.setAxisTitleTextSize(15);//�������������������С

         multipleSeriesRenderer.setShowAxes(true);//����������
//         multipleSeriesRenderer.setShowLegend(false);//����ͼ��
         multipleSeriesRenderer.setPanEnabled(false,false);//���߲��ɻ�������
         multipleSeriesRenderer.setLabelsColor(labelColor);//����̶���ɫ
         multipleSeriesRenderer.setXLabels(300);//x������̶�����������Ĭ��Ϊ5
         multipleSeriesRenderer.setYLabels(10);//y������̶���
         multipleSeriesRenderer.setXLabelsAlign(Align.RIGHT);
         multipleSeriesRenderer.setYLabelsAlign(Align.RIGHT);
//         multipleSeriesRenderer.setAxisTitleTextSize(20);
         multipleSeriesRenderer.setChartTitleTextSize(20);
//         multipleSeriesRenderer.setLabelsTextSize(20);
         multipleSeriesRenderer.setLegendTextSize(20);
         multipleSeriesRenderer.setPointSize(0.2f);//�������ߴ�
         multipleSeriesRenderer.setFitLegend(true);
//         multipleSeriesRenderer.setMargins(new int[] { 20, 30, 15, 20 });
         multipleSeriesRenderer.setShowGrid(false);//����ʾ����
         multipleSeriesRenderer.setZoomEnabled(true, false);
         multipleSeriesRenderer.setAxesColor(axeColor);
//         multipleSeriesRenderer.setGridColor(gridColor);
         multipleSeriesRenderer.setBackgroundColor(Color.WHITE);//����ɫ
//         multipleSeriesRenderer.setMarginsColor(Color.WHITE);//�߾౳��ɫ��Ĭ�ϱ���ɫΪ��ɫ�������޸�Ϊ��ɫ
		 multipleSeriesRenderer.setInScroll(true);  //������С
		 multipleSeriesRenderer.setYAxisMax(maxY);//y�᷶Χ
		 multipleSeriesRenderer.setYAxisMin(minY);
		 multipleSeriesRenderer.setXAxisMax(maxX);//x�᷶Χ
		 multipleSeriesRenderer.setXAxisMin(minX);
		 
//		 XYSeriesRenderer r = new XYSeriesRenderer();
//		 r.setColor(Color.BLUE);
//	     r.setChartValuesTextSize(15);
//	     r.setChartValuesSpacing(3);
//	     r.setPointStyle(PointStyle.CIRCLE);
//	     r.setFillPoints(true);
//	     multipleSeriesRenderer.addSeriesRenderer(r);
	     for(int no = 0;no < SERIES_NR;no++){
	    	 mRenderer.add(no,new XYSeriesRenderer());
	    	 mRenderer.get(no).setColor(curveColor[no]);
	         mRenderer.get(no).setPointStyle(PointStyle.POINT);//����񣬿���ΪԲ�㣬���ε�ȵ�
	         multipleSeriesRenderer.addSeriesRenderer(mRenderer.get(no));
	     }
         
//        
         
     }
     /**
      * �����¼ӵ����ݣ��������ߣ�ֻ�����������߳�
      * 
      */
     public void updateChart(double[][] addY) {
		    //�趨����Ϊ720,���ֻ��ʾ720������
//		    int length = mSeries.get(0).getItemCount();
//		    if(length>=20) 
//		     length = 20;
//		    addY= Math.random()*6.6;
    	 int j = 0;
    	 while(j < 5){
    		 if(addX <= 500){
    			    for(int no = 0;no < SERIES_NR;no++){
    			    	  //��ǰ��ĵ���뻺��
//    			    	for (int i = 0; i < length; i++) {
//    						xcache[i] = (long)mSeries.get(no).getX(i);
//    						ycache[i] = (int) mSeries.get(no).getY(i);
//    					}
    			    	
//    					mSeries.get(no).clear();
    					//���²����ĵ����ȼ��뵽�㼯�У�Ȼ����ѭ�����н�����任���һϵ�е㶼���¼��뵽�㼯��
    					//�����������һ�°�˳��ߵ�������ʲôЧ������������ѭ���壬������²����ĵ�
    					//???
    			    	 
    					mSeries.get(no).add(addX,addX,addY[j][no]);
    			    }
    			    addX++;
    			   
    	    	 }else{
    	    		 addX = 0;
    	    		 for(int no = 0;no < SERIES_NR;no++){
    	    			
    	    			 //TODO ��ʱ��ô��
    	    			 multipleSeriesDataset.removeSeries(mSeries.get(no));
    	    			 mSeries.get(no).clear();
//    	    			 mSeries.get(no).remove(addX);
//    	    			 mSeries.get(no).clear();
//    	    			 Log.i(TAG, "mSeries.get[no].getX("+addX+")="+mSeries.get(no).getY(addX));
    					 mSeries.get(no).add(addX,addX,addY[j][no]);
    					 multipleSeriesDataset.addSeries(mSeries.get(no));
    					
//    					 Log.i(TAG, "mSeries.get[no].getX("+addX+")="+mSeries.get(no).getY(addX));
    					//�����ݼ�������µĵ㼯
//    						
//    						
    	    		 }
    	    	 } 
    	 j++;
    	 }
    	 Log.i("�ֶ�ˢ��", "...");
		 mGraphicalView.repaint();
    	 
					
				
//				for (int k = 0; k < length; k++) {
//					mSeries.get(no).add(xcache[k], ycache[k]);
//		    	}
//				
			//���߸���
    	 //TODO ��Ϊ�ֲ�ˢ��
			
	    }
 
  
 }
