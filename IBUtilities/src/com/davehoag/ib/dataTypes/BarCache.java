package com.davehoag.ib.dataTypes;

import flanagan.analysis.Stat;

public class BarCache {
	Bar[] localCache = new Bar[5 * (60 / 5) * 60 * 8];
	int lastIdx = 0;
	boolean wrapped = false;
	double stdDevFactor = 1;

	public BarCache() {
	}

	public void setStdDevFactor(final double d) {
		stdDevFactor = d;
	}

	/**
	 * Is the close of the most recent bar above the close of the oldest bar and
	 * there are more than 1/2 closes above the prior bars's close
	 * 
	 * @param periods
	 * @return
	 */
	public boolean isTrendingUp(final int periods) {
		Bar[] bars = getBars(periods);
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
	 * Get the moving average of the Vwap data
	 * 
	 * @param periods
	 * @return
	 */
	public double getVwapMA(final int periods) {
		Bar[] bars = getBars(periods);
		double sum = 0;
		for (Bar aBar : bars) {
			sum += aBar.wap;
		}
		return sum / bars.length;
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
	public void pushLatest(final Bar aBar) {
		if (aBar == null)
			throw new IllegalArgumentException("aBar must not be null");
		if (lastIdx == localCache.length) {
			lastIdx = 0;
			wrapped = true;
		}
		localCache[lastIdx++] = aBar;
	}

	/**
	 * Get the past N bars with the first element being the most recent and the
	 * last element in the array being the oldest.
	 * 
	 * @param periods
	 * @return
	 */
	public Bar[] getBars(final int periods) {
		if ((periods > localCache.length) || (periods > lastIdx && !wrapped)) {
			throw new IllegalStateException("Not enough data to fullfill request");
		}
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

	public double[] getVwap(final int periods) {
		if ((periods > localCache.length) || (periods > lastIdx && !wrapped)) {
			throw new IllegalStateException("Not enough data to fullfill request");
		}
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
		final double[] vwapList = getVwap(periods);
		final Stat stat = new Stat(vwapList);
		final double stdDev = stat.standardDeviation() * stdDevFactor;
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
