package com.exigenservices.voa.releaseNotes;

import com.atlassian.jira.rest.client.AuthenticationHandler;
import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.auth.BasicHttpAuthenticationHandler;
import com.atlassian.jira.rest.client.domain.BasicResolution;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClient;
import com.exigenservices.voa.releaseNotes.printers.Printer;

import com.exigenservices.voa.releaseNotes.printers.TextNotesPrinter;
import org.apache.commons.cli.*;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class ReleaseNotes implements ISVNLogEntryHandler {
    private Map<String, IssueNote> notes = new HashMap<String, IssueNote>();
    private Set<String> authors = new HashSet<String>();
    private Date startDate;

    private Options options = new Options();
    private Properties properties = new Properties();
    private CommandLineParser parser = new GnuParser();

    private JiraRestClient jiraClient;

    private Printer printer;
    private Map<String, Printer> printers;

    public ReleaseNotes() {
        initDefaultDate();
        initPrinters();
        initCommandLineOptions();
        loadConfig();

        if (properties.getProperty("authors") != null) {
            setAuthors(properties.getProperty("authors").split(","));
        }
    }

    private void initPrinters() {
        printers = new HashMap<String, Printer>();
        ServiceLoader<Printer> printersSet = ServiceLoader.load(Printer.class);
        for (Printer printer : printersSet) {
            printers.put(printer.getName(), printer);
        }

        printer = new TextNotesPrinter();
    }

    private void initDefaultDate() {
        setDaysBefore(1);
    }

    /**
     * Set start date for commits on count days before current day and time
     *
     * @param count days before
     */
    public void setDaysBefore(int count) {
        // get 1 day ago date
        Calendar releaseNotesStartDate = Calendar.getInstance();
        releaseNotesStartDate.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
        releaseNotesStartDate.add(Calendar.DATE, -count);

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

        startDate = releaseNotesStartDate.getTime();
    }

    /**
     * Set authors list
     *
     * @param authors list of authors of commits
     */
    public void setAuthors(String authors []) {
        this.authors = new HashSet<String>(
                Arrays.asList(authors)
        );
    }

    public Date getStartDate() {
        return startDate;
    }

    public String getLogin() {
        return properties.getProperty("login") != null ? properties.getProperty("login") : "";
    }

    public String getPassword() {
        return properties.getProperty("password") != null ? properties.getProperty("password") : "";
    }

    public String getSvnURL() {
        return properties.getProperty("svn.url") != null ? properties.getProperty("svn.url") : "";
    }

    public String getJiraURL() {
        return properties.getProperty("jira.url") != null ? properties.getProperty("jira.url") : "";
    }

    public void loadConfig() {
        try {
            properties.load(new FileReader("config.ini"));
        } catch (FileNotFoundException ignored) {
        } catch (IOException ignored) {
        }
    }

    public void printCommandLineHelp() {
        String defaultLogin = System.getProperty("user.name").toLowerCase();

        (new HelpFormatter()).printHelp(
                1000,
                "release_notes.jar [OPTIONS]",
                null,
                options,
                "\nAlso You could create config.ini file with settings, that you need. " +
                        "Here is an example:\n\n\n" +
                        "login=" + (defaultLogin.isEmpty() ? "<login>" : defaultLogin) + "\n" +
                        "password=<password>\n" +
                        "svn.url=<url>\n" +
                        "jira.url=<url>\n" +
                        "authors=<user1>,<user2>,<user3>"
        );
    }

    /**
     * Parse the command line arguments
     *
     * @param args arguments from command line
     * @return false, if there is no applicable arguments
     * @throws org.apache.commons.cli.ParseException
     */
    public boolean parseCommandLineArguments(String[] args) throws ParseException {
        CommandLine line = parser.parse(options, args);

        if (line.getOptions().length == 0) {
            return false;
        }

        if (line.getOptionValue('l') != null) {
            properties.setProperty("login", line.getOptionValue('l'));
        }

        if (line.getOptionValue('p') != null) {
            properties.setProperty("password", line.getOptionValue('p'));
        }

        if (line.getOptionValue('s') != null) {
            properties.setProperty("svn.url", line.getOptionValue('s'));
        }

        if (line.getOptionValue('j') != null) {
            properties.setProperty("jira.url", line.getOptionValue('j'));
        }

        if (line.getOptionValue('a') != null) {
            properties.setProperty("authors", line.getOptionValue('a'));
        }

        if (line.getOptionValue('d') != null) {
            int count;
            try {
                count = Integer.parseInt(line.getOptionValue('d'));
            } catch (NumberFormatException e) {
                throw new ParseException("Wrong days delay option format");
            }

            setDaysBefore(count);
        }

        if (line.getOptionValue('f') != null) {
            String format = line.getOptionValue('f');
            if (!printers.containsKey(format)) {
                throw new ParseException("Wrong output format");
            }

            printer = printers.get(format);
        }

        return true;
    }

    /**
     * Create command line options
     */
    public void initCommandLineOptions() {
        options = new Options();
        options.addOption("p", "password", true, "your password in workspace domain");
        options.addOption("l", "login", true, "your login in workspace domain");
        options.addOption("s", "svn", true, "svn url");
        options.addOption("j", "jira", true, "jira url");
        options.addOption("a", "authors", true, "authors to filter");
        options.addOption("d", "days", true, "days delay before current");

        // generate list of output formats
        String formats = "";
        for (String printerName : printers.keySet()) {
            formats += printerName + " - " + printers.get(printerName).getDescription() + "\n";
        }

        options.addOption("f", "format", true, "output format:\n" + formats);
    }

    @Override
    public void handleLogEntry(SVNLogEntry svnLogEntry) throws SVNException {
        // filter by date
        if (svnLogEntry.getDate().before(startDate)) {
            return;
        }

        // filter by author
        if (authors.size() > 0 && !authors.contains(svnLogEntry.getAuthor())) {
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

        // load jira data about issue
        Issue issue;
        try {
            issue = getJiraClient().getIssueClient().getIssue(note.getKey(), new NullProgressMonitor());
        } catch (URISyntaxException e) {
            return;
        }

        // pass not fixed issues
        BasicResolution resolution = issue.getResolution();
        if (resolution == null || !resolution.getName().equalsIgnoreCase("fixed")) {
            return;
        }

        // @todo: add analyzing of subtask (issue.getType().isSubtask())

        note.setJiraIssue(issue);

        notes.put(note.getKey(), note);
    }

    public Map<String, IssueNote> getNotes() {
        return notes;
    }

    public void setPassword(String password) {
        properties.setProperty("password", password);
    }

    public JiraRestClient getJiraClient() throws URISyntaxException {
        if (jiraClient == null) {
            // establishing connection to JIRA
            URI jiraURI = new URI(properties.getProperty("jira.url"));
            AuthenticationHandler authenticationHandler = new BasicHttpAuthenticationHandler(
                    properties.getProperty("login"), properties.getProperty("password")
            );

            jiraClient = new JerseyJiraRestClient(jiraURI, authenticationHandler);

            // @todo: check established connection
        }

        return jiraClient;
    }

    public boolean resetJiraClient() {
        jiraClient = null;
        try {
            getJiraClient();
        } catch (URISyntaxException e) {
            return false;
        }
        return true;
    }

    public void print(OutputStream out) {
        printer.print(out, this);
    }
}

