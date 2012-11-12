package com.davehoag.ib;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import me.prettyprint.cassandra.serializers.BooleanSerializer;
import me.prettyprint.cassandra.serializers.DoubleSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.MutationResult;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import me.prettyprint.hector.api.query.SliceQuery;

import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.BarIterator;
import com.davehoag.ib.dataTypes.PagingBarIterator;
import com.davehoag.ib.util.HistoricalDateManipulation;

/**
 * 
 * @author David Hoag
 * 
 */
public class CassandraDao {

	private final Cluster cluster = HFactory.getOrCreateCluster("Test Cluster", "localhost:9160");
	private final StringSerializer stringSerializer = StringSerializer.get();
	private final LongSerializer longSerializer = LongSerializer.get();
	private final BooleanSerializer booleanSerializer = BooleanSerializer.get();
	private final DoubleSerializer doubleSerializer = DoubleSerializer.get();
	private final Keyspace keyspace = HFactory.createKeyspace("tradedata", cluster);

	private final String[] priceKeys = { ":open", ":close", ":high", ":low", ":wap" };
	private final String[] longKeys = { ":vol", ":tradeCount" };
	// assume 5 second bars of which 60/5 appear per minute
	private final int maxRecordsReturned = (60 / 5) * 60 * 8 * 1;
	//private final int maxRecordsReturned = 10;
	final static CassandraDao dao = new CassandraDao();

	public static CassandraDao getInstance() {
		return dao;
	}

	public static void main(String[] args) {
		//displayRecordsForMostRecentDate(args);
		//insertSampleData();
		//deleteSampleData();
		/*
		cleanUpDayData("SPY");
		cleanUpDayData("AGG");
		cleanUpDayData("QQQ");
		*/
		long day = dao.findMostRecentDate("QQQ", "bar1day");
		System.out.println(HistoricalDateManipulation.getDateAsStr(day));
		day = dao.findMostRecentDate("QQQ", "bar5sec");
		System.out.println(HistoricalDateManipulation.getDateAsStr(day));
		Bar aBar = dao.getOpen("QQQ", day);
		BarIterator bars = dao.getNext("QQQ", aBar, day -24*60*60*3,  3, true);
		for(Bar b: bars){
			System.out.println(b);
		}
		bars = dao.getNext("QQQ", aBar, day +24*60*60*3,  3, false);
		for(Bar b: bars){
			System.out.println(b);
		}

		bars = dao.getData("QQQ", 8, 0, "bar5sec");
		for(Bar b: bars){
			System.out.println(b);
		}
	}
	public static void deleteSampleData(){
		dao.delete("bar5min", 120123122, "IBM");
		dao.delete("bar5min", 120156567, "IBM");
	}
	public static void insertSampleData(){
		dao.insertHistoricalData("bar5min", "IBM", "120123122", 10.2, 12.45, 9.2, 10.5, 100032, 20, 10.43, false);
		dao.insertHistoricalData("bar5min", "IBM", "120156567", 10.2, 12.45, 9.2, 10.5, 100032, 20, 10.43, false);
	}
	/**
	 * @param args
	 */
	protected static void displayRecordsForMostRecentDate(String[] args) {
		String symbol = "";
		final String barSize = args[0];
		
		for (int i = 1; i < args.length; i++) {
			symbol = args[i];
			long dateInSecs = dao.findMostRecentDate(symbol, barSize);
			System.out.print("Most recent date " + symbol + " " + barSize + " "
					+ new Date(dateInSecs * 1000));
			System.out.println(" records " + dao.getRecordsOnDate(symbol, barSize, dateInSecs));
		}
	}
	/**
	 * Modify a bunch of records due to a change in how I store daily bars.
	 * 
	 * @param aSymbol
	 */
	protected static void cleanUpDayData(final String aSymbol) {
		int count1 =0; int count2 = 0;
		try {
			final BarIterator bars = dao.getData(aSymbol, "20110101 08:00:00", "20130101 08:00:00", "bar1day");
			for(Bar aBar: bars){
				final int hour = HistoricalDateManipulation.getHour(aBar.originalTime);
				if(hour != 8 ){
					count1++;
					final long openTime = HistoricalDateManipulation.getOpenTime(aBar.originalTime);
					String newTime = String.valueOf(openTime);
				//	System.out.println("new " + newTime);
					dao.insertHistoricalData("bar1day", aSymbol, newTime, aBar.open, aBar.high, aBar.low, aBar.close, aBar.volume, aBar.tradeCount, aBar.wap, aBar.hasGaps);
					dao.delete("bar1day", aBar.originalTime, aSymbol);
					//System.out.println(aBar);
				}
				else{
					count2++;
				}
			}
		} catch (final ParseException e) {
			e.printStackTrace();
		}
		System.out.println("8am " + count2 + " 4pm" + count1);
	}
	/**
	 * 
	 * @param symbol
	 * @param barSize
	 * @param dateInSecs
	 * @return the number of records for the given parameters
	 */
	public int getRecordsOnDate(final String symbol, final String barSize, final long dateInSecs ){
		try{
			String upperSymbol = StringUtils.upperCase(symbol);
			int result = countRecordsForCurrentDay(upperSymbol, barSize, dateInSecs);
			return result;
		} catch (Throwable t) {
			System.err.println("Error with " + symbol);
			t.printStackTrace();
		}
		return 0;
	}
	/**
	 * Count the records in the dao for the given bar size & symbol
	 * 
	 * @param symbol
	 * @param barSize
	 * @param day
	 * @return
	 */
	public int countRecordsForCurrentDay(String symbol, String barSize, long day) {
		long startOfDay = HistoricalDateManipulation.getOpen(day);
		int count = 0;
		Iterator<Bar> bars = dao.getData(symbol, startOfDay, startOfDay + 6 * 60 * 60 + 30 * 60, barSize);
		while (bars.hasNext()) {
			count++;
			bars.next();
		}
		return count;
	}
	/**
	 * A utility method to what data I have in the system for the given bar size
	 * 
	 * @param symbol
	 * @param barSize
	 * @return
	 */
	public long findMostRecentDate(final String symbol, final String barSize) {
		long timeInSecs = System.currentTimeMillis() / 1000;
		long openTime = HistoricalDateManipulation.getOpenTime(timeInSecs);
		final HashMap<String, List<HColumn<Long, Double>>> priceData = getPriceHistoricalData(symbol,
				openTime - 24*60*60*100,openTime, barSize ,true, 1);
		final List<HColumn<Long, Double>> entry = priceData.get(symbol + ":open");
		if( entry != null){
			final int recordCount = entry.size();
			if (recordCount > 0)	return entry.get(0).getName();
		}
		LoggerFactory.getLogger("HistoricalData").error(
				"Checked the past 100 days and there is no data for " + symbol + " in bar " + barSize);
		return 0;
	}
	/**
	 * Find the bar for the specific window and time period
	 * @param symbol
	 * @param seconds
	 * @param barSize
	 * @return
	 */
	public Bar getBar(final String symbol, final long seconds, final String barSize){
		BarIterator iterator = getData(symbol, seconds, seconds, barSize);
		if(iterator.hasNext()) {
			Bar b = iterator.next();
			if(b.originalTime == seconds){ return b; }
			Logger.getLogger("HistoricalData").warning("Bar found with wrong time: looking for " + seconds + " found: " + b.originalTime);
		}
		
		return null;
	}
	/**
	 * 
	 * @param symbol
	 * @param aBar
	 * @param end A number either larger or smaller than the time found in the bar depending upon direction
	 * @param count
	 * @param reverse
	 * @return
	 */
	public BarIterator getNext(final String symbol, final Bar aBar, final long end, final int count, final boolean reverse){
		return getNext(symbol, aBar.originalTime, aBar.barSize, end, count, reverse);
	}
	/**
	 * 
	 * @param symbol
	 * @param aBar
	 * @param end A number either larger or smaller than the time found in the bar depending upon direction
	 * @param count
	 * @param reverse
	 * @return
	 */
	public BarIterator getNext(final String symbol, final long originalTime, final String barSize, final long end, final int count, final boolean reverse){
		final HashMap<String, List<HColumn<Long, Double>>> priceData;
		final HashMap<String, List<HColumn<Long, Long>>> volData;
		if( reverse ){ 
			long start = originalTime - 1;
			priceData = getPriceHistoricalData(symbol,
					end, start, barSize ,reverse, count);
		
			volData = getHistoricalData(symbol, end, start, barSize, reverse, count);
		}
		else {
			long start = originalTime + 1;
			priceData = getPriceHistoricalData(symbol,
					start, end, barSize ,reverse, count);
		
			volData = getHistoricalData(symbol, start, end, barSize, reverse, count);
		}
		try {
			return new BarIterator(symbol, priceData,volData, barSize);
		} catch (Exception ex) {
			LoggerFactory.getLogger("DAO").warn("Exception fetching data in DAO for " + symbol + ". " + ex);
			ex.printStackTrace();
			return new BarIterator(symbol);
		}
	}

	/**
	 * Store the market data in Cassandra
	 * 
	 * @param barSize
	 *            - Must be predefined column family
	 * @param dateSecondsStr
	 *            - Seconds since some date in the 70s -
	 * @param hasGap
	 */
	protected void insertHistoricalData(final String barSize, final String aSymbol,
			final String dateSecondsStr, final double open, final double high, final double low,
			final double close, final long volume, final long count, final double WAP, final boolean hasGap) {
		final String symbol = StringUtils.upperCase(aSymbol);

		final DecimalFormat df = new DecimalFormat("#.##");
		Long dateSeconds = Long.valueOf(dateSecondsStr);

		final Mutator<String> m = HFactory.createMutator(keyspace, stringSerializer);

		String key = symbol + ":open";
		final HColumn<Long, Double> openCol = HFactory.createColumn(dateSeconds, open, longSerializer,
				doubleSerializer);
		m.addInsertion(key, barSize, openCol);
		key = symbol + ":close";
		final HColumn<Long, Double> closeCol = HFactory.createColumn(dateSeconds, close, longSerializer,
				doubleSerializer);
		m.addInsertion(key, barSize, closeCol);
		key = symbol + ":high";
		final HColumn<Long, Double> highCol = HFactory.createColumn(dateSeconds, high, longSerializer,
				doubleSerializer);
		m.addInsertion(key, barSize, highCol);
		key = symbol + ":low";
		final HColumn<Long, Double> lowCol = HFactory.createColumn(dateSeconds, low, longSerializer,
				doubleSerializer);
		m.addInsertion(key, barSize, lowCol);
		key = symbol + ":wap";
		final HColumn<Long, Double> wapCol = HFactory.createColumn(dateSeconds, WAP, longSerializer,
				doubleSerializer);
		m.addInsertion(key, barSize, wapCol);
		key = symbol + ":vol";
		final HColumn<Long, Long> volumeCol = HFactory.createColumn(dateSeconds, Long.valueOf(volume),
				longSerializer, longSerializer);
		m.addInsertion(key, barSize, volumeCol);
		key = symbol + ":tradeCount";
		final HColumn<Long, Long> tradeCountCol = HFactory.createColumn(dateSeconds, Long.valueOf(count),
				longSerializer, longSerializer);
		m.addInsertion(key, barSize, tradeCountCol);
		key = symbol + ":hasGap";
		final HColumn<Long, Boolean> hasGapCol = HFactory.createColumn(dateSeconds, Boolean.valueOf(hasGap),
				longSerializer, booleanSerializer);
		m.addInsertion(key, barSize, hasGapCol);
		m.execute();
	}

	/**
	 * Get the open (8:30 bar) for the specified date of today. If "today" is <
	 * 1000 assume its an offset of how many days to go back
	 * 
	 * @param symbol
	 * @param today
	 * @return
	 */
	public Bar getOpen(final String symbol, final long todayInSec) {
		final long openTime = HistoricalDateManipulation.getOpenTime(todayInSec);

		LoggerFactory.getLogger("HistoricalData").debug(
				"Get " + symbol + " open of day: " + new Date(openTime * 1000));
		return getBar(symbol, openTime, "bar5sec");
	}

	public BarIterator getData(final String aSymbol, String start, final String finish, final String barSize) throws ParseException {
		return getData(aSymbol, HistoricalDateManipulation.getTime(start), HistoricalDateManipulation.getTime(finish), barSize);
	}

	/**
	 * 
	 * @param symbol
	 * @param start
	 *            < 1000 and finish is zero, go back that many days
	 * @param finish
	 * @return
	 * @throws ParseException
	 */
	public BarIterator getData(final String aSymbol, long start, final long finish, final String barSize) {
		final String symbol = StringUtils.upperCase(aSymbol);

		final long actualFinish = determineEndDate(start < 1000, finish);
		final long actualStart = start < 1000 ? HistoricalDateManipulation.getOpen(actualFinish - 24 * 60
				* 60 * start) : start;
		LoggerFactory.getLogger("HistoricalData").info(
				"Getting " + barSize + " " + symbol + " data between " + new Date(actualStart * 1000)
						+ " and " + new Date(actualFinish * 1000));
		return getDataIterator(symbol, actualFinish, actualStart, barSize);
	}
	/**
	 * @param symbol
	 * @param actualFinish
	 * @param actualStart
	 * @return
	 */
	protected BarIterator getDataIterator(final String symbol, final long actualFinish,
			final long actualStart, final String barSize) {
		try {
			final HashMap<String, List<HColumn<Long, Double>>> priceData;
			final HashMap<String, List<HColumn<Long, Long>>> volData;
			priceData = getPriceHistoricalData(symbol, actualStart, actualFinish, barSize);
			volData = getHistoricalData(symbol, actualStart, actualFinish, barSize);
			final PagingBarIterator result = new PagingBarIterator(symbol, priceData,volData, barSize, actualFinish);
			result.setPagingSize(maxRecordsReturned);
			return result;
		} catch (Exception ex) {
			LoggerFactory.getLogger("DAO").warn("Exception fetching data in DAO for " + symbol + ". " + ex);
			ex.printStackTrace();
			return new BarIterator(symbol);
		}
	}

	/**
	 * If finish < 1000 figure to use today as the date. If start is also <
	 * 1000, then we are counting back N # of days ,so if it's monday or sunday
	 * move the date back to Saturday as the starting point from which to count
	 * back otherwise just leave start as it is
	 * 
	 * @param start
	 * @param finish
	 * @return
	 */
	protected long determineEndDate(boolean offsetWeekend, final long finish) {
		long todayInSeconds = finish;
		if (finish < 1000) {
			final Calendar today = Calendar.getInstance();

			if (offsetWeekend) {
				if (today.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY)
					today.add(Calendar.HOUR, -2 * 24);
				else if (today.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
					today.add(Calendar.HOUR, -24);
			}
			// Actual start date, so just put today in there
			todayInSeconds = today.getTimeInMillis() / 1000;
		}
		return todayInSeconds;
	}

	/**
	 * 
	 * @param symbol
	 * @param seconds
	 *            A timestamp from today. If today is the weekend back it up to
	 *            Friday.
	 * @return
	 */
	public Bar getYesterday(final String symbol, final long seconds) {
		Calendar today = Calendar.getInstance();
		today.setTimeInMillis(seconds * 1000);
		long yesterday = seconds - 24 * 60 * 60;
		if (today.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
				|| today.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
			yesterday -= 2 * 24 * 60 * 60;
		else if (today.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY)
			yesterday -= 1 * 24 * 60 * 60;
		final long openTime = HistoricalDateManipulation.getOpen(yesterday);
		final Iterator<Bar> bars = getData(symbol, openTime, openTime, "bar1day");
		if (bars.hasNext())
			return bars.next();
		LoggerFactory.getLogger("HistoricalData").warn(
				"No prior data for " + symbol + " " + HistoricalDateManipulation.getDateAsStr(seconds));
		return null;
	}

	/**
	 * No limit on data sent and in chronological order
	 * @param symbol
	 * @param start
	 * @param finish
	 * @param cf
	 * @return
	 */
	public HashMap<String, List<HColumn<Long, Double>>> getPriceHistoricalData(final String symbol,
			final long start, final long finish, final String cf) {
		return getPriceHistoricalData(symbol, start, finish, cf,false,  maxRecordsReturned);
	}
	/**
	 * Get the price data
	 * 
	 * @param symbol
	 * @return
	 * @throws ParseException
	 */
	public HashMap<String, List<HColumn<Long, Double>>> getPriceHistoricalData(final String symbol,
			final long start, final long finish, final String cf, final boolean reverse, final int count) {
		final HashMap<String, List<HColumn<Long, Double>>> result = new HashMap<String, List<HColumn<Long, Double>>>();
		RangeSlicesQuery<String, Long, Double> priceQuery = HFactory.createRangeSlicesQuery(keyspace,
				stringSerializer, longSerializer, doubleSerializer);
		priceQuery.setColumnFamily(cf);
		if(reverse){
			priceQuery.setRange(finish, start, reverse, count);
		}
		else {
			priceQuery.setRange(start, finish, reverse, count);
		}

		for (String key : priceKeys) {
			final String rowKey = symbol + key;
			priceQuery.setKeys(rowKey, rowKey);
			final QueryResult<OrderedRows<String, Long, Double>> queryResults = priceQuery.execute();
			final OrderedRows<String, Long, Double> rows = queryResults.get();
			final Row<String, Long, Double> row = rows.getByKey(rowKey);
			if(row != null){
				final List<HColumn<Long, Double>> column = row.getColumnSlice().getColumns();
				result.put(rowKey, column);
			}
		}
		return result;
	}

	/**
	 * Delete multiple columns from the data;
	 */
	public void delete(String barSize, long seconds, String... symbols) {
		Mutator<String> m = HFactory.createMutator(keyspace, stringSerializer);
		for (String key : symbols) {
			for(String priceKey: priceKeys){
				m.addDeletion(key + priceKey, barSize, seconds, longSerializer);
			}
			for(String longKey: longKeys){
				m.addDeletion(key + longKey, barSize, seconds, longSerializer);
			}
			m.addDeletion(key + ":hasGap", barSize, seconds, longSerializer);
		}
		MutationResult result = m.execute();
	}
	public HashMap<String, List<HColumn<Long, Long>>> getHistoricalData(final String symbol,
			final long start, final long finish, final String cf) {
		return getHistoricalData(symbol, start, finish, cf, false, maxRecordsReturned);
	}
	/**
	 * Get the volume & tradeCount data
	 * 
	 * @param symbol
	 * @return
	 * @throws ParseException
	 */
	public HashMap<String, List<HColumn<Long, Long>>> getHistoricalData(final String symbol,
			final long start, final long finish, final String cf, final boolean reverse, final int count ) {
		final HashMap<String, List<HColumn<Long, Long>>> result = new HashMap<String, List<HColumn<Long, Long>>>();
		SliceQuery<String, Long, Long> priceQuery = HFactory.createSliceQuery(keyspace, stringSerializer,
				longSerializer, longSerializer);
		priceQuery.setColumnFamily(cf);
		if(reverse){
			priceQuery.setRange(finish, start, reverse, count);
		}
		else {
			priceQuery.setRange(start, finish, reverse, count);
		}

		for (String key : longKeys) {
			String rowKey = symbol + key;
			priceQuery.setKey(rowKey);
			List<HColumn<Long, Long>> column = priceQuery.execute().get().getColumns();
			// System.out.println(rowKey + " " + cf + " " + start + " " + finish
			// + " " + column.size());
			result.put(rowKey, column);
		}
		return result;
	}

	/**
	 * @param sym
	 * @param row2
	 */
	protected <V> void printColumns(Row<String, Long, V> closeRow, Row<String, Long, V> wapRow) {
		if (closeRow == null || wapRow == null)
			return;
		System.out.println("Columns for :" + closeRow.getKey() + " " + wapRow.getKey());

		ColumnSlice<Long, V> closeSlice = closeRow.getColumnSlice();
		ColumnSlice<Long, V> wapSlice = wapRow.getColumnSlice();
		List<HColumn<Long, V>> closeVals = closeSlice.getColumns();
		List<HColumn<Long, V>> wapVals = wapSlice.getColumns();
		int size = closeVals.size();
		for (int i = 0; i < size; i++) {
			System.out.println("Close: " + closeVals.get(i) + " Wap: " + wapVals.get(i));
		}
	}

}
