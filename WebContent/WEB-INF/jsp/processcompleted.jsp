<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ page
	import="edu.unm.missingisirs.constants.Constants, java.util.List, java.util.Iterator"%>
<html>
<head>
<title>Reports</title>
<style type="text/css">
body {
	background-image:
		url('http://cdn3.crunchify.com/wp-content/uploads/2013/03/Crunchify.bg_.300.png');
}
</style>
</head>

<body>
	<br>
	<br>
	<div align="center">

		<h1>Reports</h1>
		<h3>Click on the file names to download.</h3>

		<%
			List<String> list = Constants.getReportFilesList();
		%>
		<a href="downloadReportFile"> 
		<%
 			out.println(list.get(0).substring(list.get(0).lastIndexOf('/')+1));
 		%>
		</a> <br>
		<a href="downloadIsdaFile"> 
		<%
 			out.println(list.get(1).substring(list.get(1).lastIndexOf('/')+1));
 		%>
		</a>

	</div>
</body>
</html>