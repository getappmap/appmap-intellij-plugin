package appland.files;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FileLocationTest {
    @Test
    public void parse() {
        assertNull(FileLocation.parse("file.txt:"));

        assertEquals(new FileLocation("file.txt", null), FileLocation.parse("file.txt"));
        assertEquals(new FileLocation("file.txt", 42), FileLocation.parse("file.txt:42"));

        assertEquals(new FileLocation("dir/file.txt", null), FileLocation.parse("dir/file.txt"));
        assertEquals(new FileLocation("dir/file.txt", 42), FileLocation.parse("dir/file.txt:42"));
    }
}