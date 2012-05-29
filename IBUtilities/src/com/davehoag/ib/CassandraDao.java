package com.davehoag.ib;

import java.text.DecimalFormat;

import me.prettyprint.cassandra.serializers.BooleanSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
/**
 * 
 * @author dhoag
 *
 */
public class CassandraDao {
	private final Cluster cluster = HFactory.getOrCreateCluster("Test Cluster", "localhost:9160");
	private final StringSerializer stringSerializer = StringSerializer.get();
	private final LongSerializer longSerializer = LongSerializer.get();
	private final BooleanSerializer  booleanSerializer = BooleanSerializer.get();
	private final Keyspace keyspace = HFactory.createKeyspace("tradedata", cluster);
	
	public static void main(String [] args){
		new CassandraDao().insertHistoricalData( "IBM", "23423422", 12.3d, 14,15,12,23,2312,33,true);
	}
	protected void insertHistoricalData(final String symbol, final String dateSeconds,
			final double open, final double high, final double low,
			final double close, final int volume, final int count,
			final double WAP, final boolean hasGap)
	{
		final DecimalFormat df = new DecimalFormat("#.##");
		final HColumn<String, String> symbolCol = HFactory.createColumn("symbol", "IBM", stringSerializer, stringSerializer);
		final HColumn<String, String> openCol = HFactory.createColumn("open", df.format(open), stringSerializer, stringSerializer);
		final HColumn<String, String> closeCol = HFactory.createColumn("close", df.format(open), stringSerializer, stringSerializer);
		final HColumn<String, String> highCol = HFactory.createColumn("high", df.format(open), stringSerializer, stringSerializer);
		final HColumn<String, String> lowCol = HFactory.createColumn("low", df.format(open), stringSerializer, stringSerializer);
		final HColumn<String, String> wapCol = HFactory.createColumn("wap", df.format(open), stringSerializer, stringSerializer);
		final HColumn<String, Long> volumeCol = HFactory.createColumn("volume", Long.valueOf(volume), stringSerializer, longSerializer);
		final HColumn<String, Long> tradeCountCol = HFactory.createColumn("tradeCount", Long.valueOf(count), stringSerializer, longSerializer);
		final HColumn<String, Boolean> hasGapCol = HFactory.createColumn("hasGap", Boolean.valueOf(hasGap), stringSerializer, booleanSerializer);
		
	    final Mutator<Long> m = HFactory.createMutator(keyspace, longSerializer);
	    final Long key = Long.valueOf(dateSeconds);
	    m.addInsertion( key, "15sec_candles", symbolCol );
	    m.addInsertion( key, "15sec_candles", openCol );
	    m.addInsertion( key, "15sec_candles", closeCol );
	    m.addInsertion( key, "15sec_candles", highCol );
	    m.addInsertion( key, "15sec_candles", lowCol );
	    m.addInsertion( key, "15sec_candles", wapCol );
	    m.addInsertion( key, "15sec_candles", volumeCol );
	    m.addInsertion( key, "15sec_candles", tradeCountCol);
	    m.addInsertion( key, "15sec_candles", hasGapCol);
	    m.execute();
	}
}
