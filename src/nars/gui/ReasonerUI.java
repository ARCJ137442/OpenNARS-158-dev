package nars.gui;

import javax.swing.SwingUtilities;

import nars.main.Reasoner;

/**
 * A NARS Reasoner has its memory, I/O channels, and internal clock.
 * <p>
 * Create static main window and input channel, reset memory, and manage system
 * clock.
 */
public class ReasonerUI extends Reasoner {

    /**
     * The unique main window
     */
    MainWindow mainWindow;
    /**
     * Input experience from a window
     */
    private InputWindow inputWindow;

    /**
     * Start the initial windows and memory. Called from NARS only.
     *
     * @param name The name of the reasoner
     */
    ReasonerUI(String name) {
        super(name);
        inputWindow = new InputWindow(this, name);
        mainWindow = new MainWindow(this, name);
        inputChannels.add(inputWindow);
        outputChannels.add(mainWindow);
        mainWindow.setVisible(true);
    }

    @Override
    public void tick() {
        final Reasoner reasoner = this;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                reasoner.doTick();
            }
        });
    }

    public MainWindow getMainWindow() {
        return mainWindow;
    }

    public InputWindow getInputWindow() {
        return inputWindow;
    }

    @Override
    public long updateTimer() {
        return mainWindow.updateTimer();
    }

    @Override
    public void initTimer() {
        mainWindow.initTimer();
    }

    @Override
    public void tickTimer() {
        mainWindow.tickTimer();
    }

    @Override
    public long getTimer() {
        return mainWindow.getTimer();
    }
}
