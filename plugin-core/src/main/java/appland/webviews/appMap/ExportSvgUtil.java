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
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
            if (context != null) {
                fileTextField.setText(createSystemDependantFilePath(context, defaultFileName));
            }

            var fileOrFolder = FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor()
                    .withFileFilter(file -> Comparing.equal(file.getExtension(), extension, file.isCaseSensitive()));

            fileTextField.addBrowseFolderListener(new TextBrowseFolderListener(fileOrFolder) {
                @Override
                protected @NotNull String chosenFileToResultingText(@NotNull VirtualFile chosenFile) {
                    return createSystemDependantFilePath(chosenFile, defaultFileName);
                }
            });

            setTitle(AppMapBundle.get("appmap.editor.exportSVG.dialogTitle"));
            setOKButtonText(AppMapBundle.get("appmap.editor.exportSVG.exportButton"));
            init();
        }

        private static @NotNull String createSystemDependantFilePath(@NotNull VirtualFile context,
                                                                     @NotNull String defaultFileName) {
            return context.isDirectory()
                    ? FileUtilRt.toSystemDependentName(context.getPath()) + File.separatorChar + defaultFileName
                    : FileUtilRt.toSystemDependentName(context.getPath());
        }

        public @Nullable Path getSelectedFilePath() {
            return Path.of(fileTextField.getText());
        }

        @Override
        protected @Nullable JComponent createNorthPanel() {
            var panel = new JPanel(new GridBagLayout());
            var constraints = new GridBagConstraints();
            constraints.anchor = GridBagConstraints.WEST;

            constraints.weightx = 0;
            panel.add(new JBLabel("File path:"), constraints);

            constraints.weightx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.ipadx = JBUI.scale(5);
            panel.add(fileTextField, constraints);

            panel.setMinimumSize(new JBDimension(350, 50));
            return panel;
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            return null;
        }
    }
}
