package ru.bigmilk.jiraReports.printers;

import ru.bigmilk.jiraReports.ReportRecord;

import java.io.IOException;
import java.io.Writer;

public class CsvPrinter extends AbstractPrinter {

    @Override
    protected boolean filterRecord(ReportRecord record) {
        // filter only bugs
        return record.getIssue().getIssueType().getName().equalsIgnoreCase("bug") && super.filterRecord(record);

    }

    @Override
    protected void printRecord(Writer writer, ReportRecord record) throws IOException {
        writer.write(record.getKey() + getDelimiter() +
                "\"" + record.getIssue().getSummary() + "\"" + getDelimiter() +
                record.getUsername() + getDelimiter() +
                record.getDate() +
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
        return "comma separated values: <jira key>,<jira summary>,<user>";
    }
}
