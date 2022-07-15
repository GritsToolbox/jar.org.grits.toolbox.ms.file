package org.grits.toolbox.ms.file.reader;

import org.grits.toolbox.ms.file.MSFile;
import org.grits.toolbox.ms.file.extquant.data.QuantPeakData;

public interface IMSExtQuantFileReader extends IMSFileReader {

	QuantPeakData read(MSFile file, boolean a_ppm, double a_intervalValue);
}
