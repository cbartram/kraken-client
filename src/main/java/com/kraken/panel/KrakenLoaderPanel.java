package com.kraken.panel;

import com.google.inject.Inject;
import net.runelite.client.plugins.config.PluginSearch;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class KrakenLoaderPanel extends PluginPanel {

    private final JPanel display = new JPanel();
    private final MaterialTabGroup tabGroup = new MaterialTabGroup(display);
    private final IconTextField searchBar;
    private final JScrollPane scrollPane;
    private final FixedWidthPanel mainPanel;

    @Inject
    public KrakenLoaderPanel() {
        super(false);

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        tabGroup.setBorder(new EmptyBorder(5, 0, 0, 0));

        searchBar = new IconTextField();
        searchBar.setIcon(IconTextField.Icon.SEARCH);
        searchBar.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
        searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
        searchBar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onSearchBarChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onSearchBarChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onSearchBarChanged();
            }
        });


        JPanel topPanel = new JPanel();
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        topPanel.setLayout(new BorderLayout(0, BORDER_OFFSET));
        topPanel.add(searchBar, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        mainPanel = new FixedWidthPanel();
        mainPanel.setBorder(new EmptyBorder(8, 10, 10, 10));
        mainPanel.setLayout(new DynamicGridLayout(0, 1, 0, 5));
        mainPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel northPanel = new FixedWidthPanel();
        northPanel.setLayout(new BorderLayout());
        northPanel.add(mainPanel, BorderLayout.NORTH);

        scrollPane = new JScrollPane(northPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void onSearchBarChanged() {
        final String text = searchBar.getText();
//        pluginList.forEach(mainPanel::remove);
//        PluginSearch.search(pluginList, text).forEach(mainPanel::add);
        revalidate();
    }

}
