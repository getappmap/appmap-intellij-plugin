package appland.files;

import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopes;
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
    public void scopedLocateFilenameByIndex() {
        PsiFile base = myFixture.configureByText("a.txt", "");

        myFixture.addFileToProject("file.txt", "");
        myFixture.addFileToProject("dirA/subdir/file.txt", "");
        var subdirB = myFixture.addFileToProject("dirB/file.txt", "").getVirtualFile().getParent();
        var bestResult = myFixture.addFileToProject("dirB/subdir/file.txt", "");
        myFixture.addFileToProject("dirB/subdir/subdir/file.txt", "");

        var located = FileLookup.findRelativeFile(getProject(),
                GlobalSearchScopes.directoryScope(getProject(), subdirB, true),
                base.getVirtualFile(),
                "subdir/file.txt");
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

    @Test
    public void windowsAbsolutePath() {
        assertTrue(FileLookup.isAbsolutePath("C:\\Apps\\Ruby\\file.rb", true));
        assertTrue(FileLookup.isAbsolutePath("C:/Apps//Ruby//file.rb", true));

        assertFalse(FileLookup.isAbsolutePath("\\Apps\\Ruby\\file.rb", true));
        assertFalse(FileLookup.isAbsolutePath("\\Apps\\Ruby\\file.rb", true));
        assertFalse(FileLookup.isAbsolutePath("Apps/Ruby/file.rb", true));
        assertFalse(FileLookup.isAbsolutePath("x/Ruby/file.rb", true));
    }
}