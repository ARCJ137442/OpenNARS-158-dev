/*
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
package nars.main_nogui;

import java.io.PrintStream;
import java.io.PrintWriter;

import nars.io.ExperienceReader;
import nars.io.ExperienceWriter;
import nars.main_nogui.ReasonerBatch;;

/**
 * The main class of the project.
 * <p>
 * Define an application with batch functionality;
 * TODO duplicated code with {@link nars.main.NARS}
 * TODO still instantiates windows
 * <p>
 * Manage the internal working thread. Communicate with Reasoner only.
 */
public class NARSBatch {
    /** The reasoner */
    ReasonerBatch reasoner;
	private boolean logging;
	private PrintStream out = System.out;
	private boolean dumpLastState = true;

    /** The entry point of the standalone application.
     * <p>
     * Create an instance of the class, then run the {@link #init(String[])} and {@link #run()} methods.
     * @param args optional argument used : one input file
     */
    public static void main(String args[]) {
        NARSBatch nars = new NARSBatch();
        nars.runInference(args);
        if(nars.dumpLastState) System.out.println( "==== Dump Last State ====\n"
        		+ nars.reasoner.toString() );
    }

    /** non-static equivalent to {@link #main(String[])} */
    public void runInference(String args[]) {
        init(args);
        run();
	}

	public void init(String[] args) {
        init();
        if (args.length > 0) {
            ExperienceReader experienceReader = new ExperienceReader(reasoner);
            experienceReader.openLoadFile(args[0]);
        }
        reasoner.addOutputChannel( new ExperienceWriter(reasoner,
        		new PrintWriter( out , true ) ) );
    }

    /**Initialize the system at the control center.<p>
     * Can instantiate multiple reasoners
     */
    public void init() {
        reasoner = new ReasonerBatch();
    }

    /** Repeatedly execute NARS working cycle, until Inputs are Finished, or 1000 steps.
     * This method is called when the Runnable's thread is started. */
    public void run() {
        while ( true ) {
        	log("NARSBatch.run():" +
        			" step " + reasoner.getTime()
        			+ " " + reasoner.isFinishedInputs() );
            reasoner.tick();
        	log("NARSBatch.run(): after tick"
        			+ " step " + reasoner.getTime()
        			+ " " + reasoner.isFinishedInputs()
        	);       	
			if( reasoner.isFinishedInputs() ||
					reasoner.getTime() == 1000 ) break;
        }
    }
     
     public void setPrintStream(PrintStream out) {
    	 this.out = out;
     }
     
     private void log(String mess) {
    	if(logging) System.out.println( "/ " + mess);
     }
}