package com.exigenservices.voa.releaseNotes.printers;

import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.domain.BasicResolution;
import com.exigenservices.voa.releaseNotes.ReleaseNote;
import com.exigenservices.voa.releaseNotes.ReleaseNotes;
import com.google.common.collect.HashMultimap;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

public class ReportPrinter extends AbstractPrinter {
    @Override
    public String getName() {
        return "report";
    }

    @Override
    public String getDescription() {
        return "work log report in wiki format";
    }

    @Override
    public boolean print(OutputStream out, ReleaseNotes notes) {
        Writer writer = new BufferedWriter(new OutputStreamWriter(out));
        HashMultimap<String, ReleaseNote> doneLogs = HashMultimap.create();
        HashMultimap<String, ReleaseNote> nextLogs = HashMultimap.create();

        // get done and next logs, grouped by author
        try {
            // @todo: need to filter duplicates
            for (ReleaseNote note : notes.getDoneLogs()) {
                doneLogs.put(note.getAuthor(), note);
            }

            for (ReleaseNote note : notes.getNextLogs()) {
                nextLogs.put(note.getAuthor(), note);
            }
        } catch (Exception e) {
            return false;
        }

        try {
            // print all logs, grouped by author
            for (String author : notes.getAuthors()) {
                String fullName = notes.getJiraClient().getUserClient().
                        getUser(author, new NullProgressMonitor()).getDisplayName();

                // print header with user name
                writer.write(String.format("== [[%s|%s]] ==\n\n", fullName, fullName));

                // first print what was done
                writer.write("=== What was done ===\n\n");
                for (ReleaseNote note : doneLogs.get(author)) {
                    if (!filterNote(note)) {
                        continue;
                    }

                    printNote(writer, note);
                }
                if (doneLogs.get(author).isEmpty()) {
                    writer.write("* none\n");
                }
                writer.write("\n");

                // print what's next
                writer.write("=== What's next ===\n\n");
                for (ReleaseNote note : nextLogs.get(author)) {
                    if (!filterNote(note)) {
                        continue;
                    }

                    printNote(writer, note);
                }
                if (nextLogs.get(author).isEmpty()) {
                    writer.write("* none\n");
                }
                writer.write("\n");

                // and finally write template for questions or impediments
                writer.write("=== Questions or impediments ===\n\n* none\n\n");
            }

            writer.flush();
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    @Override
    protected void printNote(Writer writer, ReleaseNote note) throws IOException {
        URI uri = note.getIssue().getSelf();
        String issueKey;

        // try to generate link to issue
        try {
            uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), null, null, null);
            issueKey = String.format("[%s/browse/%s %s]", uri.toString(), note.getKey(), note.getKey());
        } catch (URISyntaxException e) {
            issueKey = String.format("%s", note.getKey());
        }

        // init status and resolution
        String status = note.getIssue().getStatus().getName();
        BasicResolution resolution = note.getIssue().getResolution();
        if (resolution != null) {
            status += ": " + resolution.getName().toLowerCase();
        }

        // write
        writer.write(String.format("* %s - %s (%s)\n",
                issueKey,
                prepareComment(note),
                status)
        );
    }

    @Override
    protected String prepareComment(String comment, ReleaseNote note) {
        // extended trim comment
        comment = trimStringExt(comment);

        // if comment empty, replace by summary
        if (comment.isEmpty()) {
            comment = trimStringExt(note.getIssue().getSummary());
        }

        // replace new line
        comment = comment.replaceAll("\r\n|\n|\r", " ");

        // replace bad start of comment


        return super.prepareComment(comment.trim(), note);
    }

    private String trimStringExt(String comment) {
        comment = comment.replaceAll("^\\W+|[\\s.]+$", "");
        return comment;
    }
}
