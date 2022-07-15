package org.grits.toolbox.ms.file.extquant.data;

import org.grits.toolbox.ms.file.MSFile;
import org.grits.toolbox.ms.om.data.CustomExtraData;

public class ExternalQuantSettings
{
    protected MSFile m_externalQuantFile = null;
    protected Integer m_targetScanNumber = -1;
    protected boolean m_usePpm = true;
    protected Double m_peakMatchingTolerance = 0D;
    protected Boolean m_quantifyPrecursor = Boolean.TRUE;
    
    public ExternalQuantSettings( MSFile a_externalQuantFile, boolean a_usePpm, Double a_peakMatchingTolerance)
    {
    	this.m_externalQuantFile = a_externalQuantFile;
    	this.m_usePpm = a_usePpm;
    	this.m_peakMatchingTolerance = a_peakMatchingTolerance;
    }
    
	public static CustomExtraData getExternalQuantScanNumber( String _sKeyPrefix, String _sLabelPrefix ) { 
		return new CustomExtraData( _sKeyPrefix + "_quant_scan_number", _sLabelPrefix + " MS Scan #", 
					"Generic Method", CustomExtraData.Type.Integer );
	}

	public static CustomExtraData getExternalQuantFileName( String _sKeyPrefix, String _sLabelPrefix ) { 
		return new CustomExtraData( _sKeyPrefix + "_quant_file_name", _sLabelPrefix + " File Name", 
					"Generic Method", CustomExtraData.Type.String );
	}

	public static CustomExtraData getExternalQuantUsePPM( String _sKeyPrefix, String _sLabelPrefix ) { 
		return new CustomExtraData( _sKeyPrefix + "_quant_use_ppm", _sLabelPrefix + " Is PPM", 
					"Generic Method", CustomExtraData.Type.Boolean );
	}

	public static CustomExtraData getExternalQuantMatchingTolerance( String _sKeyPrefix, String _sLabelPrefix ) { 
		return new CustomExtraData( _sKeyPrefix + "_quant_tolerance", _sLabelPrefix + " Tolerance", 
					"Generic Method", CustomExtraData.Type.Double );
	}
	
    public Integer getTargetScanNumber() {
		return m_targetScanNumber;
	}
    
    public void setTargetScanNumber(Integer m_targetScanNumber) {
		this.m_targetScanNumber = m_targetScanNumber;
	}
    
    public MSFile getCorrectedFile()
    {
        return m_externalQuantFile;
    }

    public void setCorrectedFileName(MSFile a_externalQuantFile)
    {
        m_externalQuantFile = a_externalQuantFile;
    }

    public boolean isIntensityCorrectionPpm()
    {
        return m_usePpm;
    }

    public void setIntensityCorrectionPpm(boolean a_usePpm)
    {
        m_usePpm = a_usePpm;
    }

    public Double getIntensityCorrectionValue()
    {
        return m_peakMatchingTolerance;
    }

    public void setIntensityCorrectionValue(Double a_peakMatchingTolerance)
    {
        m_peakMatchingTolerance = a_peakMatchingTolerance;
    }
    
    public Boolean getQuantifyPrecursor() {
		return m_quantifyPrecursor;
	}
    public void setQuantifyPrecursor(Boolean m_quantifyPrecursor) {
		this.m_quantifyPrecursor = m_quantifyPrecursor;
	}
 }