package com.phvalt.accelerometergraphrendring;

import java.util.ArrayList;
import java.util.Arrays;

import org.achartengine.ChartFactory;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;


import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

public class ShowSpectrumActivity extends Activity{

	private double[] frequencies;
	private double[] magnitudes;
	private LinearLayout layout;
    private View mChart;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_graph_layout);
		layout = (LinearLayout) findViewById(R.id.chart_container);
		
		Bundle b=this.getIntent().getExtras();
		frequencies = b.getDoubleArray("frequences");
		magnitudes = b.getDoubleArray("amplitudes");
		
		openChart();
	}

	private void openChart() {
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		XYSeries xSeries = new XYSeries("X");
		for (int i=0;i<frequencies.length/2;i++){
			xSeries.add(frequencies[i],magnitudes[i]);
		}
		dataset.addSeries(xSeries);
		
		XYSeriesRenderer xRenderer = new XYSeriesRenderer();
		xRenderer.setColor(Color.RED);
        xRenderer.setPointStyle(PointStyle.CIRCLE);
        xRenderer.setFillPoints(true);
        xRenderer.setLineWidth(1);
        xRenderer.setChartValuesTextSize(15);
        xRenderer.setDisplayChartValues(false);

        XYMultipleSeriesRenderer multiRenderer = new XYMultipleSeriesRenderer();
        	multiRenderer.addSeriesRenderer(xRenderer);
        	multiRenderer.setZoomButtonsVisible(true);
        	multiRenderer.setExternalZoomEnabled(true);
        	multiRenderer.setZoomEnabled(true);
        	multiRenderer.setChartTitle("Spectrum analysis");
        	multiRenderer.setLabelsTextSize(30);
        	multiRenderer.setBackgroundColor(getResources().getColor(R.color.background));
        	multiRenderer.setApplyBackgroundColor(true);
        	multiRenderer.setGridColor(getResources().getColor(R.color.zero_line));
        	multiRenderer.setShowGrid(true);
        mChart = ChartFactory.getBarChartView(getBaseContext(), dataset,
                multiRenderer, Type.DEFAULT);
      
        // Adding the Line Chart to the LinearLayout
        layout.addView(mChart);   
	}
}
