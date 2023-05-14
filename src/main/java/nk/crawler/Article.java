package nk.crawler;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.*;
import org.eclipse.persistence.oxm.annotations.XmlCDATA;

import java.io.File;
import java.util.UUID;

//Lamboc
//Навесить сетеров

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

    public ArticleField getTitle() {
        return title;
    }

    public void setTitle(ArticleField title) {
        this.title = title;
    }

    public ArticleField getCategory() {
        return category;
    }

    public void setCategory(ArticleField category) {
        this.category = category;
    }

    public ArticleField getCreator() {
        return creator;
    }

    public void setCreator(ArticleField creator) {
        this.creator = creator;
    }

    public ArticleField getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(ArticleField creationDate) {
        this.creationDate = creationDate;
    }

    public ArticleField getText() {
        return text;
    }

    public void setText(ArticleField text) {
        this.text = text;
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

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public boolean isAuto() {
            return auto;
        }

        public void setAuto(boolean auto) {
            this.auto = auto;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isVerify() {
            return verify;
        }

        public void setVerify(boolean verify) {
            this.verify = verify;
        }
    }

}
