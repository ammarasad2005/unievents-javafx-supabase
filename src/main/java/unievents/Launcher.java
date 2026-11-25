package unievents;

import unievents.ui.UniEventsApp;

/**
 * Non-modular launcher so UniEvents starts cleanly both via `mvn javafx:run`
 * and directly from an IDE Run configuration without manual --module-path flags.
 */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        UniEventsApp.main(args);
    }
}
