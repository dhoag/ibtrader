package com.davehoag.ib.tools;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.Random;

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
		
		
		boolean dailyDataOnly = "bar1day".equals(barSize);
		boolean random = "rnd".equals(barSize);
		if(dailyDataOnly) {
			writeDailyDataHeader(out);
			out.println("");
		}
		else {
			if(! random ){
				writeDailyDataHeader(out);
				out.print(",");
			}
			else {
				out.print("symbol,");
			}
			writeIntradayHeader(out);
			out.println("");
		}
		//Need same seed for each symbol
		long rndSeed = System.currentTimeMillis();

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
			else if(random){
				Random rnd = new Random( rndSeed );
				System.out.println("Start " + start);
				long range = finish - start;
				for(int j = 0; j < 25; j++){

					double d = rnd.nextDouble();
					long offset = (long)(range * d);
					long newStart = start + offset;
					long newFinish = newStart + 60*60*24;
					final BarIterator data = CassandraDao.getInstance().getData(symbol, newStart, newFinish, "bar5sec");

					for(Bar aBar: data){
						final StringBuffer buffer = new StringBuffer();
						buffer.append(aBar.symbol);
						buffer.append( "," + HistoricalDateManipulation.getDateAsStr(aBar.originalTime) );
						buffer.append( "," + nf.format(aBar.open) );
						buffer.append( "," + nf.format(aBar.high) );
						buffer.append( "," + nf.format(aBar.low) );
						buffer.append( "," + nf.format(aBar.close) );
						buffer.append( "," + nf.format(aBar.volume) );
						buffer.append( "," + nf.format(aBar.wap) );
						buffer.append( "," + nf.format(aBar.tradeCount) );
						logMsgs.add(buffer);
					}
				}
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
						today = dailyDataIterator.next(); 
						dailyData.push(today); 
					}
					cache.push(aBar);
					
					bufferDailyData(dailyData, buffer);
					buffer.append( "," + HistoricalDateManipulation.getDateAsStr(aBar.originalTime) );
					buffer.append( "," + nf.format(aBar.open) );
					buffer.append( "," + nf.format(aBar.high) );
					buffer.append( "," + nf.format(aBar.low) );
					buffer.append( "," + nf.format(aBar.close) );
					buffer.append( "," + nf.format(aBar.volume) );
					buffer.append( "," + nf.format(aBar.wap) );
					buffer.append( "," + nf.format(aBar.tradeCount) );
	/*				final int count = logMsgs.size();
					int periods = 500;
					int lookBack = count < periods ? count : periods;
					buffer.append( "," + nf.format(cache.getFibonacciRetracement(lookBack, 1)) );
					buffer.append( "," + nf.format(cache.getFibonacciRetracement(lookBack, 0)) );
					buffer.append( "," + nf.format(cache.getFibonacciRetracement(lookBack, .382)) );
					buffer.append( "," + nf.format(cache.getFibonacciRetracement(lookBack, .618)) );
					buffer.append( "," + nf.format(cache.getADL(0, 60, false)));
					buffer.append( "," + nf.format(cache.getADL(0, 60, true)));
					*/
					logMsgs.add(buffer);
					
				}
			}
			System.out.println("Writing data to output stream - # rows:" + logMsgs.size());
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
	 * @param m_contract
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
		out.print("barTime,open,high,low,close,vol,vwap,count");
	/*	out.print(",fibLow,fibHigh,fib382,fib618");
		out.print(",ad,advwap");
		out.println(",100sec");
		*/
	}

	/**
	 * @param out
	 */
	protected static void writeDailyDataHeader(PrintStream out) {
		out.print("Sym,date,yesterdayClose,yH,yL," +
				"tOpen,tHigh,tLow,tClose,tWap,tVol,tTrdCnt");
	//	out.print(",fibLowD,tLow-fibLow,fibHighD,fib382D,fib618D");
	//	out.print(",psarLow,psarHigh,swingDev,dailyAd,dailyAdvwap,shortFut,medFut,longFut");
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
		buffer.append(nf.format(dailyBar.high) + ",");
		buffer.append(nf.format(dailyBar.low) + ",");
		buffer.append(nf.format(dailyBar.close) + ",");
		buffer.append(nf.format(dailyBar.wap) + ",");
		buffer.append(nf.format(dailyBar.volume) + ",");
		buffer.append(nf.format(dailyBar.tradeCount) );
		
/*
		int fibRange = 30;
		double fibLow = dailyData.getFibonacciRetracement(1, fibRange, 1);
		buffer.append(",");
		buffer.append(nf.format( fibLow) + ",");
		if(fibLow != 0) buffer.append(nf.format(dailyBar.low - fibLow));
		buffer.append(",");
		buffer.append(nf.format(dailyData.getFibonacciRetracement(1, fibRange, 0)) + ",");
		buffer.append(nf.format(dailyData.getFibonacciRetracement(1, fibRange, .382)) + ",");
		buffer.append(nf.format(dailyData.getFibonacciRetracement(1, fibRange, .618)) + ",");
		buffer.append(nf.format( dailyData.getParabolicSar(1, 15, 0)[0]) + ",");
		buffer.append(nf.format( dailyData.getParabolicSar(1, 15, 0)[1] ) + ",");
		buffer.append(nf.format(dailyData.getSwingDeviation(1, 15) ) + ",");
		buffer.append(nf.format(dailyData.getADL(1, 10, false)) + ",");
		buffer.append(nf.format(dailyData.getADL(1, 10, true)));
		*/

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