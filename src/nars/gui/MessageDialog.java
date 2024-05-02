package nars.gui;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;

/**
 * Pop-up message for the user to accept
 */
public class MessageDialog extends JDialog implements ActionListener, WindowListener {

    protected JButton button;
    protected JTextArea text;

    /**
     * Constructor
     *
     * @param parent  The parent Frame
     * @param message The text to be displayed
     */
    public MessageDialog(Frame parent, String message) {
        super(parent, "Message", false);
        setLayout(new BorderLayout(5, 5));
        getContentPane().setBackground(NarsFrame.SINGLE_WINDOW_COLOR);
        setFont(NarsFrame.NarsFont);
        text = new JTextArea(message);
        text.setBackground(NarsFrame.DISPLAY_BACKGROUND_COLOR);
        this.add("Center", text);
        button = new JButton(" OK ");
        button.addActionListener(this);
        JPanel p = new JPanel();
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
     *
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
