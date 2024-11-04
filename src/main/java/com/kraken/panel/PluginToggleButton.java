package com.kraken.panel;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;

import com.kraken.KrakenLoaderPlugin;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

public class PluginToggleButton extends JToggleButton {
	private static final ImageIcon ON_SWITCHER;
	private static final ImageIcon OFF_SWITCHER;

	static {
		BufferedImage onSwitcher = ImageUtil.loadImageResource(KrakenLoaderPlugin.class, "images/switcher_on.png");
        onSwitcher = ImageUtil.recolorImage(onSwitcher, new Color(106, 163, 60));

		ON_SWITCHER = new ImageIcon(onSwitcher);
		OFF_SWITCHER = new ImageIcon(ImageUtil.flipImage(
			ImageUtil.luminanceScale(
				ImageUtil.grayscaleImage(onSwitcher),
				0.61f
			),
			true,
			false
		));
	}

	private String conflictString = "";

	public PluginToggleButton() {
		super(OFF_SWITCHER);
		setSelectedIcon(ON_SWITCHER);
		SwingUtil.removeButtonDecorations(this);
		setPreferredSize(new Dimension(25, 0));
		addItemListener(l -> updateTooltip());
		updateTooltip();
	}

	private void updateTooltip() {
		setToolTipText(isSelected() ? "Disable plugin" :  "<html>Enable plugin" + conflictString);
	}

	public void setConflicts(List<String> conflicts) {
		if (conflicts != null && !conflicts.isEmpty()) {
			StringBuilder sb = new StringBuilder("<br>Plugin conflicts: ");
			for (int i = 0; i < conflicts.size() - 2; i++) {
				sb.append(conflicts.get(i));
				sb.append(", ");
			}
			if (conflicts.size() >= 2) {
				sb.append(conflicts.get(conflicts.size() - 2));
				sb.append(" and ");
			}

			sb.append(conflicts.get(conflicts.size() - 1));
			conflictString = sb.toString();
		} else {
			conflictString = "";
		}

		updateTooltip();
	}
}
