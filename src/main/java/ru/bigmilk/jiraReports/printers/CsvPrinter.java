package ru.bigmilk.jiraReports.printers;

import ru.bigmilk.jiraReports.ReportRecord;

import java.io.IOException;
import java.io.Writer;

public class CsvPrinter extends AbstractPrinter {

    @Override
    protected boolean filterNote(ReportRecord note) {
        // filter only bugs
        return note.getIssue().getIssueType().getName().equalsIgnoreCase("bug") && super.filterNote(note);

    }

    @Override
    protected void printNote(Writer writer, ReportRecord note) throws IOException {
        writer.write(note.getKey() + getDelimiter() +
                "\"" + note.getIssue().getSummary() + "\"" + getDelimiter() +
                note.getUsername() + getDelimiter() +
                note.getDate() +
                "\n"
        );
    }

    protected String getDelimiter() {
        return ",";
    }

    @Override
    public String getName() {
        return "csv";
    }

    @Override
    public String getDescription() {
        return "comma separated values: <jira key>,<jira summary>,<author>";
    }
}
