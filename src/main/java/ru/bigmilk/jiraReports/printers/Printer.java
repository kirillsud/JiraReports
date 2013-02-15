package ru.bigmilk.jiraReports.printers;

import ru.bigmilk.jiraReports.ReportBuilder;

import java.io.OutputStream;

public interface Printer {

    /**
     * Print records into inputStream
     *
     * @param out stream to output
     * @param reportBuilder report builder
     * @throws PrinterException
     */
    public void print(OutputStream out, ReportBuilder reportBuilder) throws PrinterException;

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
