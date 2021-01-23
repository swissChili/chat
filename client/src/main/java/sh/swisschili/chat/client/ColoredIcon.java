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

import com.github.weisj.darklaf.icons.IconLoader;
import jiconfont.IconCode;
import jiconfont.swing.IconFontSwing;

import javax.swing.*;
import java.awt.*;

public class ColoredIcon {
    public static Icon buildIcon(IconCode code, int size) {
        return IconLoader.get().createUIAwareIcon(
                buildIcon(code, size, Color.DARK_GRAY), buildIcon(code, size, Color.LIGHT_GRAY));
    }

    public static Icon buildIcon(IconCode code, int size, Color color) {
        return IconFontSwing.buildIcon(code, size, color);
    }
}
