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

    private Map<WebURL, Set<WebURL>> localLinkMap;

    public CoeCrawler() {
        this.localLinkMap = new HashMap<WebURL, Set<WebURL>>();
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

            filterUnusableUrls(destUrls);

            if (!localLinkMap.containsKey(sourceUrl))
                localLinkMap.put(page.getWebURL(), new HashSet<WebURL>(destUrls));
            else {
                Set<WebURL> destSet = localLinkMap.get(sourceUrl);
                for (WebURL destUrl : destUrls) {
                    destSet.add(destUrl);
                }
            }
        }
    }

    private void filterUnusableUrls(List<WebURL> urls) {
        Iterator<WebURL> iterator = urls.iterator();
        String domain = (String)myController.getCustomData();


        while (iterator.hasNext()) {
            WebURL url = iterator.next();
            if (FILTERS.matcher(url.getPath()).matches()) {
                iterator.remove();
                break;
            }
            if (!url.getDomain().equals(domain)) {
                iterator.remove();
            }
        }
    }

    // This function is called by controller to get the local data of this
    // crawler when job is finished
    @Override
    public Object getMyLocalData() {
        return this.localLinkMap;
    }
}
