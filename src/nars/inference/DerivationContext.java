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
import nars.entity.TruthValue;
import nars.language.Term;
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
    private Memory memory;

    public Memory getMemory() {
        return memory;
    }

    /**
     * 🆕访问「当前时间」
     * * 🎯用于在推理过程中构建「新时间戳」
     */
    public long getTime() {
        return memory.getTime();
    }

    /**
     * 用于「变量替换」中的「伪随机数生成器」
     */
    public static Random randomNumber = new Random(1);

    /* ---------- Short-term workspace for a single cycle ---------- */
    /**
     * List of new tasks accumulated in one cycle, to be processed in the next
     * cycle
     * * 🚩【2024-05-18 17:29:40】在「记忆区」与「推理上下文」中各有一个，但语义不同
     * * 📌「记忆区」的跨越周期，而「推理上下文」仅用于存储
     */
    private final LinkedList<Task> newTasks;

    public LinkedList<Task> getNewTasks() {
        return newTasks;
    }

    /**
     * List of Strings or Tasks to be sent to the output channels
     * * 🚩【2024-05-18 17:29:40】在「记忆区」与「推理上下文」中各有一个，但语义不同
     * * 📌「记忆区」的跨越周期，而「推理上下文」仅用于存储
     */
    private final ArrayList<String> exportStrings;

    public ArrayList<String> getExportStrings() {
        return exportStrings;
    }

    /**
     * * 📝在所有使用场景中，均为「当前概念要处理的词项」且只读
     * * 🚩【2024-05-20 09:15:59】故此处仅保留getter，并且不留存多余字段（减少共享引用）
     */
    public Term getCurrentTerm() {
        // ! 🚩需要假定`this.getCurrentConcept() != null`
        return this.getCurrentConcept().getTerm();
    }

    /**
     * The selected Concept
     */
    private Concept currentConcept = null;

    public Concept getCurrentConcept() {
        return currentConcept;
    }

    public void setCurrentConcept(Concept currentConcept) {
        this.currentConcept = currentConcept;
    }

    /**
     * The selected Task
     */
    private Task currentTask = null;

    public Task getCurrentTask() {
        return currentTask;
    }

    /**
     * 设置当前任务
     * * 📝仅在「开始推理」之前设置，但在「直接推理」「概念推理」中均出现
     * * ⚠️并且，在两种推理中各含不同语义：「直接推理」作为唯一根据（不含任务链），而「概念推理」则是「任务链」的目标
     * * ✅已解决「在『组合规则』中设置『当前任务』」的例外
     */
    public void setCurrentTask(Task currentTask) {
        this.currentTask = currentTask;
    }

    /**
     * The selected belief
     */
    private Sentence currentBelief = null;

    public Sentence getCurrentBelief() {
        return currentBelief;
    }

    /**
     * 设置当前任务
     * * 📝在「概念推理」仅在准备阶段设置
     * * 📝在「直接推理」会在推理过程中设置
     */
    public void setCurrentBelief(Sentence currentBelief) {
        this.currentBelief = currentBelief;
    }

    /**
     * The new Stamp
     */
    private Stamp newStamp = null;

    public Stamp getNewStamp() {
        return newStamp;
    }

    public void setNewStamp(Stamp newStamp) {
        this.newStamp = newStamp;
    }

    /**
     * The substitution that unify the common term in the Task and the Belief
     * TODO unused
     */
    private HashMap<Term, Term> substitute = null;

    public HashMap<Term, Term> getSubstitute() {
        return substitute;
    }

    // public void setSubstitute(HashMap<Term, Term> substitute) {
    // this.substitute = substitute;
    // }

    /**
     * 构造函数
     * * 🚩创建一个空的「推理上下文」，默认所有参数为空
     *
     * @param memory 所反向引用的「记忆区」对象
     */
    public DerivationContext(final Memory memory) {
        this(memory, new LinkedList<>(), new ArrayList<>());
    }

    /**
     * 🆕带参初始化
     * * 🚩包含所有`final`变量，避免「创建后赋值」如「复制时」
     *
     * @param memory
     */
    protected DerivationContext(final Memory memory,
            final LinkedList<Task> newTasks,
            final ArrayList<String> exportStrings) {
        this.memory = memory;
        this.newTasks = newTasks;
        this.exportStrings = exportStrings;
    }

    /**
     * 重置全局状态
     */
    public static void init() {
        randomNumber = new Random(1);
    }

    /**
     * 「复制」推导上下文
     * * 🚩只搬迁引用，并不更改所有权
     */
    public DerivationContext clone() {
        // * 🚩创建新上下文，并随之迁移`final`变量
        final DerivationContext self = new DerivationContext(this.memory, this.newTasks, this.exportStrings);
        // * 🚩搬迁引用
        // self.currentTerm = this.currentTerm;
        self.currentConcept = this.currentConcept;
        // self.currentTaskLink = this.currentTaskLink;
        self.currentTask = this.currentTask;
        // self.currentBeliefLink = this.currentBeliefLink;
        self.currentBelief = this.currentBelief;
        self.newStamp = this.newStamp;
        self.substitute = this.substitute;
        // * 🚩返回新上下文
        return self;
    }

    /**
     * 清理推导上下文
     * * 🎯便于断言性、学习性调试：各「推导上下文」字段的可空性、可变性
     */
    public void clear() {
        // * 🚩清理上下文变量
        // this.currentTerm = null;
        this.currentConcept = null;
        // this.currentTaskLink = null;
        this.currentTask = null;
        // this.currentBeliefLink = null;
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
     * * 📝仅被「答问」调用
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
        // * 🚩判断「导出的新任务」是否有价值
        if (!task.getBudget().aboveThreshold()) {
            memory.getRecorder().append("!!! Ignored: " + task + "\n");
            return;
        }
        // * 🚩报告
        {
            memory.getRecorder().append("!!! Derived: " + task + "\n");
            final float budget = task.getBudget().summary();
            // final float minSilent = memory.getReasoner()
            // .getMainWindow().silentW.value() / 100.0f;
            final float minSilent = memory.getSilenceValue().get() / 100.0f;
            if (budget > minSilent) { // only report significant derived Tasks
                report(task.getSentence(), ReportType.OUT);
            }
        }
        // * 🚩将「导出的新任务」添加到「新任务表」中
        newTasks.add(task);
    }

    /* --------------- new task building --------------- */
    /**
     * Shared final operations by all double-premise rules, called from the
     * rules except StructuralRules
     * * 🚩【2024-05-19 12:44:55】构造函数简化：导出的结论<b>始终可修正</b>
     *
     * @param newContent The content of the sentence in task
     * @param newTruth   The truth value of the sentence in task
     * @param newBudget  The budget value in task
     */
    public void doublePremiseTask(Term newContent, TruthValue newTruth, BudgetValue newBudget) {
        doublePremiseTask(this.currentTask, newContent, newTruth, newBudget);
    }

    /**
     * 🆕其直接调用来自组合规则
     * * 🎯避免对`currentTask`的赋值，解耦调用（并让`currentTask`不可变）
     *
     * @param currentTask
     * @param newContent
     * @param newTruth
     * @param newBudget
     */
    public void doublePremiseTask(Task currentTask, Term newContent, TruthValue newTruth, BudgetValue newBudget) {
        if (newContent != null) {
            final char newPunctuation = currentTask.getSentence().getPunctuation();
            final Sentence newSentence = new Sentence(newContent, newPunctuation, newTruth, this.newStamp, true);
            final Task newTask = new Task(newSentence, newBudget, this.currentTask, this.currentBelief);
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
        final Task parentTask = currentTask.getParentTask();
        if (parentTask != null && newContent.equals(parentTask.getContent())) { // circular structural inference
            return;
        }
        final Sentence taskSentence = currentTask.getSentence();
        // final Stamp newStamp; // * 📝实际上并不需要动
        if (taskSentence.isJudgment() || currentBelief == null) {
            this.newStamp = new Stamp(taskSentence.getStamp(), memory.getTime());
        } else { // to answer a question with negation in NAL-5 --- move to activated task?
            this.newStamp = new Stamp(currentBelief.getStamp(), memory.getTime());
        }
        final Sentence newSentence = new Sentence(newContent, punctuation, newTruth, newStamp,
                taskSentence.getRevisable());
        final Task newTask = new Task(newSentence, newBudget, currentTask, null);
        derivedTask(newTask);
    }

    /**
     * 🆕此处「报告」与记忆区的「报告」不同
     * * 🚩记忆区在「吸收上下文」时产生记忆区的「报告」
     * * 📌原则：此处不应涉及有关「记忆区」的内容
     */
    public void report(Sentence sentence, ReportType type) {
        final String s = generateReportString(sentence, type);
        exportStrings.add(s);
    }

    /**
     * 🆕生成「输出报告字符串」
     * * 🎯在「记忆区」与「推理上下文」中一同使用
     */
    public static String generateReportString(Sentence sentence, ReportType type) {
        return type.toString() + ": " + sentence.toStringBrief();
    }
}
