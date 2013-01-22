package com.davehoag.ib.chart;

import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;

import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.BarCache;

/**
 * Graph studies
 * @author David Hoag
 *
 */
public class SAR {
	double [] sarCurve = new double [30];
	XYDotRenderer dotRender = new XYDotRenderer();
	
	public double getPriceData(final Bar currentBar, final BarCache cache ){
		cache.pushLatest(currentBar);
		if(cache.size() < sarCurve.length) return 0.0;
		return cache.getParabolicSar(sarCurve, .005)[sarCurve.length - 1];
	}
	public XYItemRenderer getRenderer(){
		System.out.println(dotRender.getDotHeight() + " " + dotRender.getDotWidth());
		dotRender.setDotHeight(3);
		dotRender.setDotWidth(3);
		return dotRender;
	}
}
