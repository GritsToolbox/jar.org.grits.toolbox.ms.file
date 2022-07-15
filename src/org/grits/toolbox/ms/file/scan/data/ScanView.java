package org.grits.toolbox.ms.file.scan.data;

import java.util.ArrayList;
import java.util.List;

public class ScanView implements Comparable<ScanView>{
	private Integer scanNo = null;
    private Double retentionTime = null;
    private Integer parentScan = null;
    private Integer msLevel = null;
    private Double preCursorIntensity = null;
    private Double preCursorMz = null;
    private List<ScanView> subScans = new ArrayList<>();
    
	@Override
	public int compareTo(ScanView o) {
		return scanNo - o.scanNo;
	}

	/**
	 * @return the scanNo
	 */
	public Integer getScanNo() {
		return scanNo;
	}

	/**
	 * @param scanNo the scanNo to set
	 */
	public void setScanNo(Integer scanNo) {
		this.scanNo = scanNo;
	}

	/**
	 * @return the retentionTime
	 */
	public Double getRetentionTime() {
		return retentionTime;
	}

	/**
	 * @param retentionTime the retentionTime to set
	 */
	public void setRetentionTime(Double retentionTime) {
		this.retentionTime = retentionTime;
	}

	/**
	 * @return the parentScan
	 */
	public Integer getParentScan() {
		return parentScan;
	}

	/**
	 * @param parentScan the parentScan to set
	 */
	public void setParentScan(Integer parentScan) {
		this.parentScan = parentScan;
	}

	/**
	 * @return the msLevel
	 */
	public Integer getMsLevel() {
		return msLevel;
	}

	/**
	 * @param msLevel the msLevel to set
	 */
	public void setMsLevel(Integer msLevel) {
		this.msLevel = msLevel;
	}

	/**
	 * @return the subScans
	 */
	public List<ScanView> getSubScans() {
		return subScans;
	}

	/**
	 * @param subScans the subScans to set
	 */
	public void setSubScans(List<ScanView> subScans) {
		this.subScans = subScans;
	}
	
	/**
	 * 
	 * @return
	 */
	public Double getPreCursorIntensity() {
		return preCursorIntensity;
	}
	
	/**
	 * 
	 * @param preCursorIntensity
	 */
	public void setPreCursorIntensity(Double preCursorIntensity) {
		this.preCursorIntensity = preCursorIntensity;
	}
	
	public Double getPreCursorMz() {
		return preCursorMz;
	}
	
	public void setPreCursorMz(Double preCursorMz) {
		this.preCursorMz = preCursorMz;
	}
}
