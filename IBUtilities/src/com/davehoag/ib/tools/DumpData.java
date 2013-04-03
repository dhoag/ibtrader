package com.davehoag.ib.tools;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.davehoag.ib.CassandraDao;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.BarCache;
import com.davehoag.ib.dataTypes.BarIterator;
import com.davehoag.ib.util.HistoricalDateManipulation;

public class DumpData {
	public static void main(String [] args){
		DateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
		NumberFormat nf = NumberFormat.getNumberInstance();
		int i = 0;
		final String barSize = args[i++];
		final String startTime = args[i++];
		final String endTime = args[i++];
		BarCache cache = new BarCache();
		int periods = 500;
//		PrintStream out = System.out;
		PrintStream out = getOutputStream();
		
		
		for( ; i< args.length;)
		try 
		{
			final String symbol = args[i++];
			long start = df.parse(startTime).getTime()/1000;
			long finish = df.parse(endTime).getTime()/1000;
			//System.out.println("Start " + startTime + " " + start + " " + endTime + " " + finish);
			out.println("Sym,date,yesterdayClose,todayOpen,fibLowD,fibHighD,fib382D,fib618D,ma20,ma13,psarLow,psarHigh,open,high,low,close,vol,vwap,count,fibLow,fibHigh,fib382,fib618");
			final BarIterator data = CassandraDao.getInstance().getData(symbol, start, finish, barSize);
			//go back one year
			final BarIterator dailyDataIterator = CassandraDao.getInstance().getData(symbol, 356, finish, "bar1day");
			final BarCache dailyData = new BarCache(500);
			
			int count = 0;
			Bar today = null; 
			for(Bar aBar: data){
				while(dailyDataIterator.hasNext() && (today == null || (aBar.originalTime - today.originalTime > 60*60*10))){
					if(today != null) dailyData.pushLatest(today); 
					today = dailyDataIterator.next(); 
				}
				Bar yesterday = dailyData.get(0);
				cache.pushLatest(aBar);
				count++;
				int lookBack = count < periods ? count : periods;
				out.print(symbol + ",");
				out.print(HistoricalDateManipulation.getDateAsStr(aBar.originalTime) +",");
				out.print(nf.format(yesterday.close)+ ",");
				out.print(nf.format(today.open) + ",");
				double fibLow = dailyData.getFibonacciRetracement(30, 1);
				int fibRange = 30;
				while(fibLow == 0 && fibRange < 120){
					fibRange++;
					fibLow = dailyData.getFibonacciRetracement(fibRange, 1);
				}
				out.print(nf.format(dailyData.getFibonacciRetracement(fibRange, 1)) + ",");
				out.print(nf.format(dailyData.getFibonacciRetracement(fibRange, 0)) + ",");
				out.print(nf.format(dailyData.getFibonacciRetracement(fibRange, .382)) + ",");
				out.print(nf.format(dailyData.getFibonacciRetracement(fibRange, .618)) + ",");
				out.print(nf.format( dailyData.getMA(20, 'w')) + ",");
				out.print(nf.format( dailyData.getMA(13, 'w')) + ",");
				out.print(nf.format( dailyData.getParabolicSar(15, 0)[0]) + ",");
				out.print(nf.format( dailyData.getParabolicSar(15, 0)[1] ));
				out.print( "," + nf.format(aBar.open) );
				out.print( "," + nf.format(aBar.high) );
				out.print( "," + nf.format(aBar.low) );
				out.print( "," + nf.format(aBar.close) );
				out.print( "," + nf.format(aBar.volume) );
				out.print( "," + nf.format(aBar.wap) );
				out.print( "," + nf.format(aBar.tradeCount) );
				out.print( "," + nf.format(cache.getFibonacciRetracement(lookBack, 1)) );
				out.print( "," + nf.format(cache.getFibonacciRetracement(lookBack, 0)) );
				out.print( "," + nf.format(cache.getFibonacciRetracement(lookBack, .382)) );
				out.println( "," + nf.format(cache.getFibonacciRetracement(lookBack, .618)) );
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