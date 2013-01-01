package com.davehoag.ib.chart;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;

import org.apache.commons.lang.StringUtils;
import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SegmentedTimeline;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.HighLowItemLabelGenerator;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.HighLowRenderer;
import org.jfree.chart.renderer.xy.SamplingXYLineRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.time.ohlc.OHLCItem;
import org.jfree.data.time.ohlc.OHLCSeries;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYBarDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.Layer;
import org.jfree.ui.RefineryUtilities;

import com.davehoag.ib.CassandraDao;
import com.davehoag.ib.Strategy;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.BarIterator;
import com.davehoag.ib.dataTypes.LimitOrder;
import com.davehoag.ib.dataTypes.Portfolio;
import com.davehoag.ib.tools.SimulateTrading;
import com.davehoag.ib.util.HistoricalDateManipulation;

/**
 * Plot historical data
 * 
 * @author David Hoag
 * 
 */
public class HistoricalDataChart extends ApplicationFrame {
	Strategy currentStrategy;
	private static final long serialVersionUID = 1L;
	private static final double barWidth = 3.0;
	OHLCSeriesCollection priceData;
	OHLCSeries candlestickSeries;
	TimeSeriesCollection volumeData;
	TimeSeries volumeSeries;
	JFreeChart chart;
	JTextField startTF;
	JTextField stratTF;
	JTextField endTF;
	JTextField symbolTF;
	XYPlot pricePlot;
	CombinedScrollBar scrollBar;
	Strategy strategy;

	{
		// set a theme using the new shadow generator feature available in
		// 1.0.14 - for backwards compatibility it is not enabled by default
		ChartFactory.setChartTheme(new StandardChartTheme("JFree/Shadow", true));
	}

	/**
	 * A demonstration application showing how to create a simple time series
	 * chart. This example uses monthly data.
	 * 
	 * @param title
	 *            the frame title.
	 */
	public HistoricalDataChart(String title) {
		super(title);
		JPanel chartPanel = createMainContent();
		chartPanel.setPreferredSize(new Dimension(500, 370));
		setContentPane(chartPanel);
	}

	protected JPanel createInputFields() {
		JPanel top = new JPanel();
		top.add(new JLabel("Start & End Dates:"));
		startTF = new JTextField();
		DateFormat df = new SimpleDateFormat("yyyyMMdd");
		String dateText = df.format(new Date(System.currentTimeMillis()));
		startTF.setText(dateText);
		endTF = new JTextField();
		endTF.setText(dateText);
		symbolTF = new JTextField();
		symbolTF.setText("QQQ");
		top.add(startTF);
		top.add(endTF);
		top.add(symbolTF);
		stratTF = new JTextField();
		stratTF.setText("SimpleMomentum");
		top.add(stratTF);
		JButton refresh = new JButton("Get");
		refresh.addActionListener(getRefreshDelegate());
		top.add(refresh);
		JButton run = new JButton("Run");
		run.addActionListener(getRunDelegate());
		top.add(run);
		return top;
	}

	/**
	 * Create action list to get the historical data
	 * 
	 * @return
	 */
	protected ActionListener getRunDelegate() {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				runStrategy();
				refresh();
			}
		};
	}

	public String getStrategyName() {
		String strategyName = stratTF.getText();
		int idx = strategyName.indexOf(":");
		if (idx > 0) {
			strategyName = strategyName.substring(0, idx);
		}
		return strategyName;
	}

	public String getStrategyParms() {
		String strategyName = stratTF.getText();
		int idx = strategyName.indexOf(":");
		if (idx < 0) return "";

		final String initParms = strategyName.substring(idx + 1, strategyName.length());
		return initParms;
	}
	public void runStrategy() {
		final String strategyName = getStrategyName();
		final String initParms = getStrategyParms();
		final String aSymbol = StringUtils.upperCase(symbolTF.getText());
		final String startStr = startTF.getText();
		final String endStr = endTF.getText();
		try {
			strategy = SimulateTrading
					.runSimulation(strategyName, startStr, endStr, aSymbol, initParms);
			Portfolio port = strategy.getPortfolio();
			port.displayTradeStats(strategyName);
			port.dumpLog();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Mark the trading activity on the chart.
	 * 
	 * @param trades
	 */
	private void updateChart(ArrayList<LimitOrder> trades) {
		for (Object obj : pricePlot.getAnnotations()) {
			XYAnnotation ann = (XYAnnotation) obj;
			pricePlot.removeAnnotation(ann);
		}
		for (LimitOrder closingTrade : trades) {
			long start = closingTrade.getOnset().getPortfolioTime() * 1000;
			long end = closingTrade.getPortfolioTime() * 1000;
			double top = Math.max(closingTrade.getPrice(), closingTrade.getOnset().getPrice());
			double bottom = Math.min(closingTrade.getPrice(), closingTrade.getOnset().getPrice());
			XYShapeAnnotation shape = new XYShapeAnnotation(new Rectangle2D.Double(start - 500, bottom - .02,
					end - start + 500, top - bottom + .05));

			shape.setToolTipText("" + closingTrade.getOnset().getPrice() + "-" + closingTrade.getPrice());
			pricePlot.addAnnotation(shape);
		}

	}

	/**
	 * Create action list to get the historical data
	 * 
	 * @return
	 */
	protected ActionListener getRefreshDelegate() {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refresh();
			}
		};
	}

	/**
	 * Test live updates?
	 */
	public void refresh() {
		try {
			final String startStr = startTF.getText();
			final long startTime = HistoricalDateManipulation.getTime(startStr + " 08:30:00");
			final String endStr = endTF.getText();
			final long endTime = HistoricalDateManipulation.getTime(endStr + " 15:10:00");
			final String aSymbol = StringUtils.upperCase(symbolTF.getText());
			Thread t = new Thread(paintHistoricalData(aSymbol, startTime, endTime));
			t.start();
		} catch (ParseException pe) {
			startTF.setBackground(Color.RED);
			endTF.setBackground(Color.RED);
		}
	}

	/**
	 * @return
	 */
	protected Runnable paintHistoricalData(final String aSymbol, final long startTime, final long endTime)
			throws ParseException {

		priceData.removeAllSeries();
		volumeData.removeAllSeries();

		return new Runnable() {
			@Override
			public void run() {
				ArrayList<TimeSeries> strategyLines = new ArrayList<TimeSeries>();
				
				System.out.println("Getting " + aSymbol + " "
						+ HistoricalDateManipulation.getDateAsStr(startTime) + " - "
						+ HistoricalDateManipulation.getDateAsStr(endTime));
				BarIterator bars = CassandraDao.getInstance().getData(aSymbol, startTime, endTime, "bar5sec");
				Bar first = null;
				Bar last = null;
				double highestHigh = 0;
				double lowestLow = 999999;
				int count = 0;
				for (Bar aBar : bars) {
					count++;
					last = aBar;
					final Second sec = new Second(aBar.getTime());

					final double open = aBar.open;
					final double high = aBar.high;
					final double low = aBar.low;
					final double close = aBar.close;
					if (first == null) {
						first = aBar;
						final OHLCItem ohlc = new OHLCItem(sec, open, high, low, close);

						candlestickSeries = new OHLCSeries(ohlc);
						final TimeSeriesDataItem di = new TimeSeriesDataItem(sec, aBar.volume);
						volumeSeries = new TimeSeries(di);

						if (strategy != null) {
							double[] lines = strategy.getStrategyData(aBar);
							strategy.init(getStrategyParms());
							for (double priceData : lines) {
								TimeSeriesDataItem mdi = new TimeSeriesDataItem(sec, priceData);
								TimeSeries series = new TimeSeries(mdi);
								strategyLines.add(series);
							}
						}
					}
					else {
						candlestickSeries.add(sec, open, high, low, close);
						volumeSeries.add(sec, aBar.volume);
						if (strategy != null) {
							double[] lines = strategy.getStrategyData(aBar);
							for (int i = 0; i < strategyLines.size(); i++) {
								strategyLines.get(i).add(sec, lines[i]);
							}
						}
					}
					lowestLow = lowestLow > low ? low : lowestLow;
					highestHigh = highestHigh < high ? high : highestHigh;
				}
				updateAxis(first, last, highestHigh, lowestLow);

				priceData.addSeries(candlestickSeries);
				volumeData.addSeries(volumeSeries);

				if (strategy != null) {
					updateChart(strategy.getPortfolio().getTrades());
				}
				if (strategyLines.size() > 0) {
					addStrategyLines(strategyLines, pricePlot);
				}
				System.out.println("Displaying " + count + " records. " + last.getTime());
				if(scrollBar != null) scrollBar.updateScrollBarRanges();
				HistoricalDataChart.this.repaint();
			}

			/**
			 * @param strategyLines
			 */
			protected void addStrategyLines(ArrayList<TimeSeries> strategyLines, XYPlot plot) {
				TimeSeriesCollection maCollection = new TimeSeriesCollection();
				int i = 0;
				SamplingXYLineRenderer lineRender = new SamplingXYLineRenderer();
				for (TimeSeries t : strategyLines) {
					maCollection.addSeries(t);
					// lineRender.setSeriesPaint(i++, Color.white);
				}
				plot.setDataset(1, maCollection);
				plot.setRenderer(1, lineRender);
			}

			/**
			 * Add annotations to the historical chart
			 * 
			 * @param first
			 * @param last
			 * @param highestHigh
			 * @param lowestLow
			 */
			protected void updateAxis(final Bar first, final Bar last, final double highestHigh,
					final double lowestLow) {
				if(last == null) return;
				
				final CombinedDomainXYPlot plot = (CombinedDomainXYPlot) chart.getXYPlot();

				final ValueAxis timeAxis = plot.getDomainAxis();
				((DateAxis)timeAxis).setMinimumDate(first.getTime());
				((DateAxis)timeAxis).setMaximumDate(last.getTime());
				//timeAxis.setLowerBound(first.originalTime * 1000);
				//timeAxis.setUpperBound(last.originalTime * 1000);

				// addPriceMarker(first, last, highestHigh, lowestLow);

				final ValueAxis priceAxis = plot.getRangeAxis(0);
				if (priceAxis != null) {
					priceAxis.setLowerBound(lowestLow);
					priceAxis.setUpperBound(highestHigh);
				}
			}

			/**
			 * @param first
			 * @param last
			 * @param highestHigh
			 * @param lowestLow
			 */
			protected void addPriceMarker(final Bar first, final Bar last, final double highestHigh,
					final double lowestLow) {
				double halfway = (last.originalTime * 1000 + first.originalTime * 1000) / 2;
				double markerSpot = (highestHigh + lowestLow) / 2;

				ValueMarker marker = new ValueMarker(markerSpot);
				System.out.println("Adding value marker " + marker.getValue());
				marker.setLabel("A longer label of Test");
				marker.setPaint(ChartColor.DARK_BLUE);
				pricePlot.addRangeMarker(marker, Layer.FOREGROUND);
			};
		};
	}

	public JPanel createMainContent() {
		createDataset();
		JPanel contentPanel = new JPanel(new BorderLayout());
		
		JPanel top = createInputFields();
		contentPanel.add(top, BorderLayout.NORTH);

		ChartPanel combinedChartPanel = createChartPanels();
		contentPanel.add(combinedChartPanel, BorderLayout.CENTER);
		
	//	scrollBar = new CombinedScrollBar((CombinedDomainXYPlot)chart.getXYPlot());
	//	contentPanel.add(scrollBar, BorderLayout.SOUTH);

		return contentPanel;
	}

	/**
	 * @return
	 */
	protected ChartPanel createChartPanels() {
		// Put the chart in a new ChartPanel, set the panel attributes and
		// return it.
		chart = createJFreeChartAndPlots(priceData, volumeData);

		ChartPanel combinedChartPanel = new ChartPanel(chart);
		combinedChartPanel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		combinedChartPanel.setMouseWheelEnabled(true);

		JMenuItem item = new JMenuItem("Refresh");
		item.addActionListener(getRefreshDelegate());
		combinedChartPanel.getPopupMenu().add(item);
		return combinedChartPanel;
	}

	protected JFreeChart createJFreeChartAndPlots(final OHLCDataset dataset, final TimeSeriesCollection volume) {
		pricePlot = createPricePlot(dataset);
		XYPlot lowerPlot = createVolumePlot(volume);

		CombinedDomainXYPlot cplot = combinePriceAndVolumePlotWithDateAxis(lowerPlot, pricePlot);

		// Create the new chart

		JFreeChart jchart = new JFreeChart("", cplot);
		ChartUtilities.applyCurrentTheme(jchart);
		jchart.removeLegend();
		jchart.removeLegend();

		return jchart;
	}

	/**
	 * @param lowerPlot
	 * @return
	 */
	protected CombinedDomainXYPlot combinePriceAndVolumePlotWithDateAxis(XYPlot lowerPlot, XYPlot upperPlot) {
		// Create the domain axis that will be used by both plots.

		DateAxis domainAxis = createDateAxis();
		// domainAxis.setDateFormatOverride( this.dateFormat );

		// Create the combined domain plot with the common domain axis, the
		// upper plot
		// and the lower plot.

		CombinedDomainXYPlot cplot = new CombinedDomainXYPlot(domainAxis);
		cplot.add(upperPlot, 3);
		cplot.add(lowerPlot, 1);
		cplot.setGap(8.0);
		cplot.setDomainGridlinePaint(Color.white);
		cplot.setDomainGridlinesVisible(true);
		cplot.setDomainPannable(true);
		cplot.setRangePannable(true);
		return cplot;
	}

	/**
	 * @return
	 */
	protected DateAxis createDateAxis() {
		DateAxis domainAxis = new DateAxis("Date Time");
		domainAxis.setLowerMargin(0.0);
		domainAxis.setUpperMargin(0.02);
		//5 second intervals 8:30 - 4:00:05 (leave a space between days) 
		// (moved to end at 4 due to bug with DST)
		SegmentedTimeline timeline = new SegmentedTimeline(5000, 
				12 * 60 * 7 + 12 * 30 + 1, //6.5 hours (dst bug means 7.5)
				12 * 60 * 8 + 12 * 60 * 8 + 12 * 30 - 1); //9 + 8 + .5 = 17.5 hours (16.5)
		timeline.setStartTime((long) (SegmentedTimeline.firstMondayAfter1900() + 12 * 60 * 7.5 * 5000));
		// Limit data to Monday through Friday
		timeline.setBaseTimeline(SegmentedTimeline.newMondayThroughFridayTimeline());
		domainAxis.setTimeline(timeline);
		return domainAxis;
	}

	/**
	 * @param dataset
	 * @return
	 */
	protected XYPlot createPricePlot(OHLCDataset dataset) {
		HighLowRenderer upperRenderer = new OpenCloseRenderer();
		upperRenderer.setBaseToolTipGenerator(new HighLowItemLabelGenerator());
		upperRenderer.setSeriesShape(0, new Rectangle2D.Double(-1.0, -1.0, barWidth, 2.0));
		upperRenderer.setSeriesPaint(0, Color.blue);

		// Create the upper plot

		NumberAxis upperRangeAxis = new NumberAxis("Price");
		upperRangeAxis.setAutoRange(true);
		upperRangeAxis.setAutoRangeIncludesZero(false);
		final XYPlot upperPlot = new XYPlot(dataset, null, upperRangeAxis, upperRenderer);
		upperPlot.setBackgroundPaint(Color.lightGray);
		upperPlot.setDomainGridlinePaint(Color.white);
		upperPlot.setRangeGridlinePaint(Color.white);
		return upperPlot;
	}

	/**
	 * 
	 * @param volume
	 * @return
	 */
	protected XYPlot createVolumePlot(final TimeSeriesCollection volume) {
		// Create the renderer for the lower plot.

		XYBarRenderer lowerRenderer = new XYBarRenderer() {
			@Override
			public Paint getItemPaint(int series, int item) {
				return Color.black;
			}
		};

		lowerRenderer.setSeriesPaint(0, Color.black);
		lowerRenderer.setDrawBarOutline(false);
		lowerRenderer.setBaseToolTipGenerator(StandardXYToolTipGenerator.getTimeSeriesInstance());

		// Create the lower plot

		NumberAxis lowerRangeAxis = new NumberAxis();
		lowerRangeAxis.setAutoRange(true);
		lowerRangeAxis.setLabel("Volume");
		XYBarDataset barPlots = new XYBarDataset(volume, barWidth);
		XYPlot lowerPlot = new XYPlot(barPlots, null, lowerRangeAxis, lowerRenderer);
		lowerPlot.setBackgroundPaint(Color.lightGray);
		lowerPlot.setDomainGridlinePaint(Color.white);
		lowerPlot.setRangeGridlinePaint(Color.white);

		lowerRenderer.setBarPainter(new StandardXYBarPainter());
		lowerRenderer.setShadowVisible(false);
		return lowerPlot;
	}

	/**
	 * Initialize the structures to hold the stock data
	 */
	private void createDataset() {
		priceData = new OHLCSeriesCollection();
		Second sec = new Second(new Date());
		OHLCItem ohlc = new OHLCItem(sec, 0, 0, 0, 0);
		candlestickSeries = new OHLCSeries(ohlc);
		priceData.addSeries(candlestickSeries);

		volumeData = new TimeSeriesCollection();
		TimeSeriesDataItem di = new TimeSeriesDataItem(sec, 0);
		volumeSeries = new TimeSeries(di);
		volumeData.addSeries(volumeSeries);
	}

	/**
	 * Starting point for the demonstration application.
	 * 
	 * @param args
	 *            ignored.
	 */
	public static void main(String[] args) {

		HistoricalDataChart demo = new HistoricalDataChart("Time Series Chart Demo 1");
		demo.pack();
		RefineryUtilities.centerFrameOnScreen(demo);
		demo.setVisible(true);

	}
}
