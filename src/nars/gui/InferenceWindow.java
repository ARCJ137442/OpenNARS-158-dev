package nars.gui;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;

import nars.io.*;
//import nars.term.Term;

/**
 * JWindow displaying inference log
 */
public class InferenceWindow extends NarsFrame implements ActionListener, ItemListener {

    /**
     * Control buttons
     */
    private JButton playButton, stopButton, hideButton;
    /**
     * Display area
     */
    private JTextArea text;
    /**
     * String to be caught
     */
    private JTextField watchText;
    /**
     * Type of caught text
     */
    private JComboBox<String> watchType;
    /**
     * Type of caught text
     */
    private String watched = "";
    /**
     * Inference recorder
     */
    private IInferenceRecorder recorder = new NullInferenceRecorder();

    /**
     * Constructor
     *
     * @param recorder The inference recorder
     */
    public InferenceWindow(IInferenceRecorder recorder) {
        super("Inference log");
        this.recorder = recorder;

        getContentPane().setBackground(SINGLE_WINDOW_COLOR);
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
        text = new JTextArea("");
        text.setBackground(DISPLAY_BACKGROUND_COLOR);
        text.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(text);
        gridbag.setConstraints(scrollPane, c);
        add(scrollPane);
        // gridbag.setConstraints(text, c);
        // add(text);

        c.weighty = 0.0;
        c.gridwidth = 1;

        watchText = new JTextField(20);
        gridbag.setConstraints(watchText, c);
        add(watchText);

        watchType = new JComboBox<String>(new String[] { "No Watch",
                "Watch Term",
                "Watch String" });
        gridbag.setConstraints(watchType, c);
        watchType.addItemListener(this);
        add(watchType);

        playButton = new JButton(ON_LABEL);
        gridbag.setConstraints(playButton, c);
        playButton.addActionListener(this);
        add(playButton);

        stopButton = new JButton(OFF_LABEL);
        gridbag.setConstraints(stopButton, c);
        stopButton.addActionListener(this);
        add(stopButton);

        hideButton = new JButton("Hide");
        gridbag.setConstraints(hideButton, c);
        hideButton.addActionListener(this);
        add(hideButton);

        setBounds(600, 200, 600, 600);
    }

    /**
     * Clear display
     */
    public void clear() {
        text.setText("");
    }

    /**
     * Append a new line to display
     *
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
     *
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

    class NullInferenceRecorder implements IInferenceRecorder {

        @Override
        public void init() {
        }

        @Override
        public void show() {
        }

        @Override
        public void play() {
        }

        @Override
        public void stop() {
        }

        @Override
        public void append(String s) {
        }

        @Override
        public void openLogFile() {
        }

        @Override
        public void closeLogFile() {
        }

        @Override
        public boolean isLogging() {
            return false;
        }
    }
}
