package com.davehoag.ib.dataTypes;

import java.util.HashMap;
import java.util.List;
import com.davehoag.ib.CassandraDao;
import me.prettyprint.hector.api.beans.HColumn;
/**
 * Enable very large result sets 
 * @author dhoag
 *
 */
public class PagingBarIterator extends BarIterator {
	int pagingSize = 2;
	Bar lastBar;
	long end;
	BarIterator iterator; 
	public void setPagingSize(final int sz){
		pagingSize = sz;
	}
	public PagingBarIterator(final String sym) {
		super(sym);
	}
	public void reset(){
		super.reset();
		iterator = null;
		lastBar = null;
	}
	public PagingBarIterator(final String sym, final HashMap<String, List<HColumn<Long, Double>>> price, 
			final HashMap<String, List<HColumn<Long, Long>>> vol, final String barSz, final long endTime) throws Exception {
		super(sym, price, vol, barSz);
		end = endTime;
	}
	/**
	 * First check with the original query result (if no iterator is found)
	 * If it says looping is done, try to page in a new result set
	 * if already paged in, check with that one,
	 * if that says done then page in a new result
	 */
	public boolean hasNext() {
		boolean hasMore = iterator == null && super.hasNext();
		if(! hasMore){
			//if the original result set was completely empty, just return 
			if( lastBar == null ) return hasMore;
			//may need to page
			if(iterator == null) {
				iterator = CassandraDao.getInstance().getNext(symbol, lastBar, end, pagingSize, false);
				hasMore = iterator.hasNext();
			}
			else{
				hasMore = iterator.hasNext();
				if(! hasMore ) {
					iterator = CassandraDao.getInstance().getNext(symbol, lastBar, end, pagingSize, false);
					hasMore = iterator.hasNext();
				}
			}
		}
		return hasMore;
	}
	/**
	 * Keep track of the last bar as a reference to getting the next set of data from
	 * the result set
	 */
	public Bar next() {
		if(iterator != null){
			lastBar = iterator.next();
		}
		else {
			lastBar = super.next();
		}
		return lastBar;
	}
}
