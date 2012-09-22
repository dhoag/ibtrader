package com.davehoag.ib.chart;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.ChartColor;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.HighLowRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;

public class OpenCloseRenderer extends HighLowRenderer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2220795815275007290L;

	/**
	 * Provide my own implementation of this method simply to change the colors on bars that
	 * represent up price movements versus those that are down.
	 * 
	 */
	@Override
	public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea,
			PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis,
			XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {
		if (dataset instanceof OHLCDataset) {
			OHLCDataset hld = (OHLCDataset) dataset;
			double x = dataset.getXValue(series, item);
			if (!domainAxis.getRange().contains(x)) {
				return; // the x value is not within the axis range
			}
			double xx = domainAxis.valueToJava2D(x, dataArea, plot.getDomainAxisEdge());
	
			// setup for collecting optional entity info...
			Shape entityArea = null;
			EntityCollection entities = null;
			if (info != null) {
				entities = info.getOwner().getEntityCollection();
			}
	
			RectangleEdge location = plot.getRangeAxisEdge();
	
			Stroke itemStroke = getItemStroke(series, item);
			
			g2.setStroke(itemStroke);

			double yHigh = hld.getHighValue(series, item);
			double yLow = hld.getLowValue(series, item);
			double yOpen = hld.getOpenValue(series, item);
			double yClose = hld.getCloseValue(series, item);
			Paint barColor = (yClose > yOpen) ? ChartColor.DARK_GREEN:  (yClose == yOpen) ? ChartColor.BLACK : ChartColor.DARK_RED;
			g2.setPaint(barColor);

			if (!Double.isNaN(yHigh) && !Double.isNaN(yLow)) {
				double yyHigh = rangeAxis.valueToJava2D(yHigh, dataArea, location);
				double yyLow = rangeAxis.valueToJava2D(yLow, dataArea, location);
				g2.draw(new Line2D.Double(xx, yyLow, xx, yyHigh));
				entityArea = new Rectangle2D.Double(xx - 1.0, Math.min(yyLow, yyHigh), 4.0,
						Math.abs(yyHigh - yyLow));
			}

			double delta = getTickLength();
			if (!Double.isNaN(yOpen)) {
				double yyOpen = rangeAxis.valueToJava2D(yOpen, dataArea, location);
				g2.draw(new Line2D.Double(xx - delta, yyOpen, xx, yyOpen));
			}
			if (!Double.isNaN(yClose)) {
				double yyClose = rangeAxis.valueToJava2D(yClose, dataArea, location);
				g2.draw(new Line2D.Double(xx, yyClose, xx + delta, yyClose));
			}

			if (entities != null) {
				addEntity(entities, entityArea, dataset, series, item, 0.0, 0.0);
			}
		} else {
			// not a HighLowDataset, so just draw a line connecting this point
			super.drawItem( g2,  state,  dataArea,
					 info,  plot,  domainAxis,  rangeAxis,
					 dataset,  series,  item,  crosshairState,  pass);
		}


	}
}
