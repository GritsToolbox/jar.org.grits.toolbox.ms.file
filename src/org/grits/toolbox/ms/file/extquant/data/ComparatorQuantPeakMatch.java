package org.grits.toolbox.ms.file.extquant.data;

import java.util.Comparator;

public class ComparatorQuantPeakMatch  implements Comparator<QuantPeakMatch>
{

    public int compare(QuantPeakMatch a_entry0, QuantPeakMatch a_entry1)
    {
        return a_entry0.getMaxMz().compareTo(a_entry1.getMaxMz());
    }

}