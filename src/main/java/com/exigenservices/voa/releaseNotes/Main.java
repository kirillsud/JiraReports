package com.exigenservices.voa.releaseNotes;

import com.atlassian.jira.rest.client.AuthenticationHandler;
import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.auth.BasicHttpAuthenticationHandler;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClient;
import org.apache.commons.cli.*;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.*;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class Main implements ISVNLogEntryHandler {

    private Map<String, IssueNote> notes = new HashMap<String, IssueNote>();
    private Set<String> authors;
    private Date startDate;

    public static void main(String[] args) throws IOException, ParseException {
        Properties properties = new Properties();
        CommandLineParser parser = new GnuParser();

        // create command line options
        Options options = new Options();
        options.addOption("p", "password", true, "your password in workspace domain");
        options.addOption("l", "login", true, "your login in workspace domain");
        options.addOption("s", "svn", true, "svn url");
        options.addOption("j", "jira", true, "jira url");
        options.addOption("a", "authors", true, "authors to filter");

        // parse the command line arguments
        CommandLine line = parser.parse(options, args);
        if (line.getOptions().length == 0) {
            (new HelpFormatter()).printHelp("release_notes.jar [OPTIONS]", options);
            return;
        }

        properties.setProperty("login", line.getOptionValues('v')[0]);

        // try to load properties
        try {
            properties.load(new FileReader("config.ini"));
        } catch (FileNotFoundException e) {
            System.out.print("You need to make config.ini file with settings, that you need. " +
                    "Here is an example:\n\n" +
                    "svn.url=<url>\n" +
                    "jira.url=<url>\n" +
                    "authors=<user1>,<user2>,<user3>"
            );
            return;
        }

        // initialize login and password
        String login = "";
        String password = "";

        if (System.console() != null) {
            String answer;
            Console console = System.console();

            System.out.print("Your svn login is " + System.getProperty("user.name").toLowerCase() + "? ");
            answer = console.readLine();

            if (answer.isEmpty() || answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes")) {
                login = System.getProperty("user.name").toLowerCase();
            } else {
                System.out.print("Enter svn login (leave empty for '" + login + "'): ");
                login = console.readLine();
            }

            System.out.print("Enter svn password: ");
            password = console.readPassword().toString();
        }

        System.out.print(login + ":" + password);

        try {
            // initialize svn client
            SVNURL svnURL = SVNURL.parseURIEncoded(properties.getProperty("svn.url"));
            ISVNOptions svnOptions = SVNWCUtil.createDefaultOptions(true);
            ISVNAuthenticationManager svnAuthenticationManager =
                    SVNWCUtil.createDefaultAuthenticationManager(login, password);

            // initialize authors list
            String authors [] = properties.getProperty("authors").split(",");

            // get 1 day ago date
            Calendar releaseNotesStartDate = Calendar.getInstance();
            releaseNotesStartDate.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
            releaseNotesStartDate.set(Calendar.HOUR_OF_DAY, 13);
            releaseNotesStartDate.set(Calendar.MINUTE, 30);
            releaseNotesStartDate.add(Calendar.DATE, -1);

            // check is it holiday
            int dayOfWeek = releaseNotesStartDate.get(Calendar.DAY_OF_WEEK);
            switch (dayOfWeek) {
                case Calendar.SUNDAY:
                    releaseNotesStartDate.add(Calendar.DATE, -2);
                    break;

                case Calendar.SATURDAY:
                    releaseNotesStartDate.add(Calendar.DATE, -1);
                    break;
            }

            // get logs
            Main filteredLogs = new Main(authors, releaseNotesStartDate.getTime());
            SVNClientManager clientManager = SVNClientManager.newInstance(svnOptions, svnAuthenticationManager);
            clientManager.getLogClient().doLog(
                    svnURL,
                    new String[]{},
                    SVNRevision.create(releaseNotesStartDate.getTime()),
                    SVNRevision.create(releaseNotesStartDate.getTime()),
                    SVNRevision.HEAD,
                    true,
                    true,
                    100,
                    filteredLogs
            );

            // establishing connection to JIRA
            URI jiraURI = new URI(properties.getProperty("jira.url"));
            AuthenticationHandler authenticationHandler = new BasicHttpAuthenticationHandler(login, password);
            JiraRestClient jiraClient = new JerseyJiraRestClient(jiraURI, authenticationHandler);

            // output logs
            for (String key : filteredLogs.getNotes().keySet()) {
                IssueNote note = filteredLogs.getNotes().get(key);

                String comment;
                // @todo: add analyzing of subtask (issue.getType().isSubtask())
                try {
                    // try to get note comment from JIRA
                    Issue issue = jiraClient.getIssueClient().getIssue(key, new NullProgressMonitor());

                    comment = issue.getSummary();

                    // if it is a bug, check for fixed words in the beginning
                    if (issue.getIssueType().getName().toLowerCase().equals("bug")) {
                        // if it doesn't starts from fixed, make it
                        if (!comment.toLowerCase().startsWith("fixed")) {
                            comment = "Fixed " + comment.substring(0, 1).toLowerCase() + comment.substring(1);
                        }
                    }
                } catch (Exception e) {
                    // if we couldn't, leave it original from note
                    comment = note.getComment();
                }

                // and finally make first letter uppercase (because it's beautifully, yes?)
                comment = comment.substring(0, 1).toUpperCase() + comment.substring(1);

                System.out.println("[" + key + "] " + comment);
            }

        } catch (SVNException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public Main(String[] authors, Date startDate) {
        this.authors = new HashSet<String>(Arrays.asList(authors));
        this.startDate = startDate;
    }

    @Override
    public void handleLogEntry(SVNLogEntry svnLogEntry) throws SVNException {
        // filter by date
        if (svnLogEntry.getDate().before(startDate)) {
            return;
        }

        // filter by author
        if (!authors.contains(svnLogEntry.getAuthor())) {
            return;
        }

        IssueNote note = IssueNote.parseSVNLog(svnLogEntry);
        if (note == null) {
            return;
        }

        // filter by issue key
        if (notes.containsKey(note.getKey())) {
            return;
        }

        notes.put(note.getKey(), note);
    }

    public Map<String, IssueNote> getNotes() {
        return notes;
    }
}

class IssueNote {
    private String author;
    private String key;
    private String comment;
    private Date   date;

    IssueNote(String author, String key, String comment, Date date) {
        this.author = author;
        this.key = key;
        this.comment = comment;
        this.date = date;
    }

    public static IssueNote parseSVNLog(SVNLogEntry entry) {
        String[] messageParts = entry.getMessage().split(" ", 2);

        if (messageParts.length < 2) {
            return null;
        }

        String key = messageParts[0].trim();
        String comment = messageParts[1].trim();

        if (!key.startsWith("[") || !key.endsWith("]")) {
            return null;
        }

        key = key.substring(1, key.length()-1);

        if (key.equals("NOKEY")) {
            return null;
        }

        return new IssueNote(entry.getAuthor(), key, comment, entry.getDate());
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
}