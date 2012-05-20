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
		String endDate = "20120130 16:45:00";
		DateFormat df = new SimpleDateFormat( "yyyyMMdd HH:mm:ss");
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
	public void testGetDatesStringCalendar() throws Exception{
		ArrayList<String> res = HistoricalDateManipulation.getDates( "20120101 01:01:01", today);
		Assert.assertEquals(5, res.size());

	}
	@Test
	public void testGetDatesYearBoundary() throws Exception {
		ArrayList<String> res = HistoricalDateManipulation.getDates( "20111230 01:01:01", today);
		for (Iterator iterator = res.iterator(); iterator.hasNext();) {
			String string = (String) iterator.next();
		}
		Assert.assertEquals(5, res.size());		
	}
	@Test
	public void testGetDatesMoreThan52() throws Exception{
		
		ArrayList<String> res = HistoricalDateManipulation.getDates( "20110130 01:01:01", today);

		Assert.assertEquals(53, res.size());		
	}
	@Test
	public void testGetDatesGoofyYear() throws Exception {
		String endDate = "20110101 16:45:00";
		DateFormat df = new SimpleDateFormat( "yyyyMMdd HH:mm:ss");
		Date d = df.parse( endDate );
		Calendar day = Calendar.getInstance();
		day.setTime( d);	
		ArrayList<String> res = HistoricalDateManipulation.getDates("20101228 01:01:01", day);
		Assert.assertEquals(1, res.size() );
	}

}
