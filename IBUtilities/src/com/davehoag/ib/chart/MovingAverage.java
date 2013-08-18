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
public class MovingAverage implements PriceStudy {
	double [] sarCurve = new double [30];
	XYDotRenderer dotRender = new XYDotRenderer();
	JCheckBox checkCB = new JCheckBox("Enabled", false);
	JTextField periodsTF;
	
	{
		periodsTF = new JTextField();
		periodsTF.setText("12");
	}
	/* (non-Javadoc)
	 * @see com.davehoag.ib.chart.PriceStudy#getPriceData(com.davehoag.ib.dataTypes.Bar, com.davehoag.ib.dataTypes.BarCache)
	 */
	@Override
	public double getPriceData(final Bar currentBar, final BarCache cache ){
		int periods = 12;
		try{
			periods = Integer.parseInt(periodsTF.getText());
		} catch(Exception e){ System.out.println(e); }

		if(cache.size() < periods) return currentBar.wap;
		return cache.getMA(periods, 'w');
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
		return "MA";
	}
	/* (non-Javadoc)
	 * @see com.davehoag.ib.chart.PriceStudy#getPropertyPanel()
	 */
	@Override
	public JPanel getPropertyPanel(){
		JPanel panel = new JPanel(false);
	    JPanel filler = new JPanel(false);
	    filler.setLayout(new GridLayout(1,2));
	    filler.add( new JLabel("Periods:"));
	    filler.add(periodsTF);
	    panel.setLayout(new GridLayout(2, 1));
	    panel.add(filler);
	    panel.add(checkCB);
	    return panel;
	}
	/* (non-Javadoc)
	 * @see com.davehoag.ib.chart.PriceStudy#isActive()
	 */
	@Override
	public boolean isActive(){
		return checkCB.isSelected();
	}
	@Override
	public String toString(){
		return getName();
	}
}
