package com.davehoag.ib.tools;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;

import com.davehoag.ib.CassandraDao;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.BarCache;
import com.davehoag.ib.dataTypes.BarIterator;
import com.davehoag.ib.util.HistoricalDateManipulation;

public class DumpData {
	static final DecimalFormat nf = new DecimalFormat("###.##");
	static final DateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
	public static void main(String [] args){
		
		int i = 0;
		final String barSize = args[i++];
		final String startTime = args[i++];
		final String endTime = args[i++];
		PrintStream out = getOutputStream();
		
		writeDailyDataHeader(out);
		
		boolean dailyDataOnly = "bar1day".equals(barSize); 		
		if(dailyDataOnly) {
			out.println("");
		}
		else {
			out.print(",");
			writeIntradayHeader(out);
		}

		for( ; i< args.length;)
		try 
		{
			LinkedList<StringBuffer> logMsgs = new LinkedList<StringBuffer>();
			long start = df.parse(startTime).getTime()/1000;
			long finish = df.parse(endTime).getTime()/1000;
			final String symbol = args[i++];
			
			if(dailyDataOnly) {
				final BarIterator dailyDataIterator = CassandraDao.getInstance().getData(symbol, start, finish, "bar1day");
				dailyDataOnly(logMsgs, dailyDataIterator);
			}
			else{ 
				final BarIterator dailyDataIterator = CassandraDao.getInstance().getData(symbol, 365, finish, "bar1day");
				final BarIterator data = CassandraDao.getInstance().getData(symbol, start, finish, barSize);

				Bar today = null; 
				final BarCache dailyData = new BarCache(500);
				BarCache cache = new BarCache();
	
				for(Bar aBar: data){
					final StringBuffer buffer = new StringBuffer();
					while(dailyDataIterator.hasNext() && (today == null || (aBar.originalTime - today.originalTime > 60*60*10))){
						if(today != null) dailyData.push(today); 
						today = dailyDataIterator.next(); 
					}
					cache.push(aBar);
					
					bufferDailyData(dailyData, buffer);
					final int count = logMsgs.size();
					int periods = 500;
					int lookBack = count < periods ? count : periods;
					buffer.append( "," + nf.format(aBar.open) );
					buffer.append( "," + nf.format(aBar.high) );
					buffer.append( "," + nf.format(aBar.low) );
					buffer.append( "," + nf.format(aBar.close) );
					buffer.append( "," + nf.format(aBar.volume) );
					buffer.append( "," + nf.format(aBar.wap) );
					buffer.append( "," + nf.format(aBar.tradeCount) );
					buffer.append( "," + nf.format(cache.getFibonacciRetracement(lookBack, 1)) );
					buffer.append( "," + nf.format(cache.getFibonacciRetracement(lookBack, 0)) );
					buffer.append( "," + nf.format(cache.getFibonacciRetracement(lookBack, .382)) );
					buffer.append( "," + nf.format(cache.getFibonacciRetracement(lookBack, .618)) );
					buffer.append( "," + nf.format(cache.getADL(0, 60, false)));
					buffer.append( "," + nf.format(cache.getADL(0, 60, true)));
					logMsgs.add(buffer);
					
				}
			}
			for(StringBuffer result : logMsgs){
				out.println(result);
			}
		}//end try
		catch (ParseException e) {
			e.printStackTrace();
		}
		if(out != System.out){
			out.flush(); out.close();
		}
	}

	/**
	 * @param logMsgs
	 * @param dailyDataIterator
	 */
	protected static void dailyDataOnly(LinkedList<StringBuffer> logMsgs, final BarIterator dailyDataIterator) {
		final BarCache dailyData = new BarCache(1500);
		for(Bar dailyBar: dailyDataIterator ){
			
			dailyData.push(dailyBar);

			try { 
				final StringBuffer buffer = writeDailyData( dailyData);
				logMsgs.add(buffer);
				appendFuture(logMsgs, dailyData,3,15,60);
			}catch(IllegalStateException ex){
				//ignore this buffer
				System.out.println("Ingoring " + dailyBar + " ex: " + ex);
			}
		}
	}

	/**
	 * Add the future index of a prior bar that is stored in the logMsgs list
	 * @param logMsgs
	 * @param barCache
	 */
	protected static void appendFuture(LinkedList<StringBuffer> logMsgs, final BarCache barCache, final int... lookback) {
		final int count = logMsgs.size() -1; //zero based counting
		for(int daysBack: lookback){
			if(count >= daysBack ){
				final StringBuffer threeDaysBack = logMsgs.get(count-daysBack);
				threeDaysBack.append("," + barCache.getFutureTrend(barCache.get(daysBack), daysBack));
			}
		}
	}

	/**
	 * @param symbol
	 * @param dailyData
	 * @param dailyBar
	 * @param buffer
	 */
	protected static StringBuffer writeDailyData(final BarCache dailyData) {
		final StringBuffer buffer = new StringBuffer();
		bufferDailyData(dailyData, buffer);
		return buffer;
	}

	/**
	 * @param out
	 */
	protected static void writeIntradayHeader(PrintStream out) {
		out.print("open,high,low,close,vol,vwap,count,fibLow,fibHigh,fib382,fib618");
		out.print(",ad,advwap");
		out.println(",100sec");
	}

	/**
	 * @param out
	 */
	protected static void writeDailyDataHeader(PrintStream out) {
		out.print("Sym,date,yesterdayClose,yH,yL,tOpen,fibLowD,tLow-fibLow,fibHighD,fib382D,fib618D,ma20,");
		out.print("ma13,psarLow,psarHigh,dailyAd,dailyAdvwap,shortFut,medFut,longFut");
	}

	/**
	 * @param nf
	 * @param dailyData
	 * @param buffer
	 */
	protected static void bufferDailyData( final BarCache dailyData, final StringBuffer buffer) {
		
		final Bar yesterday = dailyData.get(1);
		//general output is to show the way info looked at the open of today
		//so, any calculations can not include today's bar. Must offset to yesterday
		final Bar dailyBar = dailyData.get(0);
		
		buffer.append(dailyBar.symbol + ",");
		buffer.append(HistoricalDateManipulation.getDateAsStr(dailyBar.originalTime) +",");
		if(yesterday != null) { 
			buffer.append(nf.format(yesterday.close)+ ",");
			buffer.append(nf.format(yesterday.high)+ ",");
			buffer.append(nf.format(yesterday.low)+ ",");
		}
		else {
			 buffer.append(",");
		}
		buffer.append(nf.format(dailyBar.open) + ",");

		int fibRange = 30;
		double fibLow = dailyData.getFibonacciRetracement(1, fibRange, 1);
		buffer.append(nf.format( fibLow) + ",");
		if(fibLow != 0) buffer.append(nf.format(dailyBar.low - fibLow));
		buffer.append(",");
		buffer.append(nf.format(dailyData.getFibonacciRetracement(1, fibRange, 0)) + ",");
		buffer.append(nf.format(dailyData.getFibonacciRetracement(1, fibRange, .382)) + ",");
		buffer.append(nf.format(dailyData.getFibonacciRetracement(1, fibRange, .618)) + ",");
		buffer.append(nf.format( dailyData.getMA(1,20, 'w')) + ",");
		buffer.append(nf.format( dailyData.getMA(1,13, 'w')) + ",");
		buffer.append(nf.format( dailyData.getParabolicSar(1, 15, 0)[0]) + ",");
		buffer.append(nf.format( dailyData.getParabolicSar(1, 15, 0)[1] ));
		buffer.append( "," + nf.format(dailyData.getADL(1, 10, false)));
		buffer.append( "," + nf.format(dailyData.getADL(1, 10, true)));

	}

	/**
	 * @return
	 */
	private static PrintStream getOutputStream() {
		PrintStream out;
		try{ 
			FileOutputStream fos = new FileOutputStream("datadump.csv");
			out = new PrintStream(fos);
		}
		catch(Exception ex){
			ex.printStackTrace();
			out = System.out;
		}
		return out;
	}
}