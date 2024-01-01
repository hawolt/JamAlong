package com.hawolt.deprecated.master;

import com.hawolt.audio.SystemAudio;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class AudioGainControl extends JPanel {
    private JSlider slider;

    public AudioGainControl() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Control Audio Gain"));
        slider = new JSlider(0, -80, 6, 0);
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                SystemAudio.setGain((float) slider.getValue());
            }
        });
        SystemAudio.setGain((float) slider.getValue());
        add(slider);
    }
}
