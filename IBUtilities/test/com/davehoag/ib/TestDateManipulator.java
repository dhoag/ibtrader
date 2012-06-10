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
	public void testGetDatesStringCalendar() throws Exception{
		ArrayList<String> res = HistoricalDateManipulation.getDates( "20120101", today);
		Assert.assertEquals(300, res.size());

	}
	@Test
	public void testGetDatesYearBoundary() throws Exception {
		ArrayList<String> res = HistoricalDateManipulation.getDates( "20111230", today);
		for (Iterator iterator = res.iterator(); iterator.hasNext();) {
			String string = (String) iterator.next();
		}
		Assert.assertEquals(320, res.size());		
	}
	@Test
	public void testGetDatesMoreThan52() throws Exception{
		
		ArrayList<String> res = HistoricalDateManipulation.getDates( "20110130", today);

		Assert.assertEquals(3660, res.size());		
	}
	@Test
	public void testGetDatesGoofyYear() throws Exception {
		String endDate = "20110101";
		DateFormat df = new SimpleDateFormat( "yyyyMMdd");
		Date d = df.parse( endDate );
		Calendar day = Calendar.getInstance();
		day.setTime( d);	
		ArrayList<String> res = HistoricalDateManipulation.getDates("20101228", day);
		for(String val: res){
			System.out.println(val);
		}
		Assert.assertEquals(50, res.size() );
	}

}
