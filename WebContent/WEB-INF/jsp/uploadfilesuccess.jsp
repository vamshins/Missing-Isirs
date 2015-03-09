<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<html>
<head>
<title>Process ISIRS</title>
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

		<h1>Process ISIRS</h1>
		<h2>Files uploaded:</h2>
		<ol>
			<c:forEach items="${files}" var="file">
           - ${file} <br>
			</c:forEach>
		</ol>
		<form method="post" action="processfiles" modelAttribute="processForm"
			enctype="multipart/form-data">
			<table id="fileTable">
				<tr>
					<td>Enter the Aid Year</td>
					<td><input type="text" name="aidYear" /></td>
				</tr>
			</table>
			<br /> 
			<input type="submit" value="Process" /> 
			<a href="http://localhost:8080/MissingISIRS/upload">
			<input type="button" value="Go Back" /></a> <br /> <br /> <br />
		</form>

	</div>
</body>
</html>