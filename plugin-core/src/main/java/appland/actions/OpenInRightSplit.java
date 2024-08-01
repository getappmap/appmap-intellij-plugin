package appland.actions;

import appland.utils.DataContexts;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class OpenInRightSplit {
    private static final String RIGHT_SPLIT_ACTION = "OpenInRightSplit";

    private OpenInRightSplit() {
    }

    /**
     * Opens the given file in a right split editor.
     * <p>
     * This method exists to work around the internal API of {@link com.intellij.ide.actions.OpenInRightSplitAction}
     * in 2024.2.
     * <p>
     * See <a href="https://youtrack.jetbrains.com/issue/IJPL-158380">YouTrack</a> for the JetBrains YouTrack ticket.
     *
     * @param project     Current project
     * @param file        File to open
     * @param navigatable Optional navigatable to navigate to
     */
    @RequiresEdt
    public static void openInRightSplit(@NotNull Project project,
                                        @NotNull VirtualFile file,
                                        @Nullable Navigatable navigatable) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        AnAction action = ActionManager.getInstance().getAction(RIGHT_SPLIT_ACTION);

        // Context suitable for the SDK's OpenInRightSplitAction
        DataContext actionContext = DataContexts.createCustomContext((dataId) -> {
            if (CommonDataKeys.PROJECT.is(dataId)) {
                return project;
            }

            if (CommonDataKeys.VIRTUAL_FILE.is(dataId)) {
                return file;
            }

            if (navigatable != null && CommonDataKeys.PSI_ELEMENT.is(dataId)) {
                // The action is using PSI_ELEMENT to retrieve a Navigatable.
                // The DataKey enforces the PsiElement type, so we have to use a wrapper to make it work.
                if (navigatable instanceof PsiElement) {
                    return navigatable;
                }

                // Wrap in a PsiElement to satisfy the DataKey.
                // OpenInRightSplitAction uses the navigate method.
                return new FakePsiElement() {
                    @Override
                    public PsiElement getParent() {
                        // try to provide a valid parent, it's the navigation target
                        return PsiManager.getInstance(project).findFile(file);
                    }

                    @Override
                    public void navigate(boolean requestFocus) {
                        navigatable.navigate(requestFocus);
                    }
                };
            }

            return null;
        });

        ActionUtil.invokeAction(action,
                actionContext,
                ActionPlaces.KEYBOARD_SHORTCUT,
                null,
                EmptyRunnable.getInstance());
    }
}