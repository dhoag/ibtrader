package com.davehoag.ib.tools;

import java.util.ArrayList;

import flanagan.analysis.Regression;
import flanagan.io.FileChooser;

/**
 * 
 * @author David Hoag
 *
 */
public class RegressionAnalysis {
	//Each row of data is a new independent variable
	double [][] independentVars;
	double [] dependentVars;
	ArrayList<String> labels; 

	/**
	 * Performs a linear regression with intercept analysis on a file. 
	 * Currently assumes a csv file with a header row of labels
	 * followed by the data. The last column is assumed to be the dependent variable and the rest are
	 * the independent variable.
	 * @param args
	 */
	public static void main(String[] args) {
		final FileChooser chooser = new FileChooser();
		chooser.selectFile();
	
		ArrayList<String> labels = getLabels(chooser.readLine());
		
		DoubleReader rdr = new DoubleReader(){ 
			@Override
			public double readDouble(){ 
				return chooser.readDouble(); 
			}
		};
		RegressionAnalysis regressionEngine = new RegressionAnalysis();
		regressionEngine.labels = labels;
		regressionEngine.regress(rdr, chooser.numberOfLines() -1);
	}

	/**
	 * @param chooser
	 * @return
	 */
	protected static ArrayList<String> getLabels(final String line) {
		System.out.println(line);
		ArrayList<String> labels = readVariables(line);
		for(String val : labels){
			System.out.print("'" + val + "',");
		}
		System.out.println();
		return labels;
	}

	protected void regress(final DoubleReader rdr, final int dataSetSize) {
		
		System.out.println("Reading " + dataSetSize + " records");
		final int variableCount = labels.size();
		
		independentVars = new double [variableCount-1][dataSetSize];
		dependentVars = new double [dataSetSize];

			
		populateDataArrays(independentVars, dependentVars, rdr);
		analyze(independentVars, dependentVars);
		
	}

	/**
	 * @param independentVars
	 * @param dependentVars
	 * @param rdr
	 */
	public static void populateDataArrays(final double[][] independentVars, final double[] dependentVars,
			final DoubleReader rdr) {
		int row = 0;
		int col = 0;
		while(col < dependentVars.length){
			double value = rdr.readDouble();
			System.out.println("["+row+"]["+col+"] " + value);
			
			if(row == independentVars.length ){
				dependentVars[col++] = value;
				row = 0;
			}
			else{
				independentVars[row++][col]= value;
			}
		}
	}
	

	/**
	 * Each row of data is a new independent variable. 
	 * The # of columns per row must match the number of dependent vars.
	 * 
	 * @param independentVars
	 * @param dependentVars
	 */
	protected static void analyze(double[][] independentVars, double[] dependentVars) {
		Regression reg = new Regression(independentVars, dependentVars);
		//reg.linear();
		reg.linearPlot();
		//reg.linearGeneralPlot();
		

		System.out.println(" How good is the model at explaining the dependent var values");
		System.out.println(reg.getAdjustedCoefficientOfDetermination());
		System.out.println("F Ratio Prob " + reg.getCoeffDeterminationFratioProb());
		System.out.println(" The coeff is the derived impact (weight) for the prediction model on each var"); 
		
		for(double d : reg.getCoeff()){
			System.out.print(d + " " );
		}
		System.out.println();

		System.out.println("How likely it is that the coeff for the independentVariable should be zero (null hypothesis, this variable is no impact on the dependent var");
		System.out.println(reg.getPvalues().length + " ");
		for(double d : reg.getPvalues()){
			System.out.print(d + " ");
		}
		System.out.println();
		double [] resid = reg.getResiduals();
		for(int i = 0; i < dependentVars.length; i++){
			System.out.println("Actual: " + dependentVars[i] + " Model: " + resid[i]);
		}
	}

	protected static ArrayList<String> readVariables(final String line) {
		ArrayList<String> result = new ArrayList<String>();
		if (line == null) { return result; }
		int tmpIndex, currentIndex = 0;
		String newString;
		char ch = ',';

		tmpIndex = line.indexOf(ch);
		while (tmpIndex >= 0) {
			newString = line.substring(currentIndex, tmpIndex).trim();
			result.add(newString);
			currentIndex = tmpIndex + 1;
			tmpIndex = line.indexOf(ch, currentIndex);
		}
		;
		newString = line.substring(currentIndex).trim();
		result.add(newString);
		return result;
	}

}
