package javascraper;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class simpleScrapper {
    private static final String BASE_URL = "https://papers.nips.cc";
    private static final String SAVE_DIR = "downloads";

    public static void main(String[] args) {
        try {
            // Step 1: Get the main page
            Document doc = Jsoup.connect(BASE_URL + "/paper_files/paper/").get();
            Elements yearLinks = doc.select("a[href^=/paper_files/paper/]");

            for (Element yearLink : yearLinks) {
                String yearPageUrl = BASE_URL + yearLink.attr("href");
                String year = yearPageUrl.replaceAll(".*paper/(\\d+).*", "$1");
                System.out.println("Scraping year: " + year);
                scrapeYear(yearPageUrl, year);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void scrapeYear(String yearUrl, String year) {
        try {
            Document doc = Jsoup.connect(yearUrl).get();
            Elements paperLinks = doc.select("a[href$=-Abstract-Conference.html]");

            for (Element paperLink : paperLinks) {
                String paperPageUrl = BASE_URL + paperLink.attr("href");
                downloadPDF(paperPageUrl, year);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void downloadPDF(String paperPageUrl, String year) {
        try {
            Document doc = Jsoup.connect(paperPageUrl).get();
            Elements pdfLinks = doc.select("a[href$=.pdf]");
            
            for (Element pdfLink : pdfLinks) {
                String pdfUrl = BASE_URL + pdfLink.attr("href");
                String fileName = pdfUrl.substring(pdfUrl.lastIndexOf('/') + 1);
                saveFile(pdfUrl, year, fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveFile(String fileURL, String year, String fileName) {
        try (InputStream in = new URL(fileURL).openStream()) {
            String yearDir = SAVE_DIR + File.separator + year;
            Files.createDirectories(Paths.get(yearDir));
            Files.copy(in, Paths.get(yearDir, fileName));
            System.out.println("Downloaded: " + fileName + " in year " + year);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
