<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <meta http-equiv="content-type" content="text/html; charset=utf-8"/>
  <title>
    Google API Sample
  </title>
  <script type="text/javascript" src="https://www.google.com/jsapi">
  <!-- variable is key to get javascript to actually work - doesnt do anything -->
  var test;
  </script>
  <script type="text/javascript">
    google.load('visualization', '1', {packages: ['corechart']});
  </script>
  <script type="text/javascript">
    function drawVisualization() {
      var data = google.visualization.arrayToDataTable([
        ['Mon', 20, 28, 38, 45],
        ['Tue', 31, 38, 55, 66],
        ['Wed', 50, 55, 77, 80],
        ['Thu', 77, 77, 66, 50],
        ['Fri', 68, 66, 22, 15]
        // Treat first row as data as well.
      ], true);

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
  <div id="chart_div" style="width: 900px; height: 500px;"></div>
  <% java.util.Date d = new java.util.Date(); %>
Today's date is <%= d.toString() %> and this jsp page worked! 
  
And its dynamic.
</body>
</html>
