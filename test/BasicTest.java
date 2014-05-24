import org.junit.*;
import play.test.*;
import models.*;

public class BasicTest extends UnitTest {


    @Before
    public void setup() {
        Fixtures.deleteDatabase();
    }

    @Test
    public void testCreateAndRetrieveWebsite(){
        // Create a new website and save it
        Website.findOrCreate("google.com");

        // Check website created
        Website google = Website.find("byUrl", "google.com").first();

        // Test exists
        assertNotNull(google);
        assertFalse(google.isCrawled);
        assertEquals(1, Website.count());
    }

    @Test
    public void testCreateLink(){
        // Create a new website and save it
        Website root = Website.findOrCreate("google.com");

        root.addOrFindLink("/about");

        //check links
        assertEquals(1, Link.count());
        assertEquals(1, root.links.size());

        //Check for duplicate

        root.addOrFindLink("/about");
        assertEquals(1, Link.count());
        assertEquals(1, root.links.size());

        //check deletion

        root.delete();

        assertEquals(0, Website.count());
        assertEquals(0, Link.count());


    }

    @Test
    public void testAddLinkToLink() {
        Website root = Website.findOrCreate("google.com");

        Link about = root.addOrFindLink("/about");

        Link product = about.addTargetLink("/about/product");

        assertNotNull(product);
        assertNotNull(about);

        //check counts
        assertEquals(2, Link.count());
        assertEquals(2, root.links.size());


        // Check target/source
        assertEquals(about.links.get(0), product);
        assertEquals(0, product.links.size());


    }

    @Test
    public void testFixture() {
        Fixtures.loadModels("data.yml");

        assertEquals(1, Website.count());

        Website google = Website.find("byUrl", "www.google.com").first();


    }

}
