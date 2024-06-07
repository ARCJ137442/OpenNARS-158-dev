package nars.main;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import nars.control.ProcessDirect;
import nars.control.ProcessReason;
import nars.entity.Task;
import nars.inference.InferenceEngine;
import nars.inference.InferenceEngineV1;
import nars.io.InputChannel;
import nars.io.OutputChannel;
import nars.io.StringParser;
import nars.io.Symbols;
import nars.storage.Memory;

/**
 * 主推理器
 * * 🚩【2024-06-07 23:44:13】扶正为独立的「推理器」类，尝试与UI代码解耦
 */
public class Reasoner {

    /**
     * global DEBUG print switch
     */
    public static boolean DEBUG = false;
    /**
     * The name of the reasoner
     */
    protected final String name;
    /**
     * The memory of the reasoner
     */
    protected final Memory memory;
    /**
     * The input channels of the reasoner
     */
    protected final ArrayList<InputChannel> inputChannels;
    /**
     * The output channels of the reasoner
     */
    protected final ArrayList<OutputChannel> outputChannels;
    /**
     * System clock, relatively defined to guarantee the repeatability of
     * behaviors
     */
    private long clock;
    /**
     * Flag for running continuously
     */
    private boolean running;
    /**
     * The remaining number of steps to be carried out (walk mode)
     */
    private int walkingSteps;
    /**
     * determines the end of {@link NARS} program (set but not accessed in
     * this class)
     */
    private boolean finishedInputs;
    /**
     * System clock - number of cycles since last output
     */
    private long timer;
    private final AtomicInteger silenceValue = new AtomicInteger(Parameters.SILENT_LEVEL);

    /**
     * serial number, a field in {@link Reasoner}
     * * 📌当前时间戳序列号
     * * 📝每个新创建的「时间戳」都有一个属于自身的「序列号」
     * * 🚩从`Stamp.currentSerial`迁移过来
     */
    private long stampCurrentSerial = 0;

    /**
     * 🆕使用的推理引擎
     */
    private final InferenceEngine inferenceEngine = new InferenceEngineV1();

    /**
     * 🆕获取自身时间戳序列号，并在此同时更新
     * * 🚩原先在「时间戳」中便是「先++，再构造」
     *
     * @return
     */
    public long updateStampCurrentSerial() {
        return ++stampCurrentSerial;
    }

    public Reasoner() {
        name = null;
        memory = new Memory(this);
        inputChannels = new ArrayList<>();
        outputChannels = new ArrayList<>();
    }

    public Reasoner(String name) {
        this.name = name;
        memory = new Memory(this);
        inputChannels = new ArrayList<>();
        outputChannels = new ArrayList<>();
    }

    /**
     * Reset the system with an empty memory and reset clock. Called locally and
     * from {@link MainWindow}.
     */
    public void reset() {
        running = false;
        walkingSteps = 0;
        clock = 0;
        memory.init();
        stampCurrentSerial = 0;
        // timer = 0;
    }

    public Memory getMemory() {
        return memory;
    }

    public void addInputChannel(InputChannel channel) {
        inputChannels.add(channel);
    }

    public void removeInputChannel(InputChannel channel) {
        inputChannels.remove(channel);
    }

    public void addOutputChannel(OutputChannel channel) {
        outputChannels.add(channel);
    }

    public void removeOutputChannel(OutputChannel channel) {
        outputChannels.remove(channel);
    }

    /**
     * Get the current time from the clock Called in {@link nars.entity.Stamp}
     *
     * @return The current time
     */
    public long getTime() {
        return clock;
    }

    /**
     * Start the inference process
     */
    public void run() {
        running = true;
    }

    /**
     * Will carry the inference process for a certain number of steps
     *
     * @param n The number of inference steps to be carried
     */
    public void walk(int n) {
        walkingSteps = n;
    }

    /**
     * Will stop the inference process
     */
    public void stop() {
        running = false;
    }

    /**
     * A clock tick. Run one working workCycle or read input. Called from NARS
     * only.
     */
    public void tick() {
        doTick();
    }

    public void doTick() {
        if (DEBUG)
            handleDebug();

        handleInput();
        // forward to output Channels
        handleOutput();
        handleWorkCycle();
    }

    private void handleDebug() {
        if (running || walkingSteps > 0 || !finishedInputs) {
            System.out.println("// doTick: "
                    + "walkingSteps " + walkingSteps
                    + ", clock " + clock
                    + ", getTimer " + getTimer()
                    + "\n//    memory.getExportStrings() " + memory.getExportStrings());
            System.out.flush();
        }
    }

    public void handleInput() {
        if (walkingSteps == 0) {
            boolean reasonerShouldRun = false;
            for (final InputChannel channelIn : inputChannels) {
                reasonerShouldRun = reasonerShouldRun
                        || channelIn.nextInput();
            }
            finishedInputs = !reasonerShouldRun;
        }
    }

    public void handleOutput() {
        final ArrayList<String> output = memory.getExportStrings();
        if (!output.isEmpty()) {
            for (final OutputChannel channelOut : outputChannels) {
                channelOut.nextOutput(output);
            }
            output.clear(); // this will trigger display the current value of timer in Memory.report()
        }
    }

    public void handleWorkCycle() {
        if (running || walkingSteps > 0) {
            clock++;
            tickTimer();
            workCycle();
            if (walkingSteps > 0) {
                walkingSteps--;
            }
        }
    }

    /* ---------- system working workCycle ---------- */
    /**
     * An atomic working cycle of the system: process new Tasks, then fire a
     * concept
     * <p>
     * Called from Reasoner.tick only
     *
     * * 🚩【2024-05-24 22:58:06】现在将「推理周期」从「记忆区」迁移到「推理器」中
     * * ✅省掉`clock`参数：本身通过`getTime`方法，仍然能获取到这个参数
     */
    public void workCycle() {
        this.memory.getRecorder().append(" --- " + this.getTime() + " ---\n");

        // * 🚩本地任务直接处理 阶段 * //
        final boolean noResult = ProcessDirect.processDirect(this.memory);

        // * 🚩内部概念高级推理 阶段 * //
        ProcessReason.processReason(this.memory, this.inferenceEngine, noResult);

        // * 🚩最后收尾 阶段 * //
        // * 🚩原「清空上下文」已迁移至各「推理」阶段
        this.memory.mut_novelTasks().refresh();
    }

    /**
     * determines the end of {@link NARS} program
     */
    public boolean isFinishedInputs() {
        return finishedInputs;
    }

    /**
     * To process a line of input text
     *
     * @param text
     */
    public void textInputLine(String text) {
        if (text.isEmpty()) {
            return;
        }
        char c = text.charAt(0);
        if (c == Symbols.RESET_MARK) {
            reset();
            memory.getExportStrings().add(text);
        } else if (c != Symbols.COMMENT_MARK) {
            // read NARS language or an integer : TODO duplicated code
            try {
                int i = Integer.parseInt(text);
                walk(i);
            } catch (NumberFormatException e) {
                Task task = StringParser.parseExperience(new StringBuffer(text), memory, clock);
                if (task != null) {
                    memory.inputTask(task);
                }
            }
        }
    }

    @Override
    public String toString() {
        return memory.toString();
    }

    /**
     * Report Silence Level
     */
    public AtomicInteger getSilenceValue() {
        return silenceValue;
    }

    /**
     * To get the timer value and then to
     * reset it by {@link #initTimer()};
     * plays the same role as {@link nars.gui.MainWindow#updateTimer()}
     *
     * @return The previous timer value
     */
    public long updateTimer() {
        long i = getTimer();
        initTimer();
        return i;
    }

    /**
     * Reset timer;
     * plays the same role as {@link nars.gui.MainWindow#initTimer()}
     */
    public void initTimer() {
        setTimer(0);
    }

    /**
     * Update timer
     */
    public void tickTimer() {
        setTimer(getTimer() + 1);
    }

    /** @return System clock : number of cycles since last output */
    public long getTimer() {
        return timer;
    }

    /** set System clock : number of cycles since last output */
    private void setTimer(long timer) {
        this.timer = timer;
    }
}