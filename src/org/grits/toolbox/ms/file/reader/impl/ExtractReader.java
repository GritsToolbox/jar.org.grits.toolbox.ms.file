package org.grits.toolbox.ms.file.reader.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.apache.log4j.Logger;
import org.grits.toolbox.ms.file.MSFile;
import org.grits.toolbox.ms.file.extquant.data.QuantPeak;
import org.grits.toolbox.ms.file.extquant.data.QuantPeakData;
import org.grits.toolbox.ms.file.extquant.data.QuantPeakMatch;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class ExtractReader extends ExternalQuantFileReader
{
	private static final Logger logger = Logger.getLogger(ExtractReader.class);
	
	public ExtractReader() {
		super();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public QuantPeakData read(String a_file, boolean a_ppm, double a_intervalValue) {
		this.m_ppm = a_ppm;
		this.m_intervalValue = a_intervalValue;
		String t_content;
		try {
			File t_file = new File(a_file);
			t_content = this.cleanTermoMess(t_file);
			SAXBuilder builder = new SAXBuilder();
			Document t_document = builder.build(new StringReader(t_content));
			Element t_root = t_document.getRootElement();
			if ( !t_root.getName().equals("Xtract") )
			{
				throw new InvalidFileFormatException("Invalid file format: missing Xtract tag");
			}
			this.m_data = new QuantPeakData();
			List<Element> t_headList = t_root.getChildren("Head");
			for (Element t_head : t_headList)
			{
				this.readHead(t_head);
			}        
			List<Element> t_list = t_root.getChildren("Mono");
			for (Element t_mono : t_list)
			{
				this.readMono(t_mono);
			}
			return this.m_data;
		} catch (IOException e) {
			logger.error("Could not read extract file", e);
		} catch (JDOMException e) {
			logger.error("Could not read extract file", e);
		} catch (InvalidFileFormatException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

    private String cleanTermoMess(File a_file) throws IOException 
    {
        StringBuffer t_buffer = new StringBuffer("");
        BufferedReader t_reader = null;
        t_reader = new BufferedReader(new FileReader(a_file));
        String t_line = null;
        while ((t_line = t_reader.readLine()) != null)
        {
            t_buffer.append(t_line.trim() + "\n");
        }
        t_reader.close();
        String t_content = t_buffer.toString();
        
        return t_content.substring(t_content.indexOf("<?xml"));
    }
    
    private void readHead(Element a_head) throws InvalidFileFormatException
    {
        String t_value = a_head.getAttributeValue("Scan");
        if ( t_value == null )
        {
            throw new InvalidFileFormatException("Scan is missing for Head tag");
        }
        t_value = t_value.trim();
        try
        {
        	this.m_data.setScanNo(Integer.parseInt(t_value));
        }
        catch (Exception e)
        {
            throw new InvalidFileFormatException("Scan is not a integer value: " + t_value);
        }
        t_value = a_head.getAttributeValue("RT");
        if ( t_value == null )
        {
            throw new InvalidFileFormatException("RT is missing for Head tag");
        }
        t_value = t_value.trim();
        try
        {
        	this.m_data.setRetentionTime(Double.parseDouble(t_value));
        }
        catch (Exception e)
        {
            throw new InvalidFileFormatException("RT is not a double value: " + t_value);
        }
    }    

    @SuppressWarnings("unchecked")
    private void readMono(Element a_mono) throws InvalidFileFormatException
    {
        QuantPeak t_peak = new QuantPeak();
        String t_value = a_mono.getAttributeValue("MonoisoMass");
        if ( t_value == null )
        {
            throw new InvalidFileFormatException("MonoisoMass is missing for Mono tag");
        }
        t_value = t_value.trim();
        try
        {
            t_peak.setMassMonoIsotopic(Double.parseDouble(t_value));
        }
        catch (Exception e)
        {
            throw new InvalidFileFormatException("MonoisoMass is not a double value: " + t_value);
        }
        t_value = a_mono.getAttributeValue("AveragineMass");
        if ( t_value == null )
        {
            throw new InvalidFileFormatException("AveragineMass is missing for Mono tag");
        }
        t_value = t_value.trim();
        try
        {
            t_peak.setMassAveragine(Double.parseDouble(t_value));
        }
        catch (Exception e)
        {
            throw new InvalidFileFormatException("AveragineMass is not a double value: " + t_value);
        }
        t_value = a_mono.getAttributeValue("SumIntensity");
        if ( t_value == null )
        {
            throw new InvalidFileFormatException("SumIntensity is missing for Mono tag");
        }
        t_value = t_value.trim();
        try
        {
            t_peak.setSumIntensity(Double.parseDouble(t_value));
        }
        catch (Exception e)
        {
            throw new InvalidFileFormatException("SumIntensity is not a double value: " + t_value);
        }
        List<Element> t_list = a_mono.getChildren("Charged");
        for (Element t_charged : t_list)
        {
            this.readCharged(t_charged,t_peak);
        }
        this.m_data.add(t_peak);
    }

    @SuppressWarnings("unchecked")
    private void readCharged(Element a_charged, QuantPeak a_peak) throws InvalidFileFormatException
    {
        List<Element> t_list = a_charged.getChildren("Match");
        for (Element t_match : t_list)
        {
            this.readMatch(t_match,a_peak);
        }
    }

    private void readMatch(Element a_match, QuantPeak a_peak) throws InvalidFileFormatException
    {
        QuantPeakMatch t_match = new QuantPeakMatch(a_peak);
        String t_value = a_match.getAttributeValue("Chg");
        if ( t_value == null )
        {
            throw new InvalidFileFormatException("Chg is missing for Match tag");
        }
        t_value = t_value.trim();
        try
        {
            t_match.setCharge(Integer.parseInt(t_value));
        }
        catch (Exception e)
        {
            throw new InvalidFileFormatException("Chg is not a number: " + t_value);
        }
        t_value = a_match.getAttributeValue("SumInt");
        if ( t_value == null )
        {
            throw new InvalidFileFormatException("SumInt is missing for Match tag");
        }
        t_value = t_value.trim();
        try
        {
            t_match.setIntensitySum(Double.parseDouble(t_value) / (double)(t_match.getCharge()));
        }
        catch (Exception e)
        {
            throw new InvalidFileFormatException("SumInt is not a double value: " + t_value);
        }
        t_value = a_match.getAttributeValue("MoiMz");
        if ( t_value == null )
        {
            throw new InvalidFileFormatException("MoiMz is missing for Match tag");
        }
        t_value = t_value.trim();
        try
        {
            t_match.setMzMostAbundant(Double.parseDouble(t_value));
            this.setMinMax(t_match,t_match.getMzMostAbundant());
        }
        catch (Exception e)
        {
            throw new InvalidFileFormatException("MoiMz is not a double value: " + t_value);
        }
        a_peak.add(t_match);
    }

    private void setMinMax(QuantPeakMatch a_match, Double a_mzMostAbundant)
    {
        if ( this.m_ppm )
        {
            double t_value = a_mzMostAbundant * this.m_intervalValue / 1000000D;
            a_match.setMinMz( a_mzMostAbundant - t_value );
            a_match.setMaxMz( a_mzMostAbundant + t_value );
        }
        else
        {
            a_match.setMinMz( a_mzMostAbundant - this.m_intervalValue );
            a_match.setMaxMz( a_mzMostAbundant + this.m_intervalValue );
        }
    }

	@Override
	public boolean isValid(MSFile file) {
		try {
			File t_file = new File(file.getFileName());
			String t_content = this.cleanTermoMess(t_file);
			SAXBuilder builder = new SAXBuilder();
			Document t_document = builder.build(new StringReader(t_content));
			Element t_root = t_document.getRootElement();
			if (t_root.getName().equals("Xtract")) 
				return true;
		} catch (IOException | JDOMException e) {
			return false;
		} 
		return false;
	}
}
