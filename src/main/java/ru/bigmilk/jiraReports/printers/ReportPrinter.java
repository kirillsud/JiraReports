package ru.bigmilk.jiraReports.printers;

import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.domain.BasicResolution;
import ru.bigmilk.jiraReports.ReportRecord;
import ru.bigmilk.jiraReports.ReportBuilder;
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
    public boolean print(OutputStream out, ReportBuilder reportBuilder) {
        Writer writer = new BufferedWriter(new OutputStreamWriter(out));
        HashMultimap<String, ReportRecord> doneLogs = HashMultimap.create();
        HashMultimap<String, ReportRecord> nextLogs = HashMultimap.create();

        // get done and next logs, grouped by user
        try {
            // @todo: need to filter duplicates
            for (ReportRecord record : reportBuilder.getDoneLogs()) {
                doneLogs.put(record.getUsername(), record);
            }

            for (ReportRecord record : reportBuilder.getNextLogs()) {
                nextLogs.put(record.getUsername(), record);
            }
        } catch (Exception e) {
            return false;
        }

        try {
            // print all logs, grouped by user
            for (String user : reportBuilder.getUsers()) {
                String fullName = reportBuilder.getJiraClient().getUserClient().
                        getUser(user, new NullProgressMonitor()).getDisplayName();

                // print header with user name
                writer.write(String.format("== [[%s|%s]] ==\n\n", fullName, fullName));

                // first print what was done
                writer.write("=== What was done ===\n\n");
                for (ReportRecord record : doneLogs.get(user)) {
                    if (!filterRecord(record)) {
                        continue;
                    }

                    printRecord(writer, record);
                }
                if (doneLogs.get(user).isEmpty()) {
                    writer.write("* none\n");
                }
                writer.write("\n");

                // print what's next
                writer.write("=== What's next ===\n\n");
                for (ReportRecord record : nextLogs.get(user)) {
                    if (!filterRecord(record)) {
                        continue;
                    }

                    printRecord(writer, record);
                }
                if (nextLogs.get(user).isEmpty()) {
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
    protected void printRecord(Writer writer, ReportRecord record) throws IOException {
        URI uri = record.getIssue().getSelf();
        String issueKey;

        // try to generate link to issue
        try {
            uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), null, null, null);
            issueKey = String.format("[%s/browse/%s %s]", uri.toString(), record.getKey(), record.getKey());
        } catch (URISyntaxException e) {
            issueKey = String.format("%s", record.getKey());
        }

        // init status and resolution
        String status = record.getIssue().getStatus().getName();
        BasicResolution resolution = record.getIssue().getResolution();
        if (resolution != null) {
            status += ": " + resolution.getName().toLowerCase();
        }

        // write
        writer.write(String.format("* %s - %s (%s)\n",
                issueKey,
                prepareComment(record),
                status)
        );
    }

    @Override
    protected String prepareComment(String comment, ReportRecord record) {
        // extended trim comment
        comment = trimStringExt(comment);

        // if comment empty, replace by summary
        if (comment.isEmpty()) {
            comment = trimStringExt(record.getIssue().getSummary());
        }

        // replace new line
        comment = comment.replaceAll("\r\n|\n|\r", " ");

        // replace bad start of comment


        return super.prepareComment(comment.trim(), record);
    }

    private String trimStringExt(String comment) {
        comment = comment.replaceAll("^\\W+|[\\s.]+$", "");
        return comment;
    }
}
