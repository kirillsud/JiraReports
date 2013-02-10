package com.exigenservices.voa.releaseNotes;

import com.atlassian.jira.rest.client.domain.Issue;
import org.tmatesoft.svn.core.SVNLogEntry;
import java.util.Date;

public class ReleaseNote {
    private final String key;

    private String comment;
    private String author;
    private Date date;
    private Issue issue;

    public ReleaseNote(String author, String key, String comment, Date date) {
        this.author = author;
        this.key = key;
        this.comment = comment;
        this.date = date;
    }

    public static ReleaseNote parseSVNLog(SVNLogEntry entry) {
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

        return new ReleaseNote(entry.getAuthor(), key, comment, entry.getDate());
    }

    public String getKey() {
        return key;
    }

    public String getComment() {
        return comment;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
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

