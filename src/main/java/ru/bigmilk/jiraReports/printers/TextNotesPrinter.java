package ru.bigmilk.jiraReports.printers;

import ru.bigmilk.jiraReports.ReportRecord;

import java.io.IOException;
import java.io.Writer;

public class TextNotesPrinter extends AbstractPrinter {

    @Override
    protected void printNote(Writer writer, ReportRecord note) throws IOException {
        writer.write("[" + note.getKey() + "] " + prepareComment(note) + "\n");
    }

    @Override
    protected String prepareComment(String comment, ReportRecord note) {
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
