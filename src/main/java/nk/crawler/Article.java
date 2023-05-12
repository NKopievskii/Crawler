package nk.crawler;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import org.eclipse.persistence.oxm.annotations.XmlCDATA;
import org.eclipse.persistence.oxm.annotations.XmlPath;

import java.io.File;
import java.util.UUID;

@XmlRootElement(name = "doc")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"title", "author", "category", "text"})
public class Article implements XmlStorable {
    private String title;
    @XmlPath("title/@auto")
    private boolean title_auto = true;
    @XmlPath("title/@type")
    private String title_type = "str"; //Стиль написания под python
    @XmlPath("title/@verify")
    private boolean title_verify = true;
    private String author;
    @XmlPath("author/@auto")
    private boolean author_auto = true;
    @XmlPath("author/@type")
    private String author_type = "str"; //Стиль написания под python
    @XmlPath("author/@verify")
    private boolean author_verify = true;
    private String category;
    @XmlPath("category/@auto")
    private boolean category_auto = true;
    @XmlPath("category/@type")
    private String category_type = "str"; //Стиль написания под python
    @XmlPath("category/@verify")
    private boolean category_verify = true;
    @XmlCDATA
    private String text;
    @XmlPath("text/@auto")
    private boolean text_auto = true;
    @XmlPath("text/@type")
    private String text_type = "str"; //Стиль написания под python
    @XmlPath("text/@verify")
    private boolean text_verify = true;

    public Article() {
    }

    public Article(String title, String category, String author, String text) {
        this.title = title;
        this.author = author;
        this.category = category;
        this.text = text;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "Article{" +
                "title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", category='" + category + '\'' +
                ", text='" + text + '\'' +
                '}';
    }

    @Override
    public void saveToXML(String path) {
        try {
            JAXBContext context = JAXBContext.newInstance(this.getClass());
            Marshaller mar = context.createMarshaller();
            mar.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            UUID uuid = UUID.nameUUIDFromBytes(title.getBytes());
            File file = new File(path + uuid + ".xml");
//            File file = new File(path + this.title + ".xml");
            mar.marshal(this, file);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }
}
