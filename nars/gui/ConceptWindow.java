/*
 * ConceptWindow.java
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
//import java.beans.PropertyChangeEvent;
//import java.beans.PropertyChangeListener;

import nars.entity.Concept;

/**
 * Window displaying the content of a Concept, such as beliefs, goals, and questions
 */
public class ConceptWindow extends NarsFrame implements ActionListener, ItemListener {

    /** Control buttons */
    private Button playButton, stopButton, playInNewWindowButton, closeButton;
    private Checkbox showDerivationCheckbox;
    /** Display area */
    private TextArea text;
    /** The concept to be displayed */
    private Concept concept;
    /** Used to adjust the screen position */
    private static int instanceCount = 0;

    /**
     * Constructor
     * @param concept The concept to be displayed
     */
    public ConceptWindow(Concept concept) {
        super(concept.getKey());
        this.concept = concept;
        setBackground(MULTIPLE_WINDOW_COLOR);
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
        playButton = new Button("Play");
        gridbag.setConstraints(playButton, c);
        playButton.addActionListener(this);
        add(playButton);

        stopButton = new Button("Stop");
        gridbag.setConstraints(stopButton, c);
        stopButton.addActionListener(this);
        add(stopButton);

        showDerivationCheckbox = new Checkbox("Derivation", true);
        gridbag.setConstraints(showDerivationCheckbox, c);
        showDerivationCheckbox.addItemListener(this);
        add(showDerivationCheckbox);

        playInNewWindowButton = new Button("Play in New Window");
        gridbag.setConstraints(playInNewWindowButton, c);
        playInNewWindowButton.addActionListener(this);
        add(playInNewWindowButton);

        closeButton = new Button("Close");
        gridbag.setConstraints(closeButton, c);
        closeButton.addActionListener(this);
        add(closeButton);

        // Offset the screen location of each new instance.
        setBounds(400 + (instanceCount % 10) * 10, 60 + (instanceCount % 10) * 20, 400, 270);
        ++instanceCount;
        setVisible(true);
    }

    /**
     * Display the content of the concept
     * @param str The text to be displayed
     */
    public void post(String str) {
        text.setText(str);
    }

    /**
     * This is called when Concept removes this as its window.
     */
    public void detachFromConcept() {
        // The Play and Stop buttons and Derivation checkbox no longer do anything, so disable.
        playButton.setEnabled(false);
        stopButton.setEnabled(false);
        showDerivationCheckbox.setEnabled(false);
    }

    /**
     * Handling button click
     * @param e The ActionEvent
     */
    public void actionPerformed(ActionEvent e) {
        Object s = e.getSource();
        if (s == playButton) {
            concept.play();
        } else if (s == stopButton) {
            concept.stop();
        } else if (s == playInNewWindowButton) {
            concept.stop();
            concept.startPlay(false);
        } else if (s == closeButton) {
            close();
        }
    }

    private void close() {
        concept.stop();
        dispose();
    }

    @Override
    public void windowClosing(WindowEvent e) {
        close();
    }

    public boolean getShowDerivation() {
        return showDerivationCheckbox.getState();
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == showDerivationCheckbox) {
            // Redisplay.  displayContent will check getShowDerivation().
            post(concept.displayContent());
        }
    }
}
