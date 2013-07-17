package com.davehoag.ib.dataTypes;

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
	public void setStdDevFactor(final double d) {
		stdDevFactor = d;
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
		Bar b = get(0);
		double high = b.high; double low = b.low;
		double candidateHigh = 0;
		boolean valid = false;
		for(int i = 1; i < periods; i++){
			final Bar aBar = get(i);
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
	public double [] getParabolicSar(final double [] result, final double accelFact){
		// Initialize trend to whatever
		boolean upTrend = false;
		final double accelFactIncrement = accelFact == 0 ? .02 : accelFact;
		int periods = result.length;
		Bar aBar = get(periods -1);
		 
		// Previous SAR: Use first data point's extreme value, depending on trend
		double pSar = aBar.high;
		double extremePoint = aBar.low;
		result[0]= pSar;
		result[1]= pSar;
		return getParabolicSar(upTrend, accelFactIncrement, accelFactIncrement, periods -2, pSar, extremePoint, result);
	}
	public double [] getParabolicSar(final int periods, final double accelFact){
		return getParabolicSar(new double [periods], accelFact);
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
	protected double [] getParabolicSar(boolean upTrend, double accelerationFactor, final double accelFactIncrement, final int barIdx, final double pSar, double extremePoint, double [] result){
		double nSar = 0;
		final Bar todaysBar = get(barIdx);
		
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
				final double maxSar = Math.min(todaysBar.low, get(barIdx + 1).low);
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
				final double minSar = Math.max(todaysBar.high, get(barIdx + 1).high);
				nSar = Math.max(minSar, nSar);
			}		
		}
		result[result.length - barIdx] = nSar;
		if(barIdx == 1) return result;
		return getParabolicSar(upTrend, accelerationFactor, accelFactIncrement, barIdx - 1, nSar, extremePoint, result);
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
	public int getFutureTrend(final long origTime, final int periods){
		final int idx = indexOf(origTime);
		if(idx < periods) throw new IllegalArgumentException("Not enough future periods for " + periods + " but only have " + idx + " after this bar");
		double resultTotal = 0;
		Bar prior = get(idx);
		final Bar original = prior;
		for(int i = idx -1; i >= (idx - periods); i--){
			final Bar next = get(i);
			double result = 0;
			if(next.close > prior.close) result +=2;
			if(next.high > prior.close) result +=1;
			if(next.wap > prior.close) result +=2;
			if(next.low >= prior.close) result +=2;
			if(next.low > prior.low) result +=1;
			if(next.high >= prior.high) result +=2;
			if(next.close < prior.close) result -=2;
			if(next.high <= prior.close) result -=2;
			if(next.high < prior.high) result -=1;
			if(next.wap < prior.close) result -=2;
			if(next.low < prior.close) result -=1;
			if(next.low < prior.low) result -=2;
			double wapChange = next.wap - original.wap;
			double absoluteChange = wapChange == 0 ? 0.0 : 2000*wapChange/original.wap;
			resultTotal += (result + absoluteChange);
			prior = next;
		}
		return (int)resultTotal;
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
	public double getMA(final int start, final int periods, final char field) {
		final Bar[] bars = getBars(periods);
		double sum = 0;
		for (int i = start; i < bars.length; i++) {
			final Bar aBar= bars[i];
			sum += aBar.getField(field);
		}
		return sum / (bars.length - start);
	}
	/**
	 * 
	 * @param start 0 will be the most recent bar
	 * @param periods
	 * @return
	 */
	public int getRSI(final int start, final int periods){
		
		int oldestIdx = start+periods+periods-1;
		validateIndex(oldestIdx);
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
	 * @param periods
	 * @return
	 */
	public boolean haslessThanNBars(final int periods) {
		return (periods > localCache.length) || (periods > lastIdx && !wrapped);
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
	 * @param periods
	 */
	protected void validateIndex(final int periods) {
		if (haslessThanNBars(periods)) {
			throw new IllegalStateException("Not enough data to fullfill request");
		}
	}

	public double[] getVwap(final int periods) {
		validateIndex(periods);
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
	public PriceBands getVwapBands(final int periods) {
		return getVwapBands(periods, stdDevFactor);
	}
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
