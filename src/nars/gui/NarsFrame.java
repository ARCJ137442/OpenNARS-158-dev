package nars.gui;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;

/**
 * Specify shared properties of NARS windows
 */
public abstract class NarsFrame extends JFrame implements WindowListener {

    /**
     * Color for the background of the main window
     */
    static final Color MAIN_WINDOW_COLOR = new Color(150, 150, 255);
    /**
     * Color for the background of the windows with unique instantiation
     */
    static final Color SINGLE_WINDOW_COLOR = new Color(200, 110, 245);
    /**
     * Color for the background of the windows with multiple instantiations
     */
    static final Color MULTIPLE_WINDOW_COLOR = new Color(155, 245, 155);
    /**
     * Color for the background of the text components that are read-only
     */
    static final Color DISPLAY_BACKGROUND_COLOR = new Color(230, 255, 230);
    /**
     * Color for the background of the text components that are being saved into
     * a file
     */
    static final Color SAVING_BACKGROUND_COLOR = new Color(255, 255, 205);
    /**
     * Font for NARS GUI
     */
    static final Font NarsFont = new Font("Helvetica", Font.PLAIN, 11);
    /**
     * Message for unimplemented functions
     */
    static final String UNAVAILABLE = "\n Not implemented in this version.";
    static final String ON_LABEL = "On";
    static final String OFF_LABEL = "Off";

    /**
     * Default constructor
     */
    NarsFrame() {
        super();
        addWindowListener(this);
    }

    /**
     * Constructor with title and font setting
     *
     * @param title The title displayed by the window
     */
    NarsFrame(String title) {
        super(" " + title);
        setFont(NarsFont);
        addWindowListener(this);
    }

    @Override
    public void windowActivated(WindowEvent arg0) {
    }

    @Override
    public void windowClosed(WindowEvent arg0) {
    }

    @Override
    public void windowClosing(WindowEvent arg0) {
    }

    @Override
    public void windowDeactivated(WindowEvent arg0) {
    }

    @Override
    public void windowDeiconified(WindowEvent arg0) {
    }

    @Override
    public void windowIconified(WindowEvent arg0) {
    }

    @Override
    public void windowOpened(WindowEvent arg0) {
    }
}
