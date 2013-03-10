/*
 * InferenceWindow.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.gui;

import java.awt.*;
import java.awt.event.*;

import nars.io.*;
//import nars.term.Term;

/**
 * Window displaying inference log
 */
public class InferenceWindow extends NarsFrame implements ActionListener, ItemListener {

    /** Control buttons */
    private Button playButton, stopButton, hideButton;
    /** Display area */
    private TextArea text;
    /** String to be caught */
    private TextField watchText;
    /** Type of caught text */
    private Choice watchType;
    /** Type of caught text */
    private String watched = "";
    /** Inference recorder */
    private InferenceRecorder recorder;

    /**
     * Constructor
     * @param recorder The inference recorder
     */
    public InferenceWindow(InferenceRecorder recorder) {
        super("Inference log");
        this.recorder = recorder;

        setBackground(SINGLE_WINDOW_COLOR);
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.ipadx = 3;
        c.ipady = 3;
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.weighty = 1.0;
        text = new TextArea("");
        text.setBackground(DISPLAY_BACKGROUND_COLOR);
        text.setEditable(false);
        gridbag.setConstraints(text, c);
        add(text);

        c.weighty = 0.0;
        c.gridwidth = 1;

        watchText = new TextField(20);
        gridbag.setConstraints(watchText, c);
        add(watchText);

        watchType = new Choice();
        watchType.add("No Watch");
        watchType.add("Watch Term");
        watchType.add("Watch String");
        gridbag.setConstraints(watchType, c);
        watchType.addItemListener(this);
        add(watchType);

        playButton = new Button(ON_LABEL);
        gridbag.setConstraints(playButton, c);
        playButton.addActionListener(this);
        add(playButton);

        stopButton = new Button(OFF_LABEL);
        gridbag.setConstraints(stopButton, c);
        stopButton.addActionListener(this);
        add(stopButton);

        hideButton = new Button("Hide");
        gridbag.setConstraints(hideButton, c);
        hideButton.addActionListener(this);
        add(hideButton);

        setBounds(400, 200, 400, 400);
    }

    /**
     * Clear display
     */
    public void clear() {
        text.setText("");
    }

    /**
     * Append a new line to display
     * @param str Text to be added into display
     */
    public void append(String str) {
        text.append(str);
        if (!watched.equals("") && (str.indexOf(watched) != -1)) {
            recorder.stop();
        }
    }

    /**
     * Handling button click
     * @param e The ActionEvent
     */
    public void actionPerformed(ActionEvent e) {
        Object s = e.getSource();
        if (s == playButton) {
            recorder.play();
        } else if (s == stopButton) {
            recorder.stop();
        } else if (s == hideButton) {
            close();
        }
    }

    // to rebuild the distinction between Term and String
    public void itemStateChanged(ItemEvent event) {
        String request = watchText.getText().trim();
        if (!request.equals("")) {
            watched = request;
        }
    }

    private void close() {
        recorder.stop();
        dispose();
    }

    @Override
    public void windowClosing(WindowEvent arg0) {
        close();
    }

    /**
     * Change background color to remind the on-going file saving
     */
    public void switchBackground() {
        text.setBackground(SAVING_BACKGROUND_COLOR);
    }

    /**
     * Reset background color after file saving
     */
    public void resetBackground() {
        text.setBackground(DISPLAY_BACKGROUND_COLOR);
    }
}
