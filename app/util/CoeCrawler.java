package util;

import com.sun.jndi.url.ldaps.ldapsURLContextFactory;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import models.Link;
import models.Website;
import org.joda.time.Weeks;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Robert Coe implementation of WebCrawler
 */
public class CoeCrawler extends WebCrawler {


    private int linksVisited;

    private Map<String, Set<String>> localLinkMap;

    public CoeCrawler() {
        this.localLinkMap = new HashMap<String, Set<String>>();
    }

    // Only html files
    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|bmp|gif|jpe?g"
            + "|png|tiff?|mid|mp2|mp3|mp4"
            + "|wav|avi|mov|mpeg|ram|m4v|pdf"
            + "|rm|smil|wmv|swf|wma|zip|rar|gz|ico))$");


    @Override
    public boolean shouldVisit(WebURL url) {
        String href = url.getURL().toLowerCase();
        if (FILTERS.matcher(href).matches())
            return false;

        String rootDomain = (String) myController.getCustomData();
        if (rootDomain == null)
            return true;
        if (!url.getDomain().equals(rootDomain))
            return false;
        return true;
    }


    /**
     * This function is called when a page is fetched and ready
     * to be processed by your program.
     */
    @Override
    public void visit(Page page) {
        // Set root domain in controller customData if not there
        if (myController.getCustomData() == null) {
            myController.setCustomData(page.getWebURL().getDomain());
        }

        if (page.getParseData() instanceof HtmlParseData) {
            WebURL sourceUrl = page.getWebURL();
            HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
            List<WebURL> destUrls = htmlParseData.getOutgoingUrls();

            Set<String> destPathSet = filterUsableUrlsToPaths(destUrls);

            if (!localLinkMap.containsKey(sourceUrl))
                localLinkMap.put(page.getWebURL().getPath(), destPathSet);
            else {
                Set<String> masterDestSet = localLinkMap.get(sourceUrl);
                masterDestSet.addAll(destPathSet);
            }
        }
    }

    private Set<String> filterUsableUrlsToPaths(List<WebURL> urls) {
        String domain = (String)myController.getCustomData();
        Set<String> pathSet = new HashSet<String>();

        for (WebURL url : urls) {
            if (!url.getDomain().equals(domain)) {
                break;
            }
            if (FILTERS.matcher(url.getPath()).matches()) {
                break;
            }
            pathSet.add(url.getPath());
        }
        return pathSet;
    }

    // This function is called by controller to get the local data of this
    // crawler when job is finished
    @Override
    public Object getMyLocalData() {
        return this.localLinkMap;
    }
}
