package sh.swisschili.chat.client;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.info.ColorToneRule;
import jiconfont.IconCode;
import jiconfont.swing.IconFontSwing;

import javax.swing.*;
import java.awt.*;

public class ColoredIcon {
    public static Icon buildIcon(IconCode code, int size) {
        // Requires re launching the app on theme update to change icons. Is there a better way to do this?
        ColorToneRule tone = LafManager.getTheme().getColorToneRule();
        Color color;
        if (tone.equals(ColorToneRule.LIGHT))
            color = Color.DARK_GRAY;
        else
            color = Color.LIGHT_GRAY;
        return IconFontSwing.buildIcon(code, size, color);
    }
}
