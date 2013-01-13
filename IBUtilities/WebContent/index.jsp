<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
 <jsp:useBean id="chartData" type="com.davehoag.ib.chart.ChartBean" class="com.davehoag.ib.chart.ChartBean" scope="session">  
</jsp:useBean>
<jsp:setProperty name="chartData" property="*" />  
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
                 ];
  </script>
  <script type="text/javascript">
    function drawVisualization() {
        var data = google.visualization.arrayToDataTable( dataTable, true);

      var options = {
    		  colors: ['black'],
    		  candlestick: 
    		  	{ risingColor: { stroke:'green' } ,
    		   	  fallingColor: { stroke:'red', fill:'red' },
    		   	  hollowIsRising: true
    		  	}, 
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
<%! private int accessCount = 0; %>
Accesses to page since server reboot: 
<%= ++accessCount %>
	<br />
	<form>
		Symbol:<input id="symbolTF" name="symbol" value=<%= request.getParameter("symbol") %> />
		Start Date:<input id="startDateTF" name="start" value=<%= request.getParameter("start") %> />
		End Date:<input id="endDateTF" name="end" value=<%= request.getParameter("end") %> />
		<input type="checkbox" name="fiveSecData" id="fiveSecCB"  />
		<input type="submit" id="drawChartBtn" />
	</form><br />
	
<%	
chartData.setDataTable(out, session);
  %>
  <br />
  <div id="chart_div" style="width: 100%; height: 500px;"></div>
  
</body>
</html>