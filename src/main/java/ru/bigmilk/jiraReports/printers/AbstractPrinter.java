package ru.bigmilk.jiraReports.printers;

import ru.bigmilk.jiraReports.ReportBuilder;
import ru.bigmilk.jiraReports.ReportRecord;

import java.io.*;
import java.util.Map;

public abstract class AbstractPrinter implements Printer {

    @Override
    public void print(OutputStream out, ReportBuilder reportBuilder) throws PrinterException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
        Map<String, ReportRecord> commits;
        try {
            commits = getData(reportBuilder);
        } catch (Exception e) {
            throw new PrinterException(e.getMessage());
        }

        for (String key : commits.keySet()) {
            ReportRecord record = commits.get(key);

            if (!filterRecord(record)) {
                continue;
            }

            try {
                printRecord(writer, record);
            } catch (IOException e) {
                throw new PrinterException(e.getMessage());
            }
        }
        try {
            writer.flush();
        } catch (IOException e) {
            throw new PrinterException(e.getMessage());
        }
    }

    protected Map<String, ReportRecord> getData(ReportBuilder reportBuilder) throws Exception {
        return reportBuilder.getCommits();
    }

    protected boolean filterRecord(ReportRecord record) {
        return true;
    }

    protected String prepareComment(ReportRecord record) {
        return prepareComment(record.getComment(), record);
    }

    protected String prepareComment(String comment, ReportRecord record) {
        // make first letter uppercase (for beautiful output)
        comment = comment.substring(0, 1).toUpperCase() + comment.substring(1);

        return comment;
    }

    protected abstract void printRecord(Writer writer, ReportRecord record) throws IOException;
}
