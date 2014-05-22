package models;

import java.lang.reflect.WildcardType;
import java.util.*;
import javax.persistence.*;

import play.db.jpa.*;

@Entity
public class Link extends Model {

    public String path;

    @ManyToOne
    public Website website;

    @ManyToMany(cascade=CascadeType.ALL)
    @JoinTable(name = "link_link",
            joinColumns = {@JoinColumn(name = "src_link_id") },
            inverseJoinColumns = { @JoinColumn(name = "dest_link_id")})
    public List<Link> links;


    private Link(Website website, String path) {
        this.website = website;
        this.path = path;
        this.links = new ArrayList<Link>();
    }

    public Link addTargetLink(String path) {
        Link link = this.website.addLink(path);
        this.links.add(link);
        this.save();
        return link;
    }

    public static Link findOrCreate(Website website, String path) {
        Link found = Link.find("byWebsiteAndPath", website, path).first();
        if (found == null) {
            return new Link(website, path).save();
        }
        return found;
    }


}
