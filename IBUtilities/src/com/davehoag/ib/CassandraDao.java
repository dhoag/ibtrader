package com.davehoag.ib;

import static me.prettyprint.hector.api.factory.HFactory.createColumnQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.text.DecimalFormat;
import java.util.Date;

import me.prettyprint.cassandra.model.CqlQuery;
import me.prettyprint.cassandra.model.CqlRows;
import me.prettyprint.cassandra.serializers.BooleanSerializer;
import me.prettyprint.cassandra.serializers.DoubleSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.KeyIterator;
import me.prettyprint.cassandra.service.template.ColumnFamilyResult;
import me.prettyprint.cassandra.service.template.ColumnFamilyTemplate;
import me.prettyprint.cassandra.service.template.ThriftColumnFamilyTemplate;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.beans.Rows;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ComparatorType;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
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
	private final DoubleSerializer doubleSerializer = DoubleSerializer.get();
	private final Keyspace keyspace = HFactory.createKeyspace("tradedata", cluster);
	
	public static void main(String [] args){
		CassandraDao dao = new CassandraDao();
		//new CassandraDao().insertHistoricalData( "IBM", "23423422", 12.3d, 14,15,12,23,2312,33,true);

		dao.query2();		
	}
	protected void insertHistoricalData(final String symbol, final String dateSecondsStr,
			final double open, final double high, final double low,
			final double close, final int volume, final int count,
			final double WAP, final boolean hasGap)
	{
		final String barSize = "bar15min";
		final DecimalFormat df = new DecimalFormat("#.##");
		Long dateSeconds = Long.valueOf(dateSecondsStr);
		
		final HColumn<Long, Double> openCol = HFactory.createColumn(dateSeconds, open, longSerializer, doubleSerializer);
		final HColumn<Long, Double> closeCol = HFactory.createColumn(dateSeconds, close, longSerializer, doubleSerializer);
		final HColumn<Long, Double> highCol = HFactory.createColumn(dateSeconds, high, longSerializer, doubleSerializer);
		final HColumn<Long, Double> lowCol = HFactory.createColumn(dateSeconds, low, longSerializer, doubleSerializer);
		final HColumn<Long, Double> wapCol = HFactory.createColumn(dateSeconds, WAP, longSerializer, doubleSerializer);
		final HColumn<Long, Long> volumeCol = HFactory.createColumn(dateSeconds, Long.valueOf(volume), longSerializer, longSerializer);
		final HColumn<Long, Long> tradeCountCol = HFactory.createColumn(dateSeconds, Long.valueOf(count), longSerializer, longSerializer);
		final HColumn<Long, Boolean> hasGapCol = HFactory.createColumn(dateSeconds, Boolean.valueOf(hasGap), longSerializer, booleanSerializer);
		
	    final Mutator<String> m = HFactory.createMutator(keyspace, stringSerializer);

	    String key = symbol + ":open";
	    m.addInsertion( key, barSize, openCol );
	    key = symbol + ":close";
	    m.addInsertion( key, barSize, closeCol );
	    key = symbol + ":high";
	    m.addInsertion( key, barSize, highCol );
	    key = symbol + ":low";
	    m.addInsertion( key, barSize, lowCol );
	    key = symbol + ":wap";
	    m.addInsertion( key, barSize, wapCol );
	    key = symbol + ":vol";
	    m.addInsertion( key, barSize, volumeCol );
	    key = symbol + ":tradeCount";
	    m.addInsertion( key, barSize, tradeCountCol);
	    key = symbol + ":hasGap";
	    m.addInsertion( key, barSize, hasGapCol);
	    m.execute();
	}
	public void query(){
	    ColumnQuery<Long, String, String> q = createColumnQuery(keyspace, longSerializer, stringSerializer, stringSerializer);
	    Long key = Long.valueOf(1337783400);
	    QueryResult<HColumn<String, String>> r = q.setKey(key).
	        setName("symbol").
	        setColumnFamily("15sec_candles").
	        execute();
	    HColumn<String, String> c = r.get();
	    System.out.println( " Result: " + c == null ? null : c.getValue());
	    
	    long start = (new Date().getTime() / 1000) - (120*24*60*60);
	    long end = new Date().getTime() / 1000;
	    System.out.println("S " + start + " " + end);
	    Iterable<Long> it = new KeyIterator<Long>(keyspace,"15sec_candles" , longSerializer, start, end);

	    int tot = 0;
	    Long lastKey = null;
	    for (Long keyVal : it){
	    	lastKey = keyVal;
		    ColumnFamilyTemplate<Long, String> template = new ThriftColumnFamilyTemplate<Long, String>(keyspace, "15sec_candles", longSerializer, stringSerializer);
		    template.addColumn("symbol", stringSerializer);
		    template.addColumn("open", stringSerializer);
		    template.addColumn("close", stringSerializer);
		    template.addColumn("wap", stringSerializer);
		    template.addColumn("high", stringSerializer);
		    template.addColumn("low", stringSerializer);
		    template.addColumn("volume", longSerializer);
		    template.addColumn("tradeCount", longSerializer);
		    template.addColumn("hasGaps", booleanSerializer);
		    ColumnFamilyResult wrapper = template.queryColumns(lastKey);
	        String sym = wrapper.getString("symbol");
		    double open= Double.valueOf(wrapper.getString("open"));
		    double close = Double.valueOf( wrapper.getString("close"));
		    double high = Double.valueOf(wrapper.getString("high"));
		    double low = Double.valueOf(wrapper.getString("low"));
		    double WAP = Double.valueOf(wrapper.getString("wap"));
		    int volume = wrapper.getLong("volume").intValue();
		    int count =  wrapper.getLong("tradeCount").intValue();
		    boolean hasGap = false;
	        System.out.print(keyVal);
		    System.out.print( "," + sym);
		    System.out.print( "," + open);
		    System.out.print( "," + close);	
		    System.out.print( "," + high);	 
		    System.out.print( "," + low);
		    System.out.print( "," + WAP);	    
		    System.out.print( "," + volume );	    
		    System.out.print( "," + count);	 
		    System.out.println( "," + hasGap); 
		    //Used to migrate to new structure
		    //insertHistoricalData(sym, String.valueOf(keyVal),  open, high, low, close, volume, count, WAP, hasGap);
	    }	    
	}
    public void query2(){
        RangeSlicesQuery<String, Long, Double> rangeSlicesQuery = HFactory.createRangeSlicesQuery(keyspace, stringSerializer, longSerializer, doubleSerializer);
        rangeSlicesQuery.setColumnFamily("bar15min");
        rangeSlicesQuery.setKeys("IBM:wap", "");
	    long start = (new Date().getTime() / 1000) - (120*24*60*60);
	    long end = new Date().getTime() / 1000;
        rangeSlicesQuery.setRange(end, start, true, 20);
        QueryResult<OrderedRows<String, Long, Double>> result = rangeSlicesQuery.execute();
        Rows<String, Long, Double> rows = result.get();
        for (Row<String, Long, Double> row2 : rows) {
            
            ColumnSlice<Long, Double> slice = row2.getColumnSlice();

            for (HColumn<Long, Double> column : slice.getColumns()) {
              System.out.println(column.getName() + " " + column.getValue());
            }

          }

    }
   
}
