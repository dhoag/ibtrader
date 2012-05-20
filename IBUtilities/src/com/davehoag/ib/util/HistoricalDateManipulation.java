package com.davehoag.ib.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class HistoricalDateManipulation {
	/**
	 * Get a list of Dates for which we want data.
	 * @param startingDateStr
	 * @return
	 * @throws ParseException
	 */
	public static ArrayList<String> getDates(final String startingDateStr) throws ParseException{
		Calendar today = Calendar.getInstance();
		
		return getDates(startingDateStr, today);
	}
	/**
	 * @param startingDateStr
	 * @param today
	 * @return
	 * @throws ParseException
	 */
	public static ArrayList<String> getDates(final String startingDateStr, final Calendar today) throws ParseException {
		DateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
		ArrayList<String> result = new ArrayList<String>();
		final Date d = df.parse(startingDateStr);
		Calendar startingDate = Calendar.getInstance();
		startingDate.setMinimalDaysInFirstWeek(4);
		today.setMinimalDaysInFirstWeek(4);
		startingDate.setTime(d);
		
		int yearDelta = today.get(Calendar.YEAR) - startingDate.get(Calendar.YEAR);
		int dayDelta = today.get(Calendar.DAY_OF_YEAR) - startingDate.get(Calendar.DAY_OF_YEAR);
		int daysInYear = startingDate.getActualMaximum( Calendar.DAY_OF_YEAR);
		
		if( yearDelta == 1){
			dayDelta = dayDelta + daysInYear;
		}
		int count = dayDelta / 7;
		
		for(int j=0; j < count; j++){
			startingDate.add( Calendar.DAY_OF_WEEK, 7);
			result.add(df.format(startingDate.getTime()));
		}
		result.add( df.format(today.getTime()));
		return result;
	}

}
