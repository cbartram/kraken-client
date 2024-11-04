package com.kraken.panel;


import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import lombok.Getter;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.ModifierlessKeybind;
import net.runelite.client.ui.FontManager;

@Getter
public class HotkeyButton extends JButton {

	private Keybind value;

	public HotkeyButton(Keybind value, boolean modifierless) {
		// Disable focus traversal keys such as tab to allow tab key to be bound
		setFocusTraversalKeysEnabled(false);
		setFont(FontManager.getDefaultFont().deriveFont(12.f));
		setValue(value);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				// Mouse buttons other than button1 don't give focus
				if (e.getButton() == MouseEvent.BUTTON1) {
					// We have to use a mouse adapter instead of an action listener so the press action key (space) can be bound
					setValue(Keybind.NOT_SET);
				}
			}
		});

		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (modifierless) {
					setValue(new ModifierlessKeybind(e));
				} else {
					setValue(new Keybind(e));
				}
			}
		});
	}

	public void setValue(Keybind value) {
		if (value == null) {
			value = Keybind.NOT_SET;
		}

		this.value = value;
		setText(value.toString());
	}
}
