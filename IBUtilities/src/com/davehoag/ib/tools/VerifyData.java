package com.davehoag.ib.tools;

import java.util.ArrayList;
import com.davehoag.ib.StoreHistoricalData;

public class VerifyData {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String barSize = args[0];
		for(int i = 1; i < args.length; i++)
		try{
			System.out.println("Checking " + barSize + " for " + args[i]);
			final StoreHistoricalData sh = new StoreHistoricalData(args[i], null);		
			sh.setBarSize(barSize);
			ArrayList<String> missingDates = sh.getDates("20111101");
			for(String data : missingDates){
				System.out.println(data);
			}
		}
		catch(Exception ex){
			System.err.println("Problem with " + args[i] + " " + ex);
		}
	}

}
