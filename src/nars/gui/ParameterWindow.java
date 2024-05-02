package nars.gui;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JWindow displaying a system parameter that can be adjusted in run time
 */
public class ParameterWindow extends NarsFrame implements ActionListener, AdjustmentListener, WindowFocusListener {

    /** Display label */
    private JLabel valueLabel;
    /** Control buttons */
    private JButton hideButton, undoButton, defaultButton;
    /** Adjusting bar */
    private JScrollBar valueBar;
    /** parameter values */
    private int defaultValue, previousValue; // , currentValue;
    private AtomicInteger currentValue;

    /**
     * Constructor
     *
     * @param title Parameter name
     * @param dft   The default value of the parameter
     * @param value
     */
    ParameterWindow(String title, int dft, AtomicInteger currentValue) {
        super(title);
        // System.out.println("ParameterWindow.ParameterWindow(): " +
        // "title " + title +
        // "currentValue " + currentValue);
        defaultValue = dft;
        this.currentValue = currentValue;

        // previousValue = dft;
        previousValue = currentValue.get();
        currentValue.set(dft);
        setLayout(new GridLayout(3, 3, 8, 4));
        getContentPane().setBackground(SINGLE_WINDOW_COLOR);
        JLabel sp1 = new JLabel("");
        sp1.setBackground(SINGLE_WINDOW_COLOR);
        add(sp1);
        valueLabel = new JLabel(String.valueOf(dft), JLabel.CENTER);
        valueLabel.setBackground(SINGLE_WINDOW_COLOR);
        add(valueLabel);
        JLabel sp2 = new JLabel("");
        sp2.setBackground(SINGLE_WINDOW_COLOR);
        add(sp2);
        add(new JLabel("0", JLabel.RIGHT));
        // valueBar = new JScrollBar(Scrollbar.HORIZONTAL, dft, 0, 0, 100);
        valueBar = new JScrollBar(Scrollbar.HORIZONTAL, currentValue.get(), 0, 0, 100);
        valueBar.addAdjustmentListener(this);
        addWindowFocusListener(this);
        add(valueBar);
        add(new JLabel("100", JLabel.LEFT));
        undoButton = new JButton("Undo");
        undoButton.addActionListener(this);
        add(undoButton);
        defaultButton = new JButton("Default");
        defaultButton.addActionListener(this);
        add(defaultButton);
        hideButton = new JButton("Hide");
        hideButton.addActionListener(this);
        add(hideButton);
        this.setBounds(600, 600, 250, 120);
    }

    /**
     * Get the value of the parameter
     *
     * @return The current value
     */
    public int value() {
        return currentValue.get();
    }

    /**
     * Handling button click
     *
     * @param e The ActionEvent
     */
    public void actionPerformed(ActionEvent e) {
        Object s = e.getSource();
        if (s == defaultButton) {
            currentValue.set(defaultValue);
            valueBar.setValue(currentValue.get());
            valueLabel.setText(String.valueOf(currentValue));
        } else if (s == undoButton) {
            currentValue.set(previousValue);
            valueBar.setValue(currentValue.get());
            valueLabel.setText(String.valueOf(currentValue));
        } else if (s == hideButton) {
            close();
        }
    }

    private void close() {
        previousValue = currentValue.get();
        setVisible(false);
    }

    @Override
    public void windowClosing(WindowEvent arg0) {
        close();
    }

    /**
     * Handling scrollbar movement
     *
     * @param e The AdjustmentEvent
     */
    public void adjustmentValueChanged(AdjustmentEvent e) {
        if (e.getSource() == valueBar) {
            int v = valueBar.getValue();
            valueLabel.setText(String.valueOf(v));
            valueBar.setValue(v);
            currentValue.set(v);
        }
    }

    @Override
    /**
     * hack to update the slider to the correct value when app. has been started
     * with
     * --silence 100
     * <p/>
     * I consider using PropertyChangeSupport for the silence level,
     * or leveraging valueBar's model.
     */
    public void windowGainedFocus(WindowEvent e) {
        valueBar.setValue(currentValue.get());
    }

    @Override
    public void windowLostFocus(WindowEvent e) {
    }
}
