package ru.bigmilk.jiraReports.printers;

import ru.bigmilk.jiraReports.ReportBuilder;
import ru.bigmilk.jiraReports.ReportRecord;

import java.io.*;
import java.util.Map;

public abstract class AbstractPrinter implements Printer {

    @Override
    public boolean print(OutputStream out, ReportBuilder notes) {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
        Map<String, ReportRecord> commits;
        try {
            commits = getData(notes);
        } catch (Exception e) {
            return false;
        }

        for (String key : commits.keySet()) {
            ReportRecord note = commits.get(key);

            if (!filterNote(note)) {
                continue;
            }

            try {
                printNote(writer, note);
            } catch (IOException e) {
                return false;
            }
        }
        try {
            writer.flush();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    protected Map<String, ReportRecord> getData(ReportBuilder notes) throws Exception {
        return notes.getCommits();
    }

    protected boolean filterNote(ReportRecord note) {
        return true;
    }

    protected String prepareComment(ReportRecord note) {
        return prepareComment(note.getComment(), note);
    }

    protected String prepareComment(String comment, ReportRecord note) {
        // make first letter uppercase (for beautiful output)
        comment = comment.substring(0, 1).toUpperCase() + comment.substring(1);

        return comment;
    }

    protected abstract void printNote(Writer writer, ReportRecord note) throws IOException;
}
