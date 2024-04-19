/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nars.main_nogui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import nars.io.OutputChannel;

/**
 *
 * @author tc
 */
public class Shell {

    static String inputString = "";

    public static void main(String[] args) {
        ReasonerBatch reasoner = new ReasonerBatch();
        reasoner.addOutputChannel(new ShellOutput());
        InputThread thr = new InputThread(System.in, reasoner);
        thr.start();
        System.out.println(
                "Welcome to the OpenNARS Shell, type some Narsese input and press enter, use questions to get answers, or increase volume with *volume=n with n=0..100");
        reasoner.run();
        reasoner.getSilenceValue().set(100);
        int cnt = 0;
        while (true) {
            synchronized (inputString) {
                if (!"".equals(inputString)) {
                    try {
                        if (inputString.startsWith("*volume=")) { // volume to be consistent with OpenNARS
                            int val = Integer.parseInt(inputString.split("\\*volume=")[1]);
                            if (val >= 0 && val <= 100) {
                                reasoner.getSilenceValue().set(100 - val);
                            } else {
                                System.out.println("Volume ignored, not in range");
                            }
                        } else if (inputString.startsWith("*debug=")) { // volume to be consistent with OpenNARS
                            String param = inputString.split("\\*debug=")[1];
                            ReasonerBatch.DEBUG = !param.isEmpty();
                        } else {
                            reasoner.textInputLine(inputString);
                        }
                        inputString = "";
                    } catch (Exception ex) {
                        inputString = "";
                    }
                }
            }
            reasoner.tick();
            cnt++;
            // if(cnt%10000 == 0) {
            // System.out.println(cnt);
            // }
        }
    }

    public static class ShellOutput implements OutputChannel {
        @Override
        public void nextOutput(ArrayList<String> arg0) {
            for (String s : arg0) {
                if (!s.matches("[0-9]+")) {
                    // 就是此处输出
                    System.out.println(s);
                }
            }
        }

        @Override
        public void tickTimer() {

        }

    }

    private static class InputThread extends Thread {
        private final BufferedReader bufIn;
        private final ReasonerBatch reasoner;

        InputThread(final InputStream in, ReasonerBatch reasoner) {
            this.bufIn = new BufferedReader(new InputStreamReader(in));
            this.reasoner = reasoner;
        }

        public void run() {
            while (true) {
                try {
                    final String line = bufIn.readLine();
                    if (line != null) {
                        synchronized (inputString) {
                            inputString = line;
                        }
                    }

                } catch (final IOException e) {
                    throw new IllegalStateException("Could not read line.", e);
                }

                try {
                    Thread.sleep(1);
                } catch (final InterruptedException e) {
                    throw new IllegalStateException("Unexpectedly interrupted while sleeping.", e);
                }
            }
        }
    }
}
