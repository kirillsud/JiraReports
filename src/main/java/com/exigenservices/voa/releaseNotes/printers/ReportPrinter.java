package com.exigenservices.voa.releaseNotes.printers;

import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.domain.BasicIssue;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.domain.SearchResult;
import com.atlassian.jira.rest.client.domain.Worklog;
import com.exigenservices.voa.releaseNotes.ReleaseNote;
import com.exigenservices.voa.releaseNotes.ReleaseNotes;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MultiHashtable;
import org.tmatesoft.svn.core.SVNException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

public class ReportPrinter extends AbstractPrinter {
    @Override
    protected void printNote(BufferedWriter writer, ReleaseNote note) throws IOException {
        writer.write(note.getKey() + " - " + note.getComment());
    }

    @Override
    public String getName() {
        return "report";
    }

    @Override
    public String getDescription() {
        return "work log report in wiki format";
    }

    @Override
    protected Map<String, ReleaseNote> getData(ReleaseNotes releaseNotes) throws Exception {
        Calendar date = new GregorianCalendar();
        date.setTime(releaseNotes.getStartDate());
        Map<String, ReleaseNote> notes = new HashMap<String, ReleaseNote>();

        // get all updated issues after start time
        SearchResult result = releaseNotes.getJiraClient().getSearchClient().searchJql(
                "updated > " +
                        date.get(Calendar.YEAR) + "/" + date.get(Calendar.MONTH) + "/" + date.get(Calendar.DATE) + " " +
                        date.get(Calendar.HOUR) + ":" + date.get(Calendar.MINUTE),
                new NullProgressMonitor()
        );

        for (BasicIssue basicIssue : result.getIssues()) {
            Issue issue = releaseNotes.getJiraClient().getIssueClient().getIssue(
                    basicIssue.getKey(), new NullProgressMonitor());

            // check for needed authors work log after start date
            for (Worklog workLog : issue.getWorklogs()) {
                // filter by date
                if (workLog.getCreationDate().toDate().before(releaseNotes.getStartDate())) {
                    continue;
                }

                // filter by author
                if (!releaseNotes.getAuthors().isEmpty() && !releaseNotes.getAuthors().contains(workLog.getAuthor().getName())) {
                    continue;
                }

                ReleaseNote note = new ReleaseNote(
                        workLog.getAuthor().getName(),
                        issue.getKey(),
                        workLog.getComment(),
                        workLog.getCreationDate().toDate()
                );
                note.setIssue(issue);
                notes.put(issue.getKey(), note);
            }

        }
        return notes;
    }
}
