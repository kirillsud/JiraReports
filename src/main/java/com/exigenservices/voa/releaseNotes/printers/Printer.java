package com.exigenservices.voa.releaseNotes.printers;

import com.exigenservices.voa.releaseNotes.ReleaseNotes;
import java.io.OutputStream;

public interface Printer {

    /**
     * Print notes into inputStream
     *
     * @param out stream to output
     * @param notes list of notes
     * @return false, if printer can't print notes
     */
    public boolean print(OutputStream out, ReleaseNotes notes);

    /**
     * @return name of the current printer
     */
    public String getName();

    /**
     *
     *
     * @return description of the current printer
     */
    public String getDescription();
}
