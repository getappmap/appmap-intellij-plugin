package appland.actions;

import appland.AppMapBaseTest;
import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapProjectSettingsService;
import appland.utils.DataContexts;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.testFramework.TestActionEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.List;

/**
 * The unauthenticated state must be quiescent: the whole AppMap feature surface of {@code Tools > AppMap}
 * is unavailable while signed out. Only the routes back into a usable state remain enabled.
 */
public class AppMapActionAuthenticationGateTest extends AppMapBaseTest {
    /**
     * Feature actions, which must be unavailable while signed out.
     */
    private static final List<String> GATED_ACTION_IDS = List.of(
            "appmap.openNavie",
            "appmap.quickReview",
            "appmap.navie.openThread",
            "appmap.navie.chooseAndPinContextFile",
            "appmap.generateOpenAPI",
            "appmap.navie.openAIKey",
            "startAppMapRemoteRecording",
            "stopAppMapRemoteRecording");

    /**
     * Subset of {@link #GATED_ACTION_IDS}, which must become available after signing in.
     * {@code appmap.navie.chooseAndPinContextFile} is excluded, because it also requires an open Navie editor.
     */
    private static final List<String> GATED_ACTION_IDS_ENABLED_WHEN_SIGNED_IN = List.of(
            "appmap.openNavie",
            "appmap.quickReview",
            "appmap.navie.openThread",
            "appmap.generateOpenAPI",
            "appmap.navie.openAIKey",
            "startAppMapRemoteRecording",
            "stopAppMapRemoteRecording");

    /**
     * Actions, which must stay available while signed out, because they're the routes back into a usable state.
     */
    private static final List<String> ALWAYS_ENABLED_ACTION_IDS = List.of(
            "appMapLogin",
            "appMapLogout",
            "appMapLoginByKey",
            "appmap.setConfigurationUrl",
            "appmap.pluginStatus");

    @Test
    public void featureActionsDisabledWhileSignedOut() {
        signOut();

        for (var actionId : GATED_ACTION_IDS) {
            assertFalse("Action must be disabled while signed out: " + actionId,
                    updateInToolsMenu(actionId).isEnabled());
        }
    }

    @Test
    public void featureActionsEnabledWhenSignedIn() {
        signIn();

        for (var actionId : GATED_ACTION_IDS_ENABLED_WHEN_SIGNED_IN) {
            assertTrue("Action must be enabled when signed in: " + actionId,
                    updateInToolsMenu(actionId).isEnabled());
        }
    }

    @Test
    public void authenticationActionsRemainEnabledWhileSignedOut() {
        signOut();

        for (var actionId : ALWAYS_ENABLED_ACTION_IDS) {
            assertTrue("Action must remain enabled while signed out: " + actionId,
                    updateInToolsMenu(actionId).isEnabled());
        }
    }

    @Test
    public void activeRecordingRemainsStoppableWhileSignedOut() {
        signOut();
        AppMapProjectSettingsService.getState(getProject()).setActiveRecordingURL("http://localhost:3000");

        assertTrue("An in-flight recording must stay stoppable while signed out",
                updateInToolsMenu("stopAppMapRemoteRecording").isEnabled());
        assertFalse("Starting a new recording must be unavailable while signed out",
                updateInToolsMenu("startAppMapRemoteRecording").isEnabled());
    }

    private void signIn() {
        AppMapApplicationSettingsService.getInstance().setApiKey("test-api-key");
    }

    private void signOut() {
        AppMapApplicationSettingsService.getInstance().setApiKey(null);
    }

    /**
     * Updates the action as if it was shown in the {@code Tools > AppMap} menu, i.e. not in an action toolbar.
     */
    private @NotNull Presentation updateInToolsMenu(@NotNull String actionId) {
        var action = ActionManager.getInstance().getAction(actionId);
        assertNotNull("Action must be registered: " + actionId, action);

        return update(action, ActionPlaces.MAIN_MENU);
    }

    private @NotNull Presentation update(@NotNull AnAction action, @NotNull String place) {
        var event = TestActionEvent.createFromAnAction(action, null, place, createProjectContext());
        ActionUtil.performDumbAwareUpdate(action, event, false);
        return event.getPresentation();
    }

    private @NotNull DataContext createProjectContext() {
        return DataContexts.createCustomContext(dataId -> {
            return PlatformDataKeys.PROJECT.is(dataId) ? getProject() : null;
        });
    }
}
