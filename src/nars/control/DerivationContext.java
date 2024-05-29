package nars.control;

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
public abstract class DerivationContext {

    /**
     * 对「记忆区」的反向引用
     * * 🚩【2024-05-18 17:00:12】目前需要访问其「输出」「概念」等功能
     */
    private Memory memory;

    public Memory getMemory() {
        return memory;
    }

    // TODO: 后续有待从`getMemory`中分离，以明确「记忆区」在各处推理中的可变性
    // * ❓记忆区在「直接推理」「转换推理」「概念推理」的过程中，是否仅为只读？
    public Memory mutMemory() {
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
     * 获取「静默值」
     * * 🎯在「推理上下文」中无需获取「推理器」`getReasoner`
     *
     * @return 静默值
     */
    public int getSilenceValue() {
        return memory.getSilenceValue().get();
    }

    // public MainWindow getMainWindow() {
    // return reasoner.getMainWindow();
    // }
    /**
     * Actually means that there are no new Tasks
     * * 🚩【2024-05-21 11:50:51】现在从「记忆区」迁移而来
     * * ❓【2024-05-21 12:04:35】尚未实装：若靠「局部是否有结果」则会改变推理结果
     */
    public boolean noResult() {
        return newTasks.isEmpty();
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
     * * 🚩【2024-05-25 16:19:51】现在已经具备所有权
     */
    private Concept currentConcept;

    public Concept getCurrentConcept() {
        return currentConcept;
    }

    public void setCurrentConcept(Concept currentConcept) {
        this.currentConcept = currentConcept;
    }

    /**
     * The selected task
     * * 🚩【2024-05-21 22:40:21】现在改为抽象方法：不同实现有不同的用法
     * * 📄「直接推理上下文」将其作为字段，而「转换推理上下文」「概念推理上下文」均只用作「当前任务链的目标」
     */
    public abstract Task getCurrentTask();

    /**
     * The selected belief
     */
    private Sentence currentBelief;

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
    private Stamp newStamp;

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
    private HashMap<Term, Term> substitute;

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
        Task task = new Task(sentence, budget, this.getCurrentTask(), sentence, candidateBelief);
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
        doublePremiseTask(this.getCurrentTask(), newContent, newTruth, newBudget);
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
            final Task newTask = new Task(newSentence, newBudget, this.getCurrentTask(), this.currentBelief);
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
            final Sentence taskSentence = this.getCurrentTask().getSentence();
            final char newPunctuation = taskSentence.getPunctuation();
            final Sentence newSentence = new Sentence(newContent, newPunctuation, newTruth, newStamp, revisable);
            final Task newTask = new Task(newSentence, newBudget, this.getCurrentTask(), currentBelief);
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
        singlePremiseTask(newContent, this.getCurrentTask().getSentence().getPunctuation(), newTruth, newBudget);
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
        final Task parentTask = this.getCurrentTask().getParentTask();
        if (parentTask != null && newContent.equals(parentTask.getContent())) { // circular structural inference
            return;
        }
        final Sentence taskSentence = this.getCurrentTask().getSentence();
        // final Stamp newStamp; // * 📝实际上并不需要动
        if (taskSentence.isJudgment() || currentBelief == null) {
            this.newStamp = new Stamp(taskSentence.getStamp(), memory.getTime());
        } else { // to answer a question with negation in NAL-5 --- move to activated task?
            this.newStamp = new Stamp(currentBelief.getStamp(), memory.getTime());
        }
        final Sentence newSentence = new Sentence(newContent, punctuation, newTruth, newStamp,
                taskSentence.getRevisable());
        final Task newTask = new Task(newSentence, newBudget, this.getCurrentTask(), null);
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

    /**
     * 让「记忆区」吸收「推理上下文」
     * * 🚩【2024-05-19 18:39:44】现在会在每次「准备上下文⇒推理」的过程中执行
     * * 🎯变量隔离，防止「上下文串线」与「重复使用」
     * * 📌传入所有权而非引用
     * * 🚩【2024-05-21 23:17:57】现在迁移到「推理上下文」处，以便进行方法分派
     */
    public void absorbedByMemory(Memory memory) {
        // TODO: 销毁「当前概念」「当前信念」「新时间戳」等（要考虑更多问题）
        // * 🚩将「当前概念」归还到「记忆区」中
        memory.putBackConcept(this.getCurrentConcept());
        // * 🚩将推理导出的「新任务」添加到自身新任务中（先进先出）
        for (final Task newTask : this.getNewTasks()) {
            memory.mut_newTasks().add(newTask);
        }
        // * 🚩将推理导出的「导出字串」添加到自身「导出字串」中（先进先出）
        for (final String output : this.getExportStrings()) {
            memory.report(output);
        }
        // * 清理上下文防串（同时清理「导出的新任务」与「导出字串」）
        this.getNewTasks().clear();
        this.getExportStrings().clear();
        // * 🚩销毁自身：在此处销毁相应变量
        drop(this.getNewTasks());
        drop(this.getExportStrings());
    }

    /**
     * 默认就是被「自身所属记忆区」吸收
     */
    public void absorbedByMemory() {
        absorbedByMemory(this.getMemory());
    }

    protected void drop(Object any) {
    }

    /**
     * 获取「已存在的概念」
     * * 🎯让「概念推理」可以在「拿出概念」的时候运行，同时不影响具体推理过程
     * * 🚩先与「当前概念」做匹配，若没有再在记忆区中寻找
     * * 📌【2024-05-24 22:07:42】目前专供「推理规则」调用
     */
    public Concept termToConcept(Term term) {
        if (term.equals(this.getCurrentTerm()))
            return this.getCurrentConcept();
        else
            return this.memory.termToConcept(term);
    }
}
