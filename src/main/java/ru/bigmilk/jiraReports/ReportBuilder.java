package ru.bigmilk.jiraReports;

import com.atlassian.jira.rest.client.AuthenticationHandler;
import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.auth.BasicHttpAuthenticationHandler;
import com.atlassian.jira.rest.client.domain.*;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClient;

import ru.bigmilk.jiraReports.printers.Printer;
import ru.bigmilk.jiraReports.printers.PrinterException;
import ru.bigmilk.jiraReports.printers.TextNotesPrinter;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Class implements business logic for querying data for reports.
 *
 * It could:
 * * initialize settings from configuration file and command line arguments
 * * gets work log from Jira
 * * gets commits log from SVN
 *
 * To print reports ReportBuilder uses @see Printer interface.
 * It dynamically initialize list of printers with @see ServiceLoader help.
 */
public class ReportBuilder implements ISVNLogEntryHandler {
    private final String CONFIG_FILE = "config.ini";

    private Map<String, ReportRecord> records = new HashMap<String, ReportRecord>();
    private Set<String> users = new HashSet<String>();
    private Date reportStartDate;

    private Options options = new Options();
    private Properties properties = new Properties();
    private CommandLineParser parser = new GnuParser();

    private JiraRestClient jiraClient;

    private Printer printer;
    private Map<String, Printer> printers;

    public ReportBuilder() {
        initDefaultDate();
        initPrinters();
        initCommandLineOptions();

        loadConfig();        
    }

    /**
     * Load all available printers for ReportBuilder
     *
     * @see Printer
     */
    private void initPrinters() {
        printers = new HashMap<String, Printer>();
        ServiceLoader<Printer> printersSet = ServiceLoader.load(Printer.class);
        for (Printer printer : printersSet) {
            printers.put(printer.getName(), printer);
        }

        printer = new TextNotesPrinter();
    }

    /**
     * Init default start date. Now it one day ago from current day.
     */
    private void initDefaultDate() {
        setReportStartDate(getWorkDaysBefore(1));
    }

    /**
     * Generate work date before current date with offset in count days
     *
     * @param count days offset
     * @return generated date
     */
    public static Date getWorkDaysBefore(int count) {
        // get count days ago date
        Calendar date = Calendar.getInstance();
        date.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
        date.add(Calendar.DATE, -count);

        // check is it holiday
        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                date.add(Calendar.DATE, count > 0 ? -2 : 2);
                break;

            case Calendar.SATURDAY:
                date.add(Calendar.DATE, count > 0 ? -1 : 1);
                break;
        }

        return date.getTime();
    }

    /**
     * Set users list
     *
     * @param users list of users of commits
     */
    public void setUsers(String users[]) {
        properties.setProperty("users", StringUtils.join(users, ","));
        this.users = null;
    }

    public void setPassword(String password) {
        properties.setProperty("password", password);
    }

    /**
     * @return start date for reporting
     */
    public Date getReportStartDate() {
        return reportStartDate;
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

    /**
     * Load config from config file
     */
    public void loadConfig() {
        try {
            properties.load(new FileReader(CONFIG_FILE));
        } catch (FileNotFoundException ignored) {
        } catch (IOException ignored) {
        }
    }

    /**
     * Print command line help
     */
    public void printCommandLineHelp() {
        String defaultLogin = System.getProperty("user.name").toLowerCase();

        (new HelpFormatter()).printHelp(
               1000,
                "jiraReports.jar [OPTIONS]",
                null,
                options,
                String.format("\n" +
                        "Also You could create %s file with settings, that you need. Here is an example:\n" +
                        "login=%s\n" +
                        "password=<password>\n" +
                        "svn.url=<url>\n" +
                        "jira.url=<url>\n" +
                        "users=<user1>,<user2>,<user3>",
                        CONFIG_FILE,
                        defaultLogin.isEmpty() ? "<login>" : defaultLogin
                )
        );
    }

    /**
     * Parse the command line arguments
     *
     * @param args arguments from command line
     * @throws org.apache.commons.cli.ParseException
     */
    public void parseCommandLineArguments(String[] args) throws ParseException {
        CommandLine line = parser.parse(options, args);

        if (line.getOptions().length == 0) {
            return;
        }

        if (line.hasOption('?')) {
            throw new ParseException("");
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

        if (line.getOptionValue('u') != null) {
            properties.setProperty("users", line.getOptionValue('u'));
        }

        if (line.getOptionValue('d') != null) {
            int count;
            try {
                count = Integer.parseInt(line.getOptionValue('d'));
            } catch (NumberFormatException e) {
                throw new ParseException("Wrong days delay option format");
            }

            setReportStartDate(getWorkDaysBefore(count));
        }

        if (line.getOptionValue('f') != null) {
            String format = line.getOptionValue('f');
            if (!printers.containsKey(format)) {
                throw new ParseException("Wrong output format");
            }

            printer = printers.get(format);
        }
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
        options.addOption("u", "users", true, "users to filter");
        options.addOption("d", "days", true, "days delay before current");
        options.addOption("?", "help", false, "display this help");

        // generate list of output formats
        String formats = "";
        for (String printerName : printers.keySet()) {
            formats += String.format("%s - %s\n", printerName, printers.get(printerName).getDescription());
        }
        options.addOption("f", "format", true, "output format:\n" + formats);
    }

    @Override
    public void handleLogEntry(SVNLogEntry svnLogEntry) throws SVNException {
        // filter by date
        if (svnLogEntry.getDate().before(reportStartDate)) {
            return;
        }

        // filter by user
        if (users.size() > 0 && !users.contains(svnLogEntry.getAuthor())) {
            return;
        }

        ReportRecord record = ReportRecord.parseSVNLog(svnLogEntry);
        if (record == null) {
            return;
        }

        // filter by issue key
        if (records.containsKey(record.getKey())) {
            return;
        }

        // load jira data about issue
        Issue issue;
        try {
            issue = getJiraClient().getIssueClient().getIssue(record.getKey(), new NullProgressMonitor());
        } catch (URISyntaxException e) {
            return;
        }

        // pass not fixed issues
        BasicResolution resolution = issue.getResolution();
        if (resolution == null || !resolution.getName().equalsIgnoreCase("fixed")) {
            return;
        }

        // @todo: add analyzing of subtask (issue.getType().isSubtask())

        record.setIssue(issue);

        records.put(record.getKey(), record);
    }

    /**
     * Load commits from SVN for selected date period
     * and filter it by user and other requirements
     *
     * @return list of commits
     * @throws SVNException
     */
    public Map<String, ReportRecord> getCommits() throws SVNException {
        // initialize svn client
        SVNURL svnURL = SVNURL.parseURIEncoded(properties.getProperty("svn.url"));
        ISVNOptions svnOptions = SVNWCUtil.createDefaultOptions(true);
        ISVNAuthenticationManager svnAuthenticationManager = SVNWCUtil.createDefaultAuthenticationManager(
                properties.getProperty("login"), properties.getProperty("password"));

        // get logs
        SVNClientManager clientManager = SVNClientManager.newInstance(svnOptions, svnAuthenticationManager);
        clientManager.getLogClient().doLog(
                svnURL,
                new String[]{},
                SVNRevision.create(this.getReportStartDate()),
                SVNRevision.create(this.getReportStartDate()),
                SVNRevision.HEAD,
                true,
                true,
                100,
                this
        );

        return records;
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

    public void print(OutputStream out) throws PrinterException {
        printer.print(out, this);
    }

    public Set<String> getUsers() {
        if (users == null) {
            if (properties.getProperty("users") != null) {
                users = new HashSet<String>(
                    Arrays.asList(properties.getProperty("users").split(",")));
            } else {
                users = new HashSet<String>();
            }
        }
        
        return users;
    }

    public List<ReportRecord> getDoneLogs() throws URISyntaxException {
        List<ReportRecord> records = new ArrayList<ReportRecord>();

        // get all updated issues after start time
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd k:m");
        String users = StringUtils.join(getUsers(), ", ");
        SearchResult result = getJiraClient().getSearchClient().searchJql(
                String.format("updatedDate > \"%s\" AND (assignee was in (%s) OR assignee in (%s))",
                        dateFormat.format(getReportStartDate()), users, users
                ), new NullProgressMonitor()
        );

        for (BasicIssue basicIssue : result.getIssues()) {
            Issue issue = getJiraClient().getIssueClient().getIssue(
                    basicIssue.getKey(), new NullProgressMonitor());

            // check for needed users work log after start date
            for (Worklog workLog : issue.getWorklogs()) {
                // filter by date
                if (workLog.getCreationDate().toDate().before(getReportStartDate())) {
                    continue;
                }

                ReportRecord record = new ReportRecord(
                        workLog.getAuthor().getName(),
                        issue.getKey(),
                        workLog.getComment(),
                        workLog.getCreationDate().toDate()
                );
                record.setIssue(issue);
                records.add(record);
            }

        }
        return records;
    }

    public List<ReportRecord> getNextLogs() throws URISyntaxException {
        List<ReportRecord> records = new ArrayList<ReportRecord>();
        Date now = new Date();

        // get all issue assigned on users and not closed or resolved
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd k:m");
        String users = StringUtils.join(getUsers(), ", ");
        SearchResult result = getJiraClient().getSearchClient().searchJql(
                String.format("assignee IN (%s) AND status NOT IN (Resolved, Closed) AND updatedDate > \"%s\"",
                        users, dateFormat.format(getWorkDaysBefore(7))),
                new NullProgressMonitor()
        );

        for (BasicIssue basicIssue : result.getIssues()) {
            Issue issue = getJiraClient().getIssueClient().getIssue(
                    basicIssue.getKey(), new NullProgressMonitor());

            BasicUser user = issue.getAssignee();
            if (user == null) continue;

            ReportRecord record = new ReportRecord(
                    user.getName(),
                    issue.getKey(),
                    issue.getSummary(),
                    now
            );
            record.setIssue(issue);
            records.add(record);
        }
        return records;
    }

    public void setReportStartDate(Date reportStartDate) {
        this.reportStartDate = reportStartDate;
    }
}

