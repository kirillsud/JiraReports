package com.exigenservices.voa.releaseNotes.printers;

import com.exigenservices.voa.releaseNotes.ReleaseNote;

import java.io.BufferedWriter;
import java.io.IOException;

public class TextNotesPrinter extends AbstractPrinter {

    @Override
    protected void printNote(BufferedWriter writer, ReleaseNote note) throws IOException {
        writer.write("[" + note.getKey() + "] " + prepareComment(note) + "\n");
    }

    @Override
    protected String prepareComment(String comment, ReleaseNote note) {
        // preparation for bug comments
        if (note.getIssue().getIssueType().getName().equalsIgnoreCase("bug")) {

            // if comment starts from "fix ", remove it
            if (comment.toLowerCase().startsWith("fix ")) {
                comment = comment.substring(4);
            }

            // if comment doesn't starts from fixed, make it
            if (!comment.toLowerCase().startsWith("fixed")) {
                comment = appendToString(comment, "Fixed");
            }
        }

        // @todo: add preparations for other types of committed issues

        return super.prepareComment(comment, note);
    }

    private String appendToString(String string, String prefix) {
        // make first letter lowercase, if second isn't in upper
        if (string.substring(1, 2).equals(string.substring(1, 2).toLowerCase())) {
            string = string.substring(0, 1).toLowerCase() + string.substring(1);
        }

        return prefix + " " + string;
    }

    @Override
    public String getName() {
        return "notes";
    }

    @Override
    public String getDescription() {
        return "it's default format, for release notes: [<JIRA_KEY>] <Jira summary>";
    }
}
