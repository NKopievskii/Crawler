package nk.crawler;

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
    private int threadPoolLength;
    private int minLength;
    private static final Set<String> IGNORED_CATEGORY = new HashSet<>(
            Arrays.asList(
                    "", "Страница категории", "Шаблоны", "Шаблон", "Правила", "Участники", "Участник", "Обсуждение",
                    "Обсуждение участника", "The Elder Scrolls Wiki", "Обсуждение The Elder Scrolls Wiki",
                    "Файл", "Обсуждение файла", "MediaWiki", "Обсуждение MediaWiki", "Обсуждение шаблона",
                    "Справка", "Обсуждение справки", "Категория", "Обсуждение категории", "Форум", "Обсуждение форума",
                    "GeoJson", "GeoJson talk", "Блог участника", "Комментарий блога участника", "Блог", "Обсуждение блога",
                    "Модуль", "Обсуждение модуля", "Стена обсуждения", "Тема", "Приветствие стены обсуждения",
                    "Board", "Board Thread", "Topic", "Гаджет", "Обсуждение гаджета", "Определение гаджета",
                    "Обсуждение определения гаджета", "Map", "Map talk"

            )); //Для проверки категории

    private static final String NAVIGATION_XPATH = "//a[@title='Служебная:Все страницы' and starts-with(text(), 'Следующая страница')]"; //Текст следующая страница
    private static final String CONTENT_XPATH = "//div[@class='mw-allpages-body']//a";
    private static final String NAME_XPATH = "//div[@class='page-header']//span[@class='mw-page-title-main']";
    private static final String AUTHOR_XPATH = "(//bdi)[last()]";
    private static final String CATEGORY_XPATH = "//li[@class='category normal']";
    private static final String TEXT_XPATH = "//div[@class='mw-parser-output']/*";
    private static final String AUTHOR_REQUEST = "?action=history&dir=prev&limit=1";
    private static final String XML_PATH = "./articles/";
    private static final Safelist SAFELIST = Safelist.relaxed()
            .removeTags("div")
            .removeTags("a")
            .removeTags("img")
            .removeTags("span");

    public FandomCrawler(int minLength) {
        this.minLength = minLength;
    }

    public FandomCrawler(int threadPoolLength, int minLength) {
        this.threadPoolLength = threadPoolLength;
        this.minLength = minLength;
    }

    public FandomCrawler() {
        this.minLength = 200;
        this.threadPoolLength = Runtime.getRuntime().availableProcessors() * 2;
//        System.out.println("Используемое кол-во потоков: " + threadPoolLength);
    }

    public int getThreadPoolLength() {
        return threadPoolLength;
    }

    public void setThreadPoolLength(int threadPoolLength) {
        this.threadPoolLength = threadPoolLength;
    }

    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(int minLength) {
        this.minLength = minLength;
    }

    public boolean crawl(String url) {
        crawlNavigation(url);
        Path cPath = Path.of(XML_PATH);
        if (!Files.exists(cPath)) {
            try {
                Files.createDirectory(cPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        boolean terminationStatus;
        try (ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(threadPoolLength, threadPoolLength,
                1, TimeUnit.SECONDS, new SynchronousQueue<>())) {
            Runnable task = this::saveArticle;
            threadPoolExecutor.setRejectedExecutionHandler((runnable, executor) -> System.out.println("Rejected"));
            for (int i = 0; i < threadPoolLength; i++) {
                threadPoolExecutor.submit(task);
            }
            threadPoolExecutor.shutdown();
            try {
                terminationStatus = threadPoolExecutor.awaitTermination(6, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
//        System.out.println("Общее количество посещённых страниц: " + urlVisited.size());
        return terminationStatus;

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
        return Jsoup.clean(document
                .selectXpath(xPath)
                .html(), SAFELIST
        );
    }

    private XmlStorable parseContent(Document document) {
        List<String> category_elements = document.selectXpath(CATEGORY_XPATH).stream().map(Element::text).toList();
        if (category_elements.isEmpty() || !Collections.disjoint(IGNORED_CATEGORY, category_elements)) {
//            System.out.println("Не содержит категорий или Содержит неподходящую категорию");
            return null;
        }
        String category = String.join("/", category_elements);
        String text = parseText(document, TEXT_XPATH);
        if (text.length() <= minLength) {
//            System.out.println("Недопустимая длина статьи");
            return null;
        }
        String title = parseByXPath(document, NAME_XPATH, ", ");
        String url = document.baseUri();
        System.out.println(title + "\t(" + UUID.nameUUIDFromBytes(title.getBytes()) + "):\t" + url);
        Document authorDocument = getContent(url + AUTHOR_REQUEST);
        String author;
        if (authorDocument != null)
            author = parseByXPath(authorDocument, AUTHOR_XPATH, "");
        else
            author = "";
        return new Article(
                title,
                category,
                author,
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
