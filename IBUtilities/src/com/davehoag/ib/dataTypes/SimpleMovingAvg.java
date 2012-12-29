package com.davehoag.ib.dataTypes;

public class SimpleMovingAvg {
	final double [] fastLeg;
	final double [] slowLeg;
	double fastEma;
	double slowEma;
	int lastFast = 0;
	int lastSlow = 0;
	boolean trendingUp;
	boolean initialized;
	boolean useEmaForCrossOvers;

	/**
	 * 0 for the most latest tick, 1 for the next to latest, etc...
	 * 
	 * @param reverseIdx
	 * @return
	 */
	public double getTick(int reverseIdx) {
		// 1 and only 1, last slow is 1, proper offset is zero, reverse idx is
		// zero
		if (reverseIdx >= slowLeg.length)
			throw new IllegalArgumentException("Can't exceed size, " + reverseIdx + ", of largest array "
					+ slowLeg.length);
		if (!initialized) throw new IllegalStateException("Structure not initialized");
		int offset = lastSlow - 1 - reverseIdx;
		if (offset >= 0) return slowLeg[offset];
		// if lastSlow is zero, then we want length -1
		offset = slowLeg.length + offset;
		return slowLeg[offset];
	}
	public boolean isInitialized(){
		return initialized;
	}
	public void setUseEmaForCrossOvers(boolean option){
		useEmaForCrossOvers = option;
	}
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
			final int slowdiff = seeds.length - slowLeg.length;
			if(slowdiff != 0) throw new IllegalArgumentException("Slow size can not be different than seeds size");
			final int diff = slowLeg.length - fastLeg.length;
			for(int i = 0; i  < slowLeg.length; i++){
				slowLeg[i] = seeds[i];
				//slow = 2, fast = 1, diff = 1, i = 0 dont add, i = 1 add
				if(i >= diff){
					fastLeg[i-diff] = seeds[i];
				}
			}
			init();
		}
		else {
			initialized = false;
		}
	}
	/**
	 * Clear out the data, set counters to zero. 
	 */
	public void reset(){
		lastSlow = 0;
		lastFast = 0;
		initialized = false;
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
			slowEma= calcEma(price,slowEma, slowLeg.length);
			fastEma = calcEma(price, fastEma, fastLeg.length);
			final boolean newTrend = isTrendingUp();
			return updateTrend(newTrend);
		}
		if(lastSlow == 0 && !initialized) // we wrapped
		{
			init();
		}
		return false;
	}
	public double getMostRecentTick(){
		return lastSlow == 0? slowLeg[slowLeg.length - 1] : slowLeg[ lastSlow - 1];
	}
	public double getOldestTick(){
		return slowLeg[lastSlow];
	}
	/**
	 * 
	 */
	protected void init() {
		initialized = true;
		trendingUp = isTrendingUp();
		slowEma = getSlowAvg();
		fastEma = getFastAvg();
	}
	/**
	 * @param price
	 */
	protected double calcEma(final double price, final double priorEma, final int numOfElements) {
		double emaMultiplier =  2.0 / (numOfElements + 1) ;
		return (price - priorEma)*emaMultiplier + priorEma;
	}
	protected final boolean updateTrend(final boolean newTrend){
		//if the old and new trend are not the same
		if( newTrend ^ trendingUp){
			//change the trend direction
			trendingUp = newTrend;
			return true;
		}
		return false;		
	}
	/**
	 * Decide if the fast avg if greater than the slow avg.
	 * This will either key of ema or sma calculations
	 * @return
	 */
	public final boolean isTrendingUp(){
		if(!initialized) throw new IllegalStateException("SMA not yet initialized, need more data");
		if(useEmaForCrossOvers) return fastEma > slowEma;
		return getFastAvg() > getSlowAvg();
	}

	public boolean isRecentlyUp() {
		if (!initialized) throw new IllegalStateException("SMA not yet initialized, need more data");
		double latest = getTick(0);
		int countOfUp = 0;
		for (int i = 1; i < 5; i++) {
			double prior = getTick(i);
			if (latest >= prior) {
				countOfUp++;
			}
			latest = prior;
		}
		return getTick(0) > getTick(4) && countOfUp >= 3;
	}
	public double getFastAvg(){
		return getAvg(fastLeg);
	}
	public double getSlowAvg(){
		return getAvg(slowLeg);
	}
	public double getFastEma(){
		return fastEma;
	}
	public double getSlowEma(){
		return slowEma;
	}
	/**
	 * See if the price swing is accelerating
	 * @return
	 */
	public boolean recentJumpExceedsAverage(){
		final double latestTick = getMostRecentTick();
		final int priorIdx = lastSlow <= 1 ? slowLeg.length - 2 + lastSlow : lastSlow - 2;
		final double priorTick = slowLeg[priorIdx];
		final double changePercent = Math.abs((latestTick - priorTick) / priorTick);
		return (100*changePercent) > getVolatilityPercent();
	}
	/**
	 * Get the percentage change from the oldest tick to the most recent for the slow leg 
	 * @return
	 */
	public double getSlowChange(){
		if (!initialized)
			throw new IllegalStateException("Must have a full set to calculate change %");
		final double latestTick = getMostRecentTick();
		final double oldestTick = getOldestTick();
		return (latestTick - oldestTick) / oldestTick;
	}
	public double getSlowDelta(){
		final double latestTick = getMostRecentTick();
		final double oldestTick = getOldestTick();
		return (latestTick - oldestTick) ;
	}
	/**
	 * For the data in the slow leg, what's the typical percent change between ticks
	 * @return
	 */
	public double getVolatilityPercent(){
		double change = 0;
		double lastPrice = 0;
		for(double price : slowLeg){
			if(lastPrice != 0) {
				change += Math.abs((price - lastPrice)/lastPrice);
			}
			lastPrice = price; 
		}
		return 100*change / (slowLeg.length - 1);
//		return Stat.volatilityPerCentChange(slowLeg);
	}
	protected double getAvg(final double [] list){
		double sum = 0;
		for(int i = 0; i < list.length; i++){
			sum+=list[i];
		}
		return sum / list.length;
	}
}
