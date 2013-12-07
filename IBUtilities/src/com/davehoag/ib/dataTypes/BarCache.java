package com.davehoag.ib.dataTypes;

import java.util.Iterator;

import com.davehoag.analytics.DoubleReader;
import com.davehoag.analytics.Functions;
import com.davehoag.ib.util.HistoricalDateManipulation;

import flanagan.analysis.Stat;

public class BarCache {
	Bar[] localCache = new Bar[5 * (60 / 5) * 60 * 8];
	int lastIdx = 0;
	transient int cachedIndexOf = 0;
	boolean wrapped = false;
	double stdDevFactor = 1;
	int hitCount = 0;
	int missCount = 0;

	public BarCache() {
	}
	public BarCache(int i) {
		localCache = new Bar[i];
	}

	public void dumpStats(){
		System.out.println("Hit count " + hitCount);
		System.out.println("Miss count " + missCount);
	}
	public int size(){
		return wrapped ? localCache.length : lastIdx;
	}
	/**
	 * Is the close of the most recent bar above the close of the oldest bar and
	 * there are more than 1/2 closes above the prior bars's close
	 * 
	 * @param periods
	 * @return
	 */
	public boolean isTrendingUp(final int periods) {
		final Bar[] bars = getBars(periods);
		int upCount = 0;
		double last = -1;
		for (Bar aBar : bars) {
			if (last != -1) {
				if (last > aBar.close) {
					upCount++;
				}
			}
			last = aBar.close;
		}
		return (bars[0].close > bars[bars.length - 1].close && upCount >= (bars.length / 2));
	}
	/**
	 * Method that kinds of works like an array. The zero index bar is the most recent, 
	 * 1 is 2nd to most recent, etc...
	 * 
	 * @param idx
	 * @return Bar
	 */
	public Bar get(final int idx){
		validateIndex(idx );
		final int actualIdx = lastIdx - idx - 1;
		if(actualIdx >= 0) return localCache[actualIdx];
		return localCache[ localCache.length + actualIdx ];
		
	}

	/**
	 * The main Fibonacci retracement levels. To be valid we need a "high" that is the
	 * most recent bar or higher than the most recent and a low that is in the same
	 * bar as the high or older.  Additional Fibonacci retracement 
	 * levels include 76.4 percent and 23.6 percent
	 * 
	 * @param periods
	 * @param percent —38.2 percent, 50 percent and 61.8 percent.
	 * @return price that represents the retracement point or zero if it can't be determined
	 */
	public double getFibonacciRetracement(final int periods, final double percent){
		return getFibonacciRetracement(0, periods, percent);
	}
	public double getFibonacciRetracement(final int start, final int periods, final double percent){
		Bar b = get(start);
		double high = b.high; double low = b.low;
		double candidateHigh = 0;
		boolean valid = false;
		for(int i = start+1; i < start+periods; i++){
			final Bar aBar = get(i);
			if(aBar == null) break;
			if(high < aBar.high) { 
				if(valid) { //had a good high low sequence but found older high
					candidateHigh = aBar.high;
				}
				else {
					high = aBar.high;
				}
			}
			if(low > aBar.low) {
				low = aBar.low;
				valid = true;
				//Found a new low after the candidate high
				if(candidateHigh != 0){
					high = candidateHigh;
					candidateHigh = 0;
				}
			}
		}
		if(!valid) return 0;
		return high - ( (high - low) * percent);
	}
	public double [] getParabolicSar(final int start, final double [] result, final double accelFact){
		// Initialize trend to whatever
		boolean upTrend = false;
		final double accelFactIncrement = accelFact == 0 ? .02 : accelFact;
		int periods = result.length;
		Bar aBar = get(periods -1 + start);
		 
		// Previous SAR: Use first data point's extreme value, depending on trend
		double pSar = aBar.high;
		double extremePoint = aBar.low;
		result[0]= pSar;
		result[1]= pSar;
		return getParabolicSar(start, upTrend, accelFactIncrement, accelFactIncrement, periods -2 , pSar, extremePoint, result);
	}
	public double [] getParabolicSar(final int start, final int periods, final double accelFact){
		return getParabolicSar(start,  new double [periods], accelFact);
	}
	public double [] getParabolicSar(final int periods, final double accelFact){
		return getParabolicSar(0, new double [periods], accelFact);
	}
	/**
	 * Return "tomorrow's" sar value.
	 * 
	 * @param upTrend
	 * @param accelerationFactor
	 * @param accelFactIncrement
	 * @param barIdx
	 * @param pSar
	 * @param extremePoint
	 * @return
	 */
	protected double [] getParabolicSar(int start, boolean upTrend, double accelerationFactor, final double accelFactIncrement, final int barIdx, final double pSar, double extremePoint, double [] result){
		double nSar = 0;
		final Bar todaysBar = get(barIdx+start);
		
		if(upTrend){
			// Making higher highs: accelerate
			if(todaysBar.high > extremePoint){
				accelerationFactor = Math.min(accelerationFactor + accelFactIncrement, .2);
				extremePoint = todaysBar.high;
			}

			//check for reversal
			if(todaysBar.low < pSar){
				upTrend = false;
				nSar = extremePoint;
				extremePoint = todaysBar.low;
				accelerationFactor = accelFactIncrement;
			}
			else { 
				//Calculate tomorrow's SAR
				nSar = pSar + accelerationFactor * (extremePoint - pSar); 

				//The nSar can not be higher than the current low (or the prior low)
				final double maxSar = Math.min(todaysBar.low, get(barIdx + 1+start).low);
				nSar = Math.min(maxSar, nSar);
			}
		} 
		else {
			// Making lower lows: accelerate
			if(todaysBar.low < extremePoint){
				accelerationFactor = Math.min(accelerationFactor + accelFactIncrement, .2);
				extremePoint = todaysBar.low;
			}

			//check for reversal
			if(todaysBar.high > pSar){
				upTrend = true;
				nSar = extremePoint;
				extremePoint = todaysBar.high;
				accelerationFactor = accelFactIncrement;
			}
			else { 
				//Calculate tomorrow's SAR
				nSar = pSar + accelerationFactor * (extremePoint - pSar); 

				//The nSar can not be lower than the current high (or the prior high)
				final double minSar = Math.max(todaysBar.high, get(barIdx + 1 + start).high);
				nSar = Math.max(minSar, nSar);
			}		
		}
		result[result.length - barIdx-start] = nSar;
		if(barIdx ==  1) return result;
		return getParabolicSar(start, upTrend, accelerationFactor, accelFactIncrement, barIdx - 1 , nSar, extremePoint, result);
	}
	/**
	 * A numeric value representing the future price action after this bar
	 * @param b
	 * @param periods
	 * @return
	 */
	public int getFutureTrend(final Bar b, final int periods){
		final int idx = indexOf(b.originalTime);
		if(get(idx) != b) throw new IllegalArgumentException("Bar not found in cache" + b);
		return getFutureTrend(b.originalTime, periods);
	}
	public int getFutureTrend(final long origTime){
		return getFutureTrend(origTime, indexOf(origTime));
	}
	public double getStdDev(final int start, final int periods, final char field){
		Iterable<DoubleReader> rdr = getIteratable(start, periods, field);
		return Functions.getStdDev(rdr);

	}
	/**
	 * Close to standard deviation, but different in several ways
	 * Use the VWAP for the mean calculation
	 * And use four points for each bar (OHLC)
	 * 
	 * @param start
	 * @param periods
	 * @return
	 */
	public double getSwingDeviation(final int start, final int periods){
		double ma = getMA(start, periods, 'w');
		double squaredSum = 0;
		int i = 0;
		for( Bar aBar : getIteratable(start, periods)){
			squaredSum += Math.pow(aBar.open - ma, 2);
			squaredSum += Math.pow(aBar.high - ma, 2);
			squaredSum += Math.pow(aBar.low - ma, 2);
			squaredSum += Math.pow(aBar.close - ma, 2);
		}
		double devMean = squaredSum / (periods * 4);
		return Math.sqrt(devMean);
	}
	/**
	 * Look at the open print for the origTime and look forward N periods. Grab the 
	 * WAP for that day as that is the most likely, high liquidity exit point 
	 * 
	 * @param origTime
	 * @param periods
	 * @return % change *100 * 10
	 */
	public int getFutureTrend(final long origTime, final int periods){
		final int idx = indexOf(origTime);
		if(idx < periods) throw new IllegalArgumentException("Not enough future periods for " + periods + " but only have " + idx + " after this bar");
		float resultTotal = 0;
		//smaller index values are in the future. The 'get' counts backward
		resultTotal = (float)((get(idx-periods).wap - get(idx).open ) / get(idx).open);
		//resultTotal = resultTotal / (getStdDev(idx-periods, periods, 'c') );
		return Math.round(resultTotal*100*10) ;
	}
	public double getSlope(final int periods, final char field ){
		Bar old = get(periods);
		Bar recent = get(0);
		return (old.getField(field) - recent.getField(field))/ periods;
	}
	public double getDelta(final int fast, final int slow, final char field){
		double fastMa = getMA(fast, field);
		double slowMa = getMA(slow, field);
		return fastMa - slowMa;
	}
	/**
	 * Do have have a MACD cross over event
	 * @param fast
	 * @param slow
	 * @param field
	 * @param up
	 * @return
	 */
	public boolean isInflection(final int fast, final int slow, final char field, final boolean up){
		double fastPrice = getMA(fast, field);
		double priorFastPrice = getMA(1, fast, field);
		double slowPrice = getMA(slow, field);
		double priorSlowPrice = getMA(1, slow, field);
		if(up)
			return (fastPrice > slowPrice && priorFastPrice < priorSlowPrice);
		else
			return (fastPrice < slowPrice && priorFastPrice > priorSlowPrice);
	}
	/**
	 * Get the moving average of data for the provided field. 
	 * 'w' Volume Weighted Price
	 * 'v' Volume
	 * 'c' Close
	 * 'o' Open
	 * 'h' High
	 * 'l' Low
	 * 't' Trade Count 
	 * @param periods
	 * @param field The price bar field to use in the calculation
	 * @return
	 */
	public double getMA(final int periods, final char field) {
		return getMA(0, periods, field);
	}
	/**
	 * @param start
	 * @param periods
	 * @param field
	 * @return
	 */
	public double getMA(final int start, final int periods, final char field) {
		Iterable<DoubleReader> rdr = getIteratable(start, periods, field);
		return Functions.getMA(rdr);
	}
	/**
	 * Calculate Accumulation/Distribution with the option to use Volume Weighted Average
	 *  price for the period instead of close; 
	 * 
	 * @param start
	 * @param periods
	 * @return
	 */
	public double getADL(final int start, final int periods, final boolean vwap){
		int oldestIdx = start+periods;
		if (oldestIdx >= size()) {
			return 0;
		}
		
		double adl = 0;
		for(int i = oldestIdx-1; i >= start ;i--){
			final Bar nextBar = get(i);
			double anchorPrice = vwap ? nextBar.wap : nextBar.close;
			double moneyFlowMult = ((anchorPrice - nextBar.low) - (nextBar.high - anchorPrice))/(nextBar.high - nextBar.low);
			double mfVol = moneyFlowMult * nextBar.volume;
			adl += mfVol;
		}
		return adl;
	}
	/**
	 * 
	 * @param start 0 will be the most recent bar
	 * @param periods Needs 2x the # of periods in data set 
	 * @return
	 */
	public int getRSI(final int start, final int periods){
		//go back 2x periods to calculate RSI
		int oldestIdx = start+periods+periods-1;
		if (oldestIdx >= size()) {
			return 0;
		}
		double firstGainSum = 0;
		double firstLossSum = 0;
		Bar b = get(oldestIdx);
		for(int i = oldestIdx-1; i >= start+periods ;i--){
			final Bar nextBar = get(i);
			if(nextBar.close > b.close) firstGainSum+= (nextBar.close - b.close);
			else firstLossSum += (b.close - nextBar.close);
			b = nextBar;
		}
		double avgGain = firstGainSum / periods;
		double avgLoss = firstLossSum / periods;
		for(int i = start+periods -1; i >= start ;i--){
			final Bar nextBar = get(i);
			if(nextBar.close > b.close) avgGain = (avgGain / (periods-1)) + 
					((nextBar.close - b.close) / periods);
			else avgLoss = (avgLoss / (periods-1)) + 
					((b.close - nextBar.close) / periods);
			b = nextBar;
		}
		final double RS = avgGain / avgLoss;
		return (int) (100 - (100 / (1+RS)));
	}
	/**
	 * Is there enough data in the cache to represent valid calculations for the
	 * specified number of periods
	 * 
	 * @param periods
	 * @return
	 */
	public boolean isInitialized(final int periods) {
		return periods < lastIdx || (wrapped && periods <= localCache.length);
	}

	/**
	 * Add another Bar into the cache
	 * 
	 * @param aBar
	 */
	public synchronized void push(final Bar aBar) {
		if (aBar == null)
			throw new IllegalArgumentException("aBar must not be null");
		if (lastIdx == localCache.length) {
			lastIdx = 0;
			wrapped = true;
		}
		localCache[lastIdx++] = aBar;
	}
	/**
	 * Find the correlation of values between two streams of bars on the provided field.
	 * 'w' Volume Weighted Price
	 * 'v' Volume
	 * 'c' Close
	 * 'o' Open
	 * 'h' High
	 * 'l' Low
	 * 't' Trade Count 
	 *  
	 * @param otherCache
	 * @param index
	 * @param periods
	 * @return
	 */
	public double correlation(final BarCache otherCache, final int start, final int periods, final char field){
		long selectedTime = get(start).originalTime;
		int otherIndex = otherCache.indexOf(selectedTime);
		double deltaA, deltaB, aXbSum = 0, aSquaredSum = 0, bSquaredSum = 0; 
		double meanA, meanB;
		meanA = getMA(start, periods,field);
		meanB = otherCache.getMA(otherIndex, periods, field);
		
		Iterator<Bar> otherList = otherCache.getIteratable(otherIndex, periods).iterator();
		for(Bar aBar: getIteratable(start, periods)){
			deltaA = aBar.getField(field) - meanA;
			Bar otherBar = otherList.next();
			deltaB = otherBar.getField(field) - meanB;
			aXbSum += (deltaA * deltaB);
			aSquaredSum += (deltaA * deltaA);
			bSquaredSum += (deltaB * deltaB);
		}
		return aXbSum / Math.sqrt(aSquaredSum*bSquaredSum);
	}
	/**
	 * 
	 * @param originalTime
	 * @return
	 */
	public synchronized int indexOf(final long originalTime ){
		if(cachedIndexOf == 0){
			if(wrapped) cachedIndexOf = localCache.length - 1;
			else cachedIndexOf = lastIdx - 1;
		}
		
		Bar b = localCache[cachedIndexOf];
		if(b.originalTime == originalTime){
			hitCount++;
			return convertToExternalIndex(cachedIndexOf);
		}
		
		final Bar mostRecent = localCache[lastIdx -1];
		final Bar oldest = wrapped ? localCache[lastIdx] : localCache[0];
		if(mostRecent.originalTime >= originalTime && oldest.originalTime <= originalTime){
			if(mostRecent.originalTime == originalTime) return 0;
			double spread = (mostRecent.originalTime - originalTime) / (mostRecent.originalTime - oldest.originalTime);
			if(spread < .01) spread = .5;
			int guess= (int) (wrapped ? lastIdx - (int)(localCache.length * spread) : Math.min(lastIdx*spread, lastIdx-1));
			if(guess < 0) guess = localCache.length + guess;
			int idx = guess;
			//There are large gaps at day boundaries that could screw this up
			//the algo only works if the initial idx yields a bar time that is 
			//greater than than the passed in parameter
			if(localCache[idx].originalTime < originalTime) idx = lastIdx -1;
			while(true){
				//where should I look for the index
				final Bar candidate = localCache[idx];
				if(originalTime >= candidate.originalTime) {
					cachedIndexOf = idx;
					missCount++;
					return convertToExternalIndex(idx);
				}
				
				idx = (idx > 0) ? idx-1 : localCache.length - 1;
			}
		}
		else{
			throw new IllegalArgumentException("Time " + HistoricalDateManipulation.getDateAsStr(originalTime) + " not in cache");
		}
	}
	/**
	 * @param idx
	 * @return
	 */
	protected int convertToExternalIndex(int idx) {
		int gotIt;
		if(idx > lastIdx -1 ) gotIt = (lastIdx ) + (localCache.length - 1 - idx );
		else gotIt = lastIdx - 1 -idx; 
		return gotIt;
	}
	/**
	 * Get the past N bars with the first element being the most recent and the
	 * last element in the array being the oldest.
	 * 
	 * @param periods
	 * @return
	 */
	public Bar[] getBars(final int periods) {
		validateIndex(periods);
		Bar[] result = new Bar[periods];
		int count = 0;
		for (int i = lastIdx - 1; i >= Math.max(lastIdx - periods, 0); i--)
			result[count++] = localCache[i];
		if (periods > lastIdx) {
			final int end = localCache.length - (periods - count);
			for (int i = localCache.length - 1; i >= end; i--) {
				result[count++] = localCache[i];
			}
		}
		return result;
	}

	/**
	 * Returns an in place Iteratable that doesn't create much garbage. 
	 * WANRING - not thread safe. If the underlying data changes it will
	 * be visible and potentially confusing.
	 * 
	 * Iterates by starting at the oldest value (start + periods) and
	 * working towards newest value (start) 
	 * @param start
	 * @param periods
	 * @return
	 */
	public Iterable<Bar> getIteratable(final int start, final int periods ){
		
		final int oldestIdx = start+periods-1;
		validateIndex(oldestIdx);
		return new Iterable<Bar>(){
			@Override
			public Iterator<Bar> iterator() {
				return new Iterator<Bar>(){
					int count = 0;
					@Override
					public boolean hasNext() {
						return count < periods;
					}
					@Override
					public Bar next() {
						final Bar result = get(oldestIdx - count);
						count++;
						return result;
					}
					@Override
					public void remove() {	} // not implemented
					
				};
			}
		};
	}
	/**
	 * Returns an in place Iteratable that doesn't create much garbage. 
	 * WANRING - not thread safe. If the underlying data changes it will
	 * be visible and potentially confusing.
	 * 
	 * Iterates by starting at the oldest value (start + periods) and
	 * working towards newest value (start) 
	 * @param start
	 * @param periods
	 * @return
	 */
	public Iterable<DoubleReader> getIteratable(final int start, final int periods, final char field ){
		
		final int oldestIdx = start+periods-1;
		validateIndex(oldestIdx);
		return new Iterable<DoubleReader>() {
			@Override
			public Iterator<DoubleReader> iterator() {
				return new Iterator<DoubleReader>(){
					final DoubleReader rdr = new DoubleReader(){
						@Override
						public double readDouble(){ 
							int idx = oldestIdx - count + 1;
							if(get(idx) == null) {
								System.out.println("null");
							}
							return get(idx ).getField(field);
						}
					};
					int count = 0;
					@Override
					public boolean hasNext() {
						return count < periods;
					}
					//returns the same reader over and over
					@Override
					public DoubleReader next() {
						//count will always be 1 ahead
						count++;
						return rdr;
					}
					@Override
					public void remove() {	} // not implemented
					
				};
			}
		};
	}	/**
	 * @param periods
	 */
	protected void validateIndex(final int idx) {
		if (idx >= size() ) {
			throw new IllegalStateException("Not enough data to fullfill request asking:"+ idx + " from " + size());
		}
	}

	public double[] getVwap(final int periods) {
		validateIndex(periods-1);//zero based index
		double[] result = new double[periods];
		int count = 0;
		final int lowerBounds = Math.max(lastIdx - periods, 0);
		for (int i = lastIdx - 1; i >= lowerBounds; i--)
			result[count++] = localCache[i].wap;
		if (periods > lastIdx) {
			final int end = localCache.length - (periods - count);
			for (int i = localCache.length - 1; i >= end; i--) {
				result[count++] = localCache[i].wap;
			}
		}
		return result;
	}

	public double getVolatility(final int periods) {
		Bar[] bars = getBars(periods);
		double[] data = new double[bars.length * 5];
		int count = 0;
		for (Bar aBar : bars) {
			data[count++] = aBar.open;
			data[count++] = aBar.low;
			data[count++] = aBar.wap;
			data[count++] = aBar.high;
			data[count++] = aBar.close;
		}
		return Stat.volatilityPerCentChange(data) / 100;
	}
	/**
	 * Set a default factor to multiply by standard deviation when
	 * calculating bands. 
	 * @param d
	 */
	public void setStdDevFactor(final double d) {
		stdDevFactor = d;
	}
	/**
	 * Get highs and low bands that surround vwap by stdDevFactor*stdDev
	 * @param periods
	 * @return
	 */
	public PriceBands getVwapBands(final int periods) {
		return getVwapBands(periods, stdDevFactor);
	}
	/**
	 * Get highs and low bands that surround vwap by stdDevAdj*stdDev
	 * @param periods 
	 * @param stdDevAdjustment
	 * @return
	 */
	public PriceBands getVwapBands(final int periods, final double stdDevAdjustment) {
		final double[] vwapList = getVwap(periods);
		final Stat stat = new Stat(vwapList);
		final double stdDev = stat.standardDeviation() * stdDevAdjustment;
		final double vwap = vwapList[0];
		final double[] result = new double[3];
		result[0] = vwap - stdDev;
		result[1] = vwap;
		result[2] = vwap + stdDev;
		return new PriceBands(result);
	}

	public void clear() {
		wrapped = false;
		lastIdx = 0;

	}
}
