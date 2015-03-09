package edu.unm.missingisirs.form;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public class FileUpload {
	
	private List<MultipartFile> isirsFiles;
	 
    public List<MultipartFile> getFiles() {
        return isirsFiles;
    }
 
    public void setFiles(List<MultipartFile> files) {
        this.isirsFiles = files;
    }

}
