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
     private XYMultipleSeriesDataset multipleSeriesDataset;// 数据集容器
     private XYMultipleSeriesRenderer multipleSeriesRenderer;// 渲染器容器
     /**曲线数量*/
     private static final int SERIES_NR=4;
     private static final String TAG = "message";
     private ArrayList<XYSeries> mSeries = new ArrayList<XYSeries>();
//     private XYSeries mSeries;// 曲线数据集数组
     private ArrayList<XYSeriesRenderer> mRenderer = new ArrayList<XYSeriesRenderer>();// 曲线渲染器数组
     private Context context;
     private double addY;
     private static  int addX;
 	 private Random random=new Random();
 	 /**时间数据*/
//     long[] xcache = new long[20];
 	/**y轴数据*/
//     int[] ycache = new int[20];
 
    public ChartService(Context context) {
         this.context = context;
     }
 
     /**
      * 获取图表
      *       
      * @return
      */
     public GraphicalView getGraphicalView() {
         mGraphicalView = ChartFactory.getCubeLineChartView(context,
                 multipleSeriesDataset, multipleSeriesRenderer, 0.1f);
         return mGraphicalView;
     }

     /**
      * 获取数据集，及xy坐标的集合
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
      * 获取渲染器
      * 
      * @param maxX
      *            x轴最大值
      * @param maxY
      *            y轴最大值
     * @param chartTitle
      *            曲线的标题
      * @param xTitle
      *            x轴标题
      * @param yTitle
      *            y轴标题
      * @param axeColor
      *            坐标轴颜色
      * @param labelColor
      *            标题颜色
      * @param curveColor
      *            曲线颜色
      * @param gridColor
      *            网格颜色
      */
     public void setXYMultipleSeriesRenderer(double minY,double maxY,double minX,double maxX,
             String chartTitle, String xTitle, String yTitle, int axeColor,
             int labelColor, int[] curveColor, int gridColor) {
         multipleSeriesRenderer = new XYMultipleSeriesRenderer();
         if (chartTitle != null) {
             multipleSeriesRenderer.setChartTitle(chartTitle);
         }
         multipleSeriesRenderer.setXTitle(xTitle);//x轴说明
         multipleSeriesRenderer.setYTitle(yTitle);//y轴说明
//         multipleSeriesRenderer.setRange(new double[] { 0, maxX, 0, maxY });//xy轴的范围
         multipleSeriesRenderer.setMargins(new int[]{5,5,5,5});//设置空白区大小
         multipleSeriesRenderer.setMarginsColor(Color.TRANSPARENT);//设置空白区颜色
         multipleSeriesRenderer.setLabelsTextSize(15);//设置坐标轴的字体大小
         multipleSeriesRenderer.setAxisTitleTextSize(15);//设置坐标轴标题的字体大小

         multipleSeriesRenderer.setShowAxes(true);//隐藏坐标轴
//         multipleSeriesRenderer.setShowLegend(false);//隐藏图例
         multipleSeriesRenderer.setPanEnabled(false,false);//曲线不可滑动触摸
         multipleSeriesRenderer.setLabelsColor(labelColor);//数轴刻度颜色
         multipleSeriesRenderer.setXLabels(300);//x轴数轴刻度数，不设置默认为5
         multipleSeriesRenderer.setYLabels(10);//y轴数轴刻度数
         multipleSeriesRenderer.setXLabelsAlign(Align.RIGHT);
         multipleSeriesRenderer.setYLabelsAlign(Align.RIGHT);
//         multipleSeriesRenderer.setAxisTitleTextSize(20);
         multipleSeriesRenderer.setChartTitleTextSize(20);
//         multipleSeriesRenderer.setLabelsTextSize(20);
         multipleSeriesRenderer.setLegendTextSize(20);
         multipleSeriesRenderer.setPointSize(0.2f);//曲线描点尺寸
         multipleSeriesRenderer.setFitLegend(true);
//         multipleSeriesRenderer.setMargins(new int[] { 20, 30, 15, 20 });
         multipleSeriesRenderer.setShowGrid(false);//不显示网格
         multipleSeriesRenderer.setZoomEnabled(true, false);
         multipleSeriesRenderer.setAxesColor(axeColor);
//         multipleSeriesRenderer.setGridColor(gridColor);
         multipleSeriesRenderer.setBackgroundColor(Color.WHITE);//背景色
//         multipleSeriesRenderer.setMarginsColor(Color.WHITE);//边距背景色，默认背景色为黑色，这里修改为白色
		 multipleSeriesRenderer.setInScroll(true);  //调整大小
		 multipleSeriesRenderer.setYAxisMax(maxY);//y轴范围
		 multipleSeriesRenderer.setYAxisMin(minY);
		 multipleSeriesRenderer.setXAxisMax(maxX);//x轴范围
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
	         mRenderer.get(no).setPointStyle(PointStyle.POINT);//描点风格，可以为圆点，方形点等等
	         multipleSeriesRenderer.addSeriesRenderer(mRenderer.get(no));
	     }
         
//        
         
     }
     /**
      * 根据新加的数据，更新曲线，只能运行在主线程
      * 
      */
     public void updateChart(double[][] addY) {
		    //设定长度为720,最多只显示720个数据
//		    int length = mSeries.get(0).getItemCount();
//		    if(length>=20) 
//		     length = 20;
//		    addY= Math.random()*6.6;
    	 int j = 0;
    	 while(j < 5){
    		 if(addX <= 500){
    			    for(int no = 0;no < SERIES_NR;no++){
    			    	  //将前面的点放入缓存
//    			    	for (int i = 0; i < length; i++) {
//    						xcache[i] = (long)mSeries.get(no).getX(i);
//    						ycache[i] = (int) mSeries.get(no).getY(i);
//    					}
    			    	
//    					mSeries.get(no).clear();
    					//将新产生的点首先加入到点集中，然后在循环体中将坐标变换后的一系列点都重新加入到点集中
    					//这里可以试验一下把顺序颠倒过来是什么效果，即先运行循环体，再添加新产生的点
    					//???
    			    	 
    					mSeries.get(no).add(addX,addX,addY[j][no]);
    			    }
    			    addX++;
    			   
    	    	 }else{
    	    		 addX = 0;
    	    		 for(int no = 0;no < SERIES_NR;no++){
    	    			
    	    			 //TODO 暂时这么做
    	    			 multipleSeriesDataset.removeSeries(mSeries.get(no));
    	    			 mSeries.get(no).clear();
//    	    			 mSeries.get(no).remove(addX);
//    	    			 mSeries.get(no).clear();
//    	    			 Log.i(TAG, "mSeries.get[no].getX("+addX+")="+mSeries.get(no).getY(addX));
    					 mSeries.get(no).add(addX,addX,addY[j][no]);
    					 multipleSeriesDataset.addSeries(mSeries.get(no));
    					
//    					 Log.i(TAG, "mSeries.get[no].getX("+addX+")="+mSeries.get(no).getY(addX));
    					//在数据集中添加新的点集
//    						
//    						
    	    		 }
    	    	 } 
    	 j++;
    	 }
    	 Log.i("手动刷新", "...");
		 mGraphicalView.repaint();
    	 
					
				
//				for (int k = 0; k < length; k++) {
//					mSeries.get(no).add(xcache[k], ycache[k]);
//		    	}
//				
			//曲线更新
    	 //TODO 改为局部刷新
			
	    }
 
  
 }
