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
			double [][] indepVars = independentVars(indepList);
			double [] depVars = depdentVars(depList);
			indepList.clear(); depList.clear(); 
			Regression reg = runRegression(indepVars, depVars);
			System.out.println("Independent Vars " + indepVars.length + " of data " + depVars.length);
			System.out.println("Model Strength: " + reg.getCoefficientOfDetermination());
			double [] pValues = reg.getPvalues();
			System.out.print("Constant " + pValues[0]);
			for(int j = 1; j < indepVars.length; j++){
				System.out.print(" [" + j + "] " + pValues[j] );
			}
			System.out.println();
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
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
		int numParms = 4;
		final Bar PLACEHOLDER = new Bar();
		while(data.hasNext()) {
			final Bar aBar = data.next();
			cache.push(aBar);
			final long openTime = HistoricalDateManipulation.getOpen(aBar.originalTime);
			final long closeTime = HistoricalDateManipulation.getClose(aBar.originalTime);
			//skip the first and last 5 minutes of the trading day in my regressions
			if(aBar.originalTime > (openTime + (5*12*5)) && aBar.originalTime < (closeTime - (5*12*5))){
				bars.add(aBar);
				double [] indies = new double[numParms];
				indepList.add(indies);
				//set some vals
				double retracementPrice = cache.getFibonacciRetracement(30, 38.2);
				indies[0] = retracementPrice != 0 ? (aBar.low - retracementPrice) / retracementPrice : 0;
				indies[1] = (cache.getMA(12, 'w') - cache.getMA(23, 'w'))/cache.getMA(23, 'w');
				indies[2] = cache.isTrendingUp(13) ? 1 : 0;
				indies[3] = cache.isTrendingUp(23) ? 1 : 0;
			}
			else {
				bars.add(PLACEHOLDER);
			}
		}
		for(final Bar headBar : bars) { 
			if(headBar != PLACEHOLDER){
				//Need to only look forward far enough to where we stopped (5 minutes back)
				final double priceAction = cache.getFutureTrend(headBar, 12*5 );
				depList.add(priceAction);
			}
		}
		if(indepList.size()!= depList.size()) throw new IllegalStateException("Failed to create dependent results for all bars! " + depList.size() + ":" + indepList.size());
		
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
		
		double [] weightingErrors = new double [dependentVar.length];
		for(int k = 0; k < weightingErrors.length; k++) weightingErrors[k] = 1;
		
		Regression reg = new Regression(indepVars, dependentVar, weightingErrors);
		reg.linear();
		return reg;
	}

}
