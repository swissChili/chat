package sh.swisschili.chat.client;

import com.github.weisj.darklaf.theme.*;

public class ThemeFactory {
    public static Theme byName(String name) {
        return switch (name) {
            case "Darcula" -> new DarculaTheme();
            case "Solarized Light" -> new SolarizedLightTheme();
            case "Solarized Dark" -> new SolarizedDarkTheme();
            case "One Dark" -> new OneDarkTheme();
            case "High Contrast Light" -> new HighContrastLightTheme();
            case "High Contrast Dark" -> new HighContrastDarkTheme();
            default -> new IntelliJTheme();
        };
    }
}
