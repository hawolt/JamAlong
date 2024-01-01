package com.hawolt.deprecated.type;

import com.hawolt.deprecated.Client;
import com.hawolt.logger.Logger;
import com.hawolt.source.Audio;
import com.hawolt.source.AudioSource;
import com.hawolt.source.StreamUpdateListener;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class PartyInterface extends JPanel {
    private static final String pattern = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    private final JRadioButton host, listener;

    public PartyInterface(Client client, SoundcloudInterface soundcloudInterface) {
        setLayout(new BorderLayout(0, 5));
        setBorder(BorderFactory.createTitledBorder("Party"));
        host = new JRadioButton("Host");
        listener = new JRadioButton("Join");
        ButtonGroup group = new ButtonGroup();
        group.add(listener);
        group.add(host);
        JPanel north = new JPanel(new GridLayout(0, 2));
        north.add(listener);
        north.add(host);
        add(north, BorderLayout.NORTH);
        JTextField input = new JTextField("room-id");
        input.setEnabled(false);
        add(input, BorderLayout.CENTER);
        JButton start = new JButton("Start");
        start.setEnabled(false);
        add(start, BorderLayout.SOUTH);
        start.addActionListener(listener -> {
            this.listener.setEnabled(false);
            this.host.setEnabled(false);
            start.setEnabled(false);
            input.setEnabled(true);
            input.setEditable(false);
            if (host.isSelected()) {
                client.execute("create", object -> {
                    String roomId = object.getString("result");
                    Logger.debug("Created room:{}", roomId);
                    soundcloudInterface.getInput().setEnabled(true);
                    soundcloudInterface.getInput().setText("");
                    client.setRoomId(roomId);
                    input.setText(roomId);
                    start.setEnabled(false);
                    client.addStreamUpdateListener(new StreamUpdateListener() {
                        @Override
                        public void onAudioUpdate(Audio audio, long timestamp) {
                            client.execute("revalidate", object -> {
                                boolean success = object.getString("result").equals(client.getRoomId());
                                Logger.debug("revalidated:{}", success);
                            }, client.getRoomId(), String.valueOf(timestamp), audio.source());
                        }

                        @Override
                        public void onAudioPeekUpdate(Audio audio) {
                            client.execute("preload", object -> {
                                boolean success = object.getString("result").equals(client.getRoomId());
                                Logger.debug("preload:{}", success);
                            }, client.getRoomId(), audio.source());
                        }
                    });
                });
            } else {
                client.execute("join", object -> {
                    String[] arguments = object.getString("result").split(" ");
                    client.setRoomId(arguments[0]);
                    if (arguments.length == 1) return;
                    client.setTimestamp(Long.parseLong(arguments[1]));
                    AudioSource source = client.getAudioSource();
                    source.load(arguments[2]);
                    if (arguments.length == 3) return;
                    source.preload(arguments[3]);
                }, input.getText());
            }
        });
        host.addActionListener(listener -> {
            start.setEnabled(true);
            input.setEnabled(false);
        });
        listener.addActionListener(listener -> {
            input.setEnabled(true);
            start.setEnabled(input.getText().matches(pattern));
        });
        input.getDocument().addDocumentListener(new DocumentListener() {
            private void handle() {
                start.setEnabled(input.getText().matches(pattern));
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                handle();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handle();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                handle();
            }
        });
    }

    public boolean isHost() {
        return host.isSelected();
    }
}
