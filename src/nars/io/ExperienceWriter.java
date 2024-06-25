package nars.io;

import java.awt.FileDialog;
import java.io.*;
import java.util.*;

import nars.control.Reasoner;

/**
 * To read and write experience as Task streams
 */
public class ExperienceWriter implements OutputChannel {

    private Reasoner reasoner;
    /**
     * Input experience from a file
     */
    private PrintWriter outExp;

    /**
     * Default constructor
     *
     * @param reasoner
     */
    public ExperienceWriter(Reasoner reasoner) {
        this.reasoner = reasoner;
    }

    public ExperienceWriter(Reasoner reasoner, PrintWriter outExp) {
        this(reasoner);
        this.outExp = outExp;
    }

    /**
     * Open an output experience file
     */
    public void openSaveFile() {
        FileDialog dialog = new FileDialog((FileDialog) null, "Save experience", FileDialog.SAVE);
        dialog.setVisible(true);
        String directoryName = dialog.getDirectory();
        String fileName = dialog.getFile();
        try {
            outExp = new PrintWriter(new FileWriter(directoryName + fileName));
        } catch (IOException ex) {
            System.out.println("i/o error: " + ex.getMessage());
        }
        reasoner.addOutputChannel(this);
    }

    /**
     * Close an output experience file
     */
    public void closeSaveFile() {
        outExp.close();
        reasoner.removeOutputChannel(this);
    }

    /**
     * Process the next chunk of output data
     *
     * @param lines The text to be displayed
     */
    @Override
    public void nextOutput(ArrayList<String> lines) {
        if (outExp != null) {
            for (Object line : lines) {
                outExp.println(line.toString());
            }
        }
    }

    @Override
    public void tickTimer() {
    }
}
