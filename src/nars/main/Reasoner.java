package nars.main;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import nars.control.DerivationContext;
import nars.control.ProcessDirect;
import nars.control.ProcessReason;
import nars.control.ReportType;
import nars.entity.Concept;
import nars.entity.Sentence;
import nars.entity.Task;
import nars.gui.MainWindow;
import nars.inference.InferenceEngine;
import nars.inference.InferenceEngineV1;
import nars.io.IInferenceRecorder;
import nars.io.InputChannel;
import nars.io.OutputChannel;
import nars.io.StringParser;
import nars.io.Symbols;
import nars.storage.Bag;
import nars.storage.BagObserver;
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
        this(null);
    }

    public Reasoner(String name) {
        this.name = name;
        this.memory = new Memory();
        this.inputChannels = new ArrayList<>();
        this.outputChannels = new ArrayList<>();
        novelTasks = new Bag<Task>(
                new AtomicInteger(Parameters.NEW_TASK_FORGETTING_CYCLE), Parameters.TASK_BUFFER_SIZE);
        recorder = new NullInferenceRecorder();
        this.newTasks = new LinkedList<>();
        this.exportStrings = new ArrayList<>();
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
        this.initTimer();
        recorder.append("\n-----RESET-----\n");
        newTasks.clear();
        novelTasks.init();
        exportStrings.clear();
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
                    + "\n//    exportStrings " + this.exportStrings);
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
        final ArrayList<String> output = this.exportStrings;
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
        this.recorder.append(" --- " + this.getTime() + " ---\n");

        // * 🚩本地任务直接处理 阶段 * //
        final boolean noResult = ProcessDirect.processDirect(this);

        // * 🚩内部概念高级推理 阶段 * //
        ProcessReason.processReason(this, this.inferenceEngine, noResult);

        // * 🚩最后收尾 阶段 * //
        // * 🚩原「清空上下文」已迁移至各「推理」阶段
        this.mut_novelTasks().refresh();
    }

    /* ---------- Short-term workspace for a single cycle ---------- */
    /**
     * List of new tasks accumulated in one cycle, to be processed in the next
     * cycle
     */
    private final LinkedList<Task> newTasks;
    /**
     * New tasks with novel composed terms, for delayed and selective processing
     */
    private final Bag<Task> novelTasks;
    /**
     * List of Strings or Tasks to be sent to the output channels
     */
    private final ArrayList<String> exportStrings;

    // /**
    // * 🆕新的「推理上下文」对象
    // * * 🚩【2024-05-18 17:12:03】目前重复使用，好像它就是「记忆区中变量的一部分」一样
    // */
    // public DerivationContext context = new DerivationContext(this);

    /* ---------- new task entries ---------- */

    /*
     * There are several types of new tasks, all added into the
     * newTasks list, to be processed in the next workCycle.
     * Some of them are reported and/or logged.
     */
    /**
     * Input task processing. Invoked by the outside or inside environment.
     * Outside: StringParser (input); Inside: Operator (feedback). Input tasks
     * with low priority are ignored, and the others are put into task buffer.
     *
     * @param task The input task
     */
    public void inputTask(Task task) {
        if (task.budgetAboveThreshold()) {
            this.recorder.append("!!! Perceived: " + task + "\n");
            this.report(task, ReportType.IN); // report input
            newTasks.add(task); // wait to be processed in the next workCycle
        } else {
            this.recorder.append("!!! Neglected: " + task + "\n");
        }
    }

    /**
     * Display input/output sentence in the output channels. The only place to
     * add Objects into exportStrings. Currently only Strings are added, though
     * in the future there can be outgoing Tasks; also if exportStrings is empty
     * display the current value of timer ( exportStrings is emptied in
     * {@link Reasoner#doTick()} - TODO fragile mechanism)
     *
     */
    public void report(Sentence sentence, ReportType type) {
        report(DerivationContext.generateReportString(sentence, type));
    }

    /**
     * 🆕只报告字符串
     * * 🎯从「吸收上下文」中调用
     * * 🎯从「直接报告」中转发
     *
     * @param output 要输出的字符串
     */
    public void report(String output) {
        if (Reasoner.DEBUG) {
            System.out.println("// report( clock " + getTime()
            // + ", input " + input
                    + ", timer " + getTimer()
                    + ", output " + output
                    + ", exportStrings " + exportStrings);
            System.out.flush();
        }
        if (exportStrings.isEmpty()) {
            long timer = updateTimer();
            if (timer > 0) {
                exportStrings.add(String.valueOf(timer));
            }
        }
        exportStrings.add(output);
    }

    /**
     * 吸收「推理上下文」
     * * 🚩【2024-05-21 23:18:55】现在直接调用「推理上下文」的对应方法，以便享受多分派
     */
    public void absorbContext(final DerivationContext context) {
        context.absorbedByReasoner(this);
    }

    /**
     * 🆕对外接口：获取可变的「新任务」列表
     * * 🚩获取的「新任务」可变
     * * 🎯用于「直接推理」
     */
    public final LinkedList<Task> mut_newTasks() {
        return newTasks;
    }

    /**
     * 🆕对外接口：获取可变的「新任务」列表
     * * 🚩获取的「新任务」可变
     * * 🎯用于「直接推理」
     */
    public final Bag<Task> mut_novelTasks() {
        return this.novelTasks;
    }

    public ArrayList<String> getExportStrings() {
        return this.exportStrings;
    }

    /**
     * Actually means that there are no new Tasks
     */
    public boolean noResult() {
        return this.newTasks.isEmpty();
    }

    // 输入输出 //

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
            this.exportStrings.add(text);
        } else if (c != Symbols.COMMENT_MARK) {
            // read NARS language or an integer : TODO duplicated code
            try {
                final int i = Integer.parseInt(text);
                walk(i);
            } catch (NumberFormatException e) {
                final Task task = StringParser.parseExperience(
                        new StringBuffer(text),
                        memory,
                        this.updateStampCurrentSerial(),
                        clock);
                if (task != null) {
                    this.inputTask(task);
                }
            }
        }
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

    /* ---- display ---- */

    /* ---------- display ---------- */
    /**
     * Inference record text to be written into a log file
     */
    private IInferenceRecorder recorder;

    /* ---------- access utilities ---------- */

    public IInferenceRecorder getRecorder() {
        return recorder;
    }

    public void setRecorder(IInferenceRecorder recorder) {
        this.recorder = recorder;
    }

    /**
     * Start display active concepts on given bagObserver, called from MainWindow.
     *
     * we don't want to expose fields concepts and novelTasks, AND we want to
     * separate GUI and inference, so this method takes as argument a
     * {@link BagObserver} and calls
     * {@link ConceptBag#addBagObserver(BagObserver, String)} ;
     *
     * see design for {@link Bag} and {@link nars.gui.BagWindow}
     * in {@link Bag#addBagObserver(BagObserver, String)}
     *
     * @param bagObserver bag Observer that will receive notifications
     * @param title       the window title
     */
    public void conceptsStartPlay(BagObserver<Concept> bagObserver, String title) {
        bagObserver.setBag(this.memory.getConceptBagForDisplay());
        this.memory.getConceptBagForDisplay().addBagObserver(bagObserver, title);
    }

    /**
     * Display new tasks, called from MainWindow. see
     * {@link #conceptsStartPlay(BagObserver, String)}
     *
     * @param bagObserver
     * @param s           the window title
     */
    public void taskBuffersStartPlay(BagObserver<Task> bagObserver, String s) {
        bagObserver.setBag(novelTasks);
        novelTasks.addBagObserver(bagObserver, s);
    }

    @Override
    public String toString() {
        String result = toStringLongIfNotNull(this.memory.getConceptBagForDisplay(), "concepts")
                + toStringLongIfNotNull(novelTasks, "novelTasks")
                + toStringIfNotNull(newTasks, "newTasks");
        // ! ❌【2024-05-21 10:52:53】因为现在「推理上下文」仅为临时变量，故不再提供其信息
        // if (context != null) {
        // result += toStringLongIfNotNull(context.getCurrentTask(), "currentTask")
        // + toStringLongIfNotNull(context.getCurrentBeliefLink(), "currentBeliefLink")
        // + toStringIfNotNull(context.getCurrentBelief(), "currentBelief");
        // }
        return result;
    }

    private String toStringLongIfNotNull(Bag<?> item, String title) {
        return item == null ? ""
                : "\n " + title + ":\n"
                        + item.toStringLong();
    }

    // private String toStringLongIfNotNull(Item item, String title) {
    // return item == null ? ""
    // : "\n " + title + ":\n"
    // + item.toStringLong();
    // }

    private String toStringIfNotNull(Object item, String title) {
        return item == null ? ""
                : "\n " + title + ":\n"
                        + item.toString();
    }

    class NullInferenceRecorder implements IInferenceRecorder {

        @Override
        public void init() {
        }

        @Override
        public void show() {
        }

        @Override
        public void play() {
        }

        @Override
        public void stop() {
        }

        @Override
        public void append(String s) {
        }

        @Override
        public void openLogFile() {
        }

        @Override
        public void closeLogFile() {
        }

        @Override
        public boolean isLogging() {
            return false;
        }
    }
}