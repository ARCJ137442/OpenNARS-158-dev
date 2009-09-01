/*
 * MessageDialog.java
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

/**
 * Pop-up message for the user to accept
 */
public class MessageDialog extends Dialog implements ActionListener, WindowListener {
    protected Button button;
    protected TextArea text;
    
    /**
     * Constructor
     * @param parent The parent Frame
     * @param message The text to be displayed
     */    
    public MessageDialog(Frame parent, String message) {
        super(parent, "Message", false);
        setLayout(new BorderLayout(5, 5));
        setBackground(NarsFrame.SINGLE_WINDOW_COLOR);
        setFont(NarsFrame.NarsFont);
        text = new TextArea(message);
        text.setBackground(NarsFrame.DISPLAY_BACKGROUND_COLOR);
        this.add("Center", text);
        button = new Button(" OK ");
        button.addActionListener(this);
        Panel p = new Panel();
        p.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        p.add(button);
        this.add("South", p);
        setModal(true);
        setBounds(200, 250, 400, 180);
        addWindowListener(this);
        setVisible(true);
    }
    
    /**
     * Handling button click
     * @param e The ActionEvent
     */    
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == button) {
        	close();
        }
    }
    
    private void close() {
        this.setVisible(false);
        this.dispose();
    }

	@Override
	public void windowActivated(WindowEvent arg0) {
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		close();
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