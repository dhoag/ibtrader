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
	public static void main(String [] args){
		DateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
		DecimalFormat nf = new DecimalFormat("###.##");
		
		int i = 0;
		final String barSize = args[i++];
		final String startTime = args[i++];
		final String endTime = args[i++];
		BarCache cache = new BarCache();
		int periods = 500;
		//PrintStream out = System.out;
		PrintStream out = getOutputStream();
		LinkedList<Bar> bars = new LinkedList<Bar>();
		LinkedList<StringBuffer> logMsgs = new LinkedList<StringBuffer>();
		
		for( ; i< args.length;)
		try 
		{
			final String symbol = args[i++];
			long start = df.parse(startTime).getTime()/1000;
			long finish = df.parse(endTime).getTime()/1000;
			
			//System.out.println("Start " + startTime + " " + start + " " + endTime + " " + finish);
			out.print("Sym,date,yesterdayClose,todayOpen,fibLowD,fibHighD,fib382D,fib618D,ma20,ma13,psarLow,psarHigh");
			out.print(",dailyAd,dailyAdvwap");
			out.print(",open,high,low,close,vol,vwap,count,fibLow,fibHigh,fib382,fib618");
			out.print(",ad,advwap");
			out.println(",100sec");
			final BarIterator data = CassandraDao.getInstance().getData(symbol, start, finish, barSize);
			//go back one year
			final BarIterator dailyDataIterator = CassandraDao.getInstance().getData(symbol, 356, finish, "bar1day");
			final BarCache dailyData = new BarCache(500);
			
			int count = 0;
			Bar today = null; 
			for(Bar aBar: data){
				final StringBuffer buffer = new StringBuffer();
				while(dailyDataIterator.hasNext() && (today == null || (aBar.originalTime - today.originalTime > 60*60*10))){
					if(today != null) dailyData.push(today); 
					today = dailyDataIterator.next(); 
				}
				Bar yesterday = dailyData.get(0);
				cache.push(aBar);
				count++;
				int lookBack = count < periods ? count : periods;
				
				buffer.append(symbol + ",");
				buffer.append(HistoricalDateManipulation.getDateAsStr(aBar.originalTime) +",");
				buffer.append(nf.format(yesterday.close)+ ",");
				buffer.append(nf.format(today.open) + ",");
				double fibLow = dailyData.getFibonacciRetracement(30, 1);
				int fibRange = 30;
				while(fibLow == 0 && fibRange < 120){
					fibRange++;
					fibLow = dailyData.getFibonacciRetracement(fibRange, 1);
				}
				buffer.append(nf.format(dailyData.getFibonacciRetracement(fibRange, 1)) + ",");
				buffer.append(nf.format(dailyData.getFibonacciRetracement(fibRange, 0)) + ",");
				buffer.append(nf.format(dailyData.getFibonacciRetracement(fibRange, .382)) + ",");
				buffer.append(nf.format(dailyData.getFibonacciRetracement(fibRange, .618)) + ",");
				buffer.append(nf.format( dailyData.getMA(20, 'w')) + ",");
				buffer.append(nf.format( dailyData.getMA(13, 'w')) + ",");
				buffer.append(nf.format( dailyData.getParabolicSar(15, 0)[0]) + ",");
				buffer.append(nf.format( dailyData.getParabolicSar(15, 0)[1] ));
				buffer.append( "," + nf.format(dailyData.getADL(0, 10, false)));
				buffer.append( "," + nf.format(dailyData.getADL(0, 10, true)));
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
				bars.add(aBar);
				if(logMsgs.size() > 20){
					//look at the next 100 seconds
					final StringBuffer msg = logMsgs.removeFirst();
					final Bar olderBar = bars.removeFirst();
					msg.append("," + cache.getFutureTrend(olderBar, 20));
					
					//look forward on the next 5 days - doesn't work as I don't add all days
					/*final long daysBack = olderBar.originalTime;
					final int tradingDayHistory = Math.min(dailyData.indexOf(daysBack), 5);
					msg.append("," + dailyData.getFutureTrend(daysBack, tradingDayHistory));
					*/
					
					out.println(msg.toString());
				}
			}
			if(out != System.out){
				out.flush(); out.close();
			}
		}//end try
		catch (ParseException e) {
			e.printStackTrace();
		}
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