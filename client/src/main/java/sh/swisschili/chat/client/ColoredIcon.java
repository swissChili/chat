package sh.swisschili.chat.client;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.info.ColorToneRule;
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
