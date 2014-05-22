package models;

import java.util.*;
import javax.persistence.*;

import play.db.jpa.*;

@Entity
public class Website extends Model {

    public String url;
    public boolean isCrawled;

    @OneToMany(mappedBy = "website", cascade = CascadeType.ALL)
    public List<Link> links;

    public Website(String url) {
        this.url = url;
        this.isCrawled = false;
        this.links = new ArrayList<Link>();
    }

    public Website addLink(String path) {
        Link newLink = new Link(this, path).save();
        this.links.add(newLink);
        this.save();
        return this;
    }


}