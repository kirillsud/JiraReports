package com.exigenservices.voa.releaseNotes.printers;

import com.exigenservices.voa.releaseNotes.ReleaseNote;
import com.exigenservices.voa.releaseNotes.ReleaseNotes;
import org.tmatesoft.svn.core.SVNException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.util.Map;

public abstract class AbstractPrinter implements Printer {

    @Override
    public boolean print(OutputStream out, ReleaseNotes notes) {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
        Map<String, ReleaseNote> commits;
        try {
            commits = getData(notes);
        } catch (Exception e) {
            return false;
        }

        for (String key : commits.keySet()) {
            ReleaseNote note = commits.get(key);

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

    protected Map<String, ReleaseNote> getData(ReleaseNotes notes) throws Exception {
        return notes.getCommits();
    }

    protected boolean filterNote(ReleaseNote note) {
        return true;
    }

    protected String prepareComment(ReleaseNote note) {
        return prepareComment(note.getComment(), note);
    }

    protected String prepareComment(String comment, ReleaseNote note) {
        // make first letter uppercase (for beautiful output)
        comment = comment.substring(0, 1).toUpperCase() + comment.substring(1);

        return comment;
    }

    protected abstract void printNote(BufferedWriter writer, ReleaseNote note) throws IOException;
}
