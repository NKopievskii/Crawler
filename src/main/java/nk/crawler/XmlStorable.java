package nk.crawler;

/**
 * Интерфейс для сохранения данных в XML
 */
public interface XmlStorable {
    /**
     * Сохранение статьи в XML
     * @param path путь сохранения данных
     */
    void saveToXML(String path);
}
