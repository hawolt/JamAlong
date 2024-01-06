package com.hawolt.chromium;

import com.hawolt.logger.Logger;
import me.friwi.jcefmaven.*;
import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefContextMenuParams;
import org.cef.callback.CefMenuModel;
import org.cef.handler.CefContextMenuHandler;
import org.cef.handler.CefFocusHandlerAdapter;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Wrapper class for the JCEF framework to provide a Chromium Window.
 */
public class Chromium {
    private final Path base = Paths.get(System.getProperty("user.home")).resolve(".hawolt").resolve(".chrome");

    private final CefApp cefApp_;
    private final CefClient client_;
    private final CefBrowser browser_;
    private final Component browserUI_;
    private boolean browserFocus = true;

    public Chromium(String startURL, Path path, boolean useOSR, IProgressHandler handler) throws UnsupportedPlatformException, CefInitializationException, IOException, InterruptedException {
        this(path, handler, startURL, useOSR, false);
    }

    private Chromium(Path path, IProgressHandler handler, String startURL, boolean useOSR, boolean isTransparent) throws UnsupportedPlatformException, CefInitializationException, IOException, InterruptedException {
        CefAppBuilder builder = new CefAppBuilder();
        Logger.info("Chromium base: {}", base.resolve("chrome.log"));
        Files.createDirectories(base);
        builder.getCefSettings().windowless_rendering_enabled = useOSR;
        builder.getCefSettings().log_severity = CefSettings.LogSeverity.LOGSEVERITY_ERROR;
        builder.getCefSettings().log_file = Paths.get(System.getProperty("user.home")).resolve(".jamalong").resolve("chrome.log").toString();
        builder.addJcefArgs("--disable-gpu");
        builder.setProgressHandler(handler);
        Files.createDirectories(path);
        builder.setInstallDir(path.toFile());
        builder.setAppHandler(new MavenCefAppHandlerAdapter() {
            @Override
            public void stateHasChanged(CefAppState state) {
                if (state == CefAppState.TERMINATED) System.exit(0);
            }
        });
        cefApp_ = builder.build();
        client_ = cefApp_.createClient();
        client_.addContextMenuHandler(new CefContextMenuHandler() {
            @Override
            public void onBeforeContextMenu(CefBrowser cefBrowser, CefFrame cefFrame, CefContextMenuParams cefContextMenuParams, CefMenuModel cefMenuModel) {
                cefMenuModel.clear();
            }

            @Override
            public boolean onContextMenuCommand(CefBrowser cefBrowser, CefFrame cefFrame, CefContextMenuParams cefContextMenuParams, int i, int i1) {
                return false;
            }

            @Override
            public void onContextMenuDismissed(CefBrowser cefBrowser, CefFrame cefFrame) {

            }
        });
        CefMessageRouter msgRouter = CefMessageRouter.create();
        client_.addMessageRouter(msgRouter);
        browser_ = client_.createBrowser(startURL, useOSR, isTransparent);
        browserUI_ = browser_.getUIComponent();
        client_.addFocusHandler(new CefFocusHandlerAdapter() {
            @Override
            public void onGotFocus(CefBrowser browser) {
                if (browserFocus) return;
                browserFocus = true;
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                browser.setFocus(true);
            }

            @Override
            public void onTakeFocus(CefBrowser browser, boolean next) {
                browserFocus = false;
            }
        });
    }

    public CefApp getCefApp() {
        return cefApp_;
    }

    public CefClient getClient() {
        return client_;
    }

    public CefBrowser getBrowser() {
        return browser_;
    }

    public boolean isBrowserFocus() {
        return browserFocus;
    }

    public Component getBrowserUI() {
        return browserUI_;
    }
}
