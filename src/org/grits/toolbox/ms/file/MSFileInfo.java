package org.grits.toolbox.ms.file;

public class MSFileInfo {
	public static final String MSMETADATA_TYPE = "details";
    public static final String MSMETADATA_DESC = "User-specified information from entry creation.";
    
    public static final String MSLOCKFILE_TYPE = "lockfile";

    public static final String MS_FILE = "MS";
    public static final String MS_FULL_FILE = "MSFULL";
    
	public static final String MSFORMAT_RAW_CURRENT_VERSION = "1.0";
	public static final String MSFORMAT_RAW_TYPE = "RAW";
    public static final String MSFORMAT_RAW_DESC = "Thermo RAW (.raw)";
    public static final String MSFORMAT_RAW_EXTENSION = "raw";

	public static final String MSFORMAT_MZML_CURRENT_VERSION = "1.0";
	public static final String MSFORMAT_MZML_TYPE = "mzML";
    public static final String MSFORMAT_MZML_DESC = "Proteomics Standards Initiative (PSI) mzML (.mzML)";
    public static final String MSFORMAT_MZML_EXTENSION = "mzML";

	public static final String MSFORMAT_MZXML_CURRENT_VERSION = "1.0";
	public static final String MSFORMAT_MZXML_TYPE = "mzXML";
    public static final String MSFORMAT_MZXML_DESC = "Institute for Systems Biology (ISB) mzXML (.mzXML)";
    public static final String MSFORMAT_MZXML_EXTENSION = "mzXML";
    
    // file type
    public static final String MS_FILE_TYPE_INSTRUMENT = "Instrument";
    public static final String MS_FILE_TYPE_DATAFILE = "Converted";
    public static final String MS_FILE_TYPE_PROCESSED = "Processed";
    
    
    /*
MGF
Agilent
Bruker FID/YEP/BAF
Thermo RAW
Waters RAW
MGF
MS2/CMS2/BMS2
WIFF

     */
    public static final String MSFILES_FILTER_NAMES = "Thermo (.raw), Waters (.raw), Bruker (.fid, .yep, .baf), Sciex (.wiff), Agilent (.d))";
    public static final String MSFILES_FILTER_EXTENSIONS = "*.raw;*.y;*.mgf;*.fid;*.yep;*.baf;*.gf;*.ms2;*.cms2;*.bms2";
	
    
    public static String getType( String msType, String fileType ) {
    	return msType + "." + fileType;
    }
    
    public static String getMSType( String sType ) {
    	String[] sToks = sType.split("\\.");
    	if( sToks.length != 2 ) {
    		return sType;
    	}
    	return sToks[0];
    }

    public static String getMSFormat( String sType ) {
    	String[] sToks = sType.split("\\.");
    	if( sToks.length != 2 ) {
    		return sType;
    	}
     	return sToks[1];
    }
    
}
