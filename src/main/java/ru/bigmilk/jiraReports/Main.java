package ru.bigmilk.jiraReports;

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
        ReportBuilder reportBuilder = new ReportBuilder();
        try {
            reportBuilder.parseCommandLineArguments(args);
        } catch (ParseException e) {
            if (!e.getMessage().isEmpty()) {
                printError(e.getMessage().replace(':', '\0'));
            }

            reportBuilder.printCommandLineHelp();
            return;
        }

        // prepare initial data
        login = reportBuilder.getLogin();
        password = reportBuilder.getPassword();
        svnUrlString = reportBuilder.getSvnURL();
        jiraUrlString = reportBuilder.getJiraURL();

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
            printError(String.format("Please, specify required parameters: %s", params));
            reportBuilder.printCommandLineHelp();
            return;
        }

        // check password
        if (password.isEmpty()) {
            Console console = System.console();
            if (console == null) {
                printError("Please, specify your domain password in config file or in command line arguments");
                return;
            }

            password = new String(console.readPassword("Password: "));
            reportBuilder.setPassword(password);

        }

        // check jira connection
        // @todo: move it to ReportBuilder
        if (!reportBuilder.resetJiraClient()) {
            printError("Couldn't connect to JIRA. Please check login, password or JIRA URL");
            return;
        }

        // print report
        try {
            reportBuilder.print(System.out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printError(String message) {
        System.out.println(String.format("ERROR: %s", message));
    }
}
