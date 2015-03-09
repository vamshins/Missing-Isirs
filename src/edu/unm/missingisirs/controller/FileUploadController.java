package edu.unm.missingisirs.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import edu.unm.missingisirs.beans.IsirsBean;
import edu.unm.missingisirs.form.FileUpload;
import edu.unm.missingisirs.utils.IsirsUtil;

@Controller
public class FileUploadController {
	@Value("${uploadServerPath}")
	private String uploadServerPath;

	@Value("${username}")
	private String username;

	@Value("${password}")
	private String password;

	@RequestMapping(value = "/upload", method = RequestMethod.GET)
	public String uploadFilesDisplayForm() {
		return "uploadfile";
	}

	@RequestMapping(value = "/savefiles", method = RequestMethod.POST)
	public String isirsSave(@ModelAttribute("uploadForm") FileUpload uploadForm, Model map) throws IllegalStateException, IOException {
		// String saveDirectory = "E:/ISIRS/";
		String saveDirectory = this.uploadServerPath;

		List<MultipartFile> isirsFiles = uploadForm.getFiles();

		List<String> fileNames = new ArrayList<String>();

		if (null != isirsFiles && isirsFiles.size() > 0) {
			for (MultipartFile multipartFile : isirsFiles) {

				String fileName = multipartFile.getOriginalFilename();
				if (!"".equalsIgnoreCase(fileName)) {
					// Handle file content - multipartFile.getInputStream()
					multipartFile.transferTo(new File(saveDirectory + fileName));
					fileNames.add(fileName);
				}
			}
		}

		map.addAttribute("files", fileNames);
		return "uploadfilesuccess";
	}

	@RequestMapping(value = "/processfiles", method = RequestMethod.POST)
	public String isirsProcess(@ModelAttribute("processForm") String aidYear, Model mapprocess) throws IllegalStateException, IOException {
		// String saveDirectory = "E:/ISIRS/";
		IsirsBean ib = new IsirsBean();
		String saveDirectory = this.uploadServerPath;
		System.out.println(saveDirectory);
		ib.listBySsnMap = IsirsUtil.loadHashMap(saveDirectory);

		try {

			// Finds the fafsas that are currently in suspense
			// (ROTIDEN) and puts that info into a HashMap
			IsirsUtil.findFafsaTransactionsInSuspense(ib.fafsaTransactionsInSuspenseBySsnMap, aidYear, username, password);

			IsirsUtil.findFafsasToLoad(ib.listBySsnMap, ib.fafsaTransToLoadList, ib.fafsaTransToNotLoadList, ib.fafsaTransKidsNotInDB, aidYear, username, password,
					ib.fafsasFromIsirFilesThatAreAlreadyInSuspenseList, ib.fafsaTransactionsInSuspenseBySsnMap);

			IsirsUtil.findFafsaTransactionsThatHaveNeverBeenLoaded(ib.listBySsnMap, ib.fafsaTransToLoadList, ib.fafsaTransKidsNotInDB, ib.fafsasThatHaveNeverBeenLoadedList, aidYear, username,
					password);

			IsirsUtil.printReportAndFileToLoad(ib.fafsaTransToLoadList, ib.fafsaTransToNotLoadList, ib.fafsaTransToNotLoadLessList, ib.fafsaTransKidsNotInDB, username,
					ib.fafsasThatHaveNeverBeenLoadedList, ib.fafsasFromIsirFilesThatAreAlreadyInSuspenseList, saveDirectory);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return "uploadfilesuccess";
	}
}
