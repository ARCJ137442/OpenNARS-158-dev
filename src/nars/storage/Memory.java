package nars.storage;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import nars.control.DerivationContext;
import nars.entity.Concept;
import nars.entity.Sentence;
import nars.entity.Task;
import nars.inference.Budget;
import nars.inference.BudgetFunctions;
import nars.io.IInferenceRecorder;
import nars.language.Term;
import nars.main_nogui.Parameters;
import nars.main_nogui.ReasonerBatch;

/**
 * The memory of the system.
 */
public class Memory {

    /**
     * The type of report.
     * * 🚩【2024-04-19 12:44:36】增加了多种输出方式
     */
    public enum ReportType {
        IN,
        OUT,
        ANSWER,
        EXE;

        /**
         * 将报告类型转换为字符串
         * * 📝Java在枚举的开头用一个语句定义所有枚举项
         *
         * @param type 报告类型
         * @return 字符串（仅名称）
         */
        @Override
        public String toString() {
            switch (this) {
                case IN:
                    return "IN";
                case OUT:
                    return "OUT";
                case ANSWER:
                    return "ANSWER";
                case EXE:
                    return "EXE";
                default: // * 穷举后不会发生
                    return "OTHER";
            }
        }
    }

    /**
     * Backward pointer to the reasoner
     */
    private final ReasonerBatch reasoner;

    /* ---------- Long-term storage for multiple cycles ---------- */
    /**
     * Concept bag. Containing all Concepts of the system
     */
    private final ConceptBag concepts;
    /**
     * New tasks with novel composed terms, for delayed and selective processing
     */
    private final NovelTaskBag novelTasks;
    /**
     * Inference record text to be written into a log file
     */
    private IInferenceRecorder recorder;
    private final AtomicInteger beliefForgettingRate = new AtomicInteger(Parameters.TERM_LINK_FORGETTING_CYCLE);
    private final AtomicInteger taskForgettingRate = new AtomicInteger(Parameters.TASK_LINK_FORGETTING_CYCLE);
    private final AtomicInteger conceptForgettingRate = new AtomicInteger(Parameters.CONCEPT_FORGETTING_CYCLE);

    /* ---------- Short-term workspace for a single cycle ---------- */
    /**
     * List of new tasks accumulated in one cycle, to be processed in the next
     * cycle
     */
    private final LinkedList<Task> newTasks;
    /**
     * List of Strings or Tasks to be sent to the output channels
     */
    private final ArrayList<String> exportStrings;

    // /**
    // * 🆕新的「推理上下文」对象
    // * * 🚩【2024-05-18 17:12:03】目前重复使用，好像它就是「记忆区中变量的一部分」一样
    // */
    // public DerivationContext context = new DerivationContext(this);

    /* ---------- Constructor ---------- */
    /**
     * Create a new memory
     * <p>
     * Called in Reasoner.reset only
     *
     * @param reasoner
     */
    public Memory(ReasonerBatch reasoner) {
        this.reasoner = reasoner;
        recorder = new NullInferenceRecorder();
        concepts = new ConceptBag(this);
        novelTasks = new NovelTaskBag(this);
        newTasks = new LinkedList<>();
        exportStrings = new ArrayList<>();
    }

    public void init() {
        concepts.init();
        novelTasks.init();
        newTasks.clear();
        exportStrings.clear();
        reasoner.initTimer();
        DerivationContext.init();
        recorder.append("\n-----RESET-----\n");
    }

    /* ---------- access utilities ---------- */

    public ArrayList<String> getExportStrings() {
        return exportStrings;
    }

    public IInferenceRecorder getRecorder() {
        return recorder;
    }

    public void setRecorder(IInferenceRecorder recorder) {
        this.recorder = recorder;
    }

    public long getTime() {
        return reasoner.getTime();
    }

    /**
     * 用于转发推理器的{@link ReasonerBatch#getTimer}
     */
    public long getTimer() {
        return reasoner.getTimer();
    }

    /**
     * 用于转发推理器的{@link ReasonerBatch#updateTimer}
     */
    public long updateTimer() {
        return reasoner.updateTimer();
    }

    /**
     * 获取「静默值」
     * * 🎯在「推理上下文」中无需获取「推理器」`getReasoner`
     *
     * @return 静默值
     */
    public AtomicInteger getSilenceValue() {
        return reasoner.getSilenceValue();
    }

    /**
     * 🆕将「更新并获取时间戳序列号」用于「时间戳的建立」中
     *
     * @return
     */
    public long updateStampCurrentSerial() {
        return reasoner.updateStampCurrentSerial();
    }

    // public MainWindow getMainWindow() {
    // return reasoner.getMainWindow();
    // }

    /* ---------- conversion utilities ---------- */
    /**
     * Get an existing Concept for a given name
     * <p>
     * called from Term and ConceptWindow.
     *
     * @param name the name of a concept
     * @return a Concept or null
     */
    public Concept nameToConcept(String name) {
        return concepts.get(name);
    }

    /**
     * Get a Term for a given name of a Concept or Operator
     * <p>
     * called in StringParser and the make methods of compound terms.
     *
     * @param name the name of a concept or operator
     * @return a Term or null (if no Concept/Operator has this name)
     */
    public Term nameToListedTerm(String name) {
        final Concept concept = concepts.get(name);
        return concept == null ? null : concept.getTerm();
    }

    /**
     * Get an existing Concept for a given Term.
     *
     * @param term The Term naming a concept
     * @return a Concept or null
     */
    public Concept termToConcept(Term term) {
        // * ✅【2024-05-24 22:09:35】现在不会在推理规则中被调用了
        return nameToConcept(term.getName());
    }

    /**
     * 🆕判断「记忆区中是否已有概念」
     * * 🚩Check if a Term has a Concept.
     *
     * @param term The Term naming a concept
     * @return true if the Term has a Concept in the memory
     */
    public boolean hasConcept(Term term) {
        return termToConcept(term) != null;
    }

    /**
     * Get the Concept associated to a Term, or create it.
     *
     * @param term indicating the concept
     * @return an existing Concept, or a new one, or null ( TODO bad smell )
     */
    public Concept getConceptOrCreate(Term term) {
        // * 🚩不给「非常量词项」新建概念 | 「非常量词项」也不可能作为一个「概念」被放进「记忆区」中
        if (!term.isConstant())
            return null;
        // * 🚩尝试从概念袋中获取「已有概念」，否则创建概念
        final Concept concept = termToConcept(term);
        return concept == null ? makeNewConcept(term) : concept;
    }

    /**
     * 🆕新建一个概念
     * * 📌概念只可能由此被创建
     *
     * @param term 概念对应的词项
     * @return 已经被置入「概念袋」的概念 | 创建失败时返回`
     */
    private Concept makeNewConcept(Term term) {
        final Concept concept = new Concept(term, this); // the only place to make a new Concept
        final boolean created = concepts.putIn(concept);
        return created ? concept : null;
    }

    /**
     * Get the current activation level of a concept.
     *
     * @param t The Term naming a concept
     * @return the priority value of the concept
     */
    public float getConceptActivation(Term t) {
        final Concept c = termToConcept(t);
        return (c == null) ? 0f : c.getPriority();
    }

    /* ---------- adjustment functions ---------- */
    /**
     * Adjust the activation level of a Concept
     * <p>
     * called in Concept.insertTaskLink only
     * * 🚩实际上也被「直接推理」调用
     *
     * @param c the concept to be adjusted
     * @param b the new BudgetValue
     */
    public void activateConcept(final Concept c, final Budget b) {
        // * 🚩存在性检查
        final boolean hasC = concepts.contains(c);
        // * 🚩若已有⇒拿出→放回 | 会改变「概念」的优先级，因此可能会调整位置
        if (hasC) {
            concepts.pickOut(c.getKey());
            BudgetFunctions.activate(c, b);
            concepts.putBack(c);
        }
        // * 🚩若没有⇒放回→拿出
        else {
            BudgetFunctions.activate(c, b);
            concepts.forget(c); // * 📝此方法将改变「概念」的预算值，需要保证顺序一致
        }
    }

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
            recorder.append("!!! Perceived: " + task + "\n");
            this.report(task, ReportType.IN); // report input
            newTasks.add(task); // wait to be processed in the next workCycle
        } else {
            recorder.append("!!! Neglected: " + task + "\n");
        }
    }

    /**
     * Display input/output sentence in the output channels. The only place to
     * add Objects into exportStrings. Currently only Strings are added, though
     * in the future there can be outgoing Tasks; also if exportStrings is empty
     * display the current value of timer ( exportStrings is emptied in
     * {@link ReasonerBatch#doTick()} - TODO fragile mechanism)
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
        if (ReasonerBatch.DEBUG) {
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
        context.absorbedByMemory(this);
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
    public final NovelTaskBag mut_novelTasks() {
        return novelTasks;
    }

    /**
     * 🆕对外接口：从「概念袋」中拿出一个概念
     *
     * @return 拿出的一个概念 / 空
     */
    public final Concept takeOutConcept() {
        return this.concepts.takeOut();
    }

    /**
     * 🆕对外接口：从「概念袋」中挑出一个概念
     * * 🚩用于「直接推理」中的「拿出概念」
     *
     * @return 拿出的一个概念 / 空
     */
    public final Concept pickOutConcept(String key) {
        return concepts.pickOut(key);
    }

    /**
     * 🆕对外接口：往「概念袋」放回一个概念
     *
     * @return 拿出的一个概念 / 空
     */
    public final void putBackConcept(Concept concept) {
        this.concepts.putBack(concept);
    }

    /**
     * Actually means that there are no new Tasks
     */
    public boolean noResult() {
        return newTasks.isEmpty();
    }

    /* ---------- display ---------- */
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
        bagObserver.setBag(concepts);
        concepts.addBagObserver(bagObserver, title);
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
        String result = toStringLongIfNotNull(concepts, "concepts")
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

    public AtomicInteger getTaskForgettingRate() {
        return taskForgettingRate;
    }

    public AtomicInteger getBeliefForgettingRate() {
        return beliefForgettingRate;
    }

    public AtomicInteger getConceptForgettingRate() {
        return conceptForgettingRate;
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
