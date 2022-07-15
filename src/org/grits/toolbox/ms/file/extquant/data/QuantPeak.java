package org.grits.toolbox.ms.file.extquant.data;

import java.util.ArrayList;
import java.util.List;

import org.grits.toolbox.ms.file.reader.impl.InvalidFileFormatException;

public class QuantPeak
{
    private Double m_massMonoIsotopic = null;
    private Double m_massAveragine = null;
    private Double m_mz = null;
    private Double m_sumIntensity = null;
    
    private List<QuantPeakMatch> m_match = new ArrayList<QuantPeakMatch>();
    
    public void setSumIntensity(Double m_sumIntensity) {
		this.m_sumIntensity = m_sumIntensity;
	}
    
    public Double getSumIntensity() {
		return m_sumIntensity;
	}
    
    public Double getMz() {
		return m_mz;
	}
    public void setMz(Double m_mz) {
		this.m_mz = m_mz;
	}
    public Double getMassMonoIsotopic()
    {
        return m_massMonoIsotopic;
    }
    public void setMassMonoIsotopic(Double a_massMonoIsotopic)
    {
        m_massMonoIsotopic = a_massMonoIsotopic;
    }
    public Double getMassAveragine()
    {
        return m_massAveragine;
    }
    public void setMassAveragine(Double a_massAveragine)
    {
        m_massAveragine = a_massAveragine;
    }
    public List<QuantPeakMatch> getMatch()
    {
        return m_match;
    }
    public void setMatch(List<QuantPeakMatch> a_match)
    {
        m_match = a_match;
    }
    public boolean add(QuantPeakMatch a_match)
    {
        return this.m_match.add(a_match);
    }
    
	public static QuantPeak getQuantPeakData(double _dMz) throws InvalidFileFormatException {
		QuantPeak qp = new QuantPeak();
		try {
			qp.setMz(_dMz);
		}
		catch (Exception e)
		{
			throw new InvalidFileFormatException("Invalid m/z value: " + _dMz);
		}
		return qp;
	}
    
}
