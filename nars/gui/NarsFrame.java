/*
 * NarsFrame.java
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
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package nars.gui;

import java.awt.*;

/**
 * Specify shared properties of NARS windows
 */
public abstract class NarsFrame extends Frame {
    /** Color for the background of the main window */
    static final Color MAIN_WINDOW_COLOR = new Color(120, 120, 255);
    /** Color for the background of the windows with unique instantiation */
    static final Color SINGLE_WINDOW_COLOR = new Color(180, 100, 230);
    /** Color for the background of the windows with multiple instantiations */
    static final Color MULTIPLE_WINDOW_COLOR = new Color(100, 220, 100);
    /** Color for the background of the text components that are read-only */
    static final Color DISPLAY_BACKGROUND_COLOR = new Color(200, 230, 220);
    /** Font for NARS GUI */
    static final Font NarsFont = new Font("Helvetica", Font.PLAIN, 11);
    /** Message for unimplemented functions */
    static final String UNAVAILABLE = "\n Not implemented in this demo applet.";
    
    /** Default constructor */
    NarsFrame() {
        super();
    }
    /**
     * Constructor with title and font setting
     * @param title The title displayed by the window
     */    
    NarsFrame(String title) {
        super(" " + title);
        setFont(NarsFont);
    }
}
