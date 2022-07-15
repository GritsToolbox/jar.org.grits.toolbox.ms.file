package org.grits.toolbox.ms.file.reader.impl;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.grits.toolbox.ms.file.FileReaderUtils;
import org.grits.toolbox.ms.file.MSFile;
import org.grits.toolbox.ms.file.extquant.data.QuantPeak;
import org.grits.toolbox.ms.file.extquant.data.QuantPeakData;
import org.grits.toolbox.ms.file.extquant.data.QuantPeakMatch;
import org.grits.toolbox.ms.om.data.Peak;
import org.grits.toolbox.ms.om.data.Scan;


public class MSXMLReader extends ExternalQuantFileReader
{
	private static final Logger logger = Logger.getLogger(MSXMLReader.class);
	private List<Peak> precursorPeaks = null;
	private Integer msLevel = null;
	private Integer parentScanNum = null;
	private Integer scanNum = null;
	
	public void setPrecursorPeaks(List<Peak> precursorPeaks) {
		this.precursorPeaks = precursorPeaks;
	}

	public List<Peak> getPrecursorPeaks() {
		return precursorPeaks;
	}

	public void setMsLevel(int msLevel) {
		this.msLevel = msLevel;
	}
	public int getMsLevel() {
		return msLevel;
	}

	public void setParentScanNum(int parentScanNum) {
		this.parentScanNum = parentScanNum;
	}
	public int getParentScanNum() {
		return parentScanNum;
	}
	
	public void setScanNum(int scanNum) {
		this.scanNum = scanNum;
	}
	public int getScanNum() {
		return scanNum;
	}

	public QuantPeakData read(String a_file, boolean a_ppm, double a_intervalValue, int msLevel, int parentScanNum, int scanNum) {
		setMsLevel(msLevel);
		setParentScanNum(parentScanNum);
		setScanNum(scanNum);
		return read(a_file, a_ppm, a_intervalValue);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public QuantPeakData read(String a_file, boolean a_ppm, double a_intervalValue) {
		this.m_ppm = a_ppm;
		this.m_intervalValue = a_intervalValue;
		try {
			MzXmlReader t_reader = new MzXmlReader();
			List<Scan> scans = t_reader.readMzXmlFile(a_file, getMsLevel(), getParentScanNum(), getScanNum() ); // assuming just grap Scan 1
			Map<Integer,Scan> t_data = FileReaderUtils.listToHashMap(scans);
			Scan t_scan = t_data.get(1);
			this.m_data = new QuantPeakData();
			this.m_data.setMaxIntensity(t_scan.getMostAbundantPeak());
			setHeaderData(t_scan);
			for( Peak peak : precursorPeaks ) {
				if( ! peak.getIsPrecursor() ) 
					continue;
				QuantPeak qp = QuantPeak.getQuantPeakData(peak.getMz());		
				double dTol = QuantPeakMatch.getDaTolerance(peak.getMz(), this.m_intervalValue, this.m_ppm);
				QuantPeakMatch qpm = QuantPeakMatch.findQuantPeakMatch(qp, peak.getMz(), t_scan.getPeaklist(), dTol );
				if( qpm != null ) {
					qp.add(qpm);
					this.m_data.add(qp);
				}
			}
		} catch( InvalidFileFormatException e ) {
			logger.error(e.getMessage(),e);
			e.printStackTrace();				
		} catch( Exception e ) {
			logger.error(e.getMessage(),e);
			e.printStackTrace();

		}

		return this.m_data;
	}

	private void setHeaderData(Scan a_scan) throws InvalidFileFormatException
	{
		try
		{
			this.m_data.setScanNo(a_scan.getScanNo());
		}
		catch (Exception e)
		{
			throw new InvalidFileFormatException("Invalid scan number: " + a_scan.getScanNo());
		}
		try
		{
			this.m_data.setRetentionTime(a_scan.getRetentionTime());
		}
		catch (Exception e)
		{
			throw new InvalidFileFormatException("Invalid retention time: " + a_scan.getRetentionTime());
		}
	}

	/**
	 * Full MS file has to have an MS1 scan
	 */
	@Override
	public boolean isValid(MSFile file) {
		MzXmlReader t_reader = new MzXmlReader();
		return t_reader.hasMS1Scan(file);
	}
}
