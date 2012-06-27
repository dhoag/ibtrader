package com.davehoag.ib;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.util.HistoricalDateManipulation;

import me.prettyprint.cassandra.serializers.BooleanSerializer;
import me.prettyprint.cassandra.serializers.DoubleSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.beans.Rows;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import me.prettyprint.hector.api.query.SliceQuery;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
/**
 * 
 * @author dhoag
 *
 */
public class CassandraDao {
	
	private final Cluster cluster = HFactory.getOrCreateCluster("Test Cluster", "localhost:9160");
	private final StringSerializer stringSerializer = StringSerializer.get();
	private final LongSerializer longSerializer = LongSerializer.get();
	private final BooleanSerializer  booleanSerializer = BooleanSerializer.get();
	private final DoubleSerializer doubleSerializer = DoubleSerializer.get();
	private final Keyspace keyspace = HFactory.createKeyspace("tradedata", cluster);
	
	private final String [] priceKeys = { ":open", ":close", ":high", ":low", ":wap" };
	private final String [] longKeys = { ":vol", ":tradeCount" };
	final static CassandraDao dao = new CassandraDao();
	public static CassandraDao getInstance(){
		return dao;
	}
	public static void main(String [] args){
		try{
			System.out.println(Calendar.getInstance().getTimeInMillis() / 1000);
			Iterator<Bar> bars = dao.getData("SPY", 5, 0, "bar5sec");
			int count = 0;
			while(bars.hasNext()){
				Bar aBar =bars.next();
				System.out.println(aBar);
				if(count++ > 10) break;
			}
			System.out.println( "OPEN " + dao.getOpen("SPY", 5));
					
		} catch(Throwable t){
			t.printStackTrace();
		}
	}
	/**
	 * Store the market data in Cassandra
	 * @param barSize - Must be predefined column family 
	 * @param dateSecondsStr - Seconds since some date in the 70s - 
	 * @param hasGap
	 */
	protected void insertHistoricalData(final String barSize, final String symbol, final String dateSecondsStr,
			final double open, final double high, final double low,
			final double close, final int volume, final int count,
			final double WAP, final boolean hasGap)
	{
		
		final DecimalFormat df = new DecimalFormat("#.##");
		Long dateSeconds = Long.valueOf(dateSecondsStr);
		
	    final Mutator<String> m = HFactory.createMutator(keyspace, stringSerializer);

	    String key = symbol + ":open";
		final HColumn<Long, Double> openCol = HFactory.createColumn(dateSeconds, open, longSerializer, doubleSerializer);
	    m.addInsertion( key, barSize, openCol );
	    key = symbol + ":close";
		final HColumn<Long, Double> closeCol = HFactory.createColumn(dateSeconds, close, longSerializer, doubleSerializer);
	    m.addInsertion( key, barSize, closeCol );
	    key = symbol + ":high";
		final HColumn<Long, Double> highCol = HFactory.createColumn(dateSeconds, high, longSerializer, doubleSerializer);
	    m.addInsertion( key, barSize, highCol );
	    key = symbol + ":low";
		final HColumn<Long, Double> lowCol = HFactory.createColumn(dateSeconds, low, longSerializer, doubleSerializer);
	    m.addInsertion( key, barSize, lowCol );
	    key = symbol + ":wap";
		final HColumn<Long, Double> wapCol = HFactory.createColumn(dateSeconds, WAP, longSerializer, doubleSerializer);
	    m.addInsertion( key, barSize, wapCol );
	    key = symbol + ":vol";
		final HColumn<Long, Long> volumeCol = HFactory.createColumn(dateSeconds, Long.valueOf(volume), longSerializer, longSerializer);
	    m.addInsertion( key, barSize, volumeCol );
	    key = symbol + ":tradeCount";
		final HColumn<Long, Long> tradeCountCol = HFactory.createColumn(dateSeconds, Long.valueOf(count), longSerializer, longSerializer);
	    m.addInsertion( key, barSize, tradeCountCol);
	    key = symbol + ":hasGap";
		final HColumn<Long, Boolean> hasGapCol = HFactory.createColumn(dateSeconds, Boolean.valueOf(hasGap), longSerializer, booleanSerializer);
	    m.addInsertion( key, barSize, hasGapCol);
	    m.execute();
	}
    
    < V> void printFirstRow(Rows<String, Long, V> rows ){
    	
        for (Row<String, Long, V> row2 : rows) {
    		ColumnSlice<Long, V> slice = row2.getColumnSlice();
    		for (HColumn<Long, V> column : slice.getColumns()) {
    			Long secs = column.getName();
    			Date d = new Date(secs.longValue()*1000);
    		    System.out.println(d + " " + column.getValue());
    		}            
        }    	
    }
    public Bar getOpen(final String symbol, long today){
    	//first assume today could be the number of days to go back from today
    	
    	long actualToday = today;
    	if(today < 1000){
    		actualToday =  getToday(today, 0) - today*24*60*60;
    	}
    	Logger.getLogger("MarketData").log(Level.INFO, "Actual today " + actualToday + " " + new Date(actualToday *1000));
    	long openTime = HistoricalDateManipulation.getOpen(actualToday);
    	Iterator<Bar> bars = getData( symbol, openTime, openTime, "bar5sec");
    	if(bars.hasNext()) return bars.next();
    	return null;
    }
    /**
     * 
     * @param symbol
     * @param start < 1000 and finish is zero, go back that many days
     * 		
     * @return
     * @throws ParseException 
     */
    public Iterator<Bar> getData(final String symbol, long start, final long finish, final String cf) {
    	
    	final long actualFinish =  getToday(start, finish);
    	final long actualStart = start < 1000 ? actualFinish - 24*60*60*start : start;
    	Logger.getLogger("MarketData").log(Level.INFO, "Getting "  + symbol +  " data between " + new Date(actualStart*1000) + " and " + new Date(actualFinish*1000));
    	return getDataIterator(symbol, actualFinish, actualStart, cf);
    }
	/**
	 * @param symbol
	 * @param actualFinish
	 * @param actualStart
	 * @return
	 */
	protected Iterator<Bar> getDataIterator(final String symbol, final long actualFinish, final long actualStart, final String cf) {
		return new Iterator<Bar>(){
    		final HashMap<String, List<HColumn<Long, Double>>> priceData = getPriceHistoricalData(symbol, actualStart, actualFinish, cf);
    		final HashMap<String, List<HColumn<Long, Long>>> volData = getHistoricalData(symbol, actualStart,actualFinish, cf);
    		int count = 0;
			@Override
			public boolean hasNext() {
				return count < volData.get(symbol + ":vol" ).size();
			}

			@Override
			public Bar next() {
				final Bar bar = new Bar();
				bar.symbol = symbol;
				bar.close = priceData.get(symbol + ":close").get(count).getValue().doubleValue();
				bar.open = priceData.get(symbol + ":open").get(count).getValue().doubleValue();
				bar.high = priceData.get(symbol + ":high").get(count).getValue().doubleValue();
				bar.low  = priceData.get(symbol + ":low").get(count).getValue().doubleValue();
				bar.wap = priceData.get(symbol + ":wap").get(count).getValue().doubleValue();
				bar.tradeCount = volData.get(symbol + ":tradeCount").get(count).getValue().intValue();
				bar.volume = volData.get(symbol + ":vol").get(count).getValue().longValue();
				bar.originalTime = volData.get(symbol + ":vol").get(count).getName().longValue();
				count++;
				return bar;
			}

			@Override
			public void remove() {
				// TODO Auto-generated method stub
			}
    	};
	}
	/**
	 * If finish < 1000 figure to use today as the date. 
	 * If start is also < 1000, then we are counting back N # of days ,so if it's monday or sunday
	 * move the date back to Saturday as the starting point from which to count back
	 * otherwise just leave start as it is
	 * @param start
	 * @param finish
	 * @return
	 */
	protected long getToday(final long start, final long finish) {
		long todayInSeconds = finish;
    	if(finish < 1000) {
        	final Calendar today = Calendar.getInstance();
        	if( start  < 1000 ){
        		if( today.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY ) today.add(Calendar.HOUR, -2*24);
        		else 
        		if(today.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY ) today.add(Calendar.HOUR, -24);
    		} 
    		//Actual start date, so just put today in there
    		todayInSeconds = today.getTimeInMillis() / 1000;
    	}
		return todayInSeconds;
	}
    /**
     * 
     * @param symbol
     * @param seconds
     * @return 
     */
    public Bar getYesterday(final String symbol, final long seconds){
    	Iterator<Bar> bars = getData(symbol, 1, seconds, "bar1day");
    	if(bars.hasNext()) return bars.next();
    	Logger.getLogger("MarketData").log(Level.WARNING, "No prior data for " + symbol + " " + HistoricalDateManipulation.getDateAsStr(seconds));
    	return null;
    }
    /**
     * Get the price data 
     * @param symbol
     * @return
     * @throws ParseException
     */
    protected HashMap<String, List<HColumn<Long, Double>>> getPriceHistoricalData(final String symbol, final long start, final long finish, final String cf) {
    	final HashMap<String, List<HColumn<Long, Double>>> result = new HashMap<String, List<HColumn<Long, Double>>>();
        RangeSlicesQuery<String, Long, Double> priceQuery = HFactory.createRangeSlicesQuery(keyspace, stringSerializer, longSerializer, doubleSerializer);
        priceQuery.setColumnFamily(cf);
        priceQuery.setRange(start, finish, false, 12*60*8);
        
        for(String key: priceKeys){
        	String rowKey = symbol + key;
            priceQuery.setKeys(rowKey, rowKey);
            List<HColumn<Long, Double>> column = priceQuery.execute().get().getByKey(rowKey).getColumnSlice().getColumns();
            result.put(rowKey, column);
        }
    	return result;
    }
    /**
     * Get the volume & tradeCount data
     * @param symbol
     * @return
     * @throws ParseException
     */
    protected HashMap<String, List<HColumn<Long, Long>>> getHistoricalData(final String symbol, final long start, final long finish, final String cf) {
    	final HashMap<String, List<HColumn<Long, Long>>> result = new HashMap<String, List<HColumn<Long, Long>>>();
        SliceQuery<String, Long, Long> priceQuery = HFactory.createSliceQuery(keyspace, stringSerializer, longSerializer, longSerializer);
        priceQuery.setColumnFamily(cf);
        priceQuery.setRange(start, finish, false, 12*60*8);
        
        for(String key: longKeys ){
        	String rowKey = symbol + key;
            priceQuery.setKey(rowKey);
            List<HColumn<Long, Long>> column = priceQuery.execute().get().getColumns();
    System.out.println(rowKey + " " + cf + " " + start + " " + finish + " " + column.size());
            result.put(rowKey, column);
        }
    	return result;
    }
    /**
	 * @param sym
	 * @param row2
	 */
	protected <V> void printColumns( Row<String, Long, V> closeRow, Row<String, Long, V> wapRow ) {
		if(closeRow == null || wapRow == null) return;
		System.out.println("Columns for :" + closeRow.getKey() + " " + wapRow.getKey());
		
		ColumnSlice<Long, V> closeSlice = closeRow.getColumnSlice();
		ColumnSlice<Long, V> wapSlice = wapRow.getColumnSlice();
		List<HColumn<Long, V>> closeVals = closeSlice.getColumns();
		List<HColumn<Long, V>> wapVals = wapSlice.getColumns();
		int size = closeVals.size();
		for(int i = 0; i < size; i++){
			System.out.println("Close: " + closeVals.get(i) + " Wap: " + wapVals.get(i)); 
		}
	}
   
}
