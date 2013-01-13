<%@page import="java.text.DecimalFormat"%>
<%@page import="java.util.Iterator"%>
<%@page import="com.davehoag.ib.util.HistoricalDateManipulation"%>
<%@page import="com.davehoag.ib.dataTypes.*"
		import="com.davehoag.ib.*"
%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Historical Chart</title>
  <script type="text/javascript" src="https://www.google.com/jsapi">
  <!-- variable is key to get javascript to actually work - doesnt do anything -->
  var test;
  </script>
  <script type="text/javascript">
    google.load('visualization', '1', {packages: ['corechart']});
    var dataTable = [
                   ['Mon', 20, 28, 38, 45],
                   ['Tue', 31, 38, 55, 66],
                   ['Wed', 50, 55, 77, 80],
                   ['Thu', 77, 77, 66, 50],
                   ['Fri', 68, 66, 22, 15]
                 ]
  </script>
  <script type="text/javascript">
    function drawVisualization() {
        var data = google.visualization.arrayToDataTable( dataTable, true);

      var options = {
        legend:'none'
      };

      var chart = new google.visualization.CandlestickChart(document.getElementById('chart_div'));
      chart.draw(data, options);
    }
    google.setOnLoadCallback(drawVisualization);
  </script>

</head>
<body>
<a href="StockChart.jsp">Chart</a>
	<br />
	<form>
		Symbol:<input id="symbolTF" name="symbol">
		Start Date:<input id="startDateTF" name="start">
		End Date:<input id="endDateTF" name="end" >
		<input type="submit" id="drawChartBtn">
	</form><br />

<%String start = request.getParameter("start");
  String end = request.getParameter("end");
  String symbol =  request.getParameter("symbol");
	final DecimalFormat df = new DecimalFormat("#.##");
  if(start == null && symbol.length() > 0){
  	long date = CassandraDao.getInstance().findMostRecentDate(symbol, "bar1day");
  	if(date == 0){
  		out.println("No data found for " + symbol);
  	} else { 
  		out.println(symbol + " most recent record: " + HistoricalDateManipulation.getDateAsStr(date));
  	}
  }
  if(start != null && start.trim().length() > 0 && end != null && symbol != null){
	  BarIterator bars = CassandraDao.getInstance().getData(symbol, start.trim() + " 00:00:00", end + " 18:00:00", "bar1day");
		out.println("<script> dataTable = ["); 
//		for(Bar aBar: bars){
//			out.println("['" + aBar.getTime() + "'," + df.format(aBar.low) + ", "+ 
//				df.format(aBar.open) +","+ df.format(aBar.close) +","+ df.format(aBar.high) +"]");
//		}
out.println("['test', 23.32, 34.23, 35, 40.3]");
		out.println("] </script>");
  }
  %>
  <br />
  <div id="chart_div" style="width: 900px; height: 500px;"></div>
  
</body>
</html>