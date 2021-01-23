/*
Decentralized chat software
Copyright (C) 2021  swissChili

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
