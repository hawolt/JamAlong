package com.hawolt.deprecated.master;

import javax.swing.*;
import java.awt.*;

public class AudioInterface extends JPanel {
    private AudioOutputSelection audioOutputSelection;
    private AudioGainControl audioGainControl;
    private final JPanel main = new JPanel();

    public AudioInterface() {
        setLayout(new BorderLayout());
        BoxLayout layout = new BoxLayout(main, BoxLayout.Y_AXIS);
        main.setLayout(layout);
        //  main.add(audioOutputSelection = new AudioOutputSelection());
        main.add(audioGainControl = new AudioGainControl());
        add(main, BorderLayout.CENTER);
    }
}
