/**
 * 
 */
package org.grits.toolbox.ms.file.reader;

import org.grits.toolbox.ms.file.MSFile;
import org.grits.toolbox.widgets.tools.INotifyingProcess;

/**
 * @author sena
 * 
 * Interface for all the readers of MassSpec files
 *
 */
public interface IMSFileReader extends INotifyingProcess{
	public static final String FILTER_PERCENTAGE = "Percentage";
	public static final String FILTER_ABSOLUTE = "Absolute Value";
	
	/**
	 * Checks whether the given file can be read by this reader
	 * 
	 * @param file MS file to be read
	 * @return true if the file is valid according to this reader, false otherwise
	 */
	boolean isValid (MSFile file);
}
