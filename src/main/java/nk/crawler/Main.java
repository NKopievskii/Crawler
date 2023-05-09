package nk.crawler;

public class Main {
    public static void main(String[] args) {
        String resource = "https://elderscrolls.fandom.com/ru/wiki/";
        FandomCrawler crawler = new FandomCrawler();
        crawler.crawl(resource);
//        crawler.crawl("https://elderscrolls.fandom.com/ru/wiki/2920,_%D0%9C%D0%B5%D1%81%D1%8F%D1%86_%D0%92%D0%B5%D1%87%D0%B5%D1%80%D0%BD%D0%B5%D0%B9_%D0%B7%D0%B2%D0%B5%D0%B7%D0%B4%D1%8B_(%D1%82._12)");
    }
}