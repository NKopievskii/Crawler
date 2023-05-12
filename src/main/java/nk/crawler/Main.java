package nk.crawler;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class Main {
    public static void main(String[] args) {
        LocalDateTime start = LocalDateTime.now();
        redirectOutput("output.txt");
        System.out.println(start.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
        String resource = "https://elderscrolls.fandom.com/ru/wiki/%D0%A1%D0%BB%D1%83%D0%B6%D0%B5%D0%B1%D0%BD%D0%B0%D1%8F:AllPages";
//        String resource = "https://elderscrolls.fandom.com/ru/wiki/";
        FandomCrawler crawler = new FandomCrawler();
        crawler.crawl(resource);
        LocalDateTime end = LocalDateTime.now();
        System.out.println(end.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
        long seconds = Duration.between(start, end).getSeconds();
        String duration = String.format(
                "%d:%02d:%02d",
                seconds / 3600,
                (seconds % 3600) / 60,
                seconds % 60);
        System.out.println("Общее время выполнения: " + duration);
    }

    public static boolean redirectOutput(String path) {
        PrintStream out;
        try {
            out = new PrintStream(new FileOutputStream(path));
        } catch (FileNotFoundException e) {
            return false;
        }
        System.setOut(out);
        return true;
    }
}