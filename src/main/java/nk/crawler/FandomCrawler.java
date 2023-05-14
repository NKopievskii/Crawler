package nk.crawler;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class FandomCrawler {
    private final Set<String> urlVisited = new HashSet<>();
    private final Queue<String> urlQueue = new LinkedList<>();
    private final Queue<String> urlNavigation = new LinkedList<>();
    @Getter
    @Setter
    private int threadPoolLength;
    @Getter
    @Setter
    private int minLength;
    private static final Set<String> IGNORED_CATEGORY = new HashSet<>(
            Arrays.asList(
                    "", "Страница категории", "Шаблоны", "Шаблон", "Правила", "Участники", "Участник", "Обсуждение",
                    "Обсуждение участника", "The Elder Scrolls Wiki", "Обсуждение The Elder Scrolls Wiki",
                    "Файл", "Обсуждение файла", "MediaWiki", "Обсуждение MediaWiki", "Обсуждение шаблона", "Справка",
                    "Обсуждение справки", "Категория", "Обсуждение категории", "Форум", "Обсуждение форума", "GeoJson",
                    "GeoJson talk", "Блог участника", "Комментарий блога участника", "Блог", "Обсуждение блога",
                    "Модуль", "Обсуждение модуля", "Стена обсуждения", "Тема", "Приветствие стены обсуждения",
                    "Board", "Board Thread", "Topic", "Гаджет", "Обсуждение гаджета", "Определение гаджета",
                    "Обсуждение определения гаджета", "Map", "Map talk", "Многозначные термины"

            )); //Для проверки категории

    private static final String NAVIGATION_XPATH =
            "//a[@title='Служебная:Все страницы' and starts-with(text(), 'Следующая страница')]";
    private static final String CONTENT_XPATH = "//div[@class='mw-allpages-body']//a";
    private static final String NAME_XPATH = "//*[@class='mw-page-title-main']";
    private static final String CATEGORY_XPATH = "//li[@class='category normal']";
    private static final String TEXT_XPATH = "//div[@class='mw-parser-output']/*";
    private static final String CREATOR_REQUEST = "?action=history&dir=prev&limit=1";
    private static final String CREATOR_XPATH = "(//bdi)[last()]";
    private static final String CREATOR_DATE_XPATH = "(//a[@class='mw-changeslist-date'])[last()]";
    private static final String XML_PATH = "./articles/";

    public FandomCrawler(int minLength) {
        this.minLength = minLength;
    }

    public FandomCrawler(int threadPoolLength, int minLength) {
        this.threadPoolLength = threadPoolLength;
        this.minLength = minLength;
    }

    public FandomCrawler() {
        this.minLength = 100;
        this.threadPoolLength = Runtime.getRuntime().availableProcessors() * 2;
    }

    private boolean crawlQueue() {
        boolean terminationStatus;
        int timeout = urlQueue.size() / 200;
        try (ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(threadPoolLength, threadPoolLength,
                0, TimeUnit.SECONDS, new SynchronousQueue<>())) {
            Runnable task = this::saveArticle;
//            threadPoolExecutor.setRejectedExecutionHandler((runnable, executor) -> System.out.println("Rejected"));
            for (int i = 0; i < threadPoolLength; i++) {
                threadPoolExecutor.submit(task);
            }
            threadPoolExecutor.shutdown();
            try {
                terminationStatus = threadPoolExecutor.awaitTermination(timeout, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
//        System.out.println("Общее количество посещённых страниц: " + urlVisited.size());
        return terminationStatus;
    }

    public void crawl(String url) {
        crawlNavigation(url);
        System.out.println(urlQueue.size());
        Path cPath = Path.of(XML_PATH);
        if (!Files.exists(cPath)) {
            try {
                Files.createDirectory(cPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        crawlQueue();

    }

    private static Document getContent(String usingUrl) {
        Document document;
        try {
            document = Jsoup.parse(new URI(usingUrl).toURL().openStream(), "UTF-8", usingUrl);
        } catch (FileNotFoundException | UnknownHostException | NullPointerException e) {
            return null;
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return document;
    }

    private String parseByXPath(@NotNull Document document, @NotNull String xPath, String delimiter) {
        return String.join(delimiter, document.selectXpath(xPath).eachText());
    }

    private String parseText(@NotNull Document document, @NotNull String xPath) {
        for (Element element : document.select("*"))
            if (!element.hasText() && element.isBlock())
                element.remove();
        List<String> strings = document.selectXpath(xPath).eachText();
        return String.join("\n", strings);
    }

    private XmlStorable parseContent(Document document) {
        List<String> category_elements = document.selectXpath(CATEGORY_XPATH).stream().map(Element::text).toList();
        if (category_elements.isEmpty() || !Collections.disjoint(IGNORED_CATEGORY, category_elements)) {
//            System.out.println("Не содержит категорий или cодержит неподходящую категорию");
            return null;
        }
        String category = String.join("/", category_elements);
        String text = parseText(document, TEXT_XPATH);
        if (text.length() <= minLength) {
//            System.out.println("Недопустимая длина статьи");
            return null;
        }
        String title = parseByXPath(document, NAME_XPATH, ", ");
        if (title.isEmpty())
            return null;
        String url = document.baseUri();

        System.out.println(title + "\t(" + UUID.nameUUIDFromBytes(title.getBytes()) + "):\t" + url);

        Document authorDocument = getContent(url + CREATOR_REQUEST);
        String creator = "";
        String creationDate = "";
        if (authorDocument != null) {
            creator = parseByXPath(authorDocument, CREATOR_XPATH, "");
            creationDate = parseByXPath(authorDocument, CREATOR_DATE_XPATH, "");
        }
        return new Article(
                title, category,
                creator, creationDate,
                text
        );
    }

    private void saveArticle() {
        String currentUrl;
        Document document;
        while (!urlQueue.isEmpty()) {
            synchronized (urlQueue) {
                currentUrl = urlQueue.poll();
            }
            if (urlVisited.contains(currentUrl))
                continue;
            urlVisited.add(currentUrl);
            document = getContent(currentUrl);
            if (document == null)
                continue;
            XmlStorable article = parseContent(document);
            if (article != null)
                article.saveToXML(XML_PATH);
        }
    }

    private void crawlNavigation(String url) {
        urlNavigation.add(url);
        String currentUrl;
        Document document;
        while (!urlNavigation.isEmpty()) {
            currentUrl = urlNavigation.poll();
            urlVisited.add(currentUrl);
            document = getContent(currentUrl);
            if (document == null)
                continue;
            currentUrl = getNextNavigation(document);
            if (urlVisited.contains(currentUrl))
                continue;
            urlNavigation.add(currentUrl);
            fillQueue(document);
            break;
        }
        urlVisited.clear();
    }

    private void fillQueue(Document document) {
        Elements elements = document.selectXpath(CONTENT_XPATH);
        for (Element element : elements) {
            urlQueue.add(element.absUrl("href"));
        }
    }

    private String getNextNavigation(Document document) {
        Element nextPage = document.selectXpath(NAVIGATION_XPATH).last();
        if (nextPage != null)
            return nextPage.absUrl("href");
        return null;
    }
}
