package nk.crawler;

import com.sun.xml.txw2.annotation.XmlCDATA;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@XmlRootElement(name = "doc")
@XmlType(propOrder = {"title", "author", "category", "text"})
public class Article implements XmlStorable{
    private  String title;
    private  String author;
    private  String category;
    private  String text;

    /*
    Optional Tags
     */

    public Article() {
    }

    public Article(String title, String category, String author, String text){
        this.title = title;
        this.author = author;
        this.category = category;
        this.text = text;
    }

    public String getTitle() {
        return title;
    }
    @XmlElement(name = "title")
    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }
    @XmlElement(name = "author")
    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCategory() {
        return category;
    }
    @XmlElement(name = "category")
    public void setCategory(String category) {
        this.category = category;
    }

    public String getText() {
        return text;
    }

    @XmlElement(name = "text")
    @XmlCDATA
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
        try
        {
            JAXBContext context = JAXBContext.newInstance(this.getClass());
            Marshaller mar= context.createMarshaller();
            mar.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            File file = new File(path + this.title+".xml");
            Path cPath = Path.of(path);
            if (!Files.exists(cPath))
                Files.createDirectory(cPath);
            mar.marshal(this, file);
        } catch (JAXBException | IOException e) {
            e.printStackTrace();
        }
    }
}
