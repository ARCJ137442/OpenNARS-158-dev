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
import java.io.PrintStream;
import java.util.ArrayList;

import nars.io.InputChannel;
import nars.io.OutputChannel;

/**
 * 🆕一个更简单的交互终端
 * * 📌单线程，仅输入输出
 *
 * @author tc, ARCJ137442
 */
public class SimpleShell {

    private boolean isRunning = false;
    private final ReasonerBatch reasoner;

    private final PrintStream out;

    public static void main(final String[] args) {
        SimpleShell shell = new SimpleShell(System.in, System.out);
        shell.main();
    }

    public SimpleShell(final InputStream in, final PrintStream out) {
        this.reasoner = new ReasonerBatch();
        // ! ❓此处的「输入通道」存在一些机制上的问题：需要「获取输入字符串组」而非其它的
        this.out = out;
        this.reasoner.addInputChannel(new ShellInput(this, in));
        this.reasoner.addOutputChannel(new ShellOutput(out));
    }

    /** 欢迎信息 */
    public static final String WELCOME_MESSAGE = "Welcome to the OpenNARS Shell, type some Narsese input and press enter, use questions to get answers, or increase volume with *volume=n with n=0..100";

    public void main() {
        this.out.println(WELCOME_MESSAGE);
        reasoner.run();
        reasoner.getSilenceValue().set(100);
        isRunning = true;
        while (isRunning) {
            reasoner.tick();
        }
    }

    public void exit(String source) {
        this.out.println("TERMINATED: OpenNARS exited by command \"" + source + "\".");
        System.exit(0);
    }

    /**
     * 终端输入通道
     */
    public static final class ShellInput implements InputChannel {
        /**
         * 🆕需要持有对「交互终端」的引用
         * * 🎯回显信息
         * * 🎯获取输入
         */
        private final SimpleShell shell;
        private final BufferedReader bufIn;

        public ShellInput(final SimpleShell shell, final InputStream in) {
            this.shell = shell;
            this.bufIn = new BufferedReader(new InputStreamReader(in));
        }

        @Override
        public boolean nextInput() {
            try {
                final String line = bufIn.readLine();
                if (line != null) {
                    inputLine(shell.reasoner, line);
                }
            } catch (final IOException e) {
                throw new IllegalStateException("Could not read line.", e);
            }
            return true;
        }

        public void inputLine(final ReasonerBatch reasoner, final String input) {
            if (input.isEmpty()) {
                return;
            }
            try {
                // 退出程序
                // * 🎯【2024-05-09 13:35:47】在其它语言中通过`java -jar`启动OpenNARS时，主动退出不容易——总是有残余进程
                if (input.startsWith("*exit") || input.startsWith("*quit")) {
                    shell.exit(input);
                }
                // 推理步进（手动）
                else if (input.matches("[0-9]+")) {
                    final int val = Integer.parseInt(input);
                    shell.out.println("INFO: running " + val + " cycles.");
                    reasoner.walk(val);
                    // for (int i = 0; i < val; i++)
                    // reasoner.tick();
                }
                // 设置音量
                else if (input.startsWith("*volume=")) { // volume to be consistent with OpenNARS
                    final int val = Integer.parseInt(input.split("\\*volume=")[1]);
                    if (val >= 0 && val <= 100) {
                        reasoner.getSilenceValue().set(100 - val);
                    } else {
                        shell.out.println("Volume ignored, not in range");
                    }
                }
                // 开启debug模式
                else if (input.startsWith("*debug=")) { // volume to be consistent with OpenNARS
                    String param = input.split("\\*debug=")[1];
                    ReasonerBatch.DEBUG = !param.isEmpty();
                }
                // 输入Narsese
                else {
                    reasoner.textInputLine(input);
                    // reasoner.tick(); // 输入之后至少先将输出打印出来
                }
            }
            // * 🚩异常捕获 & 呈现
            catch (final Exception ex) {
                printException(ex);
            }
            // * 🚩最终总是「完成输出」
            finally {
                shell.out.flush();
            }
        }

        private void printException(final Exception ex) {
            String stackTrace = "";
            for (final StackTraceElement stElement : ex.getStackTrace()) {
                stackTrace += " @ " + stElement.toString();
            }

            final String trace = ex.getStackTrace().length > 0 ? stackTrace : "";
            shell.out.println("ERROR: (" + ex.getClass().toGenericString() + ") "
                    + ex.getMessage() + trace);
        }
    }

    /**
     * 终端输出通道
     */
    public static final class ShellOutput implements OutputChannel {

        /** 🆕要指定的输出流 */
        private final PrintStream out;

        public ShellOutput(PrintStream out) {
            this.out = out;
        }

        @Override
        public void nextOutput(ArrayList<String> arg0) {
            for (final String s : arg0) {
                if (s.matches("[0-9]+")) {
                    // this.out.println("INFO: ran " + s + " cycles.");
                    // * 🚩已经在`inputLine`中输出过，此处忽略
                } else
                    this.out.println(s);
            }
        }

        @Override
        public void tickTimer() {
        }
    }
}
