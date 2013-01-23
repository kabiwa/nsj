package agentj.examples.gui;

import javax.swing.*;
import java.awt.*;

/**
   A frame that contains a button to launch a simulated activity,
   a progress bar, and a text area for the activity output.
*/
class ProgressBarFrame extends JFrame {
    protected JProgressBar progressBar;
    protected JTextArea textArea;

    public static final int DEFAULT_WIDTH = 400;
    public static final int DEFAULT_HEIGHT = 200;

    public ProgressBarFrame() {
        setTitle("Client side progress");
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);

        // this text area holds the activity output
        textArea = new JTextArea();

        // set up panel with button and progress bar

        JPanel panel = new JPanel();
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);

        panel.add(progressBar);

        add(new JScrollPane(textArea), BorderLayout.CENTER);
        add(panel, BorderLayout.SOUTH);


    }

    public void setProgress(int n) {
        progressBar.setValue(n);
    }
}
