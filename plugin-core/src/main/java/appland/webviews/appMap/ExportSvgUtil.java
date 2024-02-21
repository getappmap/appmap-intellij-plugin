package appland.webviews.appMap;

import appland.AppMapBundle;
import com.intellij.CommonBundle;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ExportSvgUtil {
    /**
     * @param project          Current project
     * @param defaultFileName  The default file name to suggest in the path selector
     * @param contextFile      The context of the export action to show a reasonable default directory
     * @param fileContent      The content to write to the selected file
     * @param afterFileWritten Consumer, which is called on the EDT with the new file after the file was successfully exported to disk.
     */
    @RequiresEdt
    public static void exportToFile(@NotNull Project project,
                                    @NotNull String defaultFileName,
                                    @Nullable VirtualFile contextFile,
                                    @NotNull Supplier<String> fileContent,
                                    @NotNull Consumer<VirtualFile> afterFileWritten) {
        var contextFileOrDir = contextFile == null || !contextFile.isInLocalFileSystem()
                ? ProjectUtil.guessProjectDir(project)
                : contextFile;
        if (contextFileOrDir != null && !contextFileOrDir.isDirectory()) {
            contextFileOrDir = contextFileOrDir.getParent();
        }

        var dialog = new ChooseExportFileDialog(project, contextFileOrDir, defaultFileName);
        if (dialog.showAndGet()) {
            var filePath = dialog.getSelectedFilePath();
            if (filePath == null) {
                return;
            }

            if (Files.exists(filePath)) {
                int result = Messages.showYesNoDialog(
                        project,
                        AppMapBundle.get("appmap.editor.exportSVG.errorFileExists.message", filePath),
                        AppMapBundle.get("appmap.editor.exportSVG.dialogTitle"),
                        IdeBundle.message("action.overwrite"),
                        CommonBundle.getCancelButtonText(),
                        Messages.getWarningIcon()
                );
                if (result != Messages.YES) {
                    return;
                }
            }

            try {
                var exportedFile = ProgressManager.getInstance().run(new Task.WithResult<VirtualFile, IOException>(project, AppMapBundle.get("appmap.editor.exportSVG.progressTitle"), false) {
                    @Override
                    protected @Nullable VirtualFile compute(@NotNull ProgressIndicator indicator) throws IOException {
                        indicator.setIndeterminate(true);
                        Files.writeString(filePath, fileContent.get());
                        return LocalFileSystem.getInstance().refreshAndFindFileByNioFile(filePath);
                    }
                });

                if (exportedFile != null) {
                    afterFileWritten.accept(exportedFile);
                }
            } catch (IOException e) {
                Messages.showErrorDialog(project,
                        AppMapBundle.get("appmap.editor.exportSVG.errorExportFailed.message", e.getMessage()),
                        AppMapBundle.get("appmap.editor.exportSVG.errorExportFailed.title"));
            }
        }
    }

    private static class ChooseExportFileDialog extends DialogWrapper {
        private final TextFieldWithBrowseButton fileTextField;

        public ChooseExportFileDialog(@NotNull Project project,
                                      @Nullable VirtualFile context,
                                      @NotNull String defaultFileName) {
            super(project, true);

            var extension = FileUtilRt.getExtension(defaultFileName);

            fileTextField = new TextFieldWithBrowseButton();
            fileTextField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(@NotNull DocumentEvent e) {
                    getOKAction().setEnabled(e.getDocument().getLength() > 0);
                }
            });

            var fileOrFolder = FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor()
                    .withFileFilter(file -> Comparing.equal(file.getExtension(), extension, file.isCaseSensitive()));
            fileTextField.addBrowseFolderListener(new TextBrowseFolderListener(fileOrFolder) {
                @Override
                protected @NotNull String chosenFileToResultingText(@NotNull VirtualFile chosenFile) {
                    return createSystemDependantFilePath(chosenFile, defaultFileName);
                }
            });

            // always set a text to trigger the document listener registered above
            var initialFilePath = context != null ? createSystemDependantFilePath(context, defaultFileName) : "";
            fileTextField.setText(initialFilePath);

            setTitle(AppMapBundle.get("appmap.editor.exportSVG.dialogTitle"));
            setOKButtonText(AppMapBundle.get("appmap.editor.exportSVG.exportButton"));

            init();
        }

        @Override
        protected @Nullable JComponent createNorthPanel() {
            var panel = new JPanel(new GridBagLayout());
            var constraints = new GridBagConstraints();
            constraints.anchor = GridBagConstraints.WEST;

            constraints.weightx = 0;
            panel.add(new JBLabel(AppMapBundle.get("appmap.editor.exportSVG.filePathLabel")), constraints);

            constraints.weightx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.ipadx = JBUI.scale(5);
            panel.add(fileTextField, constraints);

            panel.setMinimumSize(new JBDimension(450, 60));
            return panel;
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            return null;
        }

        @Override
        public @Nullable JComponent getPreferredFocusedComponent() {
            return fileTextField.getTextField();
        }

        @Override
        protected @NonNls @Nullable String getDimensionServiceKey() {
            return "#appland.exportToSvg";
        }

        public @Nullable Path getSelectedFilePath() {
            var text = fileTextField.getText();
            if (text.isBlank()) {
                return null;
            }

            try {
                return Path.of(fileTextField.getText());
            } catch (InvalidPathException e) {
                return null;
            }
        }

        @Override
        protected @NotNull List<ValidationInfo> doValidateAll() {
            if (fileTextField.getText().isBlank()) {
                return List.of(new ValidationInfo(AppMapBundle.get("appmap.editor.exportSVG.validation.missingFilePath"), fileTextField));
            }

            var path = getSelectedFilePath();
            if (path == null || !path.isAbsolute()) {
                return List.of(new ValidationInfo(AppMapBundle.get("appmap.editor.exportSVG.validation.noAbsoluteFilePath"), fileTextField));
            }

            return Collections.emptyList();
        }

        private static @NotNull String createSystemDependantFilePath(@NotNull VirtualFile context,
                                                                     @NotNull String defaultFileName) {
            var systemPath = FileUtilRt.toSystemDependentName(context.getPath());
            return context.isDirectory() ? systemPath + File.separatorChar + defaultFileName : systemPath;
        }
    }
}
