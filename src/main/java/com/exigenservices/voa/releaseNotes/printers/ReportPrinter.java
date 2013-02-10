package com.exigenservices.voa.releaseNotes.printers;

import com.exigenservices.voa.releaseNotes.IssueNote;

import java.io.BufferedWriter;
import java.io.IOException;

public class ReportPrinter extends AbstractPrinter {
    @Override
    protected void printNote(BufferedWriter writer, IssueNote note) throws IOException {
        writer.write("");
    }

    @Override
    public String getName() {
        return "report";
    }

    @Override
    public String getDescription() {
        return "work log report in wiki format";
    }
}
