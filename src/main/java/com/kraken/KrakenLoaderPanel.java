package com.kraken;

import com.google.inject.Inject;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class KrakenLoaderPanel extends PluginPanel {

    private final JPanel display = new JPanel();
    private final MaterialTabGroup tabGroup = new MaterialTabGroup(display);

    @Inject
    private KrakenLoaderPanel() {
        super(false);

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        tabGroup.setBorder(new EmptyBorder(5, 0, 0, 0));

        add(tabGroup, BorderLayout.NORTH);
        add(display, BorderLayout.CENTER);
    }

}
