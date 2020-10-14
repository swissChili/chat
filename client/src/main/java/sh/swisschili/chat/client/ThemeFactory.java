package sh.swisschili.chat.client;

import com.github.weisj.darklaf.theme.*;

public class ThemeFactory {
    public static Theme byName(String name) {
        switch (name) {
            case "Darcula":
                return new DarculaTheme();
            case "Solarized Light":
                return new SolarizedLightTheme();
            case "Solarized Dark":
                return new SolarizedDarkTheme();
            case "One Dark":
                return new OneDarkTheme();
            case "High Contrast Light":
                return new HighContrastLightTheme();
            case "High Contrast Dark":
                return new HighContrastDarkTheme();
            default:
                return new IntelliJTheme();
        }
    }
}
