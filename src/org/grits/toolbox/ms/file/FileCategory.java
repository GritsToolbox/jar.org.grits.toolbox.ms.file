package org.grits.toolbox.ms.file;


public enum FileCategory {
	
	ANNOTATION_CATEGORY ("Annotation", "File used for Annotation (such as MzXML/MZML"),
	EXTERNAL_QUANTIFICATION_CATEGORY ("External Quantification", "File used for external quantification (such as eXtract, FullMS)");
	
	private String sLabel;
    private String sDescription;

    private FileCategory( String sLabel, String sDescription ) {
        this.sLabel = sLabel;
        this.sDescription = sDescription;
    }

    public String getLabel() {  
        return this.sLabel;  
    }	

    public String getDescription() {
		return sDescription;
	}
    
    public static FileCategory lookUp( String _sKey ) {
    	if ( ANNOTATION_CATEGORY.name().equals(_sKey ) )
    		return ANNOTATION_CATEGORY;
    	if ( EXTERNAL_QUANTIFICATION_CATEGORY.name().equals(_sKey ) )
    		return EXTERNAL_QUANTIFICATION_CATEGORY;
    	
    	return null;
    }
    
    public static FileCategory findByLabel( String _sLabel ) {
    	if ( ANNOTATION_CATEGORY.getLabel().equals(_sLabel ) )
    		return ANNOTATION_CATEGORY;
    	if ( EXTERNAL_QUANTIFICATION_CATEGORY.getLabel().equals(_sLabel ) )
    		return EXTERNAL_QUANTIFICATION_CATEGORY;
    	
    	return null;
    } 
    
    public static String[] toList () {
    	return new String[] {ANNOTATION_CATEGORY.getLabel(), EXTERNAL_QUANTIFICATION_CATEGORY.getLabel()};
    }
}
