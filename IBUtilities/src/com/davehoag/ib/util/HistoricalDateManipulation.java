package com.davehoag.ib.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
/**
 * Facilitate different breakdown ranges to conform with IB constraints
 * @author dhoag
 */
public class HistoricalDateManipulation {
	final static ThreadLocal<SimpleDateFormat> df = new ThreadLocal<SimpleDateFormat>(); 
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
	 * Figure out the day I think is the closest trading day to the provided day of month
	 * @param seconds a reference date
	 * @param desiredDay a number day of month. 
	 * @return
	 */
	public static int getTargetDay(final Date date, final int desiredDay){
		Calendar c = Calendar.getInstance();
		c.setTime( date );
		c.setLenient(false);
		int maxNumDays = c.getActualMaximum(Calendar.DAY_OF_MONTH);
		final int day = desiredDay > maxNumDays ? maxNumDays: desiredDay;
		c.set(Calendar.DAY_OF_MONTH, day);
		int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
		int result = c.get(Calendar.DAY_OF_MONTH);
		if(dayOfWeek == Calendar.SUNDAY)  result -=2;
		if(dayOfWeek == Calendar.SATURDAY) result -=1;
		if(result < 0) {
			result = c.get(Calendar.DAY_OF_MONTH);
			if(dayOfWeek == Calendar.SUNDAY)  result +=1;
			if(dayOfWeek == Calendar.SATURDAY) result +=2;
		}
		return result;
	}

	/**
	 * Pass in the number of days to go back or the seconds since 1970 for a bar on the day
	 * 
	 * @param todayInSec < 1000 go back that # of days otherwise get open for the day represented
	 * @return
	 */
	public static long getOpenTime(final long todayInSec) {
		// first assume today could be the number of days to go back from today
		long actualToday = todayInSec;
		if (todayInSec < 1000) {
			actualToday = (System.currentTimeMillis() / 1000) - todayInSec * 24 * 60 * 60;
		}
		Calendar today = Calendar.getInstance();
		today.setTimeInMillis(actualToday * 1000);
		if (today.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
			actualToday -= 2 * 24 * 60 * 60;
		else if (today.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY)
			actualToday -= 1 * 24 * 60 * 60;
		return getOpen(actualToday);
	}
	/**
	 * Is the provided time the start of the day?
	 * @param time
	 * @return
	 */
	public boolean isOpen(final long time){
		final Date d = new Date(time*1000);
		final Calendar cal = Calendar.getInstance();
		final int hour = cal.get(Calendar.HOUR_OF_DAY);
		final int minutes = cal.get(Calendar.MINUTE);
		final int seconds = cal.get(Calendar.SECOND);
		return (hour == 8 && minutes == 30 && seconds == 00);
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
	 * Tweak the time to be 2:55:55, the time of the last bar (on most days)
	 * TODO Somedays its not the last trading hour
	 * @param time
	 * @return
	 */
	public static long getClose(final long time){
		final Date d = new Date(time*1000);
		final Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		cal.set(Calendar.HOUR_OF_DAY, 14);
		cal.set(Calendar.MINUTE, 55);
		cal.set(Calendar.SECOND, 55);
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
		final int hour = cal.get(Calendar.HOUR_OF_DAY);
		return hour;
	}
	/**
	 * Get the day of month represented by the seconds
	 * @param time
	 * @return
	 */
	public static int getDay(final long time){
		final Date d = new Date(time*1000);
		final Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		final int hour = cal.get(Calendar.DAY_OF_MONTH);
		return hour;
	}
	
	/**
	 * Given a bar comes out at the end of the period need to be 5 seconds ahead
	 * of the last bar. Market is closed when last bar comes.
	 * 
	 * @param time
	 * @return
	 */
	public static boolean isEndOfDay(final long time){
		final Date d = new Date(time*1000);
		final Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		final int hour = cal.get(Calendar.HOUR_OF_DAY);
		final int minutes = cal.get(Calendar.MINUTE);
		final int seconds = cal.get(Calendar.SECOND);
		return (hour == 14 && minutes == 59 && seconds == 50);
	}
	/**
	 * Convert string value into a starting time 
	 * @param dateStr
	 * @return
	 * @throws ParseException
	 */
	public static long getTime(String dateStr) throws ParseException{
		if(df.get() == null) df.set( new SimpleDateFormat("yyyyMMdd HH:mm:ss"));
		
		final Date d = df.get().parse(dateStr );
		return d.getTime() / 1000;
	}
	public static String getDateAsStr(final long seconds){
		final Date d = new Date(seconds*1000);
		return getDateAsStr(d);
	}
	public static String getDateAsStr(final Date date){
		if(df.get() == null) df.set( new SimpleDateFormat("yyyyMMdd HH:mm:ss"));

		return df.get().format(date);
	}
	public static String getTimeAsStr(final long seconds){
		final Date d = new Date(seconds*1000);
		return getTimeAsStr(d);
	}
	public static String getTimeAsStr(final Date date){
		//not a frequently called method thus no caching
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		return sdf.format(date);
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
		final Calendar startingDate = getStartCalendar(startingDateStr, today);	
		final int dayDelta = getDayDelta(today, startingDate);
		int count = dayDelta;
		
		for(int j=0; j <= count; j++){
			Calendar topDay = (Calendar)startingDate.clone();
			final int dayOfWeek = topDay.get(Calendar.DAY_OF_WEEK); 
			if( dayOfWeek != Calendar.SUNDAY && dayOfWeek != Calendar.SATURDAY)
			{
				topDay.add( Calendar.HOUR, 8);
				for(int i = 0; i < 8; i++){
					topDay.add( Calendar.HOUR, 1);
					//Add the historicalData end time, want 9am - 3pm
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
		if(df.get() == null) df.set( new SimpleDateFormat("yyyyMMdd HH:mm:ss"));

		ArrayList<String> result = new ArrayList<String>();
		Calendar startingDate = getStartCalendar(startingDateStr, today);
		
		int dayDelta = getDayDelta(today, startingDate);
		int count = dayDelta / 7;
		
		for(int j=0; j < count; j++){
			startingDate.add( Calendar.DAY_OF_WEEK, 7);
			result.add(df.get().format(startingDate.getTime()));
		}
		result.add( df.get().format(today.getTime()));
		return result;
	}
	/**
	 * 
	 * @param startingDateStr
	 * @param today
	 * @return
	 * @throws ParseException
	 */
	public static ArrayList<String> getWeekDays(final String startingDateStr, final Calendar today) throws ParseException {
		final ArrayList<String> result = new ArrayList<String>();
		final DateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
		final Calendar startingDate = getStartCalendar(startingDateStr, today);
		
		int dayDelta = getDayDelta(today, startingDate);
		int count = dayDelta;
		
		result.add( df.format(startingDate.getTime()));
		for(int j=0; j < count; j++){
			startingDate.add( Calendar.DATE, 1);
			final int dayOfWeek = startingDate.get(Calendar.DAY_OF_WEEK); 
			if( dayOfWeek != Calendar.SUNDAY && dayOfWeek != Calendar.SATURDAY)
			{
				result.add(df.format(startingDate.getTime()));
			}
		}
		return result;
	}
	/**
	 * @param startingDateStr
	 * @param today
	 * @param df
	 * @return
	 * @throws ParseException
	 */
	private static Calendar getStartCalendar(final String startingDateStr, final Calendar today) throws ParseException {
		if(df.get() == null) df.set( new SimpleDateFormat("yyyyMMdd HH:mm:ss"));

		final Date d = df.get().parse(startingDateStr + " 00:00:00");
		Calendar startingDate = Calendar.getInstance();
		startingDate.setMinimalDaysInFirstWeek(4);
		today.setMinimalDaysInFirstWeek(4);
		startingDate.setTime(d);
		return startingDate;
	}
	/**
	 * @param today
	 * @param startingDate
	 * @return
	 */
	private static int getDayDelta(final Calendar today, Calendar startingDate) {
		int yearDelta = today.get(Calendar.YEAR) - startingDate.get(Calendar.YEAR);
		int dayDelta = today.get(Calendar.DAY_OF_YEAR) - startingDate.get(Calendar.DAY_OF_YEAR);
		int daysInYear = startingDate.getActualMaximum( Calendar.DAY_OF_YEAR);
		
		if( yearDelta == 1){
			dayDelta = dayDelta + daysInYear;
		}
		return dayDelta;
	}
}
