package appland.actions;

import appland.settings.AppMapApplicationSettingsService;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Base class for actions of the AppMap feature surface, i.e. actions which must be unavailable while the
 * plugin is inactive.
 * <p>
 * The plugin is quiescent while it's inactive — no indexer, no scanner and no JSON-RPC server run — so these
 * actions would fail confusingly instead of being unavailable. The whole feature surface is disabled, not just
 * the subset depending on the JSON-RPC server, so that "inactive plugin ⇒ the plugin does nothing" holds
 * coherently.
 * <p>
 * This class exists strictly to <em>disable</em> actions. {@link #update} applies
 * {@link #isAppMapAvailable()} unconditionally, so {@link #isAvailable(AnActionEvent)} can only narrow
 * availability further, never widen it. Actions which are the routes back into an active state must therefore
 * not extend it: sign-in, sign-out, <em>Set Organization Configuration…</em> and <em>Plugin Status Report</em>.
 * <p>
 * The two remote-recording actions don't extend it either — their presentation isn't a predicate, because they
 * swap visibility in the tool window's toolbar, and Stop has to stay usable while a recording is in flight.
 * They apply {@link #isAppMapAvailable()} in their own {@code update()} instead.
 */
public abstract class AppMapFeatureAction extends AnAction {
    protected AppMapFeatureAction() {
    }

    protected AppMapFeatureAction(@Nullable Icon icon) {
        super(icon);
    }

    /**
     * The single place deciding whether the AppMap feature surface is available. Also used by the
     * remote-recording actions, which apply the same gate from a hand-written {@code update()}.
     */
    public static boolean isAppMapAvailable() {
        return AppMapApplicationSettingsService.getInstance().isSignedInOrEntitled();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * Intentionally {@code final}: the gate must not end up inside a conditional branch of an override, e.g.
     * inside an {@code e.isFromActionToolbar()} branch, which would leave the Tools menu entry enabled.
     */
    @Override
    public final void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(isAppMapAvailable() && isAvailable(e));
    }

    /**
     * An additional, action-specific precondition, e.g. that a project or an open Navie editor is present.
     * Returning {@code true} does not make the action available: {@link #isAppMapAvailable()} is applied by
     * {@link #update} regardless.
     */
    protected boolean isAvailable(@NotNull AnActionEvent e) {
        return true;
    }
}
