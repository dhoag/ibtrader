package com.davehoag.ib;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.BarIterator;
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
	private final BooleanSerializer booleanSerializer = BooleanSerializer.get();
	private final DoubleSerializer doubleSerializer = DoubleSerializer.get();
	private final Keyspace keyspace = HFactory.createKeyspace("tradedata", cluster);

	private final String[] priceKeys = { ":open", ":close", ":high", ":low", ":wap" };
	private final String[] longKeys = { ":vol", ":tradeCount" };
	// assume 5 second bars of which 60/5 appear per minute
	private final int maxRecordsReturned = (60 / 5) * 60 * 8 * 90;
	final static CassandraDao dao = new CassandraDao();

	public static CassandraDao getInstance() {
		return dao;
	}

	public static void main(String[] args) {
		String symbol = "";
		try {
			final String barSize = args[0];
			for (int i = 1; i < args.length; i++) {
				symbol = StringUtils.upperCase(args[i]);
				long dateInSecs = dao.findMostRecentDate(symbol, barSize);
				System.out.print("Most recent date " + symbol + " " + barSize + " "
						+ new Date(dateInSecs * 1000));
				System.out.println(" records " + dao.countRecordsForCurrentDay(symbol, barSize, dateInSecs));
			}
		} catch (Throwable t) {
			System.err.println("Error with " + symbol);
			t.printStackTrace();
		}
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
	public long findMostRecentDate(String symbol, String barSize) {
		long timeInSecs = System.currentTimeMillis() / 1000;
		for (int i = 0; i < 100; i++) {
			timeInSecs -= i * 24 * 60 * 60;
			long openTime = getOpenTime(timeInSecs);
			final HashMap<String, List<HColumn<Long, Double>>> priceData = getPriceHistoricalData(symbol,
					openTime, openTime, barSize);
			final int recordCount = priceData.get(symbol + ":open").size();
			if (recordCount > 0)
				return openTime;
		}
		LoggerFactory.getLogger("HistoricalData").error(
				"Checked the past 100 days and there is no data for " + symbol + " in bar " + barSize);
		return 0;
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
			final double close, final int volume, final int count, final double WAP, final boolean hasGap) {
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

	<V> void printFirstRow(Rows<String, Long, V> rows) {

		for (Row<String, Long, V> row2 : rows) {
			ColumnSlice<Long, V> slice = row2.getColumnSlice();
			for (HColumn<Long, V> column : slice.getColumns()) {
				Long secs = column.getName();
				Date d = new Date(secs.longValue() * 1000);
				System.out.println(d + " " + column.getValue());
			}
		}
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
		final long openTime = getOpenTime(todayInSec);

		LoggerFactory.getLogger("HistoricalData").debug(
				"Get " + symbol + " open of day: " + new Date(openTime * 1000));
		Iterator<Bar> bars = getData(symbol, openTime, openTime, "bar5sec");
		if (bars.hasNext())
			return bars.next();
		return null;
	}

	/**
	 * @param todayInSec
	 * @return
	 */
	protected long getOpenTime(final long todayInSec) {
		// first assume today could be the number of days to go back from today
		long actualToday = todayInSec;
		if (todayInSec < 1000) {
			actualToday = (System.currentTimeMillis() / 1000) - todayInSec * 24 * 60 * 60;
		}
		Calendar today = Calendar.getInstance();
		today.setTimeInMillis(actualToday * 1000);
		if (today.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
			actualToday -= 2 * 24 * 60 * 60;
		else if (today.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY)
			actualToday -= 1 * 24 * 60 * 60;
		return HistoricalDateManipulation.getOpen(actualToday);
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
		LoggerFactory.getLogger("HistoricalData").debug(
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
			return new BarIterator(symbol, priceData,volData);
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
	 * Get the price data
	 * 
	 * @param symbol
	 * @return
	 * @throws ParseException
	 */
	public HashMap<String, List<HColumn<Long, Double>>> getPriceHistoricalData(final String symbol,
			final long start, final long finish, final String cf) {
		final HashMap<String, List<HColumn<Long, Double>>> result = new HashMap<String, List<HColumn<Long, Double>>>();
		RangeSlicesQuery<String, Long, Double> priceQuery = HFactory.createRangeSlicesQuery(keyspace,
				stringSerializer, longSerializer, doubleSerializer);
		priceQuery.setColumnFamily(cf);
		priceQuery.setRange(start, finish, false, maxRecordsReturned);

		for (String key : priceKeys) {
			String rowKey = symbol + key;
			priceQuery.setKeys(rowKey, rowKey);
			List<HColumn<Long, Double>> column = priceQuery.execute().get().getByKey(rowKey).getColumnSlice()
					.getColumns();
			result.put(rowKey, column);
		}
		return result;
	}

	/**
	 * Get the volume & tradeCount data
	 * 
	 * @param symbol
	 * @return
	 * @throws ParseException
	 */
	public HashMap<String, List<HColumn<Long, Long>>> getHistoricalData(final String symbol,
			final long start, final long finish, final String cf) {
		final HashMap<String, List<HColumn<Long, Long>>> result = new HashMap<String, List<HColumn<Long, Long>>>();
		SliceQuery<String, Long, Long> priceQuery = HFactory.createSliceQuery(keyspace, stringSerializer,
				longSerializer, longSerializer);
		priceQuery.setColumnFamily(cf);
		priceQuery.setRange(start, finish, false, maxRecordsReturned);

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
