package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

public class Application extends Controller {

    public static void index() {

        List<Website> oldSites = Website.find(
                "order by crawledAt desc"
        ).from(1).fetch(5);
        render(oldSites);
    }

}