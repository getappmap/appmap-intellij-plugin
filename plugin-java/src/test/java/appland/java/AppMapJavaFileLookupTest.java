package appland.java;

import appland.files.AppMapFileLookup;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

public class AppMapJavaFileLookupTest extends LightJavaCodeInsightFixtureTestCase {
    public void testJavaLookup() {
        myFixture.addClass("package org.example; public class Lib {}");

        var lookup = new AppMapJavaFileLookup();
        var data = new AppMapFileLookup.Data("org/example/Lib.java", null);

        var found = lookup.findFile(getProject(), data);
        assertNotNull(found);
        assertEquals("Lib.java", found.getName());
    }
}
