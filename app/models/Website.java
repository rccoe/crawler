package models;

import java.util.*;
import javax.persistence.*;

import play.db.jpa.*;

@Entity
public class Website extends Model {

    private String url;
    private boolean isCrawled;

    public Website(String url) {
        this.url = url;
        this.isCrawled = false;
    }

}