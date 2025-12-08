package appland.webviews.webserver;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.jcef.JBCefScrollbarsHelper;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Locale;

import javax.swing.UIManager;

class IdeStyleRequest {
    // see https://github.com/getappmap/vscode-appland/blob/bfd83ad8c848d31257ab004688eb847feecbcf32/web/static/styles/navie-integration.css#L1-L0
    static @NotNull String createIdeStyles() {
        var scheme = EditorColorsManager.getInstance().getGlobalScheme();

        var background = UIUtil.getPanelBackground();

        // used for glow and border around Navie's main text input box
        var highlight = UIUtil.getFocusedBorderColor();
        var highlightLight = highlight.brighter();
        var highlightDark = highlight.darker().darker();

        // text color
        var foreground = UIUtil.getLabelForeground();
        var foregroundSecondary = UIUtil.getLabelDisabledForeground();
        var foregroundLight = foreground.brighter();
        var foregroundDark = foreground.darker();

        var linkColor = JBUI.CurrentTheme.Link.Foreground.ENABLED;
        var linkColorHover = JBUI.CurrentTheme.Link.Foreground.HOVERED;

        var error = JBUI.CurrentTheme.Focus.errorColor(true);
        var warning = JBUI.CurrentTheme.Focus.warningColor(true);
        var success = JBColor.GREEN.brighter();

        var fontFamily = UISettings.getInstance().getFontFace();
        var fontSize = UISettings.getInstance().getFontSize() + "px";
        var fontWeight = "normal";

        var fontCodeFamily = scheme.getEditorFontName();
        var fontCodeSize = String.format("%.2fpx", scheme.getEditorFontSize2D());

        var buttonBackground = JBUI.CurrentTheme.Button.defaultButtonColorStart();
        var buttonBackgroundHover = JBUI.CurrentTheme.Button.focusBorderColor(true);
        var buttonForeground = foregroundContrastColor(buttonBackground);

        var inputBackground = UIUtil.getTextFieldBackground();
        var inputForeground = UIUtil.getTextFieldForeground();

        // AppMap is using "1px solid var(--appmap-color-border,rgba(255,255,255,.1))" at some places,
        // we're applying the same alpha value to make it fit.
        var border = ColorUtil.withAlpha(JBUI.CurrentTheme.List.buttonSeparatorColor(), 0.1);
        var selection = JBUI.CurrentTheme.List.Selection.foreground(true);

        var colorTileBackground = UIManager.getColor("EditorTabs.background");
        var colorTileShadow = colorTileBackground.darker();

        // apply the global scale factor to the webview
        var ideStyles = "html { transform: scale(" + String.format(Locale.ENGLISH, "%.3f", JBUIScale.scale(1.0f)) + "); }";

        // CSS colors based on the current theme
        var ideThemeColors = ":root {\n" +
                "  --appmap-color-background: " + toCssColor(background) + ";\n" +
                "  --appmap-color-highlight: " + toCssColor(highlight) + ";\n" +
                "  --appmap-color-highlight-light: " + toCssColor(highlightLight) + ";\n" +
                "  --appmap-color-highlight-dark: " + toCssColor(highlightDark) + ";\n" +
                "  --appmap-color-input-bg: " + toCssColor(inputBackground) + ";\n" +
                "  --appmap-color-input-fg: " + toCssColor(inputForeground) + ";\n" +
                "  --appmap-color-button-fg: " + toCssColor(buttonForeground) + ";\n" +
                "  --appmap-color-button-bg: " + toCssColor(buttonBackground) + ";\n" +
                "  --appmap-color-button-bg-hover: " + toCssColor(buttonBackgroundHover) + ";\n" +
                "  --appmap-color-selection: " + toCssColor(selection) + ";\n" +
                "  --appmap-color-border: " + toCssColor(border) + ";\n" +
                "  --appmap-color-foreground: " + toCssColor(foreground) + ";\n" +
                "  --appmap-color-foreground-secondary: " + toCssColor(foregroundSecondary) + ";\n" +
                "  --appmap-color-foreground-light: " + toCssColor(foregroundLight) + ";\n" +
                "  --appmap-color-foreground-dark: " + toCssColor(foregroundDark) + ";\n" +
                "  --appmap-font-family: " + fontFamily + ";\n" +
                "  --appmap-font-size: " + fontSize + ";\n" +
                "  --appmap-font-weight: " + fontWeight + ";\n" +
                "  --appmap-font-code-family: " + fontCodeFamily + ";\n" + // custom property
                "  --appmap-font-code-size: " + fontCodeSize + ";\n" + // custom property
                "  --appmap-color-success: " + toCssColor(success) + ";\n" +
                "  --appmap-color-error: " + toCssColor(error) + ";\n" +
                "  --appmap-color-warning: " + toCssColor(warning) + ";\n" +
                "  --appmap-color-link: " + toCssColor(linkColor) + ";\n" +
                "  --appmap-color-link-hover: " + toCssColor(linkColorHover) + ";\n" +
                "  --appmap-color-tile-background: " + toCssColor(colorTileBackground) + ";\n" +
                "  --appmap-color-tile-shadow: " + toCssColor(colorTileShadow) + ";\n" +
                "}\n";

        return JBCefScrollbarsHelper.buildScrollbarsStyle() + "\n" + ideThemeColors + "\n" + ideStyles;
    }

    private static @NotNull String toCssColor(@NotNull Color color) {
        return String.format(Locale.ENGLISH,
                "rgba(%d, %d, %d, %.3f)",
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                (double) color.getAlpha() / 255.0);
    }

    private static @NotNull Color foregroundContrastColor(@NotNull Color background) {
        return contrastColor(background, JBColor.WHITE, JBColor.BLACK);
    }

    private static @NotNull Color contrastColor(@NotNull Color background, @NotNull Color first, @NotNull Color second) {
        if (ColorUtil.calculateContrastRatio(background, first) > ColorUtil.calculateContrastRatio(background, second)) {
            return first;
        }
        return second;
    }
}
