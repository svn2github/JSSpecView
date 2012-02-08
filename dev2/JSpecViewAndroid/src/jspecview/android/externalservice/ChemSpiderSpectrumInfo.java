package jspecview.android.externalservice;

import java.util.Date;

public class ChemSpiderSpectrumInfo {
	private int spectrumId;
	private int chemSpiderId;
	private String spectrumType;
	private String fileName;
	private String comments;
	private String originalUrl;
	private Date submittedDate;
	
	public int getSpectrumId() {
		return spectrumId;
	}
	public void setSpectrumId(int spectrumId) {
		this.spectrumId = spectrumId;
	}
	public int getChemSpiderId() {
		return chemSpiderId;
	}
	public void setChemSpiderId(int chemSpiderId) {
		this.chemSpiderId = chemSpiderId;
	}
	public String getSpectrumType() {
		return spectrumType;
	}
	public void setSpectrumType(String spectrumType) {
		this.spectrumType = spectrumType;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}
	public String getOriginalUrl() {
		return originalUrl;
	}
	public void setOriginalUrl(String originalUrl) {
		this.originalUrl = originalUrl;
	}
	public Date getSubmittedDate() {
		return submittedDate;
	}
	public void setSubmittedDate(Date submittedDate) {
		this.submittedDate = submittedDate;
	}

	public ChemSpiderSpectrumInfo copy(){
		
		ChemSpiderSpectrumInfo specInfo = new ChemSpiderSpectrumInfo();
		specInfo.setChemSpiderId(chemSpiderId);
		specInfo.setComments(comments);
		specInfo.setFileName(fileName);
		specInfo.setOriginalUrl(originalUrl);
		specInfo.setSpectrumId(spectrumId);
		specInfo.setSpectrumType(spectrumType);
		specInfo.setSubmittedDate(submittedDate);
		
		return specInfo;
	}
}
