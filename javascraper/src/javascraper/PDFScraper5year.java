package javascraper;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PDFScraper5year {
    private static final String BASE_URL = "https://papers.nips.cc";
    private static final String SAVE_DIR = "downloads";
    private static final int THREAD_COUNT = 5; // Number of parallel downloads
    private static final int MAX_YEARS = 5; // Download only the latest 5 years

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        try {
            Document doc = Jsoup.connect(BASE_URL + "/paper_files/paper/").get();
            Elements yearLinks = doc.select("a[href^=/paper_files/paper/]");

            // Extract and sort years in descending order
            List<String> yearUrls = new ArrayList<>();
            for (Element yearLink : yearLinks) {
                yearUrls.add(BASE_URL + yearLink.attr("href"));
            }
            yearUrls.sort(Collections.reverseOrder()); // Latest years first

            // Process only the latest MAX_YEARS
            for (int i = 0; i < Math.min(MAX_YEARS, yearUrls.size()); i++) {
                String yearUrl = yearUrls.get(i);
                String year = yearUrl.replaceAll(".*paper/(\\d+).*", "$1");
                System.out.println("Scraping year: " + year);
                executor.submit(() -> scrapeYear(yearUrl, year));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        executor.shutdown();
    }

    private static void scrapeYear(String yearUrl, String year) {
        try {
            Document doc = Jsoup.connect(yearUrl).get();
            
            // Select both old and new abstract links
            Elements paperLinks = doc.select("a[href*=-Abstract.html], a[href*=-Abstract-Conference.html]");

            ExecutorService paperExecutor = Executors.newFixedThreadPool(THREAD_COUNT);
            for (Element paperLink : paperLinks) {
                String paperPageUrl = BASE_URL + paperLink.attr("href");
                paperExecutor.submit(() -> downloadPDF(paperPageUrl, year));
            }
            paperExecutor.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void downloadPDF(String paperPageUrl, String year) {
        try {
            Document doc = Jsoup.connect(paperPageUrl).get();
            Elements pdfLinks = doc.select("a[href$=.pdf]");

            // Extract paper title
            String title = doc.select("h4").text();
            if (title.isEmpty()) {
                title = doc.select("title").text().split("-")[0].trim(); // Fallback if <h4> is missing
            }
            title = sanitizeFileName(title);

            for (Element pdfLink : pdfLinks) {
                String pdfUrl = BASE_URL + pdfLink.attr("href");
                String fileName = title + ".pdf";
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

            Path filePath = Paths.get(yearDir, fileName);
            Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Downloaded: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_"); // Remove invalid characters
    }
}
