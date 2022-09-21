package appland.problemsView;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * Annotator, which adds highlighting of detected problems to editors.
 */
public class AppMapFindingsAnnotator implements Annotator {
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof PsiFile && element.isValid()) {
            ProgressManager.checkCanceled();

            var project = element.getProject();

            // we're only handling committed PsiFiles because we have to match the problem range with the PSI tree
            var psiManager = PsiDocumentManager.getInstance(project);
            var document = psiManager.getDocument((PsiFile) element);
            if (document == null || psiManager.isUncommited(document)) {
                return;
            }

            var file = ((PsiFile) element).getVirtualFile();
            if (file == null || !file.isValid()) {
                return;
            }

            var maxLineCount = document.getLineCount();
            for (var problem : FindingsManager.getInstance(project).getScannerProblems(file)) {
                ProgressManager.checkCanceled();

                // IntelliJ's lines are 0-based
                var line = problem.getLine();
                if (line >= 0 && line < maxLineCount) {
                    var lineStart = document.getLineStartOffset(line);
                    var lineEnd = document.getLineEndOffset(line);
                    var lineText = document.getText(TextRange.create(lineStart, lineEnd));
                    var emptyLinePrefixLength = lineText.length() - StringUtil.trimLeading(lineText).length();

                    lineStart += emptyLinePrefixLength;
                    if (lineStart < lineEnd) {
                        holder.newAnnotation(HighlightSeverity.ERROR, problem.getText())
                                .range(TextRange.create(lineStart, lineEnd))
                                .create();
                    }
                }
            }
        }
    }
}
