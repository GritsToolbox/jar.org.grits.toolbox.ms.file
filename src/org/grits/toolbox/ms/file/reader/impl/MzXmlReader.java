package org.grits.toolbox.ms.file.reader.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.grits.toolbox.ms.file.MSFile;
import org.grits.toolbox.ms.file.reader.IMSAnnotationFileReader;
import org.grits.toolbox.ms.file.scan.data.ScanView;
import org.grits.toolbox.ms.om.data.Method;
import org.grits.toolbox.ms.om.data.Peak;
import org.grits.toolbox.ms.om.data.Scan;
import org.grits.toolbox.widgets.tools.NotifyingProcess;
import org.systemsbiology.jrap.grits.stax.MSXMLParser;
import org.systemsbiology.jrap.grits.stax.ScanHeader;

public class MzXmlReader extends NotifyingProcess implements IMSAnnotationFileReader{
	// log4J Logger
	private static final Logger logger = Logger.getLogger(MzXmlReader.class);

	/**
	 * {@inheritDoc}
	 * used for Direct infusion and TIM experiments, will return null for other types (LC, MS Profile)
	 */
	@Override
	public List<Scan> readMSFile(MSFile file) {
		if( file.getExperimentType().equals(Method.MS_TYPE_INFUSION) ) {
			return readMzXmlFileForDirectInfusion(file.getFileName());
		} else if ( file.getExperimentType().equals(Method.MS_TYPE_TIM) ) {
			return readMzXmlFileForTIM(file.getFileName());
		} 
		return null;
	}

	/**
	 * {@inheritDoc}
	 * used for LC/MS and MSPRofile experiments, will return null for other types (direct infusion, TIM)
	 */
	@Override
	public List<Scan> readMSFile(MSFile file, int scanNumber) {
		return readMSFile(file, scanNumber, null);
	}
	
	/**
	 * {@inheritDoc}
	 * used for LC/MS and MSPRofile experiments, will return null for other types (direct infusion, TIM)
	 */
	@Override
	public List<Scan> readMSFile(MSFile file, int scanNumber, Map<Integer, List<Integer>> subScanMap) {
		if( file.getExperimentType().equals(Method.MS_TYPE_LC) ) {
			return readMzXmlFileForLCMSMS(file.getFileName(), scanNumber, subScanMap);
		} else if (file.getExperimentType().equals(Method.MS_TYPE_MSPROFILE)) {
			return readMzXmlFileForMSProfile(file.getFileName(), scanNumber);
		} else if( file.getExperimentType().equals(Method.MS_TYPE_INFUSION) ) {
			return readMzXmlFileForDirectInfusion(file.getFileName());
		} else if ( file.getExperimentType().equals(Method.MS_TYPE_TIM) ) {
			return readMzXmlFileForTIM(file.getFileName());
		} 
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Integer> getScanList(MSFile file, int scanNumber) {
		return getScanList(file.getFileName(), scanNumber);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer getMaxScanNumber(MSFile file) {
		return MzXmlReader.getMaxScanNumber(file.getFileName());
	}

	@Override
	public Scan getFirstMS1Scan(MSFile file) {
		Scan firstScan = null;
		File mzXMLFile = new File(file.getFileName());
		try {
			MSXMLParser parser = new MSXMLParser(mzXMLFile.getAbsolutePath());
			if ( parser != null ) {
				for( int i = 1; firstScan == null && i < parser.getMaxScanNumber() + 1; i++ ) {
					ScanHeader header = parser.rapHeader(i);
					if ( header != null && header.getMsLevel() == 1 ) {
						org.systemsbiology.jrap.grits.stax.Scan s = parser.rap(i);
						if( s.getMassIntensityList() != null && s.getMassIntensityList()[0].length > 0 ) {
							firstScan = getScan(s, -1);
							return firstScan;
						}
					}
				}
			}

		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return null;
	}
	
	
	/**
	 * getMaxScanNumber returns the last scan number of the mzXML file.
	 *  
	 * @param fileName  full path to mzXML file
	 * @return int value of last scan number
	 * 
	 */
	public static int getMaxScanNumber( String fileName ) {
		File mzXMLFile = new File(fileName);
		try {
			MSXMLParser parser = new MSXMLParser(mzXMLFile.getAbsolutePath());
			return parser.getMaxScanNumber();

		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return -1;
	}

	/**
	 * readMzXMLFile: general method to read an mzXML file.
	 * 
	 * NOTE: will not currently read the entire contents of an mzXML file. You have to specify at least one of
	 * msLevel, parentScanNum, or scanNum
	 * 
	 * @param fileName  full path to mzXML file
	 * @param msLevel  the MS level of scans to load from mzXML. Use -1 if to be ignored.
	 * @param parentScanNum  the scan number to be read, along with its sub-scans. Use -1 if to be ignored.
	 * @param scanNum  the scan number to be read, ignoring all other data.  Use -1 if to be ignored.
	 * @return List<Scan> a list of org.grits.toolbox.ms.om.data.Scan objects
	 * 
	 */

	public List<Scan> readMzXmlFile(String fileName, int msLevel, int parentScanNum, int scanNum) {
		File mzXMLFile = new File(fileName);
		try {			
			MSXMLParser parser = new MSXMLParser(mzXMLFile.getAbsolutePath());
			updateListeners("Reading XML file", -1);
			List<Scan> scans = getScanData(parser, msLevel, parentScanNum, scanNum);
			if( scans != null ) {
				Collections.sort(scans);
			}
			if( scans.isEmpty() ) {
				if (!isCanceled())
					updateErrorListener("Warning: no scan data read from MS file. The file may be invalid or incorrect type.");
			}
			return scans;
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return null;
	}

	/**
	 * readMzXmlFileForTIM: reads a TIM-based mzXML file.
	 * 
	 * NOTE: Because TIM data is all MS2 data, it will first create a phony MS1 scan (scan number 0)
	 * and then create phony subscans for all MS2 precursors.
	 * 
	 * @param fileName  full path to mzXML file
	 * @return List<Scan> a list of org.grits.toolbox.ms.om.data.Scan objects
	 * 
	 */	
	public List<Scan> readMzXmlFileForTIM(String fileName) {
		File mzXMLFile = new File(fileName);
		try {
			MSXMLParser parser = new MSXMLParser(mzXMLFile.getAbsolutePath());
			updateListeners("Reading XML file", -1);
			List<Scan> scans = addAllScansTIM(parser);
			if( scans != null ) {
				Collections.sort(scans);
			}
			if( scans.isEmpty() ) {
				if (!isCanceled())
					updateErrorListener("Warning: no scan data read from MS file. The file may be invalid or incorrect type.");
			}
			return scans;
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return null;
	}

	/**
	 * readMzXmlFileForDirectInfusion: reads a direct infusion based mzXML file.
	 * 
	 * NOTE: First identifies the first MS1 scan (typically scan number 1). It then populates the MS1 scan object 
	 * with all MS2 subscans.
	 * 
	 * @param fileName  full path to mzXML file
	 * @return List<Scan> a list of org.grits.toolbox.ms.om.data.Scan objects
	 * 
	 */		
	public List<Scan> readMzXmlFileForDirectInfusion(String fileName) {
		File mzXMLFile = new File(fileName);
		try {
			MSXMLParser parser = new MSXMLParser(mzXMLFile.getAbsolutePath());
			updateListeners("Reading XML file", -1);
			List<Scan> scans = addAllScansDirectInfusion(parser);
			if( scans != null ) {
				Collections.sort(scans);
			}
			if( scans.isEmpty() ) {
				if (!isCanceled())
					updateErrorListener("Warning: no scan data read from MS file. The file may be invalid or incorrect type.");
			}
			return scans;
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return null;
	}

	/**
	 * readMzXmlFileForLCMSMS: reads a LC-MS/MS based mzXML file.
	 * 
	 * NOTE: Reads the scan and subscan data for the specified scan number
	 * 
	 * @param fileName:  full path to mzXML file
	 * @param iScanNumber: the scan number to read from the mzXML file
	 * @return List<Scan>: a list of org.grits.toolbox.ms.om.data.Scan objects
	 * 
	 */		
	public List<Scan> readMzXmlFileForLCMSMS(String fileName, int iScanNumber) {
		return readMzXmlFileForLCMSMS(fileName, iScanNumber, null);
	}
	
	public List<Scan> readMzXmlFileForLCMSMS(String fileName, int scanNumber, Map<Integer, List<Integer>> subScanMap) {
		File mzXMLFile = new File(fileName);
		try {
			MSXMLParser parser = new MSXMLParser(mzXMLFile.getAbsolutePath());
			updateListeners("Reading XML file", -1);
			List<Scan> scans = addAllScansLCMSMS(parser, scanNumber, subScanMap);
			if( scans != null ) {
				Collections.sort(scans);
			}
			if( scans.isEmpty() ) {
				if (!isCanceled()) 
					updateErrorListener("Warning: no scan data read from MS file. The file may be invalid or incorrect type.");
			}
			return scans;
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return null;
	}

	/**
	 * readMzXmlFileForMSProfile: reads an MS Profile based mzXML file
	 * 
	 * NOTE: MS Profile is typically just MS1 scans. Read the scan info and peak list for the specified parentScanNum
	 * 
	 * @param fileName:  full path to mzXML file
	 * @param parentScanNum: the scan number to read from the mzXML file
	 * @return List<Scan>: a list of org.grits.toolbox.ms.om.data.Scan objects
	 * 
	 */			
	public List<Scan> readMzXmlFileForMSProfile(String fileName, int parentScanNum) {
		File mzXMLFile = new File(fileName);
		try {
			MSXMLParser parser = new MSXMLParser(mzXMLFile.getAbsolutePath());
			updateListeners("Reading XML file", -1);
			List<Scan> scans = addAllScansMSProfile(parser,  parentScanNum);
			if( scans != null ) {
				Collections.sort(scans);
			}
			if( scans.isEmpty() ) {
				if (!isCanceled())
					updateErrorListener("Warning: no scan data read from MS file. The file may be invalid or incorrect type.");
			}
			return scans;
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return null;
	}

	public boolean isValidMzXmlFile(String fileName) {
		try {
			MSXMLParser parser = new MSXMLParser(fileName, false);
			int t_info = parser.getScanCount();
			if ( t_info == 0) {
				// try sequential
				parser = new MSXMLParser(fileName, true);
				t_info = parser.getScanCount();
				if ( t_info == 0)
					return false;
			}
			return true;
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return false;
		}
	}

	private Peak getNewPrecursorPeak( ScanHeader _scanHeader, boolean _bUseTIC ) {
		double dMz = (double) _scanHeader.getPrecursorMz();
		Peak peak = new Peak();		
		if (_scanHeader.getPrecursorCharge() == -1)
			peak.setPrecursorCharge(null);
		else
			peak.setPrecursorCharge(_scanHeader.getPrecursorCharge());

		peak.setMz(dMz);
		peak.setId(-1);
		if( _bUseTIC ) {
			peak.setIntensity((double) _scanHeader.getTotIonCurrent());
		} else {
			peak.setIntensity((double) _scanHeader.getPrecursorIntensity());
		}
		peak.setPrecursorIntensity((double) _scanHeader.getPrecursorIntensity());
		peak.setIsPrecursor(true);
		return peak;
	}

	private Peak findPeakInPeakList( List<Peak> _alPeaks, double _dPrecursorMz ) {
		double dMinDelta = Double.MAX_VALUE;
		Peak matchedPeak = null;
		for( Peak peak : _alPeaks ) {
			double delta = Math.abs(peak.getMz() - _dPrecursorMz);
			// find closest peak by mz 
			//			if( delta < dMinDelta ) {
			if( delta < 0.5 && delta < dMinDelta ) {
				dMinDelta = delta;
				matchedPeak = peak;
			}
		}
		return matchedPeak;		
	}

	private double getMostAbundantPeak( org.systemsbiology.jrap.grits.stax.Scan _scan) {
		if( _scan.getMassIntensityList() == null || _scan.getMassIntensityList().length == 0 ) {
			return 0;
		}

		double[][] scanPeaks = _scan.getMassIntensityList();
		double dMaxInt = Double.MIN_VALUE;
		for (int j = 0; j < scanPeaks[0].length; j++) {
			if ((double) scanPeaks[1][j] > dMaxInt)
				dMaxInt = (double) scanPeaks[1][j];
		}
		return dMaxInt;		
	}


	private List<Scan> addAllScansTIM(MSXMLParser parser) {
		int iStartScan = getFirstScanNumber(parser);
		if( iStartScan == -1 ) {
			return new ArrayList<>();
		}
		int iEndScan = parser.getMaxScanNumber();
		int iMinMSLevel = parser.rapHeader(iStartScan).getMsLevel(); // can't assume we have MS1 scans. Keep track of possible precursors
		if( iMinMSLevel == 1 ) { // TIM is direct infusion. If the first scan is MS1, then just run like DI
			return addAllScansDirectInfusion(parser);
		}
		Scan fullMSScan = null;
		Scan msScan = null;
		try {
			boolean flag = true;
			int precursorScanNum = 0;
			List<Scan> scans = new ArrayList<>();
			HashMap<Integer, Integer> lastParentOfEachLevel = new HashMap<Integer, Integer>();
			HashMap<Integer, Scan> msScanMap = new HashMap<Integer, Scan>();
			HashMap<Double, Peak> ms1 = new HashMap<Double, Peak>();

			List<Peak> ms1Peaks = new ArrayList<>();
			// if no full MS1 scan is present, then we create one
			fullMSScan = new Scan();
			// can we have scan num == 0???
			lastParentOfEachLevel.put(1, 0);

			fullMSScan.setMsLevel(1); // creating our own full MS1 scan
			fullMSScan.setPolarity(null);
			fullMSScan.setScanStart(-1.0d);
			fullMSScan.setScanEnd(-1.0d);
			fullMSScan.setScanNo(0);
			fullMSScan.setActivationMethode(null);
			fullMSScan.setRetentionTime(0.0d);
			msScanMap.put(fullMSScan.getScanNo(), fullMSScan);
			lastParentOfEachLevel.put(1, 0);
			double dLowMz = Double.MAX_VALUE;
			double dHighMz = Double.MIN_VALUE;

			for (int i = iStartScan; i <= iEndScan; i++) {
				if( isCanceled() ) {
					return new ArrayList<>();
				}
				if( (i%10) == 0 ) { // speed things up!
					if (!isCanceled())
						updateListeners("Reading XML file. Scan: " + i + " of " + iEndScan, i);
				}
				org.systemsbiology.jrap.grits.stax.Scan jrapScan = parser.rap(i);
				if( jrapScan == null ) {
					//if (!isCanceled())
					//	updateErrorListener("Call to getJrapScan for scan number " + i + " returned null. Skipping.");
					logger.debug("Call to getJrapScan for scan number " + i + " returned null. Skipping.");
					continue;
				}
				ScanHeader jrapScanHeader = jrapScan.getHeader();
				if (jrapScan.getMassIntensityList() == null || jrapScan.getMassIntensityList().length == 0) {
					logger.warn("Scan: " + i + " has empty peak list.");
					//					continue;
				}
				msScan = new Scan();
				double maxIntensity = getMostAbundantPeak(jrapScan);
				msScan.setMsLevel(jrapScanHeader.getMsLevel());
				lastParentOfEachLevel.put(jrapScanHeader.getMsLevel(), jrapScanHeader.getNum());
				String temp = jrapScanHeader.getPolarity();
				msScan.setPolarity(null);
				msScan.setIsCentroided( (jrapScanHeader.getCentroided() == 1) ); 
				if( temp != null && ! temp.equals("") ) {
					if ( temp.equals("+")) {
						flag = true;
					} else {
						flag = false;
					}					
					msScan.setPolarity(flag);
					// set the polarity of the phoney parent scan to the polarity of the first MS/MS w/ a polarity
					if( fullMSScan.getPolarity() == null ) {
						fullMSScan.setPolarity(flag);
					}
				}
				msScan.setMostAbundantPeak(maxIntensity);
				msScan.setScanStart((double) jrapScanHeader.getLowMz());
				msScan.setScanEnd((double) jrapScanHeader.getHighMz());
				msScan.setScanNo(jrapScanHeader.getNum());
				msScan.setActivationMethode(jrapScanHeader.getActivationMethod());
				msScan.setIsCentroided( (jrapScanHeader.getCentroided() == 1) ); 
				try {
					msScan.setRetentionTime(jrapScanHeader.getDoubleRetentionTime());
				} catch( Exception e1 ) {
					;
				}
				precursorScanNum = -1;
				if (jrapScanHeader.getPrecursorScanNum() != -1) {
					precursorScanNum = jrapScanHeader.getPrecursorScanNum();
				} else if (lastParentOfEachLevel.containsKey(msScan.getMsLevel() - 1)){
					precursorScanNum = lastParentOfEachLevel.get(msScan.getMsLevel() - 1);
				}
				msScan.setParentScan(precursorScanNum);

				Peak peak = null;
				if( msScan.getMsLevel() == 2 ) { // precursor peak is built on fly
					double dMz = (double) jrapScanHeader.getPrecursorMz();
					if( ms1.containsKey(dMz) ) {
						peak = ms1.get(dMz);
					} else {
						peak = getNewPrecursorPeak(jrapScanHeader, true);
						ms1.put(dMz, peak);
					}
					peak.setIsPrecursor(true);
					peak.setPrecursorIntensity((double) jrapScanHeader.getPrecursorIntensity());
					peak.setPrecursorCharge(jrapScanHeader.getPrecursorCharge());
					peak.setPrecursorMz((double)jrapScanHeader.getPrecursorMz());
				} else {
					Scan parentScan = msScanMap.get(msScan.getParentScan());
					List<Peak> lPeaks = parentScan.getPeaklist();
					peak = findPeakInPeakList(lPeaks, (double) jrapScanHeader.getPrecursorMz());
					if( peak == null ) {
						peak = new Peak();
						peak.setId(parentScan.getPeaklist().size()+1);
						peak.setMz((double) jrapScanHeader.getPrecursorMz());
						peak.setIntensity(0.0);
						peak.setCharge(jrapScanHeader.getPrecursorCharge());
						parentScan.getPeaklist().add(peak); // adding it if it doesn't exist!
					}

					peak.setIsPrecursor(true);
					peak.setPrecursorIntensity((double) jrapScanHeader.getPrecursorIntensity());
					peak.setPrecursorCharge(jrapScanHeader.getPrecursorCharge());
					peak.setPrecursorMz((double)jrapScanHeader.getPrecursorMz());

				}
				msScan.setPrecursor(peak);
				// get all the peaks of this scan
				double dMinDelta = Double.MAX_VALUE;
				double dBackupPrecursorIntensity = 1.0;
				double[][] scanPeaks = jrapScan.getMassIntensityList();
				double dTotalIntensity = 0.0;
				if( scanPeaks != null ) {
					for (int j = 0; j < scanPeaks[0].length; j++) {
						// ignore peaks w/ intensity = 0?
						if( scanPeaks[1][j] <= 0.0 ) {
							continue;
						}
						peak = new Peak();
						peak.setId(j + 1);
						peak.setMz((double) scanPeaks[0][j]);
						peak.setIntensity((double) scanPeaks[1][j]);
						dTotalIntensity+=peak.getIntensity();
						double dRelInt = peak.getIntensity() / msScan.getMostAbundantPeak();
						peak.setRelativeIntensity(dRelInt);						
						msScan.getPeaklist().add(peak);

						// hacky solution to handle case where precursor peak intensity < 0
						if( msScan.getPrecursor().getIntensity() <= 0.0 ) {
							double dDelta = Math.abs( msScan.getPrecursor().getMz() - peak.getMz());
							if( dDelta < 0.5 && dDelta < dMinDelta ) {
								dBackupPrecursorIntensity = peak.getIntensity();
							}
						}
					}// for j
					if( msScan.getPrecursor().getIntensity() <= 0.0 ) {
						msScan.getPrecursor().setIntensity(dBackupPrecursorIntensity);
					}
					msScan.setTotalNumPeaks(scanPeaks[0].length);
					msScan.setTotalIntensity(dTotalIntensity);
				}
				msScanMap.put(msScan.getScanNo(), msScan);
				msScanMap.get(msScan.getParentScan()).getSubScans().add(msScan.getScanNo());

				if( msScan.getScanStart() > 0 && msScan.getScanStart() < dLowMz ) {
					dLowMz = msScan.getScanStart();
				}
				if( msScan.getScanEnd() > dHighMz ) {
					dHighMz = msScan.getScanEnd();
				}

			}// for i


			fullMSScan.setScanStart(dLowMz);
			fullMSScan.setScanEnd(dHighMz);
			// get the peaks of MS1 and add them to all the MS1 scans
			int ms1PeakIndex = 1;
			for (Peak peak : ms1.values()) {
				if( isCanceled() ) {
					return new ArrayList<>();
				}

				peak.setId(ms1PeakIndex++);
				ms1Peaks.add(peak);
			}
			for (Scan scan : msScanMap.values()) {
				if( isCanceled() ) {
					return new ArrayList<>();
				}
				if (scan.getMsLevel() == 1) {
					scan.setPeaklist(ms1Peaks);
				}
				scans.add(scan);
			}
			return scans;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}	

	public List<Scan> addAllScansDirectInfusion(MSXMLParser parser) {
		Scan msScan = new Scan();
		try {
			int iStartScan = getFirstScanNumber(parser);
			if( iStartScan == -1 ) {
				return new ArrayList<>();
			}
			int iEndScan = parser.getMaxScanNumber();
		/*	int iMinMSLevel = parser.rapHeader(iStartScan).getMsLevel(); // can't assume we have MS1 scans. 
			if( iMinMSLevel != 1 ) { // No MS1 scan? Treat as TIM then. 
				return addAllScansTIM(parser);
			}*/ //don't try to read as TIM, this can still be a direct infusion
			boolean flag = true;
			int precursorScanNum = 0;
			List<Scan> scans = new ArrayList<>();
			HashMap<Integer, Integer> lastParentOfEachLevel = new HashMap<Integer, Integer>();
			HashMap<Integer, org.systemsbiology.jrap.grits.stax.Scan> originalScanMap = new HashMap<Integer, org.systemsbiology.jrap.grits.stax.Scan>();
			HashMap<Integer, Scan> msScanMap = new HashMap<Integer, Scan>();
			HashMap<Double, Peak> ms1 = new HashMap<Double, Peak>();
			List<String> scanErrorList = new ArrayList<>();

			Scan prevParentScan = null;
			List<Peak> ms1Peaks = new ArrayList<>();

			for (int i = iStartScan; i <= iEndScan; i++) {
				if( isCanceled() ) {
					return new ArrayList<>();
				}
				if( (i%10) == 0 ) { // speed things up!
					if (!isCanceled())
						updateListeners("Reading XML file. Scan: " + i + " of " + iEndScan, i);
				}
				org.systemsbiology.jrap.grits.stax.Scan jrapScan = parser.rap(i);
				if( jrapScan == null ) {
					//	if (!isCanceled())
					//		updateErrorListener("Call to getJrapScan for scan number " + i + " returned null. Skipping.");
					logger.debug("Call to getJrapScan for scan number " + i + " returned null. Skipping.");
					continue;
				}
				ScanHeader jrapScanHeader = jrapScan.getHeader();
				if (jrapScan.getMassIntensityList() == null || jrapScan.getMassIntensityList().length == 0) {
					logger.warn("Scan: " + i + " has empty peak list.");
					//					continue;
				}
				double maxIntensity = getMostAbundantPeak(jrapScan);
				msScan = new Scan();
				msScan.setMostAbundantPeak(maxIntensity);
				msScan.setMsLevel(jrapScanHeader.getMsLevel());
				String temp = jrapScanHeader.getPolarity();
				msScan.setPolarity(null);
				msScan.setIsCentroided( (jrapScanHeader.getCentroided() == 1) ); 
				if( temp != null && ! temp.equals("") ) {
					if ( temp.equals("+")) {
						flag = true;
					} else {
						flag = false;
					}					
					msScan.setPolarity(flag);
				}
				msScan.setScanStart((double) jrapScanHeader.getLowMz());
				msScan.setScanEnd((double) jrapScanHeader.getHighMz());
				msScan.setScanNo(jrapScanHeader.getNum());
				msScan.setActivationMethode(jrapScanHeader.getActivationMethod());
				try {
					msScan.setRetentionTime(jrapScanHeader.getDoubleRetentionTime());
				} catch( Exception e1 ) {
					;
				}

				// THIS CODE IS USED FOR PRELIMINARY iCRM WORK
				/*
				if( jrapScanHeader.getScanType() != null && jrapScanHeader.getScanType().equals("SRM" ) ) { 
					int lastMS1 = lastParentOfEachLevel.get(1);
					lastParentOfEachLevel.clear();
					lastParentOfEachLevel.put(1, lastMS1);
				}
				 */
				lastParentOfEachLevel.put(jrapScanHeader.getMsLevel(), jrapScanHeader.getNum());
				originalScanMap.put(jrapScanHeader.getMsLevel(), jrapScan);
				msScanMap.put(msScan.getScanNo(), msScan);

				if( jrapScanHeader.getMsLevel() > 1 ) {
					precursorScanNum = -1;
					if (jrapScanHeader.getPrecursorScanNum() != -1) {
						precursorScanNum = jrapScanHeader.getPrecursorScanNum();
					} else {
						int iParentLevel = msScan.getMsLevel() - 1;
						while( iParentLevel > 1 && ! lastParentOfEachLevel.containsKey(iParentLevel) ) {
							iParentLevel--;
						}
						precursorScanNum = lastParentOfEachLevel.get(iParentLevel);
					}
					msScan.setParentScan(precursorScanNum);
					Scan parentScan = msScanMap.get(msScan.getParentScan());
					if (parentScan == null) {
						// we don't have the parent scan for this
						// add it to the error list
						scanErrorList.add(msScan.getParentScan()+"");
						continue;
					}

					// this is a hack. We're trying to save memory by not storing all MS1 peaks in memory, but we need them until we've processed all of 
					// the precursors. So once we find a new parent, clear the peak list of the previous one.
				/*	if( parentScan.getMsLevel() == 1 ) {
						if( prevParentScan != null && prevParentScan.getScanNo() != parentScan.getScanNo() ) {
							prevParentScan.getPeaklist().clear();
						}
						prevParentScan = parentScan;
					} */ //TODO removing temporarily to make it work with MS1 and MS2 scans that are far apart
					msScan.setMsLevel(parentScan.getMsLevel()+1);
					Peak peak = null;
					List<Peak> lPeaks = parentScan.getPeaklist();
					peak = findPeakInPeakList(lPeaks, (double) jrapScanHeader.getPrecursorMz());
					if( peak == null ) {
						peak = new Peak();
						peak.setId(parentScan.getPeaklist().size()+1);
						peak.setMz((double) jrapScanHeader.getPrecursorMz());
						peak.setIntensity(0.0);
						peak.setCharge(jrapScanHeader.getPrecursorCharge());
						parentScan.getPeaklist().add(peak); // adding it if it doesn't exist!
					} 
					if( jrapScanHeader.getMsLevel() == 2 ) { // the MS2s will be the peaks for the MS1 (added below)
						ms1.put((double) jrapScanHeader.getPrecursorMz(), peak);
					}
					peak.setIsPrecursor(true);
					peak.setPrecursorIntensity((double) jrapScanHeader.getPrecursorIntensity());
					peak.setPrecursorCharge(jrapScanHeader.getPrecursorCharge());
					peak.setPrecursorMz((double)jrapScanHeader.getPrecursorMz());
					msScan.setPrecursor(peak);
					parentScan.getSubScans().add(msScan.getScanNo());
				}
				double[][] scanPeaks = jrapScan.getMassIntensityList();
				if( scanPeaks != null ) {
					double dLowMz = Double.MAX_VALUE;
					double dHighMz = Double.MIN_VALUE;
					double dTotalIntensity = 0.0;
					for (int j = 0; j < scanPeaks[0].length; j++) {
						// ignore peaks w/ intensity = 0?
						if( scanPeaks[1][j] <= 0.0 ) {
							continue;
						}
						Peak peak = new Peak();
						peak.setId(j + 1);
						peak.setMz((double) scanPeaks[0][j]);
						if( dLowMz > peak.getMz() ) {
							dLowMz = peak.getMz();
						}
						if( dHighMz < peak.getMz() ) {
							dHighMz = peak.getMz();
						}
						peak.setIntensity((double) scanPeaks[1][j]);
						dTotalIntensity+=peak.getIntensity();
						double dRelInt = peak.getIntensity() / msScan.getMostAbundantPeak();
						peak.setRelativeIntensity(dRelInt);
						msScan.getPeaklist().add(peak);
					}// for j
					if( msScan.getScanStart() <= 0 ) {
						msScan.setScanStart(dLowMz);
					}
					if( msScan.getScanEnd() <= 0 ) {
						msScan.setScanEnd(dHighMz);
					}
					msScan.setTotalNumPeaks(scanPeaks[0].length);
					msScan.setTotalIntensity(dTotalIntensity);
				}
			}// for i

			// get the peaks of MS1 and add them to all the MS1 scans
			// to save on memory, peaks in MS1 scans are ONLY precursors, so we don't add the peak list here
			// it is built as the ms2 subscans are found
			int ms1PeakIndex = 1;
			for( Double dMz : ms1.keySet() ){
				if( isCanceled() ) {
					return new ArrayList<>();
				}
				Peak peak = ms1.get(dMz);
				if( ! peak.getIsPrecursor() || peak.getPrecursorIntensity() == null )
					continue;
				peak.setId(ms1PeakIndex++);
				ms1Peaks.add(peak);
			}
			for (Scan scan : msScanMap.values()) {
				if( isCanceled() ) {
					return new ArrayList<>();
				}
				if (scan.getMsLevel() == 1) {
					scan.setPeaklist(ms1Peaks);
				}
				scans.add(scan);
			}
			if (!scanErrorList.isEmpty()) {
				if (!isCanceled())
					updateErrorListener("Warning: Some scans are skipped since their parent scans are not in the file. Skipped scans: " + String.join(",", scanErrorList));
			}
			return scans;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}

	}

	public List<Scan> addAllScansMSProfile(MSXMLParser parser, int parentScanNum ) {
		Scan msScan = new Scan();
		try {
			boolean flag = true;
			List<Scan> scans = new ArrayList<>();

			org.systemsbiology.jrap.grits.stax.Scan jrapScan = parser.rap(parentScanNum);
			if( jrapScan == null ) {
				if (!isCanceled())
					updateErrorListener("Call to parser.rap for scan number " + parentScanNum + " returned null. Returning empty list.");
				return scans;
			}
			ScanHeader jrapScanHeader = jrapScan.getHeader();
			if (jrapScan.getMassIntensityList() == null || jrapScan.getMassIntensityList().length == 0) {
				logger.warn("Scan: " + parentScanNum + " has empty peak list.");
				return scans;
			}

			double maxIntensity = getMostAbundantPeak(jrapScan);
			msScan = new Scan();
			msScan.setMostAbundantPeak(maxIntensity);
			msScan.setMsLevel(jrapScanHeader.getMsLevel());
			String temp = jrapScanHeader.getPolarity();
			msScan.setPolarity(null);
			msScan.setIsCentroided( (jrapScanHeader.getCentroided() == 1) ); 
			if( temp != null && ! temp.equals("") ) {
				if ( temp.equals("+")) {
					flag = true;
				} else {
					flag = false;
				}					
				msScan.setPolarity(flag);
			}
			msScan.setScanStart((double) jrapScanHeader.getLowMz());
			msScan.setScanEnd((double) jrapScanHeader.getHighMz());
			msScan.setScanNo(jrapScanHeader.getNum());
			msScan.setActivationMethode(jrapScanHeader.getActivationMethod());
			try {
				msScan.setRetentionTime(jrapScanHeader.getDoubleRetentionTime());
			} catch( Exception e1 ) {
				;
			}

			// get all the peaks of this scan
			double[][] scanPeaks = jrapScan.getMassIntensityList();
			if( scanPeaks != null ) {
				double dLowMz = Double.MAX_VALUE;
				double dHighMz = Double.MIN_VALUE;
				double dTotalIntensity = 0.0;
				for (int j = 0; j < scanPeaks[0].length; j++) {
					if( isCanceled() ) {
						return new ArrayList<>();
					}
					// ignore peaks w/ intensity = 0?
					if( scanPeaks[1][j] <= 0.0 ) {
						continue;
					}
					Peak peak = new Peak();
					peak.setId(j + 1);
					peak.setMz((double) scanPeaks[0][j]);
					if( dLowMz > peak.getMz() ) {
						dLowMz = peak.getMz();
					}
					if( dHighMz < peak.getMz() ) {
						dHighMz = peak.getMz();
					}
					peak.setIntensity((double) scanPeaks[1][j]);
					dTotalIntensity+=peak.getIntensity();
					double dRelInt = peak.getIntensity() / msScan.getMostAbundantPeak();
					peak.setRelativeIntensity(dRelInt);
					msScan.getPeaklist().add(peak);
				}// for j

				if( msScan.getScanStart() <= 0 ) {
					msScan.setScanStart(dLowMz);
				}
				if( msScan.getScanEnd() <= 0 ) {
					msScan.setScanEnd(dHighMz);
				}
				msScan.setTotalNumPeaks(scanPeaks[0].length);
				msScan.setTotalIntensity(dTotalIntensity);
			}
			scans.add(msScan);
			return scans;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}

	}
	
	public List<Scan> addAllScansLCMSMS(MSXMLParser parser, int parentScanNum, Map<Integer, List<Integer>> subScanMap) {
		if (subScanMap == null || subScanMap.isEmpty()) 
			return addAllScansLCMSMS(parser, parentScanNum);
		
		Scan msScan = new Scan();
		try {
			boolean flag = true;
			List<Scan> scans = new ArrayList<>();
			HashMap<Integer, Scan> msScanMap = new HashMap<Integer, Scan>();
			HashMap<Double, Peak> ms1 = new HashMap<Double, Peak>();

			List<Peak> ms1Peaks = new ArrayList<>();
			
			// System.out.println("scan No " + i);
			org.systemsbiology.jrap.grits.stax.Scan jrapScan = parser.rap(parentScanNum);
			if( jrapScan == null ) {
				//if (!isCanceled())
				//	updateErrorListener("Call to getJrapScan for scan number " + i + " returned null. Skipping.");
				logger.debug("Call to getJrapScan for scan number " + parentScanNum + " returned null. Skipping.");
				return null;
			}
			ScanHeader jrapScanHeader = jrapScan.getHeader();
			if( jrapScanHeader == null ) {
				//if (!isCanceled())
				//	updateErrorListener("Call to jrapScan.getHeader() for scan number " + i + " returned null. Skipping.");
				logger.debug("Call to jrapScan.getHeader() for scan number " + parentScanNum + " returned null. Skipping.");
				return null;
			}
			if (jrapScan.getMassIntensityList() == null || jrapScan.getMassIntensityList().length == 0) {
				logger.warn("Scan: " + parentScanNum + " has empty peak list.");
			}
			double maxIntensity = getMostAbundantPeak(jrapScan);
			msScan = new Scan();
			msScan.setMostAbundantPeak(maxIntensity);
			msScan.setMsLevel(jrapScanHeader.getMsLevel());
			String temp = jrapScanHeader.getPolarity();
			msScan.setPolarity(null);
			msScan.setIsCentroided( (jrapScanHeader.getCentroided() == 1) ); 
			if( temp != null && ! temp.equals("") ) {
				if ( temp.equals("+")) {
					flag = true;
				} else {
					flag = false;
				}					
				msScan.setPolarity(flag);
			}
			msScan.setScanStart((double) jrapScanHeader.getLowMz());
			msScan.setScanEnd((double) jrapScanHeader.getHighMz());
			msScan.setScanNo(jrapScanHeader.getNum());
			msScan.setActivationMethode(jrapScanHeader.getActivationMethod());
			try {
				msScan.setRetentionTime(jrapScanHeader.getDoubleRetentionTime());
			} catch( Exception e1 ) {
				;
			}
			
			msScanMap.put(msScan.getScanNo(), msScan);
			// get all the peaks of this scan
			double[][] scanPeaks = jrapScan.getMassIntensityList();
			if( scanPeaks != null ) {
				double dLowMz = Double.MAX_VALUE;
				double dHighMz = Double.MIN_VALUE;
				double dTotalIntensity = 0.0;
				for (int j = 0; j < scanPeaks[0].length; j++) {
					if( isCanceled() ) {
						return new ArrayList<>();
					}
					// ignore peaks w/ intensity = 0?
					if( scanPeaks[1][j] <= 0.0 ) {
						continue;
					}
					Peak peak = new Peak();
					peak.setId(j + 1);
					peak.setMz((double) scanPeaks[0][j]);
					if( dLowMz > peak.getMz() ) {
						dLowMz = peak.getMz();
					}
					if( dHighMz < peak.getMz() ) {
						dHighMz = peak.getMz();
					}
					peak.setIntensity((double) scanPeaks[1][j]);
					dTotalIntensity += peak.getIntensity();
					double dRelInt = peak.getIntensity() / msScan.getMostAbundantPeak();
					peak.setRelativeIntensity(dRelInt);
					msScan.getPeaklist().add(peak);
				}// for j
				if( msScan.getScanStart() <= 0 ) {
					msScan.setScanStart(dLowMz);
				}
				if( msScan.getScanEnd() <= 0 ) {
					msScan.setScanEnd(dHighMz);
				}
				msScan.setTotalNumPeaks(scanPeaks[0].length);
				msScan.setTotalIntensity(dTotalIntensity);
			}

			List<Integer> subScans = subScanMap.get(parentScanNum);
			for (Integer scanNo: subScans) {
				Scan subScan = processSubScan(parser, msScan, scanNo, ms1, subScanMap, msScanMap);
				msScanMap.put(scanNo, subScan);
			}
			
			int ms1PeakIndex = 1;
			for (Peak peak : ms1.values()) {
				if( isCanceled() ) {
					return new ArrayList<>();
				}
				if( ! peak.getIsPrecursor() || peak.getPrecursorIntensity() == null )
					continue;
				peak.setId(ms1PeakIndex++);
				ms1Peaks.add(peak);
			}
			for (Scan scan : msScanMap.values()) {
				if( isCanceled() ) {
					return new ArrayList<>();
				}
				if (scan.getMsLevel() == 1) {
					scan.setPeaklist(ms1Peaks);
				}
				scans.add(scan);
			}
			return scans;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}
		
	}
	
	Scan processSubScan (MSXMLParser parser, 
			Scan parentScan, Integer scanNo, HashMap<Double, Peak> ms1, 
			Map<Integer, List<Integer>> subScanMap, 
			HashMap<Integer, Scan> msScanMap) {
		org.systemsbiology.jrap.grits.stax.Scan jrapScan = parser.rap(scanNo);
		ScanHeader jrapScanHeader = jrapScan.getHeader();
		if( jrapScanHeader == null ) {
			//if (!isCanceled())
			//	updateErrorListener("Call to jrapScan.getHeader() for scan number " + i + " returned null. Skipping.");
			logger.debug("Call to jrapScan.getHeader() for scan number " + scanNo + " returned null. Skipping.");
			return null;
		}
		if (jrapScan.getMassIntensityList() == null || jrapScan.getMassIntensityList().length == 0) {
			logger.warn("Scan: " + scanNo + " has empty peak list.");
			//					continue;
		}
		boolean flag = true;
		double maxIntensity = getMostAbundantPeak(jrapScan);
		Scan msScan = new Scan();
		msScan.setMostAbundantPeak(maxIntensity);
		msScan.setMsLevel(jrapScanHeader.getMsLevel());
		String temp = jrapScanHeader.getPolarity();
		msScan.setPolarity(null);
		msScan.setIsCentroided( (jrapScanHeader.getCentroided() == 1) ); 
		if( temp != null && ! temp.equals("") ) {
			if ( temp.equals("+")) {
				flag = true;
			} else {
				flag = false;
			}					
			msScan.setPolarity(flag);
		}
		msScan.setScanStart((double) jrapScanHeader.getLowMz());
		msScan.setScanEnd((double) jrapScanHeader.getHighMz());
		msScan.setScanNo(jrapScanHeader.getNum());
		msScan.setActivationMethode(jrapScanHeader.getActivationMethod());
		try {
			msScan.setRetentionTime(jrapScanHeader.getDoubleRetentionTime());
		} catch( Exception e1 ) {
			;
		}
		msScan.setParentScan(jrapScanHeader.getPrecursorScanNum());
		msScan.setMsLevel(parentScan.getMsLevel()+1);
		Peak peak = null;
		List<Peak> lPeaks = parentScan.getPeaklist();
		peak = findPeakInPeakList(lPeaks, (double) jrapScanHeader.getPrecursorMz());
		if( peak == null ) {
			peak = new Peak();
			peak.setId(parentScan.getPeaklist().size()+1);
			peak.setMz((double) jrapScanHeader.getPrecursorMz());
			peak.setIntensity(0.0);
			peak.setCharge(jrapScanHeader.getPrecursorCharge());
			parentScan.getPeaklist().add(peak); // adding it if it doesn't exist!
		} 
		if( jrapScanHeader.getMsLevel() == 2 )
			ms1.put((double) jrapScanHeader.getPrecursorMz(), peak);						
		peak.setIsPrecursor(true);
		peak.setPrecursorIntensity((double) jrapScanHeader.getPrecursorIntensity());
		peak.setPrecursorCharge(jrapScanHeader.getPrecursorCharge());
		peak.setPrecursorMz((double)jrapScanHeader.getPrecursorMz());

		msScan.setPrecursor(peak);
		parentScan.getSubScans().add(msScan.getScanNo());
		
		
		// get all the peaks of this scan
		double[][] scanPeaks = jrapScan.getMassIntensityList();
		if( scanPeaks != null ) {
			double dLowMz = Double.MAX_VALUE;
			double dHighMz = Double.MIN_VALUE;
			double dTotalIntensity = 0.0;
			for (int j = 0; j < scanPeaks[0].length; j++) {
				// ignore peaks w/ intensity = 0?
				if( scanPeaks[1][j] <= 0.0 ) {
					continue;
				}
				peak = new Peak();
				peak.setId(j + 1);
				peak.setMz((double) scanPeaks[0][j]);
				if( dLowMz > peak.getMz() ) {
					dLowMz = peak.getMz();
				}
				if( dHighMz < peak.getMz() ) {
					dHighMz = peak.getMz();
				}
				peak.setIntensity((double) scanPeaks[1][j]);
				dTotalIntensity += peak.getIntensity();
				double dRelInt = peak.getIntensity() / msScan.getMostAbundantPeak();
				peak.setRelativeIntensity(dRelInt);
				msScan.getPeaklist().add(peak);
			}// for j
			if( msScan.getScanStart() <= 0 ) {
				msScan.setScanStart(dLowMz);
			}
			if( msScan.getScanEnd() <= 0 ) {
				msScan.setScanEnd(dHighMz);
			}
			msScan.setTotalNumPeaks(scanPeaks[0].length);
			msScan.setTotalIntensity(dTotalIntensity);
		}
		
		List<Integer> subScans = subScanMap.get(scanNo);
		for (Integer subScanNo: subScans) {
			Scan subScan = processSubScan(parser, msScan, subScanNo, ms1, subScanMap, msScanMap);
			msScanMap.put(subScanNo, subScan);
		}
		
		return msScan;
		
	}

	public List<Scan> addAllScansLCMSMS(MSXMLParser parser, int parentScanNum ) {
		Scan msScan = new Scan();
		try {
			boolean flag = true;
			int precursorScanNum = 0;
			List<Scan> scans = new ArrayList<>();
			HashMap<Integer, Integer> lastParentOfEachLevel = new HashMap<Integer, Integer>();
			HashMap<Integer, org.systemsbiology.jrap.grits.stax.Scan> originalScanMap = new HashMap<Integer, org.systemsbiology.jrap.grits.stax.Scan>();
			HashMap<Integer, Scan> msScanMap = new HashMap<Integer, Scan>();
			HashMap<Double, Peak> ms1 = new HashMap<Double, Peak>();

			List<Peak> ms1Peaks = new ArrayList<>();
			int iParentMSLevel = parser.rapHeader(parentScanNum).getMsLevel(); // can't assume we have MS1 scans. Keep track of possible precursors
			boolean bFoundParent = false;
			int iStartScan = getFirstScanNumber(parser);
			if( iStartScan == -1 ) {
				return new ArrayList<>();
			}
			int iEndScan = parser.getMaxScanNumber();

			for (int i = iStartScan; i <= iEndScan; i++) {
				if( isCanceled() ) {
					return new ArrayList<>();
				}
				if( (i%10) == 0 ) { // speed things up!
					if (!isCanceled())
						updateListeners("Reading XML file. Scan: " + i + " of " + iEndScan, i);
				}
				if( i == parentScanNum ) {
					bFoundParent = true;					
				}
				if( ! bFoundParent ) 
					continue;

				// System.out.println("scan No " + i);
				org.systemsbiology.jrap.grits.stax.Scan jrapScan = parser.rap(i);
				if( jrapScan == null ) {
					//if (!isCanceled())
					//	updateErrorListener("Call to getJrapScan for scan number " + i + " returned null. Skipping.");
					logger.debug("Call to getJrapScan for scan number " + i + " returned null. Skipping.");
					continue;
				}
				ScanHeader jrapScanHeader = jrapScan.getHeader();
				if( jrapScanHeader == null ) {
					//if (!isCanceled())
					//	updateErrorListener("Call to jrapScan.getHeader() for scan number " + i + " returned null. Skipping.");
					logger.debug("Call to jrapScan.getHeader() for scan number " + i + " returned null. Skipping.");
					continue;
				}
				if (jrapScan.getMassIntensityList() == null || jrapScan.getMassIntensityList().length == 0) {
					logger.warn("Scan: " + i + " has empty peak list.");
					//					continue;
				}
				double maxIntensity = getMostAbundantPeak(jrapScan);
				msScan = new Scan();
				msScan.setMostAbundantPeak(maxIntensity);
				msScan.setMsLevel(jrapScanHeader.getMsLevel());
				String temp = jrapScanHeader.getPolarity();
				msScan.setPolarity(null);
				msScan.setIsCentroided( (jrapScanHeader.getCentroided() == 1) ); 
				if( temp != null && ! temp.equals("") ) {
					if ( temp.equals("+")) {
						flag = true;
					} else {
						flag = false;
					}					
					msScan.setPolarity(flag);
				}
				msScan.setScanStart((double) jrapScanHeader.getLowMz());
				msScan.setScanEnd((double) jrapScanHeader.getHighMz());
				msScan.setScanNo(jrapScanHeader.getNum());
				msScan.setActivationMethode(jrapScanHeader.getActivationMethod());
				try {
					msScan.setRetentionTime(jrapScanHeader.getDoubleRetentionTime());
				} catch( Exception e1 ) {
					;
				}

				lastParentOfEachLevel.put(jrapScanHeader.getMsLevel(), jrapScanHeader.getNum());
				originalScanMap.put(jrapScanHeader.getMsLevel(), jrapScan);
				//				msScanMap.put(msScan.getScanNo(), msScan);

				precursorScanNum = -1;
				if( jrapScanHeader.getMsLevel() > iParentMSLevel ) {
					precursorScanNum = -1;
					if (jrapScanHeader.getPrecursorScanNum() != -1) {
						precursorScanNum = jrapScanHeader.getPrecursorScanNum();
					} else {
						int iParentLevel = msScan.getMsLevel() - 1;
						while( iParentLevel > 1 && ! lastParentOfEachLevel.containsKey(iParentLevel) ) {
							iParentLevel--;
						}
						precursorScanNum = lastParentOfEachLevel.get(iParentLevel);
					}
					msScan.setParentScan(precursorScanNum);
					Scan parentScan = msScanMap.get(msScan.getParentScan());
					if( parentScan == null || parentScan.getMsLevel() == null ) {
						continue;
					}
					msScan.setMsLevel(parentScan.getMsLevel()+1);
					Peak peak = null;
					List<Peak> lPeaks = parentScan.getPeaklist();
					peak = findPeakInPeakList(lPeaks, (double) jrapScanHeader.getPrecursorMz());
					if( peak == null ) {
						peak = new Peak();
						peak.setId(parentScan.getPeaklist().size()+1);
						peak.setMz((double) jrapScanHeader.getPrecursorMz());
						peak.setIntensity(0.0);
						peak.setCharge(jrapScanHeader.getPrecursorCharge());
						parentScan.getPeaklist().add(peak); // adding it if it doesn't exist!
					} 
					if( jrapScanHeader.getMsLevel() == 2 )
						ms1.put((double) jrapScanHeader.getPrecursorMz(), peak);						
					peak.setIsPrecursor(true);
					peak.setPrecursorIntensity((double) jrapScanHeader.getPrecursorIntensity());
					peak.setPrecursorCharge(jrapScanHeader.getPrecursorCharge());
					peak.setPrecursorMz((double)jrapScanHeader.getPrecursorMz());

					msScan.setPrecursor(peak);
					parentScan.getSubScans().add(msScan.getScanNo());
				} 

				/*if( i != parentScanNum && precursorScanNum != parentScanNum 
						&& msScan.getMsLevel() == iParentMSLevel ) { // we've parsed all subscans of requested parent
					break;
				}*/
				msScanMap.put(msScan.getScanNo(), msScan);
				// get all the peaks of this scan
				double[][] scanPeaks = jrapScan.getMassIntensityList();
				if( scanPeaks != null ) {
					double dLowMz = Double.MAX_VALUE;
					double dHighMz = Double.MIN_VALUE;
					double dTotalIntensity = 0.0;
					for (int j = 0; j < scanPeaks[0].length; j++) {
						if( isCanceled() ) {
							return new ArrayList<>();
						}
						// ignore peaks w/ intensity = 0?
						if( scanPeaks[1][j] <= 0.0 ) {
							continue;
						}
						Peak peak = new Peak();
						peak.setId(j + 1);
						peak.setMz((double) scanPeaks[0][j]);
						if( dLowMz > peak.getMz() ) {
							dLowMz = peak.getMz();
						}
						if( dHighMz < peak.getMz() ) {
							dHighMz = peak.getMz();
						}
						peak.setIntensity((double) scanPeaks[1][j]);
						dTotalIntensity += peak.getIntensity();
						double dRelInt = peak.getIntensity() / msScan.getMostAbundantPeak();
						peak.setRelativeIntensity(dRelInt);
						msScan.getPeaklist().add(peak);
					}// for j
					if( msScan.getScanStart() <= 0 ) {
						msScan.setScanStart(dLowMz);
					}
					if( msScan.getScanEnd() <= 0 ) {
						msScan.setScanEnd(dHighMz);
					}
					msScan.setTotalNumPeaks(scanPeaks[0].length);
					msScan.setTotalIntensity(dTotalIntensity);
				}

			} // for i
			// get the peaks of MS1 and add them to all the MS1 scans
			// to save on memory, peaks in MS1 scans are ONLY precursors, so we don't add the peak list here
			// it is built as the ms2 subscans are found
			int ms1PeakIndex = 1;
			for (Peak peak : ms1.values()) {
				if( isCanceled() ) {
					return new ArrayList<>();
				}
				if( ! peak.getIsPrecursor() || peak.getPrecursorIntensity() == null )
					continue;
				peak.setId(ms1PeakIndex++);
				ms1Peaks.add(peak);
			}
			for (Scan scan : msScanMap.values()) {
				if( isCanceled() ) {
					return new ArrayList<>();
				}
				if (scan.getMsLevel() == 1) {
					scan.setPeaklist(ms1Peaks);
				}
				scans.add(scan);
			}
			return scans;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}

	}

	public List<Integer> getScanList( String fileName, int parentScanNum ) {
		File mzXMLFile = new File(fileName);	
		List<Integer> lScans = new ArrayList<>();
		try {
			MSXMLParser parser = new MSXMLParser(mzXMLFile.getAbsolutePath());
			int iMinMSLevel = getMinMSLevel(parser);
			int iStartScan = getFirstScanNumber(parser);
			if( iStartScan == -1 ) {
				return new ArrayList<Integer>();
			}
			int iEndScan = parser.getMaxScanNumber();
			boolean bFoundParent = false;
			int iParentMSLevel = -1;
			for (int i = iStartScan; i <= iEndScan; i++) {
				if( isCanceled() ) {
					return new ArrayList<Integer>();
				}
				if( (i%10) == 0 ) { // speed things up!
					if (!isCanceled())
						updateListeners("Reading XML file. Scan: " + i + " of " + iEndScan, i);
				}
				ScanHeader jrapScanHeader = parser.rapHeader(i);
				if( jrapScanHeader == null ) {
					//if (!isCanceled())
					//	updateErrorListener("Call to parser.rapHeader() for scan number " + i + " returned null. Skipping.");
					logger.debug("Call to parser.rapHeader() for scan number " + i + " returned null. Skipping.");
					continue;
				}
				if( parentScanNum < 0 ) {
					if( jrapScanHeader.getMsLevel() == iMinMSLevel ) {
						lScans.add(i);
					}
				} else {
					if( ! bFoundParent ) {
						if( jrapScanHeader.getNum() == parentScanNum ) {
							bFoundParent = true;
							iParentMSLevel = jrapScanHeader.getMsLevel();							
						}
					} else {
						if( jrapScanHeader.getMsLevel() == (iParentMSLevel - 1) ) {
							lScans.add(i);					
						} else if ( jrapScanHeader.getMsLevel() == iParentMSLevel ) {
							bFoundParent = false;
						}
					}					
				}
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return null;
		}
		return lScans;

	}

	@Override
	public Integer getMinMSLevel(MSFile file) {
		return getMinMSLevel(file.getFileName());
	}
	
	public static int getMinMSLevel(String fileName) {
		File mzXMLFile = new File(fileName);
		try {
			MSXMLParser parser = new MSXMLParser(mzXMLFile.getAbsolutePath());
			return getMinMSLevel(parser);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return -1;
	}

	private static int getMinMSLevel(MSXMLParser parser) {
		try {
			int iStartScan = getFirstScanNumber(parser);
			if( iStartScan == -1 ) {
				return -1;
			}
			int iEndScan = parser.getMaxScanNumber();
			int iMinMSLevel = Integer.MAX_VALUE;
			for (int i = iStartScan; i <= iEndScan; i++) {
				try {
					ScanHeader jrapScanHeader = parser.rapHeader(i);
					if( jrapScanHeader == null ) {
						continue;
					}
					if( jrapScanHeader.getMsLevel() < iMinMSLevel ) {
						iMinMSLevel = jrapScanHeader.getMsLevel();
					}
				} catch( Exception ex ) {
					logger.debug("Error parsing scan: i");
					logger.error(ex.getMessage(), ex);
				}
			}
			return iMinMSLevel;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return -1;
	}

	private int countScansByMSLevel(MSXMLParser parser, int iMSLevel ) {
		try {
			int iStartScan = getFirstScanNumber(parser);
			if( iStartScan == -1 ) {
				return -1;
			}
			int iEndScan = parser.getMaxScanNumber();
			int iNumScans = 0;
			for (int i = iStartScan; i <= iEndScan; i++) {
				try {
					ScanHeader jrapScanHeader = parser.rapHeader(i);
					if( jrapScanHeader == null ) {
						continue;
					}
					if (jrapScanHeader.getNum() > 0 && jrapScanHeader.getMsLevel() == iMSLevel ) {
						iNumScans++;
					}
				} catch( Exception ex ) {
					logger.debug("Error parsing scan: i");
					logger.error(ex.getMessage(), ex);
				}
			}
			return iNumScans;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return 0;
	}

	@Override
	public int getNumMS1Scans(MSFile file) {
		File mzXMLFile = new File(file.getFileName());
		try {
			MSXMLParser parser = new MSXMLParser(mzXMLFile.getAbsolutePath());
			return countScansByMSLevel(parser, 1);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return 0;
	}

	@Override
	public int getNumMS2Scans(MSFile file) {
		File mzXMLFile = new File(file.getFileName());
		try {
			MSXMLParser parser = new MSXMLParser(mzXMLFile.getAbsolutePath());
			return countScansByMSLevel(parser, 2);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return 0;
	}
	
	private static int getFirstScanNumber(MSXMLParser parser) {
		int count = parser.getMaxScanNumber();
		for (int i = 1; i <= count; i++) {
			org.systemsbiology.jrap.grits.stax.Scan jrapScan = parser.rap(i);
			if( jrapScan != null ) {
				return i;
			}
		}
		return -1;
	}

	private List<Scan> getScanData(MSXMLParser parser, int msLevel, int parentScanNum, int scanNum) {
		try {
			int precursorScanNum = 0;
			List<Scan> scans = new ArrayList<>();
			HashMap<Integer, Integer> lastParentOfEachLevel = new HashMap<Integer, Integer>();
			HashMap<Integer, Scan> msScanMap = new HashMap<Integer, Scan>();
			int iStartScan = getFirstScanNumber(parser);
			if( iStartScan == -1 ) {
				return new ArrayList<>();
			}
			int iEndScan = parser.getMaxScanNumber();

			int iLastPossiblePrecursorMSLevel = parser.rapHeader(iStartScan).getMsLevel(); // can't assume we have MS1 scans. Keep track of possible precursors
			int iMinMSLevel = iLastPossiblePrecursorMSLevel; // can't assume we have MS1 scans. Keep track of possible precursors
			Scan firstParentScan = null;
			double dMinMz = Double.MAX_VALUE;
			double dMaxMz = Double.MIN_VALUE;
			if( iLastPossiblePrecursorMSLevel > 1 ) { // no parent scan. Create one so everything jives
				firstParentScan = new Scan();
				firstParentScan.setScanNo(0);
				firstParentScan.setMsLevel(1);
				firstParentScan.setPolarity(null);
				firstParentScan.setRetentionTime(0.0);
				msScanMap.put(firstParentScan.getScanNo(), firstParentScan);
				iLastPossiblePrecursorMSLevel = 1;
				lastParentOfEachLevel.put(1, 0);
				scans.add(firstParentScan);
			} 

			Scan msScan = null;
			Scan parentScan = null;
			List<Integer> skippedScans = new ArrayList<>();
			for (int i = iStartScan; i <= iEndScan; i++) {
				try {
					if( isCanceled() ) {
						return new ArrayList<>();
					}
					if( (i % 10) == 0 ) {
						if (!isCanceled())
							updateListeners("Reading XML file. Scan: " + i + " of " + iEndScan, i);
					}
					if( scanNum != -1 && i != scanNum) { // if scan number is specified, we don't need to rap every scan!
						continue;
					} 

					// I've never seen more than 1000 ms/ms events for a single parent scan
					// this should improve performance
					if( parentScanNum != -1 && (i < parentScanNum || i > (parentScanNum+1000) ) ) {
						continue;
					}
					org.systemsbiology.jrap.grits.stax.Scan jrapScan = getJrapScan(parser, i);
					if( jrapScan == null ) {
						skippedScans.add(i);
						//if (!isCanceled())
						//	updateErrorListener("Call to getJrapScan for scan number " + i + " returned null. Skipping.");
						continue;
					}
					org.systemsbiology.jrap.grits.stax.ScanHeader jrapScanHeader = jrapScan.getHeader();

					// Create a peak for the precursor to be added to MS1 scan
					int iCurMSLevel = jrapScanHeader.getMsLevel();

					if( iLastPossiblePrecursorMSLevel < iCurMSLevel - 1 ) {
						iLastPossiblePrecursorMSLevel = iCurMSLevel -1;
					} else if ( iLastPossiblePrecursorMSLevel == iCurMSLevel && iCurMSLevel - 1 >= iMinMSLevel ) {
						iLastPossiblePrecursorMSLevel = iCurMSLevel - 1;
					}
					precursorScanNum = -1;
					if( jrapScanHeader.getPrecursorScanNum() != -1 ) {
						precursorScanNum = jrapScanHeader.getPrecursorScanNum();
					} else if( iCurMSLevel > iLastPossiblePrecursorMSLevel && lastParentOfEachLevel.containsKey(iCurMSLevel - 1)) { // has precursor
						precursorScanNum = lastParentOfEachLevel.get(iCurMSLevel - 1);
					} else if ( parentScanNum != -1 && iCurMSLevel > 1 ) {
						precursorScanNum = parentScanNum; // what else can we do??
					}

					msScan = getScan(jrapScan, precursorScanNum);
					int iParentScanNum = msScan.getParentScan();
					if( iParentScanNum != -1 ) {
						if( msScanMap.containsKey(iParentScanNum) ) {
							parentScan = msScanMap.get(iParentScanNum);
						} else {
							org.systemsbiology.jrap.grits.stax.Scan jrapParentScan = getJrapScan(parser, iParentScanNum);
							parentScan = getScan(jrapParentScan, -1);
							if( parentScan == null ) {
								parentScan = firstParentScan;
								iLastPossiblePrecursorMSLevel = firstParentScan.getMsLevel();								
							}
							msScanMap.put(parentScan.getScanNo(), parentScan);
						}
					}
					lastParentOfEachLevel.put(iCurMSLevel, i);
					msScanMap.put(msScan.getScanNo(), msScan);

					Peak peak = null;
					if( msScan.getMsLevel() > iLastPossiblePrecursorMSLevel && parentScan != null) { // precursor peak is built on fly
						peak = getPrecursorPeak(parentScan, jrapScanHeader);				
						msScan.setPrecursor(peak);
						parentScan.getSubScans().add(i);
						if( peak.getMz() > dMaxMz ) {
							dMaxMz = peak.getMz();
						}
						if( peak.getMz() < dMinMz ) {
							dMinMz = peak.getMz();
						}
						if (firstParentScan != null && firstParentScan.getPolarity() == null && msScan.getPolarity() != null) {
							firstParentScan.setPolarity(msScan.getPolarity());
						}
					}

					if( i == scanNum || i == parentScanNum ||
							(precursorScanNum != -1 && (precursorScanNum == parentScanNum)) ||
							(scanNum == -1 && parentScanNum == -1 && iCurMSLevel == msLevel) ) {

						setPeakList(jrapScan.getMassIntensityList(), msScan, (scanNum != -1 || parentScanNum != -1));
						scans.add(msScan);
					}

					if( iCurMSLevel < iMinMSLevel ) {
						iMinMSLevel = iCurMSLevel;
					}
				} catch( Exception ex ) {
					logger.error(ex.getMessage(), ex);
					updateErrorListener("Error parsing scan number: " + i, ex);
				}
			}

			if( firstParentScan != null ) {
				firstParentScan.setScanStart(dMinMz);
				firstParentScan.setScanEnd(dMaxMz);
			}

			if (!isCanceled) {
				if (!skippedScans.isEmpty()) {
					updateErrorListener("Several scans are skipped due to being null. Check the log for details");
					logger.info("The following scans are skipped: " + skippedScans.toString());
				}
			}
			return scans;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	private org.systemsbiology.jrap.grits.stax.Scan getJrapScan( MSXMLParser parser, int iScanNumber) {

		org.systemsbiology.jrap.grits.stax.Scan jrapScan = parser.rap(iScanNumber);
		if( jrapScan == null ) {
			return null;
		}
		return jrapScan;
	}

	private Scan getScan( org.systemsbiology.jrap.grits.stax.Scan jrapScan, int iLastPrecursorScanNum) {

		if( jrapScan == null ) {
			return null;
		}
		ScanHeader jrapScanHeader = jrapScan.getHeader();
		double maxIntensity = getMostAbundantPeak(jrapScan);
		Scan msScan = new Scan();
		msScan.setMostAbundantPeak(maxIntensity);
		int precursorScanNum = -1;
		if (jrapScanHeader.getPrecursorScanNum() != -1) {
			precursorScanNum = jrapScanHeader.getPrecursorScanNum();
		} else  { // has precursor
			precursorScanNum = iLastPrecursorScanNum;						 
		}
		msScan.setParentScan(precursorScanNum);
		msScan.setMsLevel(jrapScanHeader.getMsLevel());
		String temp = jrapScanHeader.getPolarity();
		msScan.setPolarity(null);
		if( temp != null && ! temp.equals("") ) {
			boolean flag;
			if ( temp.equals("+")) {
				flag = true;
			} else {
				flag = false;
			}					
			msScan.setPolarity(flag);
		}
		msScan.setScanStart((double) jrapScanHeader.getLowMz());
		msScan.setScanEnd((double) jrapScanHeader.getHighMz());
		msScan.setScanNo(jrapScanHeader.getNum());
		msScan.setActivationMethode(jrapScanHeader.getActivationMethod());
		try {
			msScan.setRetentionTime(jrapScanHeader.getDoubleRetentionTime());
		} catch( Exception e1 ) {
			;
		}
		msScan.setIsCentroided( (jrapScanHeader.getCentroided() == 1) );
		return msScan;
	}

	private Peak getPrecursorPeak( Scan parentScan, ScanHeader jrapScanHeader ) {
		if( parentScan == null ) {
			return null;
		}
		Peak peak = null;
		List<Peak> lPeaks = parentScan.getPeaklist();
		peak = findPeakInPeakList(lPeaks, (double) jrapScanHeader.getPrecursorMz());
		if( peak == null ) {
			peak = new Peak();
			peak.setId(parentScan.getPeaklist().size()+1);
			peak.setMz((double) jrapScanHeader.getPrecursorMz());
			peak.setIntensity(0.0);
			peak.setCharge(jrapScanHeader.getPrecursorCharge());
			parentScan.getPeaklist().add(peak); // adding it if it doesn't exist!
		}
		peak.setIsPrecursor(true);
		peak.setPrecursorIntensity((double) jrapScanHeader.getPrecursorIntensity());
		peak.setPrecursorCharge(jrapScanHeader.getPrecursorCharge());
		peak.setPrecursorMz((double)jrapScanHeader.getPrecursorMz());

		return peak;
	}

	private void setPeakList(double[][] scanPeaks, Scan msScan, boolean bAddPeak) {
		// get all the peaks of this scan
		if( scanPeaks != null ) {
			double dLowMz = Double.MAX_VALUE;
			double dHighMz = Double.MIN_VALUE;
			double dTotalIntensity = 0.0;
			for (int j = 0; j < scanPeaks[0].length; j++) {
				if( isCanceled() ) {
					return;
				}
				// ignore peaks w/ intensity = 0?
				if( scanPeaks[1][j] <= 0.0 ) {
					continue;
				}
				Peak peak = new Peak();
				peak.setId(j + 1);
				peak.setMz((double) scanPeaks[0][j]);
				if( dLowMz > peak.getMz() ) {
					dLowMz = peak.getMz();
				}
				if( dHighMz < peak.getMz() ) {
					dHighMz = peak.getMz();
				}
				peak.setIntensity((double) scanPeaks[1][j]);
				dTotalIntensity += peak.getIntensity();
				double dRelInt = peak.getIntensity() / msScan.getMostAbundantPeak();
				peak.setRelativeIntensity(dRelInt);
				if( bAddPeak ) {
					msScan.getPeaklist().add(peak);
				}
			}// for j
			if( msScan.getScanStart() <= 0 ) {
				msScan.setScanStart(dLowMz);
			}
			if( msScan.getScanEnd() <= 0 ) {
				msScan.setScanEnd(dHighMz);
			}
			msScan.setTotalNumPeaks(scanPeaks[0].length);
			msScan.setTotalIntensity(dTotalIntensity);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isValid(MSFile file) {
		return isValidMzXmlFile(file.getFileName());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer getMinScanNumber(MSFile file) {
		MSXMLParser parser = new MSXMLParser(file.getFileName());
		return getFirstScanNumber(parser);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Scan> readMSFile(MSFile file, int msLevel, int parentScanNum, int scanNum) {
		return readMzXmlFile(file.getFileName(), msLevel, parentScanNum, scanNum);
	}

	@Override
	public boolean hasMS1Scan(MSFile file) {
		MSXMLParser m_parser = new MSXMLParser(file.getFileName());
		org.systemsbiology.jrap.grits.stax.Scan firstScan = null;
		if ( m_parser != null ) {
			for( int i = 1; firstScan == null && i < m_parser.getMaxScanNumber() + 1; i++ ) {
				ScanHeader header = m_parser.rapHeader(i);
				if ( header != null && header.getMsLevel() == 1 ) {
					org.systemsbiology.jrap.grits.stax.Scan s = m_parser.rap(i);
					if( s.getMassIntensityList() != null && s.getMassIntensityList()[0].length > 0 ) {
						firstScan = m_parser.rap(i);
						if (firstScan != null)
							return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public List<ScanView> readMSFileForView(MSFile file, int msLevel, int parentScanNum, int scanNum) {
		File mzXMLFile = new File(file.getFileName());
		try {			
			MSXMLParser parser = new MSXMLParser(mzXMLFile.getAbsolutePath());
			updateListeners("Reading XML file", -1);
			List<ScanView> scans = getScanViewData(parser, msLevel, parentScanNum, scanNum);
			if( scans != null ) {
				Collections.sort(scans);
			}
			if( scans.isEmpty() ) {
				if (!isCanceled())
					updateErrorListener("Warning: no scan data read from MS file. The file may be invalid or incorrect type.");
			}
			return scans;
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return null;
	}

	private List<ScanView> getScanViewData(MSXMLParser parser, int msLevel, int parentScanNum, int scanNum) {
		try {
			int precursorScanNum = 0;
			List<ScanView> scans = new ArrayList<>();
			HashMap<Integer, Integer> lastParentOfEachLevel = new HashMap<Integer, Integer>();
			HashMap<Integer, ScanView> msScanMap = new HashMap<Integer, ScanView>();
			int iStartScan = getFirstScanNumber(parser);
			if( iStartScan == -1 ) {
				return new ArrayList<>();
			}
			int iEndScan = parser.getMaxScanNumber();

			int iLastPossiblePrecursorMSLevel = parser.rapHeader(iStartScan).getMsLevel(); // can't assume we have MS1 scans. Keep track of possible precursors
			int iMinMSLevel = iLastPossiblePrecursorMSLevel; // can't assume we have MS1 scans. Keep track of possible precursors
			ScanView firstParentScan = null;
			if( iLastPossiblePrecursorMSLevel > 1 ) { // no parent scan. Create one so everything jives
				firstParentScan = new ScanView();
				firstParentScan.setScanNo(0);
				firstParentScan.setMsLevel(1);
				firstParentScan.setRetentionTime(0.0);
				msScanMap.put(firstParentScan.getScanNo(), firstParentScan);
				iLastPossiblePrecursorMSLevel = 1;
				lastParentOfEachLevel.put(1, 0);
				scans.add(firstParentScan);
			} 

			ScanView msScan = null;
			ScanView parentScan = null;
			for (int i = iStartScan; i <= iEndScan; i++) {
				try {
					if( isCanceled() ) {
						return new ArrayList<>();
					}
					if( (i % 10) == 0 ) {
						if (!isCanceled())
							updateListeners("Reading XML file. Scan: " + i + " of " + iEndScan, i);
					}
					/*	if( scanNum != -1 && i != scanNum) { // if scan number is specified, we don't need to rap every scan!
						continue;
					} */

					// I've never seen more than 1000 ms/ms events for a single parent scan
					// this should improve performance
					if( parentScanNum != -1 && (i < parentScanNum || i > (parentScanNum+1000) ) ) {
						continue;
					}
					org.systemsbiology.jrap.grits.stax.Scan jrapScan = getJrapScan(parser, i);
					if( jrapScan == null ) {
						//	if (!isCanceled())
						//		updateErrorListener("Call to getJrapScan for scan number " + i + " returned null. Skipping.");
						continue;
					}
					org.systemsbiology.jrap.grits.stax.ScanHeader jrapScanHeader = jrapScan.getHeader();

					// Create a peak for the precursor to be added to MS1 scan
					int iCurMSLevel = jrapScanHeader.getMsLevel();

					if( iLastPossiblePrecursorMSLevel < iCurMSLevel - 1 ) {
						iLastPossiblePrecursorMSLevel = iCurMSLevel -1;
					} else if ( iLastPossiblePrecursorMSLevel == iCurMSLevel && iCurMSLevel - 1 >= iMinMSLevel ) {
						iLastPossiblePrecursorMSLevel = iCurMSLevel - 1;
					}
					precursorScanNum = -1;
					if( jrapScanHeader.getPrecursorScanNum() != -1 ) {
						precursorScanNum = jrapScanHeader.getPrecursorScanNum();
					} else if( iCurMSLevel > iLastPossiblePrecursorMSLevel && lastParentOfEachLevel.containsKey(iCurMSLevel - 1)) { // has precursor
						precursorScanNum = lastParentOfEachLevel.get(iCurMSLevel - 1);
					} else if ( parentScanNum != -1 && iCurMSLevel > 1 ) {
						precursorScanNum = parentScanNum; // what else can we do??
					}

					msScan = getScanView(jrapScan, precursorScanNum);
					int iParentScanNum = msScan.getParentScan();
					if( iParentScanNum != -1 ) {
						if( msScanMap.containsKey(iParentScanNum) ) {
							parentScan = msScanMap.get(iParentScanNum);
						} else {
							org.systemsbiology.jrap.grits.stax.Scan jrapParentScan = getJrapScan(parser, iParentScanNum);
							parentScan = getScanView(jrapParentScan, -1);
							if( parentScan == null ) {
								continue;
							}
							msScanMap.put(parentScan.getScanNo(), parentScan);
						}
					}
					lastParentOfEachLevel.put(iCurMSLevel, i);
					msScanMap.put(msScan.getScanNo(), msScan);

					if( msScan.getMsLevel() > iLastPossiblePrecursorMSLevel && parentScan != null) { // precursor peak is built on fly
						parentScan.getSubScans().add(msScan);
					}

					if( i == scanNum || i == parentScanNum ||
							(precursorScanNum != -1 && (precursorScanNum == parentScanNum)) ||
							(scanNum == -1 && parentScanNum == -1 && iCurMSLevel == msLevel) ) {
						if (scanNum != -1 && i != scanNum && scanNum != msScan.getParentScan()) {
							// do nothing
						} else
							scans.add(msScan);
					}

					if( iCurMSLevel < iMinMSLevel ) {
						iMinMSLevel = iCurMSLevel;
					}
				} catch( Exception ex ) {
					logger.error(ex.getMessage(), ex);
					updateErrorListener("Error parsing scan number: " + i, ex);
				}
			}
			return scans;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	private ScanView getScanView(org.systemsbiology.jrap.grits.stax.Scan jrapScan, int iLastPrecursorScanNum) {
		if( jrapScan == null ) {
			return null;
		}
		ScanHeader jrapScanHeader = jrapScan.getHeader();
		ScanView msScan = new ScanView();
		int precursorScanNum = -1;
		if (jrapScanHeader.getPrecursorScanNum() != -1) {
			precursorScanNum = jrapScanHeader.getPrecursorScanNum();
		} else  { // has precursor
			precursorScanNum = iLastPrecursorScanNum;						 
		}
		msScan.setPreCursorMz((double)jrapScanHeader.getPrecursorMz());
		msScan.setPreCursorIntensity((double)jrapScanHeader.getPrecursorIntensity());
		msScan.setParentScan(precursorScanNum);
		msScan.setMsLevel(jrapScanHeader.getMsLevel());

		msScan.setScanNo(jrapScanHeader.getNum());
		try {
			msScan.setRetentionTime(jrapScanHeader.getDoubleRetentionTime());
		} catch( Exception e1 ) {
			;
		}
		return msScan;
	}

	@Override
	public Map<Integer, List<Integer>> readMSFileForSubscans(MSFile file) {
		Map<Integer, List<Integer>> subScanMap = new HashMap<>();
		File mzXMLFile = new File(file.getFileName());	
		try {
			MSXMLParser parser = new MSXMLParser(mzXMLFile.getAbsolutePath());
			int iStartScan = getFirstScanNumber(parser);
			int iEndScan = parser.getMaxScanNumber();
			for (int i=iStartScan; i < iEndScan; i++) {
				List<Integer> subScans = new ArrayList<>();
				org.systemsbiology.jrap.grits.stax.Scan jrapScan = parser.rap(i);
				subScanMap.put(i, subScans);
			}
			for (int i=iStartScan; i < iEndScan; i++) {
				org.systemsbiology.jrap.grits.stax.Scan jrapScan = parser.rap(i);
				ScanHeader jrapHeader = jrapScan.getHeader();
				if (jrapHeader != null && jrapHeader.getMsLevel() > 1) {
					Integer parentScan = jrapHeader.getPrecursorScanNum();
					if (parentScan != null) {
						subScanMap.get(parentScan).add(i);
					}
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return subScanMap;
	}

}
