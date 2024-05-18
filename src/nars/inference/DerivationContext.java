package nars.inference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import nars.entity.BudgetValue;
import nars.entity.Concept;
import nars.entity.Sentence;
import nars.entity.Stamp;
import nars.entity.Task;
import nars.entity.TaskLink;
import nars.entity.TermLink;
import nars.entity.TruthValue;
import nars.language.Term;
import nars.main_nogui.ReasonerBatch;
import nars.storage.Memory;
import nars.storage.Memory.ReportType;

/**
 * 🆕新的「推理上下文」对象
 * * 📄仿自OpenNARS 3.1.0
 */
public class DerivationContext {

    /**
     * 对「记忆区」的反向引用
     * * 🚩【2024-05-18 17:00:12】目前需要访问其「输出」「概念」等功能
     */
    public Memory memory;

    /* ---------- Short-term workspace for a single cycle ---------- */
    /**
     * List of new tasks accumulated in one cycle, to be processed in the next
     * cycle
     * * 🚩【2024-05-18 17:29:40】在「记忆区」与「推理上下文」中各有一个，但语义不同
     * * 📌「记忆区」的跨越周期，而「推理上下文」仅用于存储
     */
    public final LinkedList<Task> newTasks = new LinkedList<>();
    /**
     * List of Strings or Tasks to be sent to the output channels
     * * 🚩【2024-05-18 17:29:40】在「记忆区」与「推理上下文」中各有一个，但语义不同
     * * 📌「记忆区」的跨越周期，而「推理上下文」仅用于存储
     */
    public final ArrayList<String> exportStrings = new ArrayList<>();
    /**
     * The selected Term
     */
    public Term currentTerm = null;
    /**
     * The selected Concept
     */
    public Concept currentConcept = null;
    /**
     * The selected TaskLink
     */
    public TaskLink currentTaskLink = null;
    /**
     * The selected Task
     */
    public Task currentTask = null;
    /**
     * The selected TermLink
     */
    public TermLink currentBeliefLink = null;
    /**
     * The selected belief
     */
    public Sentence currentBelief = null;
    /**
     * The new Stamp
     */
    public Stamp newStamp = null;
    /**
     * The substitution that unify the common term in the Task and the Belief
     * TODO unused
     */
    protected HashMap<Term, Term> substitute = null;

    /**
     * 用于「变量替换」中的「伪随机数生成器」
     */
    public static Random randomNumber = new Random(1);

    /**
     * 构造函数
     * * 🚩创建一个空的「推理上下文」
     *
     * @param memory 所反向引用的「记忆区」对象
     */
    public DerivationContext(final Memory memory) {
        this.memory = memory;
    }

    /**
     * 重置全局状态
     */
    public static void init() {
        randomNumber = new Random(1);
    }

    /**
     * 清理推导上下文
     * * 🎯便于断言性、学习性调试：各「推导上下文」字段的可空性、可变性
     */
    public void clear() {
        // * 🚩清理上下文变量
        this.currentTerm = null;
        this.currentConcept = null;
        this.currentTaskLink = null;
        this.currentTask = null;
        this.currentBeliefLink = null;
        this.currentBelief = null;
        this.newStamp = null;
        this.substitute = null;
        // * 🚩清理推理结果
        this.newTasks.clear();
        this.exportStrings.clear();
    }

    /**
     * Activated task called in MatchingRules.trySolution and
     * Concept.processGoal
     *
     * @param budget          The budget value of the new Task
     * @param sentence        The content of the new Task
     * @param candidateBelief The belief to be used in future inference, for
     *                        forward/backward correspondence
     */
    public void activatedTask(BudgetValue budget, Sentence sentence, Sentence candidateBelief) {
        Task task = new Task(sentence, budget, this.currentTask, sentence, candidateBelief);
        memory.getRecorder().append("!!! Activated: " + task.toString() + "\n");
        if (sentence.isQuestion()) {
            float s = task.getBudget().summary();
            // float minSilent = memory.getReasoner().getMainWindow().silentW.value() /
            // 100.0f;
            float minSilent = memory.getSilenceValue().get() / 100.0f;
            if (s > minSilent) { // only report significant derived Tasks
                report(task.getSentence(), ReportType.OUT);
            }
        }
        newTasks.add(task);
    }

    /**
     * Derived task comes from the inference rules.
     *
     * @param task the derived task
     */
    private void derivedTask(Task task) {
        if (task.getBudget().aboveThreshold()) {
            memory.getRecorder().append("!!! Derived: " + task + "\n");
            final float budget = task.getBudget().summary();
            // final float minSilent = memory.getReasoner()
            // .getMainWindow().silentW.value() / 100.0f;
            final float minSilent = memory.getSilenceValue().get() / 100.0f;
            if (budget > minSilent) { // only report significant derived Tasks
                report(task.getSentence(), ReportType.OUT);
            }
            newTasks.add(task);
        } else {
            memory.getRecorder().append("!!! Ignored: " + task + "\n");
        }
    }

    /* --------------- new task building --------------- */
    /**
     * Shared final operations by all double-premise rules, called from the
     * rules except StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth   The truth value of the sentence in task
     * @param newBudget  The budget value in task
     */
    public void doublePremiseTask(Term newContent, TruthValue newTruth, BudgetValue newBudget) {
        if (newContent != null) {
            final char newPunctuation = currentTask.getSentence().getPunctuation();
            final Sentence newSentence = new Sentence(newContent, newPunctuation, newTruth, newStamp);
            final Task newTask = new Task(newSentence, newBudget, currentTask, currentBelief);
            derivedTask(newTask);
        }
    }

    /**
     * Shared final operations by all double-premise rules, called from the
     * rules except StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth   The truth value of the sentence in task
     * @param newBudget  The budget value in task
     * @param revisable  Whether the sentence is revisable
     */
    public void doublePremiseTask(Term newContent, TruthValue newTruth, BudgetValue newBudget, boolean revisable) {
        if (newContent != null) {
            final Sentence taskSentence = currentTask.getSentence();
            final char newPunctuation = taskSentence.getPunctuation();
            final Sentence newSentence = new Sentence(newContent, newPunctuation, newTruth, newStamp, revisable);
            final Task newTask = new Task(newSentence, newBudget, currentTask, currentBelief);
            derivedTask(newTask);
        }
    }

    /**
     * Shared final operations by all single-premise rules, called in
     * StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth   The truth value of the sentence in task
     * @param newBudget  The budget value in task
     */
    public void singlePremiseTask(Term newContent, TruthValue newTruth, BudgetValue newBudget) {
        singlePremiseTask(newContent, currentTask.getSentence().getPunctuation(), newTruth, newBudget);
    }

    /**
     * Shared final operations by all single-premise rules, called in
     * StructuralRules
     *
     * @param newContent  The content of the sentence in task
     * @param punctuation The punctuation of the sentence in task
     * @param newTruth    The truth value of the sentence in task
     * @param newBudget   The budget value in task
     */
    public void singlePremiseTask(Term newContent, char punctuation, TruthValue newTruth, BudgetValue newBudget) {
        Task parentTask = currentTask.getParentTask();
        if (parentTask != null && newContent.equals(parentTask.getContent())) { // circular structural inference
            return;
        }
        Sentence taskSentence = currentTask.getSentence();
        // final Stamp newStamp; // * 📝实际上并不需要动
        if (taskSentence.isJudgment() || currentBelief == null) {
            newStamp = new Stamp(taskSentence.getStamp(), memory.getTime());
        } else { // to answer a question with negation in NAL-5 --- move to activated task?
            newStamp = new Stamp(currentBelief.getStamp(), memory.getTime());
        }
        Sentence newSentence = new Sentence(newContent, punctuation, newTruth, newStamp,
                taskSentence.getRevisable());
        Task newTask = new Task(newSentence, newBudget, currentTask, null);
        derivedTask(newTask);
    }

    /**
     * Display input/output sentence in the output channels. The only place to
     * add Objects into exportStrings. Currently only Strings are added, though
     * in the future there can be outgoing Tasks; also if exportStrings is empty
     * display the current value of timer ( exportStrings is emptied in
     * {@link ReasonerBatch#doTick()} - TODO fragile mechanism)
     *
     * @param sentence the sentence to be displayed
     * @param input    whether the task is input
     */
    public void report(Sentence sentence, ReportType type) {
        if (ReasonerBatch.DEBUG) {
            System.out.println("// report( clock " + memory.getTime()
            // + ", input " + input
                    + ", timer " + memory.getTimer()
                    + ", Sentence " + sentence
                    + ", exportStrings " + exportStrings);
            System.out.flush();
        }
        if (exportStrings.isEmpty()) {
            long timer = memory.updateTimer();
            if (timer > 0) {
                exportStrings.add(String.valueOf(timer));
            }
        }
        String s = type.toString() + ": ";
        s += sentence.toStringBrief();
        exportStrings.add(s);
    }
}
