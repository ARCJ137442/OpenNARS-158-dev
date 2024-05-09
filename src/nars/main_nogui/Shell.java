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

    static boolean isRunning = false;

    public static void main(String[] args) {
        ReasonerBatch reasoner = new ReasonerBatch();
        reasoner.addOutputChannel(new ShellOutput());
        InputThread thr = new InputThread(System.in, reasoner);
        thr.start();
        System.out.println(
                "Welcome to the OpenNARS Shell, type some Narsese input and press enter, use questions to get answers, or increase volume with *volume=n with n=0..100");
        reasoner.run();
        reasoner.getSilenceValue().set(100);
        isRunning = true;
        while (isRunning) {
            // 此处的代码交给inputThread
        }
    }

    public static void inputLine(ReasonerBatch reasoner, String inputString) {

        if (!"".equals(inputString)) {
            try {
                // 退出程序
                // * 🎯【2024-05-09 13:35:47】在其它语言中通过`java -jar`启动OpenNARS时，主动退出不容易——总是有残余进程
                if (inputString.startsWith("*exit") || inputString.startsWith("*quit")) {
                    System.out.println("TERMINATED: OpenNARS exited by command \"" + inputString + "\".");
                    System.exit(0);
                }
                // 推理步进（手动）
                else if (inputString.matches("[0-9]+")) {
                    System.out.println("INFO: running " + inputString + " cycles.");
                    int val = Integer.parseInt(inputString);
                    for (int i = 0; i < val; i++)
                        reasoner.tick();
                }
                // 设置音量
                else if (inputString.startsWith("*volume=")) { // volume to be consistent with OpenNARS
                    int val = Integer.parseInt(inputString.split("\\*volume=")[1]);
                    if (val >= 0 && val <= 100) {
                        reasoner.getSilenceValue().set(100 - val);
                    } else {
                        System.out.println("Volume ignored, not in range");
                    }
                }
                // 开启debug模式
                else if (inputString.startsWith("*debug=")) { // volume to be consistent with OpenNARS
                    String param = inputString.split("\\*debug=")[1];
                    ReasonerBatch.DEBUG = !param.isEmpty();
                }
                // 输入Narsese
                else {
                    reasoner.textInputLine(inputString);
                    reasoner.tick(); // 输入之后至少推理步进一步
                }
                inputString = "";
            } catch (Exception ex) {
                inputString = "";
            }
        }
    }

    public static class ShellOutput implements OutputChannel {
        @Override
        public void nextOutput(ArrayList<String> arg0) {
            for (String s : arg0) {
                if (s.matches("[0-9]+")) {
                    // System.out.println("INFO: ran " + s + " cycles.");
                    // * 🚩已经在`inputLine`中输出过，此处忽略
                } else
                    System.out.println(s);
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
                            inputLine(reasoner, inputString);
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
