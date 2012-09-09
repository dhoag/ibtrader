package com.davehoag.ib.chart;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SegmentedTimeline;
import org.jfree.chart.demo.TimeSeriesChartDemo1;
import org.jfree.chart.labels.HighLowItemLabelGenerator;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.HighLowRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.ohlc.*;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.DateChooserPanel;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.RefineryUtilities;

import com.davehoag.ib.CassandraDao;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.BarIterator;
import com.davehoag.ib.util.HistoricalDateManipulation;
public class HistoricalDataChart extends ApplicationFrame {
    private static final long serialVersionUID = 1L;
    OHLCSeriesCollection data;
    static OHLCSeries series;
    static ChartPanel panel;
    static JFreeChart chart;
    JTextField start;
    JTextField end;

    {
        // set a theme using the new shadow generator feature available in
        // 1.0.14 - for backwards compatibility it is not enabled by default
        ChartFactory.setChartTheme(new StandardChartTheme("JFree/Shadow",
                true));
    }
    /**
     * A demonstration application showing how to create a simple time series
     * chart.  This example uses monthly data.
     *
     * @param title  the frame title.
     */
    public HistoricalDataChart(String title) {
        super(title);
        ChartPanel chartPanel = (ChartPanel) createPriceVolumePanels();
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
        setContentPane(chartPanel);
        JMenuItem item = new JMenuItem("Refresh");
        item.addActionListener(getRefreshDelegate());
        chartPanel.getPopupMenu().add(item);
        addInputFields();
        chart.setTitle("I");
    }
    
    protected void addInputFields(){
        super.add(new JLabel("Start & End Dates:"));
        start = new JTextField();
        DateFormat df = new SimpleDateFormat( "yyyyMMdd");
        String dateText = df.format(new Date(System.currentTimeMillis()));
        start.setText(dateText);
        end = new JTextField();
        end.setText(dateText);
        super.add(start); super.add(end);
        JButton refresh = new JButton("Get");
        refresh.addActionListener(getRefreshDelegate());
        super.add(refresh);
    }
    /**
     * Create action list to get the historical data
     * @return
     */
    protected ActionListener getRefreshDelegate(){
    	return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refresh();
			} };
    }
    /**
     * Test live updates?
     */
    public void refresh(){
    	try { 
	    	Thread t = new Thread( paintHistoricalData() );
	    	t.start();
    	}
    	catch(ParseException pe){
    		start.setBackground(Color.RED);
    		end.setBackground(Color.RED);
    	}
    }

	/**
	 * @return
	 */
	protected Runnable paintHistoricalData() throws ParseException {
		String startStr = start.getText();
		final long startTime = HistoricalDateManipulation.getTime(startStr+ " 08:30:00");
		String endStr = end.getText();
		final long endTime = HistoricalDateManipulation.getTime(endStr + " 15:00:00");
		final String aSymbol = "QQQ";
		data.removeAllSeries();
		
		return new Runnable() { public void run() {
			System.out.println("Getting " + aSymbol + " " + HistoricalDateManipulation.getDateAsStr(startTime) + " - " + HistoricalDateManipulation.getDateAsStr(endTime));
			BarIterator bars = CassandraDao.getInstance().getData(aSymbol, startTime, endTime, "bar5sec");
			Bar first = null; Bar last = null;
			double highestHigh = 0; double lowestLow = 999999;
			for(Bar aBar: bars){
				last = aBar;
				final Second sec = new Second(aBar.getTime());

		    	final double open = aBar.open; final double high = aBar.high; 
		    	final double low = aBar.low; final double close = aBar.close;
				if(first == null) {
					first = aBar;
			    	final OHLCItem ohlc = new OHLCItem(sec, open, high, low, close);
			    	
			    	series = new OHLCSeries(ohlc);
				}
				System.out.println("Adding " + aBar);
		    	series.add(sec, open , high, low, close);
		    	lowestLow = lowestLow > low ? low : lowestLow;
		    	highestHigh = highestHigh < high ? high : highestHigh;
			}
	    	series.setDescription(aSymbol);

	    	data.addSeries(series);
	    	chart.getXYPlot().getDomainAxis().setLowerBound(first.originalTime*1000);
	    	chart.getXYPlot().getDomainAxis().setUpperBound(last.originalTime*1000);
	    	chart.getXYPlot().getRangeAxis().setLowerBound(lowestLow);
	    	chart.getXYPlot().getRangeAxis().setUpperBound(highestHigh);
	    	
	    	HistoricalDataChart.this.repaint();
    	}; };
	}
	public JPanel createPriceVolumePanels(){
		OHLCDataset dataset = createDataset();
		DefaultXYDataset volume = new DefaultXYDataset();
        // Put the chart in a new ChartPanel, set the panel attributes and return it.
        chart = createCustomChart(dataset, volume) ;
        ChartPanel combinedChartPanel = new ChartPanel( chart );
        combinedChartPanel.setBorder( new EtchedBorder( EtchedBorder.RAISED ) );
        Dimension combinedChartPanelDim = new Dimension(500, 270);
		combinedChartPanel.setPreferredSize( combinedChartPanelDim  );
		combinedChartPanel.setMouseWheelEnabled(true);
		panel = combinedChartPanel;
        return combinedChartPanel;
	}
    /**
     * Creates a panel for the demo (used by SuperDemo.java).
     *
     * @return A panel.
     */
    public JPanel createDemoPanel() {
    	
        chart = createChart(createDataset());
        panel = new ChartPanel(chart);
        panel.setFillZoomRectangle(true);
        panel.setMouseWheelEnabled(true);
        return panel;
    }
    /**
     * Creates a chart.
     *
     * @param dataset  a dataset.
     *
     * @return A chart.
     */
    private static JFreeChart createChart(OHLCDataset dataset) {

        JFreeChart chart = ChartFactory.createHighLowChart(
            "",  // title
            "Sec",             // x-axis label
            "Price",   // y-axis label
            dataset,            // data
            false               // create legend?
        );

        chart.setBackgroundPaint(Color.white);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);

        XYItemRenderer r = plot.getRenderer();
        if (r instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
            renderer.setBaseShapesVisible(true);
            renderer.setBaseShapesFilled(true);
            renderer.setDrawSeriesLineAsPath(true);
        }

        plot.getRangeAxis().setAutoRange(false);
        DateAxis axis = (DateAxis) plot.getDomainAxis();
 //       axis.setDateFormatOverride(new SimpleDateFormat("MMM-yyyy"));
        axis.setAutoRange(false);

        return chart;

    }
    protected JFreeChart createCustomChart(OHLCDataset dataset, DefaultXYDataset volume ){
      // this.upperDataSet.addSeries( this.priceTimeSeries );
      //  this.lowerDataSet.addSeries( this.volumeTimeSeries );
        
        XYPlot upperPlot = createPricePanel(dataset);
        XYPlot lowerPlot = createVolumePanel(volume);

        // Create the domain axis that will be used by both plots.
        
        DateAxis domainAxis = new DateAxis( "Date Time" );
        domainAxis.setLowerMargin( 0.0 );
        domainAxis.setUpperMargin( 0.02 );
        //5 second intervals
        SegmentedTimeline timeline = new SegmentedTimeline( 5000, 6*12*60 + 6*60 , 9*12*60 + 12*60*8 + 6*60);
        timeline.setStartTime((long) (SegmentedTimeline.firstMondayAfter1900() + 12*60*7.5*5000));
        //Limit data to Monday through Friday
        timeline.setBaseTimeline(SegmentedTimeline.newMondayThroughFridayTimeline());
        domainAxis.setTimeline( timeline );
       // domainAxis.setDateFormatOverride( this.dateFormat );
        
        // Create the combined domain plot with the common domain axis, the upper plot
        // and the lower plot.
        
        CombinedDomainXYPlot cplot = new CombinedDomainXYPlot( domainAxis );
        cplot.add( upperPlot, 3 );
        cplot.add( lowerPlot, 1 );
        cplot.setGap( 8.0 );
        cplot.setDomainGridlinePaint( Color.white );
        cplot.setDomainGridlinesVisible( true );
        cplot.setDomainPannable( false );

        // Create the new chart
        
        JFreeChart jchart = new JFreeChart( "Contract Name", cplot );
        ChartUtilities.applyCurrentTheme( jchart );
        
        return jchart;
    }

	/**
	 * @param dataset
	 * @return
	 */
	protected XYPlot createPricePanel(OHLCDataset dataset) {
		HighLowRenderer upperRenderer = new HighLowRenderer();
        upperRenderer.setBaseToolTipGenerator(new HighLowItemLabelGenerator());
        upperRenderer.setSeriesShape( 0, new Rectangle2D.Double( -1.0, -1.0, 2.0, 2.0 ) );
        upperRenderer.setSeriesPaint( 0, Color.blue );

        // Create the upper plot
        
        NumberAxis upperRangeAxis = new NumberAxis( "Price" );
        upperRangeAxis.setAutoRange( true );
        upperRangeAxis.setAutoRangeIncludesZero( false );
        XYPlot upperPlot = new XYPlot( dataset, null, upperRangeAxis, 
                                       upperRenderer );
        upperPlot.setBackgroundPaint( Color.lightGray );
        upperPlot.setDomainGridlinePaint( Color.white );
        upperPlot.setRangeGridlinePaint( Color.white );
		return upperPlot;
	}
    protected XYPlot createVolumePanel(final DefaultXYDataset volume){
        // Create the renderer for the lower plot.
        
        XYBarRenderer lowerRenderer = new XYBarRenderer() {
          public Paint getItemPaint( int series, int item ) {
            return Color.black;
          }
        };
        	           
        lowerRenderer.setSeriesPaint( 0, Color.black );
        lowerRenderer.setDrawBarOutline( false );
        lowerRenderer.setBaseToolTipGenerator( StandardXYToolTipGenerator.getTimeSeriesInstance()  );

        // Create the lower plot
        
        NumberAxis lowerRangeAxis = new NumberAxis();
        lowerRangeAxis.setAutoRange( true );
        lowerRangeAxis.setLabel( "Volume" );
        XYPlot lowerPlot = new XYPlot( volume, null, lowerRangeAxis, 
                                       lowerRenderer );
        lowerPlot.setBackgroundPaint( Color.lightGray );
        lowerPlot.setDomainGridlinePaint( Color.white );
        lowerPlot.setRangeGridlinePaint( Color.white );

        lowerRenderer.setBarPainter( new StandardXYBarPainter() );
        lowerRenderer.setShadowVisible( false );
        return lowerPlot;
    }

    private OHLCDataset createDataset() {
    	data = new OHLCSeriesCollection();
    	Second sec = new Second(new Date());
    	OHLCItem ohlc = new OHLCItem(sec, 0, 0, 0, 0);
    	series = new OHLCSeries(ohlc);
    	series.setDescription("QQQ");
    	data.addSeries(series);
    	return data;
    }
    /**
     * Starting point for the demonstration application.
     *
     * @param args  ignored.
     */
    public static void main(String[] args) {

        HistoricalDataChart demo = new HistoricalDataChart(
                "Time Series Chart Demo 1");
        demo.pack();
        RefineryUtilities.centerFrameOnScreen(demo);
        demo.setVisible(true);

    }
}
