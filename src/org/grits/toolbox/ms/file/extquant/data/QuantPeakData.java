package org.grits.toolbox.ms.file.extquant.data;

import java.util.ArrayList;
import java.util.List;


public class QuantPeakData
{
    private List<QuantPeak> m_peaks = new ArrayList<QuantPeak>();
    private int m_scanNo = -1;
    private double m_retentionTime = -1.0d;
    private double m_maxIntensity = 0.0d;
    
    public double getMaxIntensity() {
		return m_maxIntensity;
	}
    
    public void setMaxIntensity(double m_maxIntensity) {
		this.m_maxIntensity = m_maxIntensity;
	}
    
    public List<QuantPeak> getPeaks()
    {
        return m_peaks;
    }

    public void setPeaks(List<QuantPeak> peaks)
    {
        m_peaks = peaks;
    }

    public boolean add(QuantPeak a_peak)
    {
        return this.m_peaks.add(a_peak);
    } 
    
    public List<QuantPeakMatch> generateAllMatches() {
        List<QuantPeakMatch> t_result = new ArrayList<QuantPeakMatch>();
        for (QuantPeak t_peak : this.m_peaks) {
            for (QuantPeakMatch t_extractMatch : t_peak.getMatch()) {
                t_result.add(t_extractMatch);
            }
        }
        
//        Collections.sort(t_result);
        return t_result;
    }
    
    public int getScanNo() {
		return m_scanNo;
	}
    
    public void setScanNo(int m_scanNo) {
		this.m_scanNo = m_scanNo;
	}
    
    public double getRetentionTime() {
		return m_retentionTime;
	}

    public void setRetentionTime(double m_retentionTime) {
		this.m_retentionTime = m_retentionTime;
	}
    
}
