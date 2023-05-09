package nk.crawler;

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
import java.util.*;

public class FandomCrawler {
    //TODO: Сделать файлик с мапой наличия ссылки и титула файла, чтобы проверять какие файлы есть, каких нет и при краулинге дозаписывать
    //TODO: Если страница категории - игнорить (так и написано: страница категории)
    private final Set<String> urlVisited = new HashSet<>();
    private final Queue<String> urlQueue = new LinkedList<>();
    private final Queue<String> urlNavigation = new LinkedList<>();
    private int breakpoint;
    private int minLength;
    //
    //div[class=mw-allpages-nav] contains Следующая страница
    private static final String NAVIGATION_QUERY = "a[title=Служебная:Все страницы] , a[data-tracking=explore-all-pages]";
    private static final String NAVIGATION_XPATH = "//a[@title='Служебная:Все страницы' or @data-tracking='explore-all-pages']";


    //div[class=mw-allpages-body]
    private static final String CONTENT_QUERY = "div[class=mw-allpages-body]";
    private static final String CONTENT_XPATH = "//div[@class='mw-allpages-body']//a";

    private static final String NAME_XPATH = "//div[@class='page-header']//span[@class='mw-page-title-main']";
    //<span class="mw-page-title-main">«Безумцы» Предела</span>
    private static final String AUTHOR_XPATH = "(//bdi)[last()]"; // Тут всё сложнее чем просто запрос с этой же страницы
    //?action=history&dir=prev&limit=1
    private static final String CATEGORY_XPATH = "//li[@class='category normal']"; //Вообще категории больше похоже на ключевые слова
    //    private static final String KEYWORDS_XPATH = ""; // Ключевых слов нет
    private static final String TEXT_XPATH = "//div[@class='mw-parser-output']";
    private static final String AUTHOR_REQUEST = "?action=history&dir=prev&limit=1";
    private static final String XML_PATH = "./articles/";

    public FandomCrawler() {
        this.breakpoint = 3;
        this.minLength = 200;
    }

    public int getBreakpoint() {
        return breakpoint;
    }

    public void setBreakpoint(int breakpoint) {
        this.breakpoint = breakpoint;
    }

    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(int minLength) {
        this.minLength = minLength;
    }

    public void crawl(String url) {
        crawlNavigation(url);
        crawlAndSave();
//        System.out.println(urlQueue.size());
//        // -----
//        int threadBound = 2;
//        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(0, threadBound,
//                0L, TimeUnit.SECONDS, new SynchronousQueue<>());
//        Callable<String> task = () -> {
//            this.crawlAndSave();
//        };
//        for (int i = 0; i < threadBound + 1; i++) {
//            threadPoolExecutor.submit(task);
//        }
//        threadPoolExecutor.shutdown();
//
//        // -----
//        Runnable task = this::crawlAndSave;
//        Thread currentThread = Thread.currentThread();
//        ThreadGroup threadGroup = currentThread.getThreadGroup();
//        crawlPages();
    }

    private static Document getContent(String usingUrl) {
        Document document;
        try {
            document = Jsoup.parse(new URI(usingUrl).toURL().openStream(), "UTF-8", usingUrl);
        } catch (FileNotFoundException | UnknownHostException e) {
            return null;
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return document;
    }

    private String parseByXPath(@NotNull Document document, String xPath, String delimiter) {
        return String.join(delimiter, document.selectXpath(xPath).eachText());
    }

    private XmlStorable parseContent(Document document) {
        String text = parseByXPath(document, TEXT_XPATH, "\n");
        if (text.length() <= minLength)
            return null;
        String title = parseByXPath(document, NAME_XPATH, ", ");
        String category = parseByXPath(document, CATEGORY_XPATH, "|");
        String url = document.baseUri();
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

    private void crawlAndSave() {
        String currentUrl;
        Document document;
        while (!urlQueue.isEmpty()) {
            currentUrl = urlQueue.poll();
            if (urlVisited.contains(currentUrl))
                continue;
            urlVisited.add(currentUrl);
            document = getContent(currentUrl);
            if (document == null)
                continue;
            XmlStorable article = parseContent(document);
            if (article != null)
                article.saveToXML(XML_PATH);
            if (urlVisited.size() >= breakpoint)
                break;
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
            if (urlQueue.size() >= breakpoint)
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
        Element nextPage = document.select(NAVIGATION_QUERY).last();
        if (nextPage != null)
            return nextPage.absUrl("href");
        return null;
    }
}
