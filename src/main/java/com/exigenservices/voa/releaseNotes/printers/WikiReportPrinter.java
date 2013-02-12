package com.exigenservices.voa.releaseNotes.printers;

import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.exigenservices.voa.releaseNotes.ReleaseNotes;
import net.sourceforge.jwbf.core.actions.util.ActionException;
import net.sourceforge.jwbf.core.actions.util.ProcessException;
import net.sourceforge.jwbf.core.contentRep.SimpleArticle;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.*;

public class WikiReportPrinter extends ReportPrinter {
    protected final String REPORT_TEMPLATE = "wiki-report.txt";

    URI wikiURL;
    String articleTitle;
    String articleContent;

    @Override
    public String getName() {
        return "wiki-report";
    }

    @Override
    public String getDescription() {
        return String.format(
                "work log report, generates article on wiki. To generate article need to create " +
                "file '%s' with next content:\n" +
                " * 1st line: <wiki url>\n" +
                " * 2nd line: <article title>\n" +
                " * other lines: <article content>\n\n" +
                "For <article title> and <article content> you could use next placeholders:\n" +
                " * <<report>> - place to insert report\n" +
                " * <<current date>> - current date in format YYYY-MM-DDTHH:MM:SSX\n" +
                " * <<current day>> - current day in format YYYY-MM-DD\n" +
                " * <<previous work day>> - previous work day in format YYYY-MM-DD\n" +
                " * <<next work day>> - next work day in format YYYY-MM-DD\n",
                " * <<current user>> - full name of logged user\n",
                " * <<users>> - list of users full name, comma separated\n",
                REPORT_TEMPLATE
        );
    }

    @Override
    public boolean print(OutputStream out, ReleaseNotes notes) {
        // read template report file
        if (!readTemplate()) {
            return false;
        }

        // init placeholders list
        Map<String, String> placeholders = new HashMap<String, String>();

        // generate report
        OutputStream reportOutput = new ByteArrayOutputStream();
        if (!super.print(reportOutput, notes)) {
            return false;
        }
        // init placeholders list
        placeholders.put("report", reportOutput.toString());

        // init dates variables
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        placeholders.put("current day", dateFormat.format(new Date()));
        placeholders.put("previous work day", dateFormat.format(notes.getDaysBefore(1)));
        placeholders.put("next work day", dateFormat.format(notes.getDaysBefore(-1)));
        placeholders.put("current date", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").format(new Date()));

        // init users variables
        try {
            placeholders.put("current user", notes.getJiraClient().getUserClient().
                    getUser(notes.getLogin(), new NullProgressMonitor()).getDisplayName());

            String users = "";
            for (String user : notes.getAuthors()) {
                users += notes.getJiraClient().getUserClient().
                        getUser(user, new NullProgressMonitor()).getDisplayName() + ", ";
            }
            placeholders.put("users", users);
        } catch (URISyntaxException e) {
            return false;
        }

        // replace article placeholders
        for (String placeholder : placeholders.keySet()) {
            articleTitle = articleTitle.replaceAll("<<" + placeholder + ">>", placeholders.get(placeholder));
        }
        for (String placeholder : placeholders.keySet()) {
            articleContent = articleContent.replaceAll("<<" + placeholder + ">>", placeholders.get(placeholder));
        }

        // try to create a wiki article
        try {
            MediaWikiBot wikiBot = new MediaWikiBot(wikiURL.toString());
            wikiBot.login(notes.getLogin(), notes.getPassword(), "PM");
            SimpleArticle article = new SimpleArticle(articleTitle);
            article.setText(articleContent);
            wikiBot.writeContent(article);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProcessException e) {
            e.printStackTrace();
        } catch (ActionException e) {
            e.printStackTrace();
        }

        // try output article content
        try {
            out.write(articleContent.getBytes());
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    protected boolean readTemplate() {
        BufferedReader templateReader;

        // open report template file for read
        try {
            templateReader = new BufferedReader(new FileReader(REPORT_TEMPLATE));
        } catch (FileNotFoundException e) {
            // @todo: move this message to error wrapper
            System.out.println("Can't generate report. First create report template. More information in help.");
            return true;
        }

        // read wiki URL
        try {
            wikiURL = new URI(templateReader.readLine());
        } catch (Exception e) {
            // @todo: move this message to error wrapper
            System.out.println("Wrong report template. First line must contains wiki URL");
            return true;
        }

        // read wiki Title
        try {
            articleTitle = templateReader.readLine();
        } catch (IOException e) {
            // @todo: move this message to error wrapper
            System.out.println("Wrong report template. Second line must contains article title");
            return true;
        }

        // read first line of article content
        try {
            articleContent = templateReader.readLine() + "\n";
        } catch (IOException e) {
            // @todo: move this message to error wrapper
            System.out.println("Wrong report template. Third line must contains article content");
            return true;
        }

        // read other lines of article content
        try {
            String line;
            while((line = templateReader.readLine()) != null) {
                articleContent += line + "\n";
            }
        } catch (IOException ignored) {}
        return true;
    }
}
