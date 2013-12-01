package com.davehoag.ib.chart;

import java.awt.GridLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;

import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.BarCache;

/**
 * Graph studies
 * @author David Hoag
 *
 */
public class SAR implements PriceStudy {
	double [] sarCurve = new double [30];
	XYDotRenderer dotRender = new XYDotRenderer();
	JCheckBox check = new JCheckBox("Enabled", false);
	JTextField accelField;
	
	{
		accelField = new JTextField();
		accelField.setText(".005");
	}
	/* (non-Javadoc)
	 * @see com.davehoag.ib.chart.PriceStudy#getPriceData(com.davehoag.ib.dataTypes.Bar, com.davehoag.ib.dataTypes.BarCache)
	 */
	@Override
	public double getPriceData(final Bar currentBar, final BarCache cache ){
		if(cache.size() < sarCurve.length) return 0.0;
		double accel = .005;
		try{
			accel = Double.parseDouble(accelField.getText());
		} catch(Exception e){ System.out.println(e); }
		return cache.getParabolicSar(0,sarCurve, accel)[sarCurve.length - 1];
	}
	/* (non-Javadoc)
	 * @see com.davehoag.ib.chart.PriceStudy#getRenderer()
	 */
	@Override
	public XYItemRenderer getRenderer(){
		System.out.println(dotRender.getDotHeight() + " " + dotRender.getDotWidth());
		dotRender.setDotHeight(3);
		dotRender.setDotWidth(3);
		return dotRender;
	}
	/* (non-Javadoc)
	 * @see com.davehoag.ib.chart.PriceStudy#getName()
	 */
	@Override
	public String getName(){
		return "SAR";
	}
	/* (non-Javadoc)
	 * @see com.davehoag.ib.chart.PriceStudy#getPropertyPanel()
	 */
	@Override
	public JPanel getPropertyPanel(){
		JPanel panel = new JPanel(false);
	    JPanel filler = new JPanel(false);
	    filler.setLayout(new GridLayout(1,2));
	    filler.add( new JLabel("Accel Fact:"));
	    filler.add(accelField);
	    panel.setLayout(new GridLayout(2, 1));
	    panel.add(filler);
	    panel.add(check);
	    return panel;
	}
	/* (non-Javadoc)
	 * @see com.davehoag.ib.chart.PriceStudy#isActive()
	 */
	@Override
	public boolean isActive(){
		return check.isSelected();
	}
	@Override
	public String toString(){
		return getName();
	}
}
