package nk.crawler;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Класс содержит различные методы для получения информации из указанного ресурса (рассчитан для работы с The Elder Scrolls Wiki), и сохранения полученной информации по средствам реализации интерфейса XmlStorable.
 *
 * @author Копиевский Н. Ю.
 */
@Slf4j
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
            ));
    private static final String NAVIGATION_XPATH =
            "//a[@title='Служебная:Все страницы' and starts-with(text(), 'Следующая страница')]";
    private static final String CONTENT_XPATH = "//div[@class='mw-allpages-body']//a";
    private static final String NAME_XPATH = "//*[@class='mw-page-title-main']";
    private static final String CATEGORY_XPATH = "//li[@class='category normal']";
    private static final String TEXT_XPATH = "//div[@class='mw-parser-output']/*";
    private static final String CREATOR_REQUEST = "?action=history&dir=prev&limit=1";
    private static final String CREATOR_XPATH = "(//bdi)[last()]";
    private static final String CREATOR_DATE_XPATH = "(//a[contains(@class, 'mw-changeslist-date')])[last()]";
    private static final String XML_PATH = "./articles/";

    public FandomCrawler(int threadPoolLength, int minLength) {
        this.threadPoolLength = threadPoolLength;
        this.minLength = minLength;
    }

    public FandomCrawler() {
        this.minLength = 100;
        this.threadPoolLength = 64;
    }
    /**
     * Стартовый метод получения информации из ресурса
     * @param url URL страницы с которой необходимо выполнить получения информации
     */
    public void crawl(String url) {
        crawlNavigation(url);
        log.info("Размер очереди страниц: " + urlQueue.size());
        Path cPath = Path.of(XML_PATH);
        if (!Files.exists(cPath)) {
            try {
                Files.createDirectory(cPath);
            } catch (IOException e) {
                log.error("Ошибка создания директории: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
        crawlQueue();

    }
    /**
     * Создаёт пул потоков (ThreadPoolExecutor) для работы с очередью страниц и назначает им задачу получения и сохранения статей.
     * @return true если краулинг очереди завершился корректно и false, если возникли ошибки
     */
    private boolean crawlQueue() {
        boolean terminationStatus;
        int timeout = urlQueue.size() / 200;
        try (ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(threadPoolLength, threadPoolLength,
                0, TimeUnit.SECONDS, new SynchronousQueue<>())) {
            Runnable task = this::saveArticle;
            for (int i = 0; i < threadPoolLength; i++) {
                threadPoolExecutor.submit(task);
            }
            threadPoolExecutor.shutdown();
            try {
                terminationStatus = threadPoolExecutor.awaitTermination(timeout, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                log.error("Ошибка в работе потоков: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
        return terminationStatus;
    }

    /**
     * Выполняет переход по навигационной странице и получение списка адресов статей
     * @param url URL страницы навигации
     */
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

    /**
     * Получает содержимое страницы по средствам GET запроса и обработки jsoup
     * @param usingUrl URL страницы с которой необходимо получить содержимое
     * @return содержимое страницы в формате Document (jsoup)
     */
    private static Document getContent(String usingUrl) {
        Document document;
        try {
            document = Jsoup.parse(new URI(usingUrl).toURL().openStream(), "UTF-8", usingUrl);
        } catch (FileNotFoundException | UnknownHostException | NullPointerException e) {
            log.warn("Нет доступа к содержимому страницы: " + e.getMessage());
            return null;
        } catch (IOException | URISyntaxException e) {
            log.error("Ошибка получения страницы: " + e.getMessage());
            throw new RuntimeException(e);
        }
        return document;
    }

    /**
     * Получает следующую страницу навигации
     * @param document содержимое страницы навигации
     * @return URL следующей страницы навигации
     */
    private String getNextNavigation(Document document) {
        Element nextPage = document.selectXpath(NAVIGATION_XPATH).last();
        if (nextPage != null)
            return nextPage.absUrl("href");
        return null;
    }

    /**
     * Получает список статей и заполняет им очередь
     * @param document содержимое страницы навигации
     */
    private void fillQueue(Document document) {
        Elements elements = document.selectXpath(CONTENT_XPATH);
        for (Element element : elements) {
            urlQueue.add(element.absUrl("href"));
        }
    }
    /**
     * Получение конкретных данных со страницы: название, категории, автор, дата создания, текст.
     * @param document содержимое страницы
     * @return объект класса данных, сохраняемых в XML (XMLStorable)
     */
    private XmlStorable parseContent(Document document) {
        List<String> category_elements = document.selectXpath(CATEGORY_XPATH).stream().map(Element::text).toList();
        if (category_elements.isEmpty() || !Collections.disjoint(IGNORED_CATEGORY, category_elements)) {
            return null;
        }
        String category = String.join("/", category_elements);
        String text = parseText(document);
        if (text.length() <= minLength) {
            return null;
        }
        String title = parseByXPath(document, NAME_XPATH, ", ");
        if (title.isEmpty())
            return null;
        String url = document.baseUri();
        log.info(title + "\t(" + UUID.nameUUIDFromBytes(title.getBytes()) + "):\t" + url);
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

    /**
     * Получение строки по XPath-запросу, с разделением по указанному разделителю
     * @param document содержимое страницы на которой необходимо выполнить запрос
     * @param xPath XPath-запрос
     * @param delimiter разделитель
     * @return строка с данным полученными по XPath-запросу
     */
    private String parseByXPath(@NotNull Document document, @NotNull String xPath, String delimiter) {
        return String.join(delimiter, document.selectXpath(xPath).eachText());
    }

    /**
     * Получение текстового содержимого страницы
     * @param document содержимое страницы
     * @return строка с текстовыми данными страницы
     */
    private String parseText(@NotNull Document document) {
        for (Element element : document.select("*"))
            if (!element.hasText() && element.isBlock())
                element.remove();
        List<String> strings = document.selectXpath(FandomCrawler.TEXT_XPATH).eachText();
        //String regex = "Примечания Галерея Описания Источники";
        return String.join("\n", strings);
    }

    /**
     * Сохраняет статьи из списка статей в XML файл
     */
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




}
