package com.davehoag.ib.tools;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;

import com.davehoag.ib.CassandraDao;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.BarIterator;
import com.davehoag.ib.dataTypes.SimpleMovingAvg;
import com.davehoag.ib.util.HistoricalDateManipulation;

public class MineData {
	public static void main(String [] args){
		DateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
		try 
		{
			long start = df.parse("20120701 08:00:00").getTime()/1000;
			long finish = df.parse("20120823 04:00:00").getTime()/1000;
			BarIterator data = CassandraDao.getInstance().getData("QQQ", start, finish, "bar5sec");
			for(int i = 0; i < 20; i++){
				int slowLegSize = 6 + i;
				int count = 0;
				double total = 0;
				SimpleMovingAvg sma = new SimpleMovingAvg(5, slowLegSize);
				while(data.hasNext()){
					Bar b = data.next();
					boolean ignore = (b.originalTime < HistoricalDateManipulation.getOpen(b.originalTime) + 5*slowLegSize);
					sma.newTick(b.close);
					if(!ignore && sma.isInitialized()){
						count++; total += Math.abs(sma.getSlowDelta());
						if(Math.abs(sma.getSlowDelta()) > .25){
							System.out.println(sma.getOldestTick() + " -> " + b);
							
						}//end if
					}//end if
				}//end while
				System.out.println("Complete [" + slowLegSize + "] " + total + "/" + count + " = " + (total/count));
				data.reset();
			}//end for
			
		}//end try
		catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}