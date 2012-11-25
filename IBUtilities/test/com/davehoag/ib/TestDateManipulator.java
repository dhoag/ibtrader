package com.davehoag.ib;

import static org.junit.Assert.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.davehoag.ib.dataTypes.StockContract;
import com.davehoag.ib.util.HistoricalDateManipulation;

public class TestDateManipulator {
	Calendar today;
	@Before
	public void setUp() throws Exception {
		String endDate = "20120130";
		DateFormat df = new SimpleDateFormat( "yyyyMMdd");
		Date d = df.parse( endDate );
		today = Calendar.getInstance();
		today.setTime( d);		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testReqHisData() throws ParseException {
		StockContract stock = new StockContract("IBM");
	//	rh.reqHisData("20120501 01:01:01", stock, 12);
	}
	@Test
	public void testGetDatesStringCalendarHours() throws Exception{
		ArrayList<String> res = HistoricalDateManipulation.getDatesBrokenIntoHours( "20120127", today);
		Assert.assertEquals(8*2, res.size());
	}
	@Test
	public void testGetDatesYearBoundaryHours() throws Exception {
		ArrayList<String> res = HistoricalDateManipulation.getDatesBrokenIntoHours( "20111230", today);
		Assert.assertEquals(176, res.size());		
	}
	@Test
	public void testGetDatesMoreThan52Hours() throws Exception{
		
		ArrayList<String> res = HistoricalDateManipulation.getDatesBrokenIntoHours( "20110130", today);

		Assert.assertEquals(2088, res.size());		
	}
	@Test
	public void testGetDatesGoofyYearHours() throws Exception {
		String endDate = "20110101";
		DateFormat df = new SimpleDateFormat( "yyyyMMdd");
		Date d = df.parse( endDate );
		Calendar day = Calendar.getInstance();
		day.setTime( d);	
		ArrayList<String> res = HistoricalDateManipulation.getDatesBrokenIntoHours("20101228", day);
		Assert.assertEquals(8*4, res.size() );
	}
	@Test
	public void testGetDatesStringCalendar() throws Exception{
		ArrayList<String> res = HistoricalDateManipulation.getDatesBrokenIntoWeeks( "20120101", today);
		Assert.assertEquals(5, res.size());

	}
	@Test
	public void testGetDatesYearBoundary() throws Exception {
		ArrayList<String> res = HistoricalDateManipulation.getDatesBrokenIntoWeeks( "20111230", today);

		Assert.assertEquals(5, res.size());		
	}
	@Test
	public void testGetDatesMoreThan52() throws Exception{
		
		ArrayList<String> res = HistoricalDateManipulation.getDatesBrokenIntoWeeks("20110130", today);
		Assert.assertEquals(53, res.size());		
	}
	@Test
	public void testGetDatesGoofyYear() throws Exception {
		String endDate = "20110101";
		DateFormat df = new SimpleDateFormat( "yyyyMMdd");
		Date d = df.parse( endDate );
		Calendar day = Calendar.getInstance();
		day.setTime( d);	
		ArrayList<String> res = HistoricalDateManipulation.getDatesBrokenIntoWeeks("20101228", day);
		Assert.assertEquals(1, res.size() );
	}
	@Test
	public void testGetWeekDays() throws Exception {
		String endDate = "20110101";
		DateFormat df = new SimpleDateFormat( "yyyyMMdd");
		Date d = df.parse( endDate );
		Calendar day = Calendar.getInstance();
		day.setTime( d);	
		ArrayList<String> res = HistoricalDateManipulation.getWeekDays("20101228", day);

		Assert.assertEquals(4, res.size() );
	}
}
