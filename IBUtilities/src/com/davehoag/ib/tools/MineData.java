package com.davehoag.ib.tools;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;

import com.davehoag.ib.CassandraDao;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.SimpleMovingAvg;

public class MineData {
	public static void main(String [] args){
		DateFormat df = new SimpleDateFormat("yyyyMMdd  HH:mm:ss");
		long start;
		try {
			start = df.parse("20120701 08:00:00").getTime()/1000;
			long finish = df.parse("20120725 04:00:00").getTime()/1000;
			Iterator<Bar> data = CassandraDao.getInstance().getData("QQQ", start, finish, "bar5sec");
			SimpleMovingAvg sma = new SimpleMovingAvg(5, 20);
			while(data.hasNext()){
				Bar b = data.next();
				sma.newTick(b.close);
				if(sma.isInitialized()){
					if(sma.getSlowChange() > .01){
						System.out.println(b);
					}
				}
			}
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
