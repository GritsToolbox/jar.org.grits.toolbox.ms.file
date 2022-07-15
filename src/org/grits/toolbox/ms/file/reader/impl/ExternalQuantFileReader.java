package org.grits.toolbox.ms.file.reader.impl;

import org.grits.toolbox.ms.file.MSFile;
import org.grits.toolbox.ms.file.extquant.data.QuantPeakData;
import org.grits.toolbox.ms.file.reader.IMSExtQuantFileReader;
import org.grits.toolbox.widgets.tools.NotifyingProcess;

public abstract class ExternalQuantFileReader extends NotifyingProcess implements IMSExtQuantFileReader
{
    protected QuantPeakData m_data = null;
    protected boolean m_ppm = false;
    protected double m_intervalValue = 500D;
        
    public abstract QuantPeakData read(String a_file, boolean a_ppm, double a_intervalValue);

    @Override
    public QuantPeakData read(MSFile file, boolean a_ppm, double a_intervalValue) {
    	return read(file.getFileName(), a_ppm, a_intervalValue);
    }
     
}
