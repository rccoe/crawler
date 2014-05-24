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

import java.util.*;
import java.util.concurrent.ExecutorService;

public class CoeCrawlController extends CrawlController{

    public CoeCrawlController(CrawlConfig config, PageFetcher pageFetcher, RobotstxtServer robotstxtServer)
            throws Exception {
        super(config, pageFetcher, robotstxtServer);
    }

    public static void crawl(Website website, int maxPages) {
        CrawlConfig config = new CrawlConfig();
        String tempFolder = Play.configuration.getProperty("play.tmp");
        System.out.println(tempFolder);
        config.setCrawlStorageFolder(tempFolder);
        config.setPolitenessDelay(300);
        config.setMaxPagesToFetch(maxPages);

        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
//        robotstxtConfig.setEnabled(false);
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        try {
            CoeCrawlController controller = new CoeCrawlController(config, pageFetcher, robotstxtServer);
            controller.addSeed(website.url);

            controller.start(CoeCrawler.class, 1);

            List<Object> crawlersLocalDataList = controller.getCrawlersLocalData();

            Map<String, Set<String>> masterLinkMap;

            // Ensure all links unique while in memory to avoid DB collisions
            if (crawlersLocalDataList.size() == 1) {
                masterLinkMap = (Map<String, Set<String>>) (crawlersLocalDataList.get(0));
            }
            else {
                masterLinkMap = new HashMap<String, Set<String>>();
                for (Object crawlerLocalData : crawlersLocalDataList) {
                    Map<String, Set<String>> crawlerLinkMap = (Map<String, Set<String>>) crawlerLocalData;
                    for (Map.Entry<String, Set<String>> entry : crawlerLinkMap.entrySet()) {

                        // Check if in master map
                        Set<String> destUrls = masterLinkMap.get(entry.getKey());
                        if (destUrls == null) {
                            masterLinkMap.put(entry.getKey(), entry.getValue());
                        }
                        else {
                            destUrls.addAll(entry.getValue());
                        }

                    }

                }
            }
            saveLinks(website, masterLinkMap);

        }
        catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

    private static void saveLinks (Website website, Map<String, Set<String>> linkMap) {
        for (Map.Entry<String, Set<String>> entry : linkMap.entrySet()) {

            Link sourceLink = website.addOrFindLink(entry.getKey());
            System.out.println("Saving " + sourceLink.path);
            for (String destPath : entry.getValue()) {
                sourceLink.addTargetLink(destPath);
            }
        }
    }

    /*
        This method needed overriding for the following reasons:

            1) We are only checking 1 host, so politeness restricts our request
               frequency, so parallel threads is useless
            2) The implementation for concurrency was bad (nonexistent)
            3) There were several sleep() methods that were holding this back

     */
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
                                Thread.sleep(100);
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
//                                    sleep(1);
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
//                                            sleep(1);
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
//                                        sleep(1);
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