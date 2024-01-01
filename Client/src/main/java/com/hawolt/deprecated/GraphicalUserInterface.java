package com.hawolt.deprecated;

import com.hawolt.deprecated.master.AudioInterface;
import com.hawolt.deprecated.type.PartyInterface;
import com.hawolt.deprecated.type.SoundcloudInterface;
import com.hawolt.deprecated.util.NoSelectionModel;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;

public class GraphicalUserInterface {
    private static JList<String> list;
    private static DefaultListModel<String> model;
    private static PartyInterface partyInterface;
    private static JButton button;
    private static JTextField username;

    public static JFrame create(Client client) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Jamalong");
        Container container = frame.getContentPane();
        container.setPreferredSize(new Dimension(600, 240));
        container.setLayout(new BorderLayout());
        JPanel main = new JPanel();
        main.setLayout(new BorderLayout());
        AudioInterface audioInterface = new AudioInterface();
        main.add(audioInterface, BorderLayout.NORTH);
        SoundcloudInterface soundcloudInterface = new SoundcloudInterface(client);
        main.add(soundcloudInterface, BorderLayout.SOUTH);
        partyInterface = new PartyInterface(client, soundcloudInterface);
        main.add(partyInterface, BorderLayout.CENTER);
        container.add(main, BorderLayout.CENTER);
        JPanel side = new JPanel();
        side.setLayout(new BorderLayout());
        side.setBorder(BorderFactory.createTitledBorder("Party"));
        model = new DefaultListModel<>();
        list = new JList<>(model);
        list.setBackground(UIManager.getColor("Panel.background"));
        list.setSelectionModel(new NoSelectionModel());
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(200, 0));
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        side.add(scrollPane, BorderLayout.CENTER);
        JPanel east = new JPanel();
        east.setLayout(new BorderLayout());
        JPanel name = new JPanel();
        name.setBorder(BorderFactory.createTitledBorder("Username"));
        name.setLayout(new GridLayout(0, 1, 0, 5));
        username = new JTextField("anon");
        name.add(username);
        button = new JButton("Update Name");
        name.add(button);
        east.add(name, BorderLayout.NORTH);
        east.add(side, BorderLayout.CENTER);
        container.add(east, BorderLayout.EAST);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.pack();
        frame.setVisible(true);
        return frame;
    }

    public static JButton getNameButton() {
        return button;
    }

    public static JTextField getUsernameField() {
        return username;
    }

    public static boolean isHost() {
        return partyInterface.isHost();
    }

    public static JList<String> getList() {
        return list;
    }

    public static DefaultListModel<String> getModel() {
        return model;
    }

    public static void setUIFont(FontUIResource font) {
        java.util.Enumeration keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, font);
            }
        }
    }
}
