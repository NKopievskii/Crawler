package nk.crawler;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.*;
import lombok.Data;
import org.eclipse.persistence.oxm.annotations.XmlCDATA;

import java.io.File;
import java.util.UUID;

@Data
@XmlRootElement(name = "doc")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"title", "category", "creator", "creationDate", "text"})
public class Article implements XmlStorable {
    @XmlElement(name = "title")
    private ArticleField title = new ArticleField();
    @XmlElement(name = "category")
    private ArticleField category = new ArticleField();
    @XmlElement(name = "creator")
    private ArticleField creator = new ArticleField();
    @XmlElement(name = "creation_date")
    private ArticleField creationDate = new ArticleField();
    @XmlElement(name = "text")
    private ArticleField text = new ArticleField();


    public Article() {
    }

    public Article(String title, String category, String creator, String creationDate, String text) {
        this.title.setText(title);
        this.category.setText(category);
        this.creator.setText(creator);
        this.creationDate.setText(creationDate);
        this.text.setText(text);
    }

    @Override
    public void saveToXML(String path) {
        try {
            JAXBContext context = JAXBContext.newInstance(this.getClass());
            Marshaller mar = context.createMarshaller();
            mar.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            UUID uuid = UUID.nameUUIDFromBytes(title.getText().getBytes());
            File file = new File(path + uuid + ".xml");
//            File file = new File(path + this.title + ".xml");
            mar.marshal(this, file);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    @Data
    public static class ArticleField {

        @XmlElement
        @XmlCDATA
        public String text;
        @XmlAttribute
        public boolean auto = true;
        @XmlAttribute
        public String type = "str";
        @XmlAttribute
        public boolean verify = true;

    }

}
