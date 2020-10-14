package sh.swisschili.chat.client;

import com.github.weisj.darklaf.theme.event.ThemePreferenceChangeEvent;
import com.github.weisj.darklaf.theme.event.ThemePreferenceListener;
import com.github.weisj.darklaf.theme.info.PreferredThemeStyle;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class ThemeListener implements ThemePreferenceListener {
    @Override
    public void themePreferenceChanged(ThemePreferenceChangeEvent e) {
        Preferences prefs = Preferences.userNodeForPackage(getClass());

        Logger logger = Logger.getLogger(ThemeListener.class.getName());

        try {
            PreferencesObject.putObject(prefs, "theme.preferredStyle", e.getPreferredThemeStyle());
            logger.info("Saved object to theme.preferredStyle");
        } catch (Exception exception) {
            logger.warning(String.format("Failed to save preferred theme: %s", exception));
        }
    }
}
