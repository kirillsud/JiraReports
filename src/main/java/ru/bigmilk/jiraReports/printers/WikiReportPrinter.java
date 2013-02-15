package ru.bigmilk.jiraReports.printers;

import com.atlassian.jira.rest.client.NullProgressMonitor;
import ru.bigmilk.jiraReports.ReportBuilder;
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
                "work log report, generates article on wiki, " +
                        "see https://github.com/kirillsud/JiraReports/wiki/Wiki-Report"
        );
    }

    @Override
    public void print(OutputStream out, ReportBuilder reportBuilder) throws PrinterException {
        // read template report file
        try {
            readTemplate();
        } catch (Exception e) {
            throw new PrinterException(e.getMessage());
        }

        // init placeholders list        https://github.com/kirillsud/JiraReports/wiki/Wiki-Report
        Map<String, String> placeholders = new HashMap<String, String>();

        // generate report
        OutputStream reportOutput = new ByteArrayOutputStream();
        super.print(reportOutput, reportBuilder);

        // init placeholders list
        placeholders.put("report", reportOutput.toString());

        // init dates variables
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        placeholders.put("current day", dateFormat.format(new Date()));
        placeholders.put("previous work day", dateFormat.format(ReportBuilder.getWorkDaysBefore(1)));
        placeholders.put("next work day", dateFormat.format(ReportBuilder.getWorkDaysBefore(-1)));
        placeholders.put("current date", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").format(new Date()));

        // init users variables
        try {
            placeholders.put("current user", reportBuilder.getJiraClient().getUserClient().
                    getUser(reportBuilder.getLogin(), new NullProgressMonitor()).getDisplayName());

            String users = "";
            for (String user : reportBuilder.getUsers()) {
                users += reportBuilder.getJiraClient().getUserClient().
                        getUser(user, new NullProgressMonitor()).getDisplayName() + ", ";
            }
            placeholders.put("users", users);
        } catch (URISyntaxException e) {
            throw new PrinterException(e.getMessage());
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
            wikiBot.login(reportBuilder.getLogin(), reportBuilder.getPassword(), "PM");
            SimpleArticle article = new SimpleArticle(articleTitle);
            article.setText(articleContent);
            wikiBot.writeContent(article);
        } catch (MalformedURLException e) {
            throw new PrinterException(e.getMessage());
        } catch (ProcessException e) {
            throw new PrinterException(e.getMessage());
        } catch (ActionException e) {
            throw new PrinterException(e.getMessage());
        }

        // try output article content
        try {
            out.write(articleContent.getBytes());
        } catch (IOException e) {
            throw new PrinterException(e.getMessage());
        }
    }

    protected void readTemplate() throws Exception {
        BufferedReader templateReader;

        // open report template file for read
        try {
            templateReader = new BufferedReader(new FileReader(REPORT_TEMPLATE));
        } catch (FileNotFoundException e) {
            // @todo: move this message to error wrapper
            throw new Exception("Can't generate report. First create report template. More information in help.");
        }

        // read wiki URL
        try {
            wikiURL = new URI(templateReader.readLine());
        } catch (Exception e) {
            // @todo: move this message to error wrapper
            throw new Exception("Wrong report template. First line must contains wiki URL");
        }

        // read wiki Title
        try {
            articleTitle = templateReader.readLine();
        } catch (IOException e) {
            // @todo: move this message to error wrapper
            throw new Exception("Wrong report template. Second line must contains article title");
        }

        // read first line of article content
        try {
            articleContent = templateReader.readLine() + "\n";
        } catch (IOException e) {
            // @todo: move this message to error wrapper
            throw new Exception("Wrong report template. Third line must contains article content");
        }

        // read other lines of article content
        try {
            String line;
            while((line = templateReader.readLine()) != null) {
                articleContent += line + "\n";
            }
        } catch (IOException ignored) {}
    }
}
