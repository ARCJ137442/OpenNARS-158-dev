package nars.main;

import nars.io.ExperienceReader;
import nars.main_nogui.CommandLineParameters;
import nars.main_nogui.NARSBatch;
import nars.main_nogui.ReasonerBatch;

/**
 * The main class of the open-nars project.
 * <p>
 * Manage the internal working thread. Communicate with Reasoner only.
 */
public class NARS implements Runnable {

    /**
     * The information about the version and date of the project.
     */
    public static final String INFO = "Open-NARS\tVersion 1.5.8\tFebruary 2019 \n";
    /**
     * The project web sites.
     */
    public static final String WEBSITE = " Open-NARS website:  http://code.google.com/p/open-nars/ \n"
            + "      NARS website:  http://sites.google.com/site/narswang/";
    /**
     * The internal working thread of the system.
     */
    Thread narsThread = null;
    /**
     * The reasoner
     */
    ReasonerBatch reasoner;

    /**
     * The entry point of the standalone application.
     * <p>
     * Create an instance of the class
     *
     * @param args optional argument used : one input file, possibly followed by
     *             --silence <integer>
     */
    public static void main(String args[]) {
        NARSBatch.setStandAlone(true);
        NARS nars = new NARS();
        nars.init(args);
        nars.start();
    }

    /**
     * TODO multiple files
     *
     * @param args Input file
     */
    public void init(String[] args) {
        reasoner = new Reasoner("NARS Reasoner");
        if ((args.length > 0) && CommandLineParameters.isReallyFile(args[0])) {
            ExperienceReader experienceReader = new ExperienceReader(reasoner);
            experienceReader.openLoadFile(args[0]);
        }
        CommandLineParameters.decode(args, reasoner);
    }

    /**
     * Start the thread if necessary, called when the page containing the applet
     * first appears on the screen.
     */
    public void start() {
        if (narsThread == null) {
            narsThread = new Thread(this, "Inference");
            narsThread.start();
        }
    }

    /* Implementing the Runnable Interface */
    /**
     * Repeatedly execute NARS working cycle. This method is called when the
     * Runnable's thread is started.
     */
    @Override
    public void run() {
        Thread thisThread = Thread.currentThread();
        while (narsThread == thisThread) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
            try {
                // NOTE: try/catch not necessary for input errors , but may be useful for other
                // troubles
                reasoner.tick();
            } catch (Exception e) {
            }
        }
    }
}
