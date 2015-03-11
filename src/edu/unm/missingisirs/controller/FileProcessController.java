package edu.unm.missingisirs.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import edu.unm.missingisirs.beans.IsirsBean;
import edu.unm.missingisirs.constants.Constants;
import edu.unm.missingisirs.form.FileProcess;
import edu.unm.missingisirs.form.FileUpload;
import edu.unm.missingisirs.utils.IsirsUtil;

@Controller
public class FileProcessController {
	
	private static final Logger logger = Logger.getLogger(FileProcessController.class);
	
	@Value("${uploadServerPath}")
	private String uploadServerPath;

	@Value("${username}")
	private String username;

	@Value("${password}")
	private String password;

	@RequestMapping(value = "/upload", method = RequestMethod.GET)
	public String uploadFilesDisplayForm() {
		logger.info("Loading upload page");
		return "uploadfile";
	}

	@RequestMapping(value = "/savefiles", method = RequestMethod.POST)
	public String isirsSave(@ModelAttribute("uploadForm") FileUpload uploadForm, Model map) throws IllegalStateException, IOException {
		// String saveDirectory = "E:/ISIRS/";
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		Constants.serverUploadSubDirPath = this.uploadServerPath + timeStamp;

		logger.info("Creating the directory to upload files on the server. Directory path: " + Constants.serverUploadSubDirPath);
		boolean success = (new File(Constants.serverUploadSubDirPath)).mkdir();
		
		if (success) {
			logger.info("Directory to upload files created successfully.");
			List<MultipartFile> isirsFiles = uploadForm.getFiles();
			List<String> fileNames = new ArrayList<String>();

			if (null != isirsFiles && isirsFiles.size() > 0) {
				for (MultipartFile multipartFile : isirsFiles) {

					String fileName = multipartFile.getOriginalFilename();
					if (!"".equalsIgnoreCase(fileName)) {
						// Handle file content - multipartFile.getInputStream()
						multipartFile.transferTo(new File(Constants.serverUploadSubDirPath + "/"+fileName));
						logger.info("Copied file \""+ Constants.serverUploadSubDirPath + "/"+fileName + "\" successfully");
						fileNames.add(fileName);
					}
				}
			}

			map.addAttribute("files", fileNames);
		}
		return "uploadfilesuccess";
	}

	@RequestMapping(value = "/processfiles", method = RequestMethod.POST)
	public String isirsProcess(@ModelAttribute("processForm") FileProcess fileProcess, Model mapprocess) throws IllegalStateException, IOException {
		// String saveDirectory = "E:/ISIRS/";
		IsirsBean ib = new IsirsBean();
		
		logger.info("Processing request submitted. Processing files in the director: " + Constants.serverUploadSubDirPath);
		ib.listBySsnMap = IsirsUtil.loadHashMap(Constants.serverUploadSubDirPath);
		String aidYear = fileProcess.getAidYear();
		logger.info("Aid Year: " + aidYear);
//		System.exit(1);
		
		Constants.reportFilesList.clear();
		try {

			// Finds the fafsas that are currently in suspense
			// (ROTIDEN) and puts that info into a HashMap
			IsirsUtil.findFafsaTransactionsInSuspense(ib.fafsaTransactionsInSuspenseBySsnMap, aidYear, username, password);

			IsirsUtil.findFafsasToLoad(ib.listBySsnMap, ib.fafsaTransToLoadList, ib.fafsaTransToNotLoadList, ib.fafsaTransKidsNotInDB, aidYear,
					username, password, ib.fafsasFromIsirFilesThatAreAlreadyInSuspenseList, ib.fafsaTransactionsInSuspenseBySsnMap);

			IsirsUtil.findFafsaTransactionsThatHaveNeverBeenLoaded(ib.listBySsnMap, ib.fafsaTransToLoadList, ib.fafsaTransKidsNotInDB,
					ib.fafsasThatHaveNeverBeenLoadedList, aidYear, username, password);

			Constants.reportFilesList = IsirsUtil.printReportAndFileToLoad(ib.fafsaTransToLoadList, ib.fafsaTransToNotLoadList, ib.fafsaTransToNotLoadLessList,
					ib.fafsaTransKidsNotInDB, username, ib.fafsasThatHaveNeverBeenLoadedList, ib.fafsasFromIsirFilesThatAreAlreadyInSuspenseList,
					Constants.serverUploadSubDirPath, aidYear);
			
			mapprocess.addAttribute("reportFilesList", Constants.reportFilesList);
		} catch (Exception e) {
			e.printStackTrace();
			// TODO - throw the exception to view and show it in alert.
		}
		
		/*Constants.reportFilesList.add("C:/ISIRS/20150310_142457/report/reportForISIR_stirlingcrow.txt");
		Constants.reportFilesList.add("C:/ISIRS/20150310_142457/report/IDSA16OP.txt");*/
		
		return "processcompleted";
	}
	
	/**
	 * Size of a byte buffer to read/write file
	 */
	private static final int BUFFER_SIZE = 4096;

	/**
	 * Method for handling file download request from client
	 */
	@RequestMapping(value = "/downloadReportFile", method = RequestMethod.GET)
	public void reportDownload(HttpServletRequest request, HttpServletResponse response) throws IOException {

		ServletContext context = request.getServletContext();

		// construct the complete absolute path of the file
		String fullPath = Constants.reportFilesList.get(0);
		File downloadFile = new File(fullPath);
		FileInputStream inputStream = new FileInputStream(downloadFile);

		// get MIME type of the file
		String mimeType = context.getMimeType(fullPath);
		if (mimeType == null) {
			// set to binary type if MIME mapping not found
			mimeType = "application/octet-stream";
		}
		System.out.println("MIME type: " + mimeType);

		// set content attributes for the response
		response.setContentType(mimeType);
		response.setContentLength((int) downloadFile.length());

		// set headers for the response
		String headerKey = "Content-Disposition";
		String headerValue = String.format("attachment; filename=\"%s\"", downloadFile.getName());
		response.setHeader(headerKey, headerValue);

		// get output stream of the response
		OutputStream outStream = response.getOutputStream();

		byte[] buffer = new byte[BUFFER_SIZE];
		int bytesRead = -1;

		// write bytes read from the input stream into the output stream
		while ((bytesRead = inputStream.read(buffer)) != -1) {
			outStream.write(buffer, 0, bytesRead);
		}

		inputStream.close();
		outStream.close();

	}
	
	@RequestMapping(value = "/downloadIsdaFile", method = RequestMethod.GET)
	public void isdaDownload(HttpServletRequest request, HttpServletResponse response) throws IOException {

		ServletContext context = request.getServletContext();

		// construct the complete absolute path of the file
		String fullPath = Constants.reportFilesList.get(1);
		File downloadFile = new File(fullPath);
		FileInputStream inputStream = new FileInputStream(downloadFile);

		// get MIME type of the file
		String mimeType = context.getMimeType(fullPath);
		if (mimeType == null) {
			// set to binary type if MIME mapping not found
			mimeType = "application/octet-stream";
		}
		System.out.println("MIME type: " + mimeType);

		// set content attributes for the response
		response.setContentType(mimeType);
		response.setContentLength((int) downloadFile.length());

		// set headers for the response
		String headerKey = "Content-Disposition";
		String headerValue = String.format("attachment; filename=\"%s\"", downloadFile.getName());
		response.setHeader(headerKey, headerValue);

		// get output stream of the response
		OutputStream outStream = response.getOutputStream();

		byte[] buffer = new byte[BUFFER_SIZE];
		int bytesRead = -1;

		// write bytes read from the input stream into the output stream
		while ((bytesRead = inputStream.read(buffer)) != -1) {
			outStream.write(buffer, 0, bytesRead);
		}

		inputStream.close();
		outStream.close();

	}
}
