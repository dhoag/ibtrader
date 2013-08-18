package com.davehoag.ib.chart;

import java.awt.GridLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

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
	JCheckBox check = new JCheckBox("Enabled", false);
	
	public double getPriceData(final Bar currentBar, final BarCache cache ){
		cache.push(currentBar);
		if(cache.size() < sarCurve.length) return 0.0;
		return cache.getParabolicSar(sarCurve, .005)[sarCurve.length - 1];
	}
	public XYItemRenderer getRenderer(){
		System.out.println(dotRender.getDotHeight() + " " + dotRender.getDotWidth());
		dotRender.setDotHeight(3);
		dotRender.setDotWidth(3);
		return dotRender;
	}
	public String getName(){
		return "SAR";
	}
	public JPanel getPropertyPanel(){
		JPanel panel = new JPanel(false);
	    JLabel filler = new JLabel("HEY");
	    filler.setHorizontalAlignment(JLabel.CENTER);
	    panel.setLayout(new GridLayout(2, 1));
	    panel.add(filler);
	    panel.add(check);
	    return panel;
	}
	public boolean isActive(){
		return check.isSelected();
	}
	@Override
	public String toString(){
		return getName();
	}
}
