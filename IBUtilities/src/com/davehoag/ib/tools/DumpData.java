package com.davehoag.ib.tools;

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
		
		for( ; i< args.length;)
		try 
		{
			final String symbol = args[i++];
			long start = df.parse(startTime).getTime()/1000;
			long finish = df.parse(endTime).getTime()/1000;
			//System.out.println("Start " + startTime + " " + start + " " + endTime + " " + finish);
			System.out.println("Sym,date,open,high,low,close,vol,vwap,fibLow,fibHigh,fib382,fib618");
			BarIterator data = CassandraDao.getInstance().getData(symbol, start, finish, barSize);
			int count = 0;
			for(Bar aBar: data){
				cache.pushLatest(aBar);
				count++;
				int lookBack = count < periods ? count : periods;
				System.out.print(symbol + "," + HistoricalDateManipulation.getDateAsStr(aBar.originalTime));
				System.out.print( "," + nf.format(aBar.open) );
				System.out.print( "," + nf.format(aBar.high) );
				System.out.print( "," + nf.format(aBar.low) );
				System.out.print( "," + nf.format(aBar.close) );
				System.out.print( "," + nf.format(aBar.volume) );
				System.out.print( "," + nf.format(aBar.wap) );
				System.out.print( "," + nf.format(cache.getFibonacciRetracement(lookBack, 1)) );
				System.out.print( "," + nf.format(cache.getFibonacciRetracement(lookBack, 0)) );
				System.out.print( "," + nf.format(cache.getFibonacciRetracement(lookBack, .382)) );
				System.out.println( "," + nf.format(cache.getFibonacciRetracement(lookBack, .618)) );
			}
		}//end try
		catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}