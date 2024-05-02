package nars.io;

import java.awt.FileDialog;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import nars.main_nogui.ReasonerBatch;

/**
 * To read and write experience as Task streams
 */
public class ExperienceReader implements InputChannel {

    /**
     * Reference to the reasoner
     */
    private ReasonerBatch reasoner;
    /**
     * Input experience from a file
     */
    private BufferedReader inExp;
    /**
     * Remaining working cycles before reading the next line
     */
    private int timer;

    /**
     * Default constructor
     *
     * @param reasoner Backward link to the reasoner
     */
    public ExperienceReader(ReasonerBatch reasoner) {
        this.reasoner = reasoner;
        inExp = null;
    }

    /**
     * Open an input experience file with a FileDialog
     */
    public void openLoadFile() {
        FileDialog dialog = new FileDialog((FileDialog) null, "Load experience", FileDialog.LOAD);
        dialog.setVisible(true);
        String directoryName = dialog.getDirectory();
        String fileName = dialog.getFile();
        String filePath = directoryName + fileName;
        openLoadFile(filePath);
    }

    /**
     * Open an input experience file from given file Path
     *
     * @param filePath File to be read as experience
     */
    public void openLoadFile(String filePath) {
        try {
            inExp = new BufferedReader(new FileReader(filePath));
        } catch (IOException ex) {
            System.out.println("i/o error: " + ex.getMessage());
        }
        reasoner.addInputChannel(this);
    }

    /**
     * Close an input experience file (close the reader in fact)
     */
    public void closeLoadFile() {
        try {
            inExp.close();
        } catch (IOException ex) {
            System.out.println("i/o error: " + ex.getMessage());
        }
        reasoner.removeInputChannel(this);
    }

    public void setBufferedReader(BufferedReader inExp) {
        this.inExp = inExp;
        reasoner.addInputChannel(this);
    }

    /**
     * Process the next chunk of input data;
     * TODO some duplicated code with
     * {@link nars.gui.InputWindow#nextInput()}
     *
     * @return Whether the input channel should be checked again
     */
    @Override
    public boolean nextInput() {
        if (timer > 0) {
            timer--;
            return true;
        }
        if (inExp == null) {
            return false;
        }
        String line = null;
        while (timer == 0) {
            try {
                line = inExp.readLine();
                if (line == null) {
                    inExp.close();
                    inExp = null;
                    return false;
                }
            } catch (IOException ex) {
                System.out.println("i/o error: " + ex.getMessage());
            }
            line = line.trim();
            // read NARS language or an integer
            if (line.length() > 0) {
                try {
                    timer = Integer.parseInt(line);
                    reasoner.walk(timer);
                } catch (NumberFormatException e) {
                    reasoner.textInputLine(line);
                }
            }
        }
        return true;
    }
}
