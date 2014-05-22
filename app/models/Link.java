package models;

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
    public List<Link> link_lists = new ArrayList<Link>();


    public Link(Website website, String path) {
        this.website = website;
        this.path = path;
    }


}
