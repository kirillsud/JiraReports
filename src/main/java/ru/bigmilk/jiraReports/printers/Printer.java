package ru.bigmilk.jiraReports.printers;

import ru.bigmilk.jiraReports.ReportBuilder;

import java.io.OutputStream;

public interface Printer {

    /**
     * Print notes into inputStream
     *
     * @param out stream to output
     * @param notes list of notes
     * @return false, if printer can't print notes
     */
    public boolean print(OutputStream out, ReportBuilder notes);

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
