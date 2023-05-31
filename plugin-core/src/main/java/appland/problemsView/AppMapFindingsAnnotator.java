package appland.problemsView;

import appland.AppMapBundle;
import appland.files.AppMapFiles;
import appland.files.OpenAppMapFileNavigatable;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import lombok.AllArgsConstructor;
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

            annotateAppMapFindings(holder, project, document, file);
        }
    }

    private void annotateAppMapFindings(@NotNull AnnotationHolder holder, Project project, Document document, VirtualFile file) {
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
                            .withFix(new OpenAppMapFindingQuickFix(problem))
                            .range(TextRange.create(lineStart, lineEnd))
                            .create();
                }
            }
        }
    }

    @AllArgsConstructor
    private static class OpenAppMapFindingQuickFix implements IntentionAction {
        private final ScannerProblem problem;

        @Override
        public @IntentionName @NotNull String getText() {
            return AppMapBundle.get("annotator.openQuickFix.name");
        }

        @Override
        public @NotNull @IntentionFamilyName String getFamilyName() {
            return AppMapBundle.get("annotator.quickFixFamily");
        }

        @Override
        public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
            return true;
        }

        @Override
        public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
            var findingsFile = problem.getFindingsFile();
            if (findingsFile != null) {
                var appMapFile = AppMapFiles.findAppMapFileByMetadataFile(findingsFile);
                if (appMapFile != null) {
                    new OpenAppMapFileNavigatable(project, appMapFile, problem.getFinding()).navigate(true);
                }
            }
        }

        @Override
        public boolean startInWriteAction() {
            return false;
        }
    }
}
