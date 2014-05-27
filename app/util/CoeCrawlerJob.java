package util;

import models.Link;
import models.Website;
import play.jobs.*;
import play.libs.F;

import java.util.Date;
import java.util.Map;
import java.util.Set;

public class CoeCrawlerJob extends Job {

    private String url;
    private int maxPages;

    public CoeCrawlerJob(String url, int maxPages) {
        this.url = url;
        this.maxPages = maxPages;
    }

    @Override
    public  Map<String, Set<String>> doJobWithResult() {
        return CoeCrawlController.crawl(url, maxPages);

    }

}
