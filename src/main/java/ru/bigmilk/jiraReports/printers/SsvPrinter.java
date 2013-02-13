package ru.bigmilk.jiraReports.printers;

public class SsvPrinter extends CsvPrinter {

    @Override
    public String getName() {
        return "ssv";
    }

    @Override
    public String getDescription() {
        return "semicolon separated values: <jira key>;<jira summary>;<author>";
    }

    @Override
    protected String getDelimiter() {
        return ";";
    }
}
