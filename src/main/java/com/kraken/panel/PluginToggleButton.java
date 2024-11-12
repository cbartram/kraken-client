package com.kraken.panel;

import com.kraken.KrakenLoaderPlugin;
import lombok.Getter;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class PluginToggleButton extends JToggleButton {
	private static final ImageIcon ON_SWITCHER;
	private static final ImageIcon OFF_SWITCHER;
	private static final ImageIcon INVALID_SWITCHER; // When the plugin license is invalid

	@Getter
	private boolean valid;

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
		INVALID_SWITCHER = new ImageIcon(ImageUtil.recolorImage(onSwitcher, new Color(255, 59, 62)));
	}

	private String conflictString = "";

	public PluginToggleButton(boolean valid) {
		super(OFF_SWITCHER);
		this.valid = valid;
		setSelectedIcon(this.valid ? ON_SWITCHER : INVALID_SWITCHER);
		SwingUtil.removeButtonDecorations(this);
		setPreferredSize(new Dimension(25, 0));
		addItemListener(l -> updateTooltip());
		updateTooltip();
	}

	public void setValid(boolean valid) {
		if(!valid) {
			setSelectedIcon(INVALID_SWITCHER);
			return;
		}
		setSelectedIcon(ON_SWITCHER);
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
