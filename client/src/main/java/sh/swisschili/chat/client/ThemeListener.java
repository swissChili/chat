package sh.swisschili.chat.client;

import com.github.weisj.darklaf.theme.event.ThemePreferenceChangeEvent;
import com.github.weisj.darklaf.theme.event.ThemePreferenceListener;
import com.github.weisj.darklaf.theme.info.PreferredThemeStyle;

import java.util.prefs.Preferences;

public class ThemeListener implements ThemePreferenceListener {
    @Override
    public void themePreferenceChanged(ThemePreferenceChangeEvent e) {
        // Preferences prefs = Preferences.userNodeForPackage(getClass());
        System.out.println(e.getPreferredThemeStyle());
    }
}
