package com.hawolt.deprecated.master;

import com.hawolt.audio.AudioOutputDevice;
import com.hawolt.audio.SystemAudio;

import javax.swing.*;
import java.awt.*;

public class AudioOutputSelection extends JPanel {
    private JComboBox<AudioOutputDevice> box = new JComboBox<>();

    public AudioOutputSelection() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Select Audio Output device"));
        for (AudioOutputDevice device : SystemAudio.getSupportedOutputDeviceList()) {
            box.addItem(device);
        }
        AudioOutputDevice defaultSelection = SystemAudio.getDefaultMixer();
        for (int i = 0; i < box.getModel().getSize(); i++) {
            AudioOutputDevice device = box.getItemAt(i);
            if (device.mixer() == defaultSelection.mixer()) {
                box.setSelectedItem(device);
            }
        }
        box.addActionListener(event -> {
            AudioOutputDevice device = (AudioOutputDevice) box.getSelectedItem();
            SystemAudio.setSelectedAudioOutputDevice(device);
            SystemAudio.openSourceDataLine(SystemAudio.audioFormat);
        });
        add(box, BorderLayout.NORTH);
    }
}
