package controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import flexjson.JSONSerializer;
import flexjson.transformer.DateTransformer;
import play.data.validation.Check;
import play.data.validation.CheckWith;
import play.db.jpa.Transactional;
import play.mvc.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import models.*;
import util.CoeCrawlController;

public class Application extends Controller {

    public static void index() {
        render();
    }

    public static void list() {

        List<Website> oldSites = Website.find("order by crawledAt desc").fetch(5);

        JSONSerializer websiteSerializer = new JSONSerializer().include("id","url", "isCrawled", "crawledAt").exclude("*");
        websiteSerializer.transform(new DateTransformer("yyyy-MM-dd HH:mm:ss"), "crawledAt");

        renderJSON(websiteSerializer.serialize(oldSites));
    }

    @Transactional
    public static void crawlWebsite(@CheckWith(UrlExistenceValidator.class) String url) {

        if(validation.hasErrors()) {
            response.status = 400;
            renderText(validation.errors());
        }
        try {
            URL urlObject = new URL(url);
        } catch (MalformedURLException mux ) {/* already validated)*/
            url = "http://" + url;
        }
        Website website = Website.findOrCreate(url);
        if (!website.isCrawled) {
            CoeCrawlController.crawl(website, 50);
        }
        renderJSON(website.id);
    }

    public static void show(Long id) {
        Website website = Website.findById(id);
        render(website);
    }

    public static void getLinksFromWebsite(Long id) {
        Website website = Website.findById(id);
        JSONSerializer linkSerializer = new JSONSerializer().include("id","path","links.id").exclude("*");
        String json = linkSerializer.serialize(website.links);
        renderJSON(linkSerializer.serialize(website.links));
    }




    static class UrlExistenceValidator extends Check {
        @Override
        public boolean isSatisfied(Object model, Object urlObj) {
            if (!(urlObj instanceof String)) {
                return false;
            }

            final String urlString = (String) urlObj;
            try {
                URL url;
                try {
                    url = new URL(urlString);
                }
                catch (MalformedURLException muex) {
                    url = new URL("http://" + urlString);
                }
                if (!url.getPath().isEmpty()) {
                    setMessage(urlString + " contains a path, not just a hostname", urlString);
                    return false;
                }
                final HttpURLConnection huc =  (HttpURLConnection) url.openConnection();
                huc.setRequestMethod("HEAD");
                huc.connect();
                final int code = huc.getResponseCode();
                setMessage("URL didn't respond", Integer.toString(code) );
                return code != 404;
            }

            catch (IOException ioex) {
                setMessage("Problem connecting to URL", ioex.getMessage() );
                return false;
            }


        }
    }
}