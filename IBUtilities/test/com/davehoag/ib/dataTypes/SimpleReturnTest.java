package com.davehoag.ib.dataTypes;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.util.Calendar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.davehoag.ib.util.HistoricalDateManipulation;

public class SimpleReturnTest {
	SimpleReturn sr;
	@Before
	public void setUp() throws Exception {
		
	}

	@After
	public void tearDown() throws Exception {
	}
	void setSmallDataSet(){
		sr = new SimpleReturn(5);
		Bar aBar = new Bar();
		aBar.open = 10; aBar.close = 100; aBar.high = 120; aBar.low = 5;
		sr.newBar(aBar);

		aBar = new Bar();
		aBar.open = 10; aBar.close = 100; aBar.high = 120; aBar.low = 5;
		sr.newBar(aBar);
		
		aBar = new Bar();
		aBar.open = 10; aBar.close = 100; aBar.high = 120; aBar.low = 5;
		sr.newBar(aBar);
		
		aBar = new Bar();
		aBar.open = 10; aBar.close = 100; aBar.high = 120; aBar.low = 5;
		sr.newBar(aBar);
		
		aBar = new Bar();
		aBar.open = 10; aBar.close = 100; aBar.high = 120; aBar.low = 5;
		sr.newBar(aBar);

		aBar = new Bar();
		aBar.open = 10; aBar.close = 100; aBar.high = 120; aBar.low = 5;
		sr.newBar(aBar);

		aBar = new Bar();
		aBar.open = 10; aBar.close = 100; aBar.high = 120; aBar.low = 5;
		sr.newBar(aBar);
	}
	@Test
	public void testIteration() {
		setSmallDataSet();
		int count=0;
		for(Bar bar: sr){
			count++;
		}
		assertEquals(5, count);
	}

	@Test
	public void testGetTargetDay() throws ParseException{
		sr = new SimpleReturn(10);
		Bar aBar = new Bar();
		aBar.open = 10; aBar.close = 100; aBar.high = 120; aBar.low = 5;
		aBar.originalTime = HistoricalDateManipulation.getTime("20120404 08:30:00");
		int res = sr.getTargetDay(aBar, 1);
		assertEquals(2, res);
		res = sr.getTargetDay(aBar, 7);
		assertEquals(6, res);
		aBar.originalTime = HistoricalDateManipulation.getTime("20110404 08:30:00");
		res = sr.getTargetDay(aBar, 31);
		assertEquals(29, res);
	}
	@Test
	public void testDayOfWeek() throws ParseException{
		sr = new SimpleReturn(10);
		Bar aBar = new Bar();
		aBar.open = 10; aBar.close = 100; aBar.high = 120; aBar.low = 5;
		aBar.originalTime = HistoricalDateManipulation.getTime("20120910 08:30:00");
		sr.newBar(aBar);

		aBar = new Bar();
		aBar.open = 10; aBar.close = 100; aBar.high = 120; aBar.low = 5;
		aBar.originalTime = HistoricalDateManipulation.getTime("20120911 08:30:00");
		sr.newBar(aBar);
		
		aBar = new Bar();
		aBar.open = 10; aBar.close = 12; aBar.high = 120; aBar.low = 5;
		aBar.originalTime = HistoricalDateManipulation.getTime("20120912 08:30:00");
		sr.newBar(aBar);
		
		aBar = new Bar();
		aBar.open = 10; aBar.close = 100; aBar.high = 120; aBar.low = 5;
		aBar.originalTime = HistoricalDateManipulation.getTime("20120913 08:30:00");
		sr.newBar(aBar);
		
		aBar = new Bar();
		aBar.open = 10; aBar.close = 100; aBar.high = 120; aBar.low = 5;
		aBar.originalTime = HistoricalDateManipulation.getTime("20120914 08:30:00");
		sr.newBar(aBar);

		aBar = new Bar();
		aBar.open = 10; aBar.close = 100; aBar.high = 120; aBar.low = 5;
		aBar.originalTime = HistoricalDateManipulation.getTime("20120917 08:30:00");
		sr.newBar(aBar);

		aBar = new Bar();
		aBar.open = 10; aBar.close = 100; aBar.high = 120; aBar.low = 5;
		aBar.originalTime = HistoricalDateManipulation.getTime("20120918 08:30:00");
		sr.newBar(aBar);

		aBar = new Bar();
		aBar.open = 10; aBar.close = 19; aBar.high = 120; aBar.low = 5;
		aBar.originalTime = HistoricalDateManipulation.getTime("20120919 08:30:00");
		sr.newBar(aBar);
		aBar = new Bar();
		aBar.open = 10; aBar.close = 100; aBar.high = 120; aBar.low = 5;
		aBar.originalTime = HistoricalDateManipulation.getTime("20120920 08:30:00");
		sr.newBar(aBar);
		aBar = new Bar();
		aBar.open = 10; aBar.close = 100; aBar.high = 120; aBar.low = 5;
		aBar.originalTime = HistoricalDateManipulation.getTime("20120921 08:30:00");
		sr.newBar(aBar);
		
		Bar [] data = sr.getDayOfWeekBars(Calendar.WEDNESDAY);
		//using the # in the "close" field to represent the numeric days
		assertEquals(data[0].close, 12, .001);
		assertEquals(data[1].close, 19, .001);
	}
	@Test
	public void testDayOfMonth() throws ParseException{
		sr = new SimpleReturn(10);
		Bar aBar = new Bar();
		aBar.open = 10; aBar.close = 100; aBar.high = 120; aBar.low = 5;
		aBar.originalTime = HistoricalDateManipulation.getTime("20120910 08:30:00");
		sr.newBar(aBar);

		aBar = new Bar();
		aBar.open = 10; aBar.close = 14; aBar.high = 120; aBar.low = 5;
		aBar.originalTime = HistoricalDateManipulation.getTime("20120914 08:30:00");
		sr.newBar(aBar);
		
		aBar = new Bar();
		aBar.open = 10; aBar.close = 19; aBar.high = 120; aBar.low = 5;
		aBar.originalTime = HistoricalDateManipulation.getTime("20120919 08:30:00");
		sr.newBar(aBar);
		
		aBar = new Bar();
		aBar.open = 10; aBar.close = 12; aBar.high = 120; aBar.low = 5;
		aBar.originalTime = HistoricalDateManipulation.getTime("20121012 08:30:00");
		sr.newBar(aBar);
		
		aBar = new Bar();
		aBar.open = 10; aBar.close = 15; aBar.high = 120; aBar.low = 5;
		aBar.originalTime = HistoricalDateManipulation.getTime("20121015 08:30:00");
		sr.newBar(aBar);

		aBar = new Bar();
		aBar.open = 10; aBar.close = 17; aBar.high = 120; aBar.low = 5;
		aBar.originalTime = HistoricalDateManipulation.getTime("20121017 08:30:00");
		sr.newBar(aBar);

		aBar = new Bar();
		aBar.open = 10; aBar.close = 100; aBar.high = 120; aBar.low = 5;
		aBar.originalTime = HistoricalDateManipulation.getTime("20121103 08:30:00");
		sr.newBar(aBar);

		aBar = new Bar();
		aBar.open = 10; aBar.close = 19; aBar.high = 120; aBar.low = 5;
		aBar.originalTime = HistoricalDateManipulation.getTime("20121104 08:30:00");
		sr.newBar(aBar);
		aBar = new Bar();
		aBar.open = 10; aBar.close = 100; aBar.high = 120; aBar.low = 5;
		aBar.originalTime = HistoricalDateManipulation.getTime("20121105 08:30:00");
		sr.newBar(aBar);
		aBar = new Bar();
		aBar.open = 10; aBar.close = 100; aBar.high = 120; aBar.low = 5;
		aBar.originalTime = HistoricalDateManipulation.getTime("20121106 08:30:00");
		sr.newBar(aBar);
		
		Bar [] data = sr.getDayOfMonthBars(14);
		//using the # in the "close" field to represent the numeric days
		assertEquals(14,data[0].close,  .001);
		assertEquals(12,data[1].close,  .001);
	}

}
