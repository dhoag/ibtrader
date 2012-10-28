package com.davehoag.ib.dataTypes;

import java.util.Date;
import java.util.Calendar;

import com.davehoag.ib.util.HistoricalDateManipulation;

public class Bar {
	public String barSize;
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
	public Date getTime(){
		Date d = originalTime != 0 ? new Date(originalTime * 1000) : Calendar.getInstance().getTime();
		return d;
	}
	public String toString(){
		Date d = getTime();
		return symbol + " "+ d + " O:" + open + " C: " + close + " H:" + high + " L:" + low + " W:" + wap + " V:" + volume + " TC:" + tradeCount;
	}
	public double delta(){
		return close - open;
	}
	public boolean isUp(){
		return close > open;
	}
	public boolean isTrendingUp(){
		return close > wap;
	}
	public boolean isSameDay(final Bar aBar) {
		long open = HistoricalDateManipulation.getOpen(originalTime);
		long otherOpen = HistoricalDateManipulation.getOpen(aBar.originalTime);
		return open == otherOpen;
	}
}
