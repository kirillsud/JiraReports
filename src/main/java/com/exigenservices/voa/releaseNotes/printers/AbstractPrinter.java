package com.exigenservices.voa.releaseNotes.printers;

import com.exigenservices.voa.releaseNotes.IssueNote;
import com.exigenservices.voa.releaseNotes.ReleaseNotes;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public abstract class AbstractPrinter implements Printer {

    @Override
    public boolean print(OutputStream out, ReleaseNotes notes) {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));

        for (String key : notes.getNotes().keySet()) {
            IssueNote note = notes.getNotes().get(key);

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

    protected boolean filterNote(IssueNote note) {
        return true;
    }

    protected String prepareComment(IssueNote note) {
        return prepareComment(note.getComment(), note);
    }

    protected String prepareComment(String comment, IssueNote note) {
        // make first letter uppercase (for beautiful output)
        comment = comment.substring(0, 1).toUpperCase() + comment.substring(1);

        return comment;
    }

    protected abstract void printNote(BufferedWriter writer, IssueNote note) throws IOException;
}
