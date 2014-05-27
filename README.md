
<h1> CoeCrawler </h1>

<p>This application uses the Java Play framework v. 1.2.7 to crawl a single website and display the connections.</p>

<h3> Models </h3>

<p>
    Websites are stored individually, with all of their links mapped to that website. Link interconnections are
    also stored in a joinTable of links -> other links
</p>

<h3> Crawling </h3>

<p>
    I decided to use crawler4j as my crawling library, as it has a good amount of documentation and apparent user base.
    This had several drawbacks, unfortunately. The concurrency built into the application isn't great, and relies on
    lots of 'sleep' calls to avoid deadlocks. A future task that should be done is to implement asynchronous crawling, currently
    the application freezes up while crawling a website. Crawling should be kicked off by a Promise from the Application
    controller and the results saved to the database either while crawling or once it has finished.

</p>

<h3> Visualization </h3>

<p>
    The d3.js hierarchical edge bundling was an appropriate example to use for this type of visualization - a digraph
    with the possibility of cyclic links. A regular sitemap or tree structure would make interlinking very difficult to
    represent.

    </p>