package appland.problemsView;

import appland.AppMapBaseTest;
import appland.problemsView.model.ScannerFinding;
import com.intellij.testFramework.LightVirtualFile;
import org.junit.Test;

import java.util.List;

public class ScannerProblemTest extends AppMapBaseTest {
    @Test
    public void zeroBasedLine() {
        var scannerFinding = new ScannerFinding();
        scannerFinding.stack = List.of("parent/child/package/file.java:100");
        scannerFinding.setFindingsFile(new LightVirtualFile());
        var problemLocation = scannerFinding.getProblemLocation();
        assertNotNull(problemLocation);
        assertEquals(Integer.valueOf(100), problemLocation.line);

        var scannerProblem = new ScannerProblem(FindingsManager.getInstance(getProject()), new LightVirtualFile(), scannerFinding);
        assertEquals("IntelliJ's line is 0-based", 99, scannerProblem.getLine());
    }
}