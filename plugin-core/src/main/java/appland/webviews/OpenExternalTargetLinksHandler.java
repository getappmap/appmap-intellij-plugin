package appland.webviews;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandlerAdapter;

/**
 * Links with 'target="_blank"' are not passed by JCEF to our external link handler.
 * This handler takes care of links with a target.
 */
public final class OpenExternalTargetLinksHandler extends CefLifeSpanHandlerAdapter {
    @Override
    public boolean onBeforePopup(CefBrowser browser, CefFrame frame, String target_url, String target_frame_name) {
        return OpenExternalLinksHandler.openExternalLink(target_url);
    }
}
