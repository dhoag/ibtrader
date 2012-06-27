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
 */
public class HistoricalDateManipulation {
	final static DateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
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
	 * Tweak the time to be 8:30, the time of the first bar
	 * @param time
	 * @return
	 */
	public static long getOpen(final long time){
		final Date d = new Date(time*1000);
		final Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		cal.set(Calendar.HOUR_OF_DAY, 8);
		cal.set(Calendar.MINUTE, 30);
		cal.set(Calendar.SECOND, 0);
		return cal.getTimeInMillis() / 1000;
	}
	/**
	 * @param time
	 * @return
	 */
	public static int getHour(final long time) {
		final Date d = new Date(time*1000);
		final Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		final int hour = cal.get(cal.HOUR_OF_DAY);
		return hour;
	}
	/**
	 * 
	 * @param time
	 * @return
	 */
	public static boolean isEndOfDay(final long time){
		final Date d = new Date(time*1000);
		final Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		final int hour = cal.get(cal.HOUR_OF_DAY);
		final int minutes = cal.get(cal.MINUTE);
		final int seconds = cal.get(cal.SECOND);
		return(hour == 14 && minutes == 59 && seconds == 55);
	}
	/**
	 * Convert string value into a starting time 
	 * @param dateStr
	 * @return
	 * @throws ParseException
	 */
	public static long getTime(String dateStr) throws ParseException{
		final Date d = df.parse(dateStr );
		return d.getTime() / 1000;
	}
	public static String getDateAsStr(final long seconds){
		final Date d = new Date(seconds*1000);
		return getDateAsStr(d);
	}
	public static String getDateAsStr(final Date date){
		return df.format(date);
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
					result.add(getDateAsStr(topDay.getTime()));
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
