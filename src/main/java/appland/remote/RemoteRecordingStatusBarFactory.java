package appland.remote;

import appland.AppMapBundle;
import appland.Icons;
import appland.actions.StopAppMapRecordingAction;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IconLikeCustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.event.MouseEvent;

public class RemoteRecordingStatusBarFactory implements StatusBarWidgetFactory {
    @Override
    public @NonNls @NotNull String getId() {
        return "appmap.recordingStatusFactory";
    }

    @Override
    public @Nls @NotNull String getDisplayName() {
        return AppMapBundle.get("statusBar.recording.displayName");
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return RemoteRecordingStatusService.getInstance(project).getActiveRecordingURL() != null;
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return new RecordingStatusWidget();
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
    }

    @Override
    public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
        return false;
    }

    private static class RecordingStatusWidget implements StatusBarWidget, IconLikeCustomStatusBarWidget {
        private final JBLabel label;

        public RecordingStatusWidget() {
            label = new JBLabel(AppMapBundle.get("statusBar.recording.recordingStatus"));
            label.setIcon(Icons.STOP_RECORDING_ACTION);
            label.addMouseListener(new MouseInputAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        e.consume();

                        ActionUtil.invokeAction(new StopAppMapRecordingAction(), label, ActionPlaces.STATUS_BAR_PLACE, null, null);
                    }
                }
            });
        }

        @Override
        public @NonNls @NotNull String ID() {
            return "appmap.recordingStatus";
        }

        @Override
        public void install(@NotNull StatusBar statusBar) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public JComponent getComponent() {
            return label;
        }
    }
}
