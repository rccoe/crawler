package models;

import java.util.*;
import javax.persistence.*;

import play.db.jpa.*;

@Entity
public class Website extends Model {

    public String url;
    public boolean isCrawled;
    public Date crawledAt;

    @OneToMany(mappedBy = "website", cascade = CascadeType.ALL)
    public List<Link> links;

    public Website(String url) {
        this.url = url;
        this.isCrawled = false;
        this.links = new ArrayList<Link>();
        this.crawledAt = new Date();
    }

    public Link addLink(String path) {
        Link newLink = Link.findOrCreate(this, path);
        this.addLink(newLink);
        return newLink;
    }

    public Website addLink(Link link) {
        if (this.links.contains(link))
            return this;
        this.links.add(link);
        this.save();
        return this;
    }


}