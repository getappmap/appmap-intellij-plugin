package appland.cli;

import org.junit.Test;

import static org.junit.Assert.*;

public class ManifestTest {

    @Test
    public void testParseValidManifest() {
        var json = """
                {
                  "tag_name": "v1.2.3",
                  "assets": [
                    {
                      "name": "appmap-linux-x64",
                      "url": "https://example.com/appmap-linux-x64",
                      "digest": "sha256:12345"
                    },
                    {
                      "name": "appmap-win-x64.exe",
                      "url": "https://example.com/appmap-win-x64.exe",
                      "digest": "sha256:67890"
                    }
                  ]
                }
                """;
        var manifest = Manifest.parse(json);
        assertNotNull(manifest);
        assertEquals("1.2.3", manifest.version);

        var linuxAsset = manifest.getAsset("linux-x64");
        assertNotNull(linuxAsset);
        assertEquals("https://example.com/appmap-linux-x64", linuxAsset.url);
        assertEquals("sha256:12345", linuxAsset.digest);

        var winAsset = manifest.getAsset("win-x64");
        assertNotNull(winAsset);
        assertEquals("https://example.com/appmap-win-x64.exe", winAsset.url);
        assertEquals("sha256:67890", winAsset.digest);
    }

    @Test
    public void testParseInvalidVersion() {
        var json = """
                {
                  "tag_name": "invalid",
                  "assets": []
                }
                """;
        var manifest = Manifest.parse(json);
        assertNull(manifest);
    }

    @Test
    public void testParseMissingFields() {
        var manifest = Manifest.parse("{}");
        assertNull(manifest);
    }
}
