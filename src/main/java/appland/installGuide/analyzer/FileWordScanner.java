package appland.installGuide.analyzer;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

class FileWordScanner {
    @Getter
    private final @NotNull VirtualFile file;
    private final String fileContent;

    public FileWordScanner(@NotNull VirtualFile file) {
        this.file = file;

        var document = FileDocumentManager.getInstance().getDocument(file);
        if (document != null) {
            fileContent = document.getText();
        } else {
            fileContent = "";
        }
    }

    boolean containsWord(@NotNull String word) {
        var pattern = Pattern.compile("(\\W|^)" + Pattern.quote(word) + "(\\W|$)", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(fileContent).find();
    }
}
