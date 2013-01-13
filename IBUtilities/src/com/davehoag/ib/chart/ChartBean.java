package com.davehoag.ib.chart;

import java.text.DecimalFormat;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;

import com.davehoag.ib.CassandraDao;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.BarIterator;
import com.davehoag.ib.util.HistoricalDateManipulation;

public class ChartBean {
	String start = "";
	String end = "";
	boolean fiveSecData = false;
	String symbol = "";
	
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		System.out.println("set sym " + symbol);
		this.symbol = symbol;
		fiveSecData = false; 
	}
	public String getStart() {
		return start;
	}
	public void setStart(String start) {
		this.start = start;
	}
	public String getEnd() {
		return end;
	}
	public void setEnd(String end) {
		this.end = end;
	}
	public boolean getFiveSecData() {
		return fiveSecData;
	}
	public void setFiveSecData(boolean fiveSecData) {
		System.out.println("bool" + fiveSecData);
		this.fiveSecData = fiveSecData;
	}
	public void setDataTable(JspWriter out, HttpSession session) throws Exception{
		System.out.println("Here");
		final DecimalFormat df = new DecimalFormat("#.##");

		boolean isFiveSec = fiveSecData;

		String barSize = isFiveSec ? "bar5sec" : "bar1day";
		if (start == null && symbol != null && symbol.length() > 0) {
			long date = CassandraDao.getInstance().findMostRecentDate(symbol, barSize);
			if (date == 0) {
				out.println("No data found for " + symbol);
			}
			else {
				out.println(symbol + " most recent record: " + HistoricalDateManipulation.getDateAsStr(date));
			}
		}
		if (start != null && start.trim().length() > 0 && end != null && symbol != null) {
			BarIterator bars = CassandraDao.getInstance().getData(symbol, start.trim() + " 00:00:00",
					end + " 18:00:00", barSize);
			out.println("<script> dataTable = [");
			boolean first = true;
			for (Bar aBar : bars) {
				if (first) {
					first = false;
				}
				else {
					out.println(",");
				}
				out.print("['" + HistoricalDateManipulation.getDateAsStr(aBar.originalTime) + "', "
						+ df.format(aBar.low) + ", " + df.format(aBar.open) + ", " + df.format(aBar.close)
						+ ", " + df.format(aBar.high) + "]");
			}
			out.println("] </script>");
			System.out.println("wrote data");
		}
	}
}
