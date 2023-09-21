package org.grits.toolbox.ms.file.reader;

import java.util.List;
import java.util.Map;

import org.grits.toolbox.ms.file.MSFile;
import org.grits.toolbox.ms.file.scan.data.ScanView;
import org.grits.toolbox.ms.om.data.Scan;

public interface IMSAnnotationFileReader extends IMSFileReader {
	/**
	 * @param file MS file to be read
	 * @return List of {@link org.grits.toolbox.ms.om.Scan} from the given MS file
	 */
	List<Scan> readMSFile (MSFile file);
	
	/**
	 * 
	 * @param file MS file to be read
	 * @param scanNumber scan number to be read
	 * @return List of {@link org.grits.toolbox.ms.om.Scan} from the given MS file for the given scan number
	 */
	List<Scan> readMSFile (MSFile file, int scanNumber);
	
	/**
	 * 
	 * @param file MS file to be read
	 * @param scanNumber scan number to be read
	 * @param subScanMap list of all scans and their corresponding sub scans
	 * @return List of {@link org.grits.toolbox.ms.om.Scan} from the given MS file for the given scan number
	 */
	List<Scan> readMSFile (MSFile file, int scanNumber, Map<Integer, List<Integer>> subScanMap);
	
	/**
	 * readMSFile: general method to read an mzXML file.
	 * 
	 * NOTE: will not currently read the entire contents of an MS file. You have to specify at least one of
	 * msLevel, parentScanNum, or scanNum
	 * 
	 * @param file  MS file to be read
	 * @param msLevel  the MS level of scans to load from MS file. Use -1 if to be ignored.
	 * @param parentScanNum  the scan number to be read, along with its sub-scans. Use -1 if to be ignored.
	 * @param scanNum  the scan number to be read, ignoring all other data.  Use -1 if to be ignored.
	 * @return List<Scan> a list of {@link org.grits.toolbox.ms.om.data.Scan} objects
	 * 
	 */
	List<Scan> readMSFile(MSFile file, int msLevel, int parentScanNum, int scanNum);
		
	/**
	 * 
	 * @param file MS file to be read
	 * @param scanNumber parent scan number
	 * @return List of scan numbers from the given MS file for the given parent scan number
	 */
	List<Integer> getScanList (MSFile file, int scanNumber);
	
	/**
	 * 
	 * @param file MS file to be read
	 * @return a map of scans to their corresponding sub-scan numbers
	 */
	Map<Integer, List<Integer>> readMSFileForSubscans (MSFile file);
	
	/**
	 * getMaxScanNumber: returns the last scan number of the MS file.
	 *  
	 * @param file MS file to be read
	 * @return value of last scan number
	 */
	Integer getMaxScanNumber (MSFile file);
	
	/**
	 * returns the lowest scan number
	 * @param file MS file to be read
	 * @return value of the first scan number
	 */
	Integer getMinScanNumber(MSFile file);
	
	/**
	 * return the minimum MS Level from the file
	 * 
	 * @param file MS file to be read
	 * @return minimum MS Level
	 */
	Integer getMinMSLevel(MSFile file);
	
	/**
	 * checks whether the file has MS1 scan
	 * 
	 * @param file MS file to be read
	 * @return true if there is MS1 scan, false otherwise
	 */
	boolean hasMS1Scan(MSFile file);
	
	/**
	 * counts the number of MS1 scans in the file
	 * 
	 * @param file MS file to be read
	 * @return number of MS1 scans
	 */
	public int getNumMS1Scans(MSFile file);

	/**
	 * counts the number of MS2 scans in the file
	 * 
	 * @param file MS file to be read
	 * @return number of MS2 scans
	 */
	public int getNumMS2Scans(MSFile file);

	/**
	 * readMSFile for viewing Scan Hierarchy: general method to read an mzXML file.
	 * 
	 * NOTE: will not currently read the entire contents of an MS file. You have to specify at least one of
	 * msLevel, parentScanNum, or scanNum
	 * 
	 * @param file  MS file to be read
	 * @param msLevel  the MS level of scans to load from MS file. Use -1 if to be ignored.
	 * @param parentScanNum  the scan number to be read, along with its sub-scans. Use -1 if to be ignored.
	 * @param scanNum  the scan number to be read, ignoring all other data.  Use -1 if to be ignored.
	 * @return List<ScanView> a list of {@link org.grits.toolbox.ms.file.scan.data.ScanView} objects
	 * 
	 */
	List<ScanView> readMSFileForView(MSFile file, int msLevel, int parentScanNum, int scanNum);
	
	/**
	 * return the first MS1 scan in the file, if present
	 * 
	 * @param file
	 * @return
	 */
	public Scan getFirstMS1Scan(MSFile file);
}
