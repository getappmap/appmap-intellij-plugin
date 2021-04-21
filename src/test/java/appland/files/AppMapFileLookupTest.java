package appland.files;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class AppMapFileLookupTest extends LightPlatformCodeInsightFixture4TestCase {
    @Test
    public void locateFilenameByRelativePath() {
        PsiFile base = myFixture.configureByText("a.txt", "");
        var bestResult = myFixture.addFileToProject("dir/subdir1/subdir2/searched-file.txt", "");
        var located = FileLookup.findRelativeFile(getProject(), base.getVirtualFile(), "dir/subdir1/subdir2/searched-file.txt");
        assertEquals(bestResult.getVirtualFile(), located);
    }

    @Test
    public void locateFilenameByIndex() {
        PsiFile base = myFixture.configureByText("a.txt", "");

        myFixture.addFileToProject("searched-file.txt", "");
        myFixture.addFileToProject("dir/searched-file.txt", "");
        myFixture.addFileToProject("dir/subdir1/searched-file.txt", "");
        var bestResult = myFixture.addFileToProject("dir/subdir1/subdir2/searched-file.txt", "");

        var located = FileLookup.findRelativeFile(getProject(), base.getVirtualFile(), "subdir2/searched-file.txt");
        assertEquals(bestResult.getVirtualFile(), located);
    }

    @Test
    public void filename() {
        assertEquals("file.txt", FileLookup.filename("file.txt"));
        assertEquals("file.txt", FileLookup.filename("dir/file.txt"));
        assertEquals("file.txt", FileLookup.filename("dir/subdir/file.txt"));
        assertEquals("file.txt", FileLookup.filename("/dir/subdir/file.txt"));
    }

    @Test
    public void parents() {
        assertTrue(FileLookup.parentsReversed("file.txt").isEmpty());
        assertEquals(Collections.singletonList("dir"), FileLookup.parentsReversed("dir/file.txt"));
        assertEquals(Arrays.asList("subdir", "dir"), FileLookup.parentsReversed("dir/subdir/file.txt"));
        assertEquals(Arrays.asList("subdir2", "subdir", "dir"), FileLookup.parentsReversed("dir/subdir/subdir2/file.txt"));
    }
}