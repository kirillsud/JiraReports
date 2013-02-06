package com.exigenservices.voa.releaseNotes;

import org.apache.commons.cli.*;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ReleaseNotes implements ISVNLogEntryHandler {
    private Map<String, IssueNote> notes = new HashMap<String, IssueNote>();
    private Set<String> authors;
    private Date startDate;

    private Options options = new Options();
    private Properties properties = new Properties();
    private CommandLineParser parser = new GnuParser();

    public ReleaseNotes() {
        initDefaultDate();
        initCommandLineOptions();
        loadConfig();
        setAuthors(properties.getProperty("authors").split(","));
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
            int count = 1;
            try {
                count = Integer.parseInt(line.getOptionValue('d'));
            } catch (NumberFormatException e) {
                throw new ParseException("Wrong days delay option format");
            }

            setDaysBefore(count);
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

