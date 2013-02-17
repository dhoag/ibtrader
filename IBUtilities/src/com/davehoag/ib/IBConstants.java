package com.davehoag.ib;

public interface IBConstants {
	final int port = 7496;
	//Not sure what clientId means, but zero seems to support repeat connections
	final int clientId = (int) System.currentTimeMillis();
	final String host = "";
	
	//http://www.interactivebrokers.com/php/apiUsersGuide/apiguide/excel/historical_data_page_query_specification_fields.htm
	//Bar size
	final String bar5sec = "5 secs";
	final String bar1min = "1 min";
	final String bar5min = "5 mins";
	final String bar15min = "15 mins";
	final String bar1hour = "1 hour";
	final String bar1day = "1 day";
	final String dur1year = "1 Y";
	final String dur6mon = "6 M";
	final String dur1week = "1 W";
	final String dur1day = "1 D";
	final String dur30min = "1800 S";
	final String dur1hour = "3600 S";
	final String dur5min = "300 S";
	final int rthOnly = 1;
	final int allData = 0;
	final String showTrades = "TRADES";
	final String showMidpoint = "MIDPOINT";
	final String showBids = "BID";
	final String showAsk = "ASK";
	final String showBidAsk = "BID_ASK";
	final String showHistoricVol = "HISTORICAL_VOLATILITY";
	final String showOptionImpliedVol = "OPTION_IMPLIED_VOLATILITY";
	final int datesAsNumbers = 2;
	final int datesAsStrings = 1;

}
