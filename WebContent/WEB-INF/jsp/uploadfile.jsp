<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<title>Missing ISIRS - Upload Multiple Files</title>
<script
	src="http://ajax.googleapis.com/ajax/libs/jquery/1.8.2/jquery.min.js"></script>
<script>
	$(document)
			.ready(
					function() {
						//add more file components if Add is clicked
						$('#addFile')
								.click(
										function() {
											var fileIndex = $('#fileTable tr')
													.children().length;
											$('#fileTable')
													.append(
															'<tr><td>'
																	+ '   <input type="file" name="files['+ fileIndex +']" />'
																	+ '</td></tr>');
										});

					});
</script>
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
		<h1>Upload Files</h1>

		<form method="post" action="savefiles"
			modelAttribute="uploadForm" enctype="multipart/form-data">

			<p>Select files to upload. Press Add button to add more file
				inputs.</p>

			<table id="fileTable">
				<tr>
					<td><input name="files[0]" type="file" /></td>
				</tr>
				<tr>
					<td><input name="files[1]" type="file" /></td>
				</tr>
			</table>
			<br />
			<input type="submit" value="Upload" />
			<input id="addFile" type="button" value="Add File" />
		</form>

		<br />
	</div>
</body>
</html>