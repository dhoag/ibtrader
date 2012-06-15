package com.davehoag.ib.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
/**
 * Facilitate different breakdown ranges to conform with IB constraints
 * @author thinkpad7
 *
 */
public class HistoricalDateManipulation {
	/**
	 * Get a list of Dates for which we want data.
	 * @param startingDateStr
	 * @return
	 * @throws ParseException
	 */
	public static ArrayList<String> getDatesBrokenIntoHours(final String startingDateStr) throws ParseException{
		Calendar today = Calendar.getInstance();
		
		return getDatesBrokenIntoHours(startingDateStr, today);
	}
	/**
	 * Convert string value into a starting time 
	 * @param dateStr
	 * @return
	 * @throws ParseException
	 */
	public static long getTime(String dateStr) throws ParseException{
		final DateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
		final Date d = df.parse(dateStr );
		return d.getTime() / 1000;
	}
	/**
	 * From the starting date figure out how how many discrete entries are required to request
	 * historical data from the given start date.
	 * 
	 * @param startingDateStr
	 * @param today
	 * @return
	 * @throws ParseException
	 */
	public static ArrayList<String> getDatesBrokenIntoHours(final String startingDateStr, final Calendar today) throws ParseException {
		final DateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
		final ArrayList<String> result = new ArrayList<String>();
		final Date d = df.parse(startingDateStr + " 00:00:00");
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
		int count = dayDelta;
		
		for(int j=0; j <= count; j++){
			Calendar topDay = (Calendar)startingDate.clone();
			final int dayOfWeek = topDay.get(Calendar.DAY_OF_WEEK); 
			if( dayOfWeek != Calendar.SUNDAY && dayOfWeek != Calendar.SATURDAY)
			{
				topDay.add( Calendar.HOUR, 7);
				for(int i = 0; i < 10; i++){
					topDay.add( Calendar.HOUR, 1);
					result.add(df.format(topDay.getTime()));
				}
			}
			startingDate.add( Calendar.DAY_OF_WEEK, 1);
		}
		return result;
	}
	/**
	 * @param startingDateStr
	 * @param today
	 * @return
	 * @throws ParseException
	 */
	public static ArrayList<String> getDatesBrokenIntoWeeks(final String startingDateStr, final Calendar today) throws ParseException {
		DateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
		ArrayList<String> result = new ArrayList<String>();
		final Date d = df.parse(startingDateStr + " 00:00:00");
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
