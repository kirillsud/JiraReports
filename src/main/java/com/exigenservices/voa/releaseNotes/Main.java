package com.exigenservices.voa.releaseNotes;

import org.apache.commons.cli.*;

import java.io.Console;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws URISyntaxException, IOException {
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
            releaseNotes.setPassword(password);

        }

        // check jira connection
        // @todo: move it to ReleaseNotes
        if (!releaseNotes.resetJiraClient()) {
            System.out.println("ERROR: Couldn't connect to JIRA. Please check login, password or JIRA URL");
            return;
        }

        // output logs
        releaseNotes.print(System.out);
    }
}
