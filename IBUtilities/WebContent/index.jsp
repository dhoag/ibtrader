<%@page import="com.davehoag.ib.dataTypes.StockContract"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Insert title here</title>
</head>
<body>
<% java.util.Date d = new java.util.Date();
	com.davehoag.ib.dataTypes.StockContract cont = new StockContract("IBM");
%>
<h1>
Today's date is <%= d.toString() %> and this jsp page worked!
<br/>
And its dynamic. <%= cont.toString() %>
</h1>
</body>
</html>