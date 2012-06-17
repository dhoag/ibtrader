package com.davehoag.ib;

import static me.prettyprint.hector.api.factory.HFactory.createColumnQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.davehoag.ib.dataTypes.Bar;
import com.davehoag.ib.util.HistoricalDateManipulation;

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
import me.prettyprint.hector.api.query.SliceQuery;
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
	
	private final String [] priceKeys = { ":open", ":close", ":high", ":low", ":wap" };
	private final String [] longKeys = { ":vol", ":tradeCount" };
	public static void main(String [] args){
		try{
			CassandraDao dao = new CassandraDao();
			//new CassandraDao().insertHistoricalData( "IBM", "23423422", 12.3d, 14,15,12,23,2312,33,true);
	
			dao.query3();		
		} catch(Throwable t){
			t.printStackTrace();
		}
	}
	protected void insertHistoricalData(final String symbol, final String dateSecondsStr,
			final double open, final double high, final double low,
			final double close, final int volume, final int count,
			final double WAP, final boolean hasGap)
	{
		final String barSize = "bar5sec";
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
        RangeSlicesQuery<String, Long, Boolean> rangeSlicesQuery = HFactory.createRangeSlicesQuery(keyspace, stringSerializer, longSerializer, booleanSerializer);
        rangeSlicesQuery.setColumnFamily("bar5sec");
        rangeSlicesQuery.setKeys("SPY:hasGap", "SPY:hasGap");
	    long start = (new Date().getTime() / 1000) - (4*24*60*60);
	    long end = new Date().getTime() / 1000;
        rangeSlicesQuery.setRange(end, start, true, 20);
        QueryResult<OrderedRows<String, Long, Boolean>> result = rangeSlicesQuery.execute();
        Rows<String, Long, Boolean> rows = result.get();
        printFirstRow(rows);

        RangeSlicesQuery<String, Long, Double> priceQuery = HFactory.createRangeSlicesQuery(keyspace, stringSerializer, longSerializer, doubleSerializer);
        priceQuery.setKeys("QQQ:close", "QQQ:close");
        priceQuery.setColumnFamily("bar5sec");
        priceQuery.setRange(end, start, true, 2);
        QueryResult<OrderedRows<String, Long, Double>> priceResults = priceQuery.execute();
        Rows<String, Long, Double> closeRows = priceResults.get();
        priceQuery.setKeys("QQQ:wap", "QQQ:wap");
        priceResults = priceQuery.execute();
        Rows<String, Long, Double> wapRows = priceResults.get();
        printFirstRow(closeRows);
        printFirstRow(wapRows);
        
        printColumns( closeRows.getByKey("QQQ:close"), wapRows.getByKey("QQQ:wap"));

    }
    public void query3() throws ParseException{
    	Iterator<Bar> bars = getData("QQQ");
    	while(bars.hasNext()){
    		System.out.println(bars.next());
    	}
    }
    
    < V> void printFirstRow(Rows<String, Long, V> rows ){
    	
        for (Row<String, Long, V> row2 : rows) {
    		ColumnSlice<Long, V> slice = row2.getColumnSlice();
    		for (HColumn<Long, V> column : slice.getColumns()) {
    			Long secs = column.getName();
    			Date d = new Date(secs.longValue()*1000);
    		    System.out.println(d + " " + column.getValue());
    		}            
        }    	
    }
    /**
     * 
     * @param symbol
     * @return
     * @throws ParseException 
     */
    public Iterator<Bar> getData(final String symbol) throws ParseException{

    	return new Iterator<Bar>(){
    		final HashMap<String, List<HColumn<Long, Double>>> priceData = getPriceHistoricalData(symbol);
    		final HashMap<String, List<HColumn<Long, Long>>> volData = getHistoricalData(symbol);
    		int count = 0;
			@Override
			public boolean hasNext() {
				return count < volData.get(symbol + ":vol" ).size();
			}

			@Override
			public Bar next() {
				final Bar bar = new Bar();
				bar.symbol = symbol;
				bar.close = priceData.get(symbol + ":close").get(count).getValue().doubleValue();
				bar.open = priceData.get(symbol + ":open").get(count).getValue().doubleValue();
				bar.high = priceData.get(symbol + ":high").get(count).getValue().doubleValue();
				bar.low  = priceData.get(symbol + ":low").get(count).getValue().doubleValue();
				bar.wap = priceData.get(symbol + ":wap").get(count).getValue().doubleValue();
				bar.tradeCount = volData.get(symbol + ":tradeCount").get(count).getValue().intValue();
				bar.volume = volData.get(symbol + ":vol").get(count).getValue().longValue();
				bar.originalTime = volData.get(symbol + ":vol").get(count).getName().longValue();
				count++;
				return bar;
			}

			@Override
			public void remove() {
				// TODO Auto-generated method stub
				
			}
    		
    	};
    }
    /**
     * Get the price data 
     * @param symbol
     * @return
     * @throws ParseException
     */
    public HashMap<String, List<HColumn<Long, Double>>> getPriceHistoricalData(final String symbol) throws ParseException{
    	final HashMap<String, List<HColumn<Long, Double>>> result = new HashMap<String, List<HColumn<Long, Double>>>();
        RangeSlicesQuery<String, Long, Double> priceQuery = HFactory.createRangeSlicesQuery(keyspace, stringSerializer, longSerializer, doubleSerializer);
        priceQuery.setColumnFamily("bar5sec");
        final Long start = Long.valueOf(HistoricalDateManipulation.getTime("20111101 07:00:00"));
        final Long finish = Long.valueOf(HistoricalDateManipulation.getTime("20111101 16:00:00"));
        priceQuery.setRange(start, finish, false, 12*60*8);
        
        for(String key: priceKeys){
        	String rowKey = symbol + key;
            priceQuery.setKeys(rowKey, rowKey);
            List<HColumn<Long, Double>> column = priceQuery.execute().get().getByKey(rowKey).getColumnSlice().getColumns();
            result.put(rowKey, column);
        }
    	return result;
    }
    /**
     * Get the volume & tradeCount data
     * @param symbol
     * @return
     * @throws ParseException
     */
    public HashMap<String, List<HColumn<Long, Long>>> getHistoricalData(final String symbol) throws ParseException{
    	final HashMap<String, List<HColumn<Long, Long>>> result = new HashMap<String, List<HColumn<Long, Long>>>();
        SliceQuery<String, Long, Long> priceQuery = HFactory.createSliceQuery(keyspace, stringSerializer, longSerializer, longSerializer);
        priceQuery.setColumnFamily("bar5sec");
        final Long start = Long.valueOf(HistoricalDateManipulation.getTime("20111101 07:00:00"));
        final Long finish = Long.valueOf(HistoricalDateManipulation.getTime("20111101 16:00:00"));
        priceQuery.setRange(start, finish, false, 12*60*8);
        
        for(String key: longKeys ){
        	String rowKey = symbol + key;
            priceQuery.setKey(rowKey);
            List<HColumn<Long, Long>> column = priceQuery.execute().get().getColumns();
            result.put(rowKey, column);
        }
    	return result;
    }
    /**
	 * @param sym
	 * @param row2
	 */
	protected <V> void printColumns( Row<String, Long, V> closeRow, Row<String, Long, V> wapRow ) {
		if(closeRow == null || wapRow == null) return;
		System.out.println("Columns for :" + closeRow.getKey() + " " + wapRow.getKey());
		
		ColumnSlice<Long, V> closeSlice = closeRow.getColumnSlice();
		ColumnSlice<Long, V> wapSlice = wapRow.getColumnSlice();
		List<HColumn<Long, V>> closeVals = closeSlice.getColumns();
		List<HColumn<Long, V>> wapVals = wapSlice.getColumns();
		int size = closeVals.size();
		for(int i = 0; i < size; i++){
			System.out.println("Close: " + closeVals.get(i) + " Wap: " + wapVals.get(i)); 
		}
	}
   
}
