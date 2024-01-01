package com.hawolt.deprecated.type;

import com.hawolt.deprecated.Client;

import javax.swing.*;
import java.awt.*;

public class SoundcloudInterface extends JPanel {
    private final JTextField input;
    public SoundcloudInterface(Client client) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Load a SoundCloud link"));
        this. input = new JTextField("only the host can use this feature");
        add(input, BorderLayout.CENTER);
        input.setEnabled(false);
        input.addActionListener(listener -> {
            client.getAudioSource().load(input.getText());
            input.setText("");
        });
    }

    public JTextField getInput() {
        return input;
    }
}
