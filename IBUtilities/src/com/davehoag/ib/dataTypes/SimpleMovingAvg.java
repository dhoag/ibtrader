package com.davehoag.ib.dataTypes;

public class SimpleMovingAvg {
	final double [] fastLeg;
	final double [] slowLeg;
	int lastFast = 0;
	int lastSlow = 0;
	boolean trendingUp;
	boolean initialized;
	/**
	 * 
	 * @param fastSize take the last N values from the seeds 
	 * @param slowSize take the last N values from the seeds
	 * @param seeds A time series of values with oldest at position 0
	 */
	public SimpleMovingAvg(final int fastSize, final int slowSize){
		this(fastSize, slowSize, null);
	}
	/**
	 * 
	 * @param fastSize take the last N values from the seeds 
	 * @param slowSize take the last N values from the seeds
	 * @param seeds A time series of values with oldest at position 0
	 */
	public SimpleMovingAvg(final int fastSize, final int slowSize, final double[] seeds){
		if(fastSize > slowSize) throw new IllegalArgumentException("Fast can't be larger than slow " + fastSize + " > " + slowSize);
		fastLeg = new double[fastSize];
		slowLeg = new double[slowSize];
		if( seeds != null ) {
			final int slowdiff = seeds.length - slowSize;
			if(slowdiff != 0) throw new IllegalArgumentException("Slow size can not be different than seeds size");
			final int diff = slowSize - fastSize;
			for(int i = 0; i  < slowSize; i++){
				slowLeg[i] = seeds[i];
				//slow = 2, fast = 1, diff = 1, i = 0 dont add, i = 1 add
				if(i >= diff){
					fastLeg[i-diff] = seeds[i];
				}
			}
			initialized = true;
			trendingUp = isTrendingUp();
		}
		else {
			initialized = false;
		}
	}
	/**
	 * Until this is initialized it will always return false;
	 * @param price
	 * @return boolean true if the trend was reversed
	 */
	public synchronized boolean newTick(final double price){
		
		fastLeg[lastFast++] = price;
		slowLeg[lastSlow++] = price;
		if(lastFast == fastLeg.length) lastFast = 0;
		if(lastSlow == slowLeg.length) lastSlow = 0;
		if(initialized){
			final boolean newTrend = isTrendingUp();
			return initTrend(newTrend);
		}
		if(lastSlow == 0) // we wrapped
		{
			initialized = true;
			trendingUp = isTrendingUp();
		}
		return false;
	}
	protected final boolean initTrend(final boolean newTrend){
		//if the old and new trend are not the same
		if( newTrend ^ trendingUp){
			//change the trend direction
			trendingUp = newTrend;
			return true;
		}
		return false;		
	}
	public final boolean isTrendingUp(){
		if(!initialized) throw new IllegalStateException("SMA not yet initialized, need more data");
		return getFastAvg() > getSlowAvg();
	}
	public double getFastAvg(){
		return getAvg(fastLeg);
	}
	public double getSlowAvg(){
		return getAvg(slowLeg);
	}
	protected double getAvg(final double [] list){
		double sum = 0;
		for(int i = 0; i < list.length; i++){
			sum+=list[i];
		}
		return sum / list.length;
	}
}
