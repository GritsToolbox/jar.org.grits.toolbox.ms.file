package org.grits.toolbox.ms.file;

import org.grits.toolbox.ms.file.reader.IMSFileReader;

/**
 * 
 * @author sena
 *
 */
public class MSFile {
	String fileName;        // full path
	String version;
	FileCategory category;        
	String experimentType;  // Method.MS_TYPES
	IMSFileReader reader;
	
	/**
	 * @return the fileName
	 */
	public String getFileName() {
		return fileName;
	}
	/**
	 * @param fileName the fileName to set
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}
	/**
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}
	/**
	 * @return the category
	 */
	public FileCategory getCategory() {
		return category;
	}
	/**
	 * @param category the category to set
	 */
	public void setCategory(FileCategory category) {
		this.category = category;
	}
	/**
	 * @return the experimentType
	 */
	public String getExperimentType() {
		return experimentType;
	}
	/**
	 * @param experimentType the experimentType to set
	 */
	public void setExperimentType(String experimentType) {
		this.experimentType = experimentType;
	}
	
	/**
	 * 
	 * @return the reader
	 */
	public IMSFileReader getReader() {
		return reader;
	}
	
	/**
	 * 
	 * @param reader the reader to set
	 */
	public void setReader(IMSFileReader reader) {
		this.reader = reader;
	}
}
