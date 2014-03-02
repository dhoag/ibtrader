package com.davehoag.ib.tools;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.LinkedList;

import com.davehoag.ib.CassandraDao;
import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.dataTypes.BarCache;
import com.davehoag.ib.dataTypes.BarIterator;
import com.davehoag.ib.util.HistoricalDateManipulation;

import flanagan.analysis.Regression;
import flanagan.analysis.Stat;
public class FindModel {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		DateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
		int i= 0;
		final String startTime = args[i++];
		final String endTime = args[i++];
		try 
		{
			final String symbol = args[i++];
			long start = df.parse(startTime).getTime()/1000;
			long finish = df.parse(endTime).getTime()/1000;
						
			final LinkedList<Double > depList = new LinkedList<Double >();
			final LinkedList<double []> indepList = new LinkedList<double []>();
			populateRegressionModelData(symbol, start, finish, depList, indepList);
			final double [][] indepVars = independentVars(indepList);
			final double [] depVars = depdentVars(depList);
			System.out.println("Clearing lists");
			indepList.clear(); depList.clear(); 
			final Regression reg = runRegression(indepVars, depVars);
			printStats(indepVars, depVars, reg);
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
	}
	/**
	 * @param indepVars
	 * @param depVars
	 * @param reg
	 */
	protected static void printStats(double[][] indepVars, double[] depVars, Regression reg) {
		System.out.println("Independent Vars " + indepVars.length + " of data " + depVars.length);
		for(int i = 0; i < indepVars.length; i++){
			double [] col = indepVars[i];
			System.out.println("### Col ### " + i);
			System.out.println("Mean: " + Stat.mean(col));
			System.out.println("StdDev: " + Stat.standardDeviation(col));
			System.out.println("CoefVar: " + Stat.coefficientOfVariation(col));
		}
		System.out.println("Model Strength: " + reg.getCoefficientOfDetermination());
		final double [] pValues = reg.getPvalues();
		System.out.print("Constant " + pValues[0]);
		for(int j = 1; j < indepVars.length; j++){
			System.out.print(" [" + j + "] " + pValues[j] );
		}
		System.out.println();
	}
	/**
	 * @param symbol
	 * @param start
	 * @param finish
	 * @param depList Fill this with the regression model dependent vars
	 * @param indepList Fill this with the independent variables of the model
	 * @return
	 */
	protected static void populateRegressionModelData(final String symbol, long start, long finish,
			final LinkedList<Double> depList, final LinkedList<double[]> indepList) {
		final String barSize = "bar5sec";
		final BarIterator data = CassandraDao.getInstance().getData(symbol, start, finish, barSize);
		final BarCache cache = new BarCache(12*60*24*90);
		final LinkedList<Bar> bars = new LinkedList<Bar >();

		final Bar PLACEHOLDER = new Bar();
		System.out.println("Populating historical trade data");
		while(data.hasNext()) {
			final Bar aBar = data.next();
			cache.push(aBar);
			final long openTime = HistoricalDateManipulation.getOpen(aBar.originalTime);
			final long closeTime = HistoricalDateManipulation.getClose(aBar.originalTime);
			//skip the first and last 5 minutes of the trading day in my regressions
			if(aBar.volume == 0 || (aBar.originalTime > (openTime + (5*12*5)) && aBar.originalTime < (closeTime - (5*12*5)))){
				final double[] indies = getIndepentVariables(cache, aBar, openTime);
				final boolean valid = containsOnlyValidData(cache, indies);
				if( valid ){
					bars.add(aBar);
					indepList.add(indies);
				}
				else {
					bars.add(PLACEHOLDER);
				}
			}
			else {
				bars.add(PLACEHOLDER);
			}
		}
		final double oneTenth = bars.size() / 10;
		System.out.println("Populating future trend data " + bars.size() + " " + oneTenth);
		int count = 0;
		for(final Bar headBar : bars) { 
			if(headBar != PLACEHOLDER){
				//Need to only look forward far enough to where we stopped (5 minutes back)
				final double priceAction = cache.getFutureTrend(headBar, 12*5 );
				depList.add(priceAction);
				if((count++ % oneTenth) == 0){ System.out.println( count ); }
			}
		}
		cache.dumpStats();
		if(indepList.size()!= depList.size()) throw new IllegalStateException("Failed to create dependent results for all bars! " + depList.size() + ":" + indepList.size());
		
	}
	/**
	 * @param cache
	 * @param indies
	 * @param pullRecord
	 * @return
	 */
	protected static boolean containsOnlyValidData(final BarCache cache, final double[] indies) {
		boolean valid = true;
		for(double d : indies){
			if( Double.isNaN(d)|| Double.isInfinite(d)) {
				valid = false;
//				for(int c = 0; c < indies.length; c++) System.out.println("[" + c + "] " + indies[c]);
//				System.out.println( "MA tradecounts " + cache.getMA(5,'t') + " " + cache.getMA(30,'t'));   
			}
		}
		return valid;
	}
	/**
	 * @param cache
	 * @param aBar
	 * @param openTime
	 * @return
	 */
	protected static double[] getIndepentVariables(final BarCache cache, final Bar aBar, final long openTime) {
		final int numParms = 8;
		double [] indies = new double[numParms];
		//set some vals
		double retracementPrice = cache.getFibonacciRetracement(30, 38.2);
		indies[5] = retracementPrice != 0 ? 100-((aBar.wap - retracementPrice)*100 / retracementPrice) : 0;
		indies[0] = 100*cache.getMA(5, 'v') / cache.getMA(30, 'v');
		indies[7] = 100*cache.getMA(5,'t') / cache.getMA(30, 't');
		indies[1] = aBar.originalTime < (openTime + 60*60) ? 1 : 0;
		indies[2] = cache.isInflection(13, 20, 'w', true) ? 1 : 0;
		indies[3] = cache.isInflection(13, 20, 'w', false) ? 1  : 0;
		indies[6] = cache.getSlope(15, 'l');
		//There are 12 5 second bars in a minute and look at the prior 10 minutes
		final int minutesBack = 20;
		final long startTime = Math.max( aBar.originalTime - 5*12*minutesBack, openTime);
		indies[4] = cache.getFutureTrend(startTime, 5);
		return indies;
	}
	static double [] depdentVars(final LinkedList<Double> depList){
		final double[] dependentVar = new double[depList.size()];
		final Iterator<Double> itr = depList.iterator();
		int j = 0;
		while(itr.hasNext()){ dependentVar[j++] = itr.next(); };
		return dependentVar;
	}
	static double [][] independentVars(final LinkedList<double[]> indepList){
		double [] firstRow = indepList.peek();
		double [][] independentVars = new double[firstRow.length][indepList.size()];
		int j = 0;
		for(double [] row : indepList){
			for(int i = 0; i < firstRow.length; i++)
				independentVars[i][j] = row[i];
			j++;
		}
		return independentVars;
	}

	/**
	 * @param depList
	 * @param indepList
	 * @return
	 */
	protected static Regression runRegression(final double [][] indepVars, 
			final double [] dependentVar) {
		System.out.println("Starting regression");
		double [] weightingErrors = new double [dependentVar.length];
		for(int k = 0; k < weightingErrors.length; k++) weightingErrors[k] = 1;
		
		Regression reg = new Regression(indepVars, dependentVar, weightingErrors);
		reg.linear();
		return reg;
	}

}
