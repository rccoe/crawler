import org.junit.*;
import java.util.*;
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
        new Website("google.com").save();

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
        Website root = new Website("google.com").save();

        root.addLink("/about");

        //check links
        assertEquals(1, Link.count());
        assertEquals(1, root.links.size());

        //check deletion

        root.delete();

        assertEquals(0, Website.count());
        assertEquals(0, Link.count());


    }

}
