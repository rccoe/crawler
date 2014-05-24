package models;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.persistence.*;

import play.data.validation.*;
import play.db.jpa.*;

@Entity
public class Website extends Model {

    public String url;

    public boolean isCrawled;
    public Date crawledAt;

    @OneToMany(mappedBy = "website", cascade = CascadeType.ALL)
    public List<Link> links;

    private Website(String url) {
        this.url = url;
        this.isCrawled = false;
        this.links = new ArrayList<Link>();
        this.crawledAt = new Date();
    }

    public static Website findOrCreate(String url) {
        Website website = Website.find("byUrl", url).first();
        if (website == null) {
            website = new Website(url).save();
        }
        return website;
    }

    public Link addOrFindLink(String path) {
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