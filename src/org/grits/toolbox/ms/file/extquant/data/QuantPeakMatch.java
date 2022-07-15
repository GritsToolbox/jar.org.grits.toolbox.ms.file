package org.grits.toolbox.ms.file.extquant.data;

import java.util.List;

import org.grits.toolbox.ms.om.data.Peak;

public class QuantPeakMatch implements Comparable<QuantPeakMatch>
{
    private Integer m_charge = null;
    private Double m_mzMostAbundant = null;
    private Double m_intensitySum = null;
    private Double m_minMz = null;
    private Double m_maxMz = null;
    private QuantPeak parent = null;
    
    public QuantPeakMatch(QuantPeak parent) {
		this.parent = parent;
	}
    
    public QuantPeak getParent() {
		return parent;
	}
    
    public Integer getCharge()
    {
        return m_charge;
    }
    public void setCharge(Integer a_charge)
    {
        m_charge = a_charge;
    }
    public Double getMzMostAbundant()
    {
        return m_mzMostAbundant;
    }
    public void setMzMostAbundant(Double a_mzMostAbundant)
    {
        m_mzMostAbundant = a_mzMostAbundant;
    }
    public Double getIntensitySum()
    {
        return m_intensitySum;
    }
    public void setIntensitySum(Double a_intensitySum)
    {
        m_intensitySum = a_intensitySum;
    }
    public Double getMinMz()
    {
        return m_minMz;
    }
    public void setMinMz(Double minMz)
    {
        m_minMz = minMz;
    }
    public Double getMaxMz()
    {
        return m_maxMz;
    }
    public void setMaxMz(Double maxMz)
    {
        m_maxMz = maxMz;
    }

	@Override
	public int compareTo(QuantPeakMatch o) {
		if( getMinMz() < o.getMinMz() ) 
			return -1;
		if( getMinMz() > o.getMinMz() ) 
			return 1;
		return 0;
	}
	
	public static QuantPeakMatch findQuantPeakMatch( QuantPeak _quantPeak, double _candidateMz, List<Peak> _destPeaks, double dTol ) {
//		double dTol = getDaTolerance(_precursorPeak.getMz());
		
		// first find the peak closest to the precursor m/z
		if( _destPeaks == null ) {
			return null;
		}
		int iStartInx = 0; // performance helper since we're going through list multiple times
		Peak closestPeak = null;
		double dMinDelta = Double.MAX_VALUE;
		for( int i = 0; i < _destPeaks.size(); i++ ) {
			Peak msPeak = _destPeaks.get(i);			
			double dDelta = Math.abs( msPeak.getMz() - _candidateMz);
			if( dDelta < (4.0 * dTol) && iStartInx == 0 ) { // starting point of list...4 * tol should do trick
				iStartInx = i;
			}
			if( dDelta < dTol && dDelta < dMinDelta ) {
				closestPeak = msPeak;
				dMinDelta = dDelta;
			}
		}
		if( closestPeak == null )
			return null;
		
		// we have closest peak. Now find highest peak w/in tolerance
		double dMaxInt = Double.MIN_VALUE;
		Peak topPeak = null;
		for( int i = iStartInx; i < _destPeaks.size(); i++ ) {
			Peak msPeak = _destPeaks.get(i);			
			double dDelta = Math.abs( msPeak.getMz() - closestPeak.getMz());
			if( dDelta < 0.05 && msPeak.getIntensity() > dMaxInt ) {
				dMaxInt = msPeak.getIntensity();
				topPeak = msPeak;
			}
		}
		if( topPeak == null )
			return null;
		
		// we have the top peak. create the quant peak match and sum intensity
		QuantPeakMatch qpm = new QuantPeakMatch(_quantPeak);
		qpm.setMzMostAbundant(topPeak.getMz());
		double dSum = 0.0;
		double dMinMz = Double.MAX_VALUE;
		double dMaxMz = Double.MIN_VALUE;
		for( int i = iStartInx; i < _destPeaks.size(); i++ ) {
			Peak msPeak = _destPeaks.get(i);			
			double dDelta = Math.abs( msPeak.getMz() - topPeak.getMz());
			if( dDelta < dTol ) {
				dSum += msPeak.getIntensity();
				if( msPeak.getMz() < dMinMz ) {
					dMinMz = msPeak.getMz();
				}
				if( msPeak.getMz() > dMaxMz ) {
					dMaxMz = msPeak.getMz();
				}
			}
		}
//		qpm.setIntensitySum(dSum);
		qpm.getParent().setSumIntensity(dSum);
		qpm.setIntensitySum(topPeak.getIntensity());
		// DBW: 03-03-15: accounting for tolerance in case the peak is outside the range of the MS profile
		qpm.setMaxMz(dMaxMz + dTol);
		qpm.setMinMz(dMinMz - dTol);
		qpm.setCharge(-1); // not going there....
		
		return qpm;
	}
 
	public static double getDaTolerance( double _dMz, double a_intervalValue, boolean a_ppm ) {
		if ( a_ppm ) {
			double t_value = _dMz * a_intervalValue / 1000000D;
			return t_value;
		}
		return a_intervalValue;
	}

	public void setMinMax(QuantPeakMatch a_match, Double a_mzMostAbundant, double a_intervalValue, boolean a_ppm) {
		if ( a_ppm ) {
			double t_value = a_mzMostAbundant * a_intervalValue / 1000000D;
			a_match.setMinMz( a_mzMostAbundant - t_value );
			a_match.setMaxMz( a_mzMostAbundant + t_value );
		} else {
			a_match.setMinMz( a_mzMostAbundant - a_intervalValue );
			a_match.setMaxMz( a_mzMostAbundant + a_intervalValue);
		}
	}
	
	@Override
	public String toString() {
		return this.getMzMostAbundant().toString();
	}
}
