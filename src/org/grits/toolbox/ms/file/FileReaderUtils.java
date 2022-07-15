package org.grits.toolbox.ms.file;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.grits.toolbox.ms.file.reader.IMSFileReader;
import org.grits.toolbox.ms.om.data.Peak;
import org.grits.toolbox.ms.om.data.Scan;

public class FileReaderUtils {
	
	// log4J Logger
	private static final Logger logger = Logger.getLogger(FileReaderUtils.class);

	public static LinkedHashMap<Integer, Scan> listToHashMap(List<Scan> scans) {
		try {
			LinkedHashMap<Integer, Scan> scansMap = new LinkedHashMap<Integer, Scan>();
			if( scans == null ) 
				return null;
			for (Scan scan : scans) {
				List<Peak> alPeaks = scan.getPeaklist();
				if( alPeaks != null ) {
					Collections.sort(alPeaks);
				}
				scansMap.put(scan.getScanNo(), scan);
			}
			return scansMap;
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return null;
	}

	public static HashMap<Integer, Scan> listToFilteredHashMap(List<Scan> scans, double dFragCutoff, String sFragCutoffType, double dPreCutoff, String sPreCutoffType) {
		HashMap<Integer, Scan> scansMap = new HashMap<Integer, Scan>();
		try {
			for (Scan scan : scans) {
				if( sFragCutoffType != null && dFragCutoff > 0.0 && scan.getMsLevel() != 1 ) {
					filterPeakList(scan, dFragCutoff, sFragCutoffType);
				}
				if( sPreCutoffType != null && dPreCutoff > 0.0 && scan.getMsLevel() == 1 ) {
					filterPeakList(scan, dPreCutoff, sPreCutoffType);
				}
				scansMap.put(scan.getScanNo(), scan);
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return scansMap;
	}
	
	private static void filterPeakList(Scan scan, double cutOff, String cutOffType) {
		double maxIntensity = 0.0, minIntensity = 0.0;
		List<Peak> peakList = new ArrayList<Peak>();
		try {
			if (cutOffType.equals(IMSFileReader.FILTER_PERCENTAGE)) {
				maxIntensity = scan.getMostAbundantPeak();
			}
			if (maxIntensity != 0.0) {
				minIntensity = maxIntensity * (cutOff / 100);
			}
			double dTotalIntensity = 0.0;
			int iTotalNumPeaks = 0;
			for (Peak peak : scan.getPeaklist()) {
				// added 03/02/16 to skip any empty peaks
				if( peak.getMz() <= 0.0 ) {
					continue;
				}
				iTotalNumPeaks++;
				dTotalIntensity+=peak.getMz();
				if (cutOffType.equals(IMSFileReader.FILTER_PERCENTAGE)) {
					if (peak.getIntensity() > minIntensity)
						peakList.add(peak);
				} else {
					if (peak.getIntensity() > cutOff)
						peakList.add(peak);
				}
			}
			scan.setTotalNumPeaks(iTotalNumPeaks);
			scan.setTotalIntensity(dTotalIntensity);
			scan.setPeaklist(peakList);

		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
	}
}
