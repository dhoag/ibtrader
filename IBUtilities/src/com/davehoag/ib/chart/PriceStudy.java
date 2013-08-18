package com.davehoag.ib.chart;

import javax.swing.JPanel;

import org.jfree.chart.renderer.xy.XYItemRenderer;

import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.BarCache;

public interface PriceStudy {

	public abstract double getPriceData(Bar currentBar, BarCache cache);

	public abstract XYItemRenderer getRenderer();

	public abstract String getName();

	public abstract JPanel getPropertyPanel();

	public abstract boolean isActive();

}