package nars.gui;

import java.awt.FileDialog;
import java.io.*;

import nars.io.IInferenceRecorder;

/**
 * Inference log, which record input/output of each inference step
 * interface with 1 implementation: GUI ( batch not implemented )
 */
public class InferenceRecorder implements IInferenceRecorder {

    /** the display window */
    private InferenceWindow window = new InferenceWindow(this);
    /** whether to display */
    private boolean isReporting = false;
    /** the log file */
    private PrintWriter logFile = null;

    @Override
    public void init() {
        window.clear();
    }

    @Override
    public void show() {
        window.setVisible(true);
    }

    @Override
    public void play() {
        isReporting = true;
    }

    @Override
    public void stop() {
        isReporting = false;
    }

    @Override
    public void append(String s) {
        if (isReporting) {
            window.append(s);
        }
        if (logFile != null) {
            logFile.println(s);
        }
    }

    @Override
    public void openLogFile() {
        FileDialog dialog = new FileDialog((FileDialog) null, "Inference Log", FileDialog.SAVE);
        dialog.setVisible(true);
        String directoryName = dialog.getDirectory();
        String fileName = dialog.getFile();
        try {
            logFile = new PrintWriter(new FileWriter(directoryName + fileName));
        } catch (IOException ex) {
            System.out.println("i/o error: " + ex.getMessage());
        }
        window.switchBackground();
        window.setVisible(true);
    }

    @Override
    public void closeLogFile() {
        logFile.close();
        logFile = null;
        window.resetBackground();
    }

    @Override
    public boolean isLogging() {
        return (logFile != null);
    }
}
