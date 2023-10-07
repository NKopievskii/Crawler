package nk.crawler;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * Класс содержит метод запуска выполнения работы класса FandomCrawler
 */
@Slf4j
public class FandomCrawlerLauncher {
    /**
     * Выполняет настройку и вывод дополнительной информации для работы краулера класса FandomCrawler
     */
    public static void execute() {
        LocalDateTime start = LocalDateTime.now();
        log.info(start.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
        String resource = "https://elderscrolls.fandom.com/ru/wiki/%D0%A1%D0%BB%D1%83%D0%B6%D0%B5%D0%B1%D0%BD%D0%B0%D1%8F:AllPages";
        //String resource = "https://elderscrolls.fandom.com/ru/wiki/%D0%A1%D0%BB%D1%83%D0%B6%D0%B5%D0%B1%D0%BD%D0%B0%D1%8F:AllPages";
        //String resource = "https://harrypotter.fandom.com/ru/wiki/%D0%A1%D0%BB%D1%83%D0%B6%D0%B5%D0%B1%D0%BD%D0%B0%D1%8F:AllPages";
        //String resource = "https://starwars.fandom.com/ru/wiki/%D0%A1%D0%BB%D1%83%D0%B6%D0%B5%D0%B1%D0%BD%D0%B0%D1%8F:AllPages";
        log.info("Краулинг ресурса: " + resource);
        FandomCrawler crawler = new FandomCrawler();
        crawler.crawl(resource);
        LocalDateTime end = LocalDateTime.now();
        log.info(end.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
        long seconds = Duration.between(start, end).getSeconds();
        String duration = String.format(
                "%d:%02d:%02d",
                seconds / 3600,
                (seconds % 3600) / 60,
                seconds % 60);
        log.info("Общее время выполнения: " + duration);
    }
}
