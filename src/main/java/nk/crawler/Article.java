package nk.crawler;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.persistence.oxm.annotations.XmlCDATA;

import java.io.File;
import java.util.UUID;


//ObjectMapper
@Data
@NoArgsConstructor
@XmlRootElement(name = "doc")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"title", "categories", "creator", "creationDate", "text"})
public class Article implements XmlStorable {
    private ArticleField title = new ArticleField();
    private ArticleField categories = new ArticleField();
    private ArticleField creator = new ArticleField();
    @XmlElement(name = "creation_date")
    private ArticleField creationDate = new ArticleField();
    private ArticleField text = new ArticleField();

    public Article(String title, String category, String creator, String creationDate, String text) {
        this.title.setText(title);
        this.categories.setText(category);
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
        @XmlValue
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
