package ru.bigmilk.jiraReports;

import com.atlassian.jira.rest.client.domain.Issue;
import org.tmatesoft.svn.core.SVNLogEntry;
import java.util.Date;

public class ReportRecord {
    private final String key;

    private String comment;
    private String username;
    private Date date;
    private Issue issue;

    public ReportRecord(String username, String key, String comment, Date date) {
        this.username = username;
        this.key = key;
        this.comment = comment;
        this.date = date;
    }

    public static ReportRecord parseSVNLog(SVNLogEntry entry) {
        String[] messageParts = entry.getMessage().split(" ", 2);

        if (messageParts.length < 2) {
            return null;
        }

        String key = messageParts[0].trim();
        String comment = messageParts[1].trim();

        if (!key.startsWith("[") || !key.endsWith("]")) {
            return null;
        }

        key = key.substring(1, key.length() - 1);

        if (key.equals("NOKEY")) {
            return null;
        }

        return new ReportRecord(entry.getAuthor(), key, comment, entry.getDate());
    }

    public String getKey() {
        return key;
    }

    public String getComment() {
        return comment;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setIssue(Issue issue) {
        this.issue = issue;
    }

    public Issue getIssue() {
        return issue;
    }
}

