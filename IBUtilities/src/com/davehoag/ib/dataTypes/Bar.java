package com.davehoag.ib.dataTypes;

import java.util.Date;
import java.util.Calendar;

public class Bar {
	public long originalTime;
	public String symbol = "NONE";
	public double high;
	public double low;
	public double wap;
	public double close;
	public double open;
	public int tradeCount;
	public long volume;
	public boolean hasGaps = false;
	public String toString(){
		
		Date d = originalTime != 0 ? new Date(originalTime * 1000) : Calendar.getInstance().getTime();
		
		return symbol + " "+ d + " O:" + open + " C: " + close + " H:" + high + " L:" + low + " W:" + wap + " V:" + volume + " TC:" + tradeCount;
	}
}
