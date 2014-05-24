package util;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import edu.uci.ics.crawler4j.url.WebURL;
import models.Link;
import models.Website;
import play.Play;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class CoeCrawlController extends CrawlController{

    public CoeCrawlController(CrawlConfig config, PageFetcher pageFetcher, RobotstxtServer robotstxtServer)
            throws Exception {
        super(config, pageFetcher, robotstxtServer);
    }

    public static void crawl(Website website, int maxPages, int numberOfCrawlers) {
        CrawlConfig config = new CrawlConfig();
        String tempFolder = Play.configuration.getProperty("play.tmp");
        System.out.println(tempFolder);
        config.setCrawlStorageFolder(tempFolder);
        config.setPolitenessDelay(1000);
        config.setMaxPagesToFetch(maxPages);

        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
//        robotstxtConfig.setEnabled(false);
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        try {
            CoeCrawlController controller = new CoeCrawlController(config, pageFetcher, robotstxtServer);
            controller.addSeed(website.url);

            controller.start(CoeCrawler.class, numberOfCrawlers);

            List<Object> crawlersLocalData = controller.getCrawlersLocalData();

            for (Object localData : crawlersLocalData) {
                Map<WebURL, Set<WebURL>> linkMap = (Map<WebURL, Set<WebURL>>) localData;
                saveLinks(website, linkMap);
            }
        }
        catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

    private static void saveLinks (Website website, Map<WebURL, Set<WebURL>> linkMap) {
        for (Map.Entry<WebURL, Set<WebURL>> entry : linkMap.entrySet()) {

            Link sourceLink = Link.findOrCreate(website, entry.getKey().getPath());
            System.out.println("Saving " + sourceLink.path);
            for (WebURL destUrl : entry.getValue()) {
                sourceLink.addTargetLink(destUrl.getPath());
            }
        }
    }


    @Override
    protected <T extends WebCrawler> void start(final Class<T> _c, final int numberOfCrawlers, boolean isBlocking) {
        try {
            finished = false;
            crawlersLocalData.clear();
            final List<Thread> threads = new ArrayList<Thread>();
            final List<T> crawlers = new ArrayList<T>();

            for (int i = 1; i <= numberOfCrawlers; i++) {
                T crawler = _c.newInstance();
                Thread thread = new Thread(crawler, "Crawler " + i);
                crawler.setThread(thread);
                crawler.init(i, this);
                thread.start();
                crawlers.add(crawler);
                threads.add(thread);
                System.out.println("Crawler " + i + " started.");
            }

            final CrawlController controller = this;

            Thread monitorThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        synchronized (waitingLock) {

                            while (true) {
                                sleep(10);
                                boolean someoneIsWorking = false;
                                for (int i = 0; i < threads.size(); i++) {
                                    Thread thread = threads.get(i);
                                    if (!thread.isAlive()) {
                                        if (!shuttingDown) {
                                            System.out.println("Thread " + i + " was dead, I'll recreate it.");
                                            T crawler = _c.newInstance();
                                            thread = new Thread(crawler, "Crawler " + (i + 1));
                                            threads.remove(i);
                                            threads.add(i, thread);
                                            crawler.setThread(thread);
                                            crawler.init(i + 1, controller);
                                            thread.start();
                                            crawlers.remove(i);
                                            crawlers.add(i, crawler);
                                        }
                                    } else if (crawlers.get(i).isNotWaitingForNewURLs()) {
                                        someoneIsWorking = true;
                                    }
                                }
                                if (!someoneIsWorking) {
                                    // Make sure again that none of the threads
                                    // are
                                    // alive.
                                    System.out.println("It looks like no thread is working, waiting for 1 second to make sure...");
                                    sleep(1);
                                    // Annoyingly slow

                                    someoneIsWorking = false;
                                    for (int i = 0; i < threads.size(); i++) {
                                        Thread thread = threads.get(i);
                                        if (thread.isAlive() && crawlers.get(i).isNotWaitingForNewURLs()) {
                                            someoneIsWorking = true;
                                        }
                                    }
                                    if (!someoneIsWorking) {
                                        if (!shuttingDown) {
                                            long queueLength = frontier.getQueueLength();
                                            if (queueLength > 0) {
                                                continue;
                                            }
                                            System.out.println("No thread is working and no more URLs are in queue waiting for another 1 second to make sure...");
                                            sleep(1);
                                            // Annoyingly slow
                                            queueLength = frontier.getQueueLength();
                                            if (queueLength > 0) {
                                                continue;
                                            }
                                        }

                                        System.out.println("All of the crawlers are stopped. Finishing the process...");
                                        // At this step, frontier notifies the
                                        // threads that were
                                        // waiting for new URLs and they should
                                        // stop
                                        frontier.finish();
                                        for (T crawler : crawlers) {
                                            crawler.onBeforeExit();
                                            crawlersLocalData.add(crawler.getMyLocalData());
                                        }

                                        System.out.println("Waiting for 1 second before final clean up...");
                                        sleep(1);
                                        // Annoyingly slow

                                        frontier.close();
                                        docIdServer.close();
                                        pageFetcher.shutDown();

                                        finished = true;
                                        waitingLock.notifyAll();

                                        return;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            monitorThread.start();

            if (isBlocking) {
                waitUntilFinish();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }






}