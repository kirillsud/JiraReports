package com.exigenservices.voa.releaseNotes;

import com.atlassian.jira.rest.client.AuthenticationHandler;
import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.auth.BasicHttpAuthenticationHandler;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClient;
import org.apache.commons.cli.*;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.*;

import java.io.Console;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws SVNException, URISyntaxException {
        // initialize login and password
        String login, password, svnUrlString, jiraUrlString;

        // prepare release nodes
        ReleaseNotes releaseNotes = new ReleaseNotes();
        try {
            releaseNotes.parseCommandLineArguments(args);
        } catch (ParseException e) {
            System.out.println("ERROR: Wrong command line arguments\n");
            releaseNotes.printCommandLineHelp();
            return;
        }

        // prepare initial data
        login = releaseNotes.getLogin();
        password = releaseNotes.getPassword();
        svnUrlString = releaseNotes.getSvnURL();
        jiraUrlString = releaseNotes.getJiraURL();

        // check login and etc.
        List<String> emptyParametersList = new ArrayList<String>();
        if (login.isEmpty()) {
            emptyParametersList.add("login");
        }

        if (svnUrlString.isEmpty()) {
            emptyParametersList.add("SVN URL");
        }

        if (jiraUrlString.isEmpty()) {
            emptyParametersList.add("Jira URL");
        }

        if (!emptyParametersList.isEmpty()) {
            // join all params
            String params = "";
            for (String param : emptyParametersList) {
                params += ", " + param;
            }
            params = params.substring(2);

            // print message
            System.out.println("ERROR: Please, specify required parameters: " + params + "\n");
            releaseNotes.printCommandLineHelp();
            return;
        }

        // check password
        if (password.isEmpty()) {
            Console console = System.console();
            if (console == null) {
                System.out.println("ERROR: Please, specify your domain password in config file or " +
                        "in command line arguments\n");
                return;
            }

            password = new String(console.readPassword("Password: "));
        }

        // initialize svn client
        SVNURL svnURL = SVNURL.parseURIEncoded(svnUrlString);
        ISVNOptions svnOptions = SVNWCUtil.createDefaultOptions(true);
        ISVNAuthenticationManager svnAuthenticationManager =
                SVNWCUtil.createDefaultAuthenticationManager(login, password);

        // get logs
        SVNClientManager clientManager = SVNClientManager.newInstance(svnOptions, svnAuthenticationManager);
        clientManager.getLogClient().doLog(
                svnURL,
                new String[]{},
                SVNRevision.create(releaseNotes.getStartDate()),
                SVNRevision.create(releaseNotes.getStartDate()),
                SVNRevision.HEAD,
                true,
                true,
                100,
                releaseNotes
        );

        // establishing connection to JIRA
        URI jiraURI = new URI(jiraUrlString);
        AuthenticationHandler authenticationHandler = new BasicHttpAuthenticationHandler(login, password);
        JiraRestClient jiraClient = new JerseyJiraRestClient(jiraURI, authenticationHandler);

        // output logs
        for (String key : releaseNotes.getNotes().keySet()) {
            IssueNote note = releaseNotes.getNotes().get(key);

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
    }
}
