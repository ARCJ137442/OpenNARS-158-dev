package nars.control;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

import nars.entity.BudgetValue;
import nars.entity.Concept;
import nars.entity.Sentence;
import nars.entity.SentenceV1;
import nars.entity.Stamp;
import nars.entity.Task;
import nars.entity.TaskV1;
import nars.inference.Budget;
import nars.inference.Truth;
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
     * * 📝可空性：非空
     * * 📝可变性：可变 | 【2024-05-30 08:47:16】在「概念链接建立」的过程中需要
     */
    private Memory memory;

    public Memory getMemory() {
        return memory;
    }

    // * ❓记忆区在「直接推理」「转换推理」「概念推理」的过程中，是否仅为只读？
    // * ✅【2024-05-30 08:55:00】初步明确其可变性
    public Memory mutMemory() {
        return memory;
    }

    /**
     * 缓存的「当前时间」
     * * 🎯与「记忆区」解耦
     *
     * * ️📝可空性：非空
     * * 📝可变性：只读 | 仅构造时赋值
     * * 📝所有权：具所有权
     */
    private final long time;

    /**
     * 🆕访问「当前时间」
     * * 🎯用于在推理过程中构建「新时间戳」
     * * ️📝可空性：非空
     * * 📝可变性：只读
     */
    public long getTime() {
        return time;
    }

    /**
     * 获取「静默值」
     * * 🎯在「推理上下文」中无需获取「推理器」`getReasoner`
     * * ️📝可空性：非空
     * * 📝可变性：只读
     *
     * @return 静默值
     */
    public float getSilencePercent() {
        return silenceValue / 100.0f;
    }

    /**
     * 缓存的「静默值」
     * * 🚩【2024-05-30 09:02:10】现仅在构造时赋值，其余情况不变
     * * ️📝可空性：非空
     * * 📝可变性：只读
     * * 📝所有权：具所有权
     */
    protected final int silenceValue;

    /**
     * Actually means that there are no new Tasks
     * * 🚩【2024-05-21 11:50:51】现在从「记忆区」迁移而来
     * * ❓【2024-05-21 12:04:35】尚未实装：若靠「局部是否有结果」则会改变推理结果
     * * ️📝可空性：非空
     * * 📝可变性：只读
     * * 📝所有权：仅引用
     */
    public boolean noResult() {
        return newTasks.isEmpty();
    }

    /**
     * 用于「变量替换」中的「伪随机数生成器」
     * * ️📝可空性：非空
     * * 📝可变性：可变 | 在「打乱集合」时被`shuffle`函数修改
     * * 📝所有权：具所有权
     */
    public static Random randomNumber = new Random(1);

    /* ---------- Short-term workspace for a single cycle ---------- */
    /**
     * List of new tasks accumulated in one cycle, to be processed in the next
     * cycle
     * * 🚩【2024-05-18 17:29:40】在「记忆区」与「推理上下文」中各有一个，但语义不同
     * * 📌「记忆区」的跨越周期，而「推理上下文」仅用于存储
     * * ️📝可空性：非空
     * * 📝可变性：可变 | 单次推理的结果存放至此
     * * 📝所有权：具所有权
     */
    private final LinkedList<Task> newTasks;

    public LinkedList<Task> getNewTasks() {
        return newTasks;
    }

    /**
     * List of Strings or Tasks to be sent to the output channels
     * * 🚩【2024-05-18 17:29:40】在「记忆区」与「推理上下文」中各有一个，但语义不同
     * * 📌「记忆区」的跨越周期，而「推理上下文」仅用于存储
     * * ️📝可空性：非空
     * * 📝可变性：可变 | 单次推理的结果存放至此
     * * 📝所有权：具所有权
     */
    private final ArrayList<String> exportStrings;

    public ArrayList<String> getExportStrings() {
        return exportStrings;
    }

    /**
     * * 📝在所有使用场景中，均为「当前概念要处理的词项」且只读
     * * 🚩【2024-05-20 09:15:59】故此处仅保留getter，并且不留存多余字段（减少共享引用）
     * * ️📝可空性：非空
     * * 📝可变性：只读 | 完全依赖「当前概念」而定，且「当前概念」永不变更词项
     * * 📝所有权：仅引用
     */
    public Term getCurrentTerm() {
        // ! 🚩需要假定`this.getCurrentConcept() != null`
        return this.getCurrentConcept().getTerm();
    }

    /**
     * The selected Concept
     * * 🚩【2024-05-25 16:19:51】现在已经具备所有权
     * * ️📝可空性：非空
     * * 📝可变性：可变 | 「链接到任务」等
     * * 📝所有权：具所有权
     */
    private Concept currentConcept;

    public Concept getCurrentConcept() {
        return currentConcept;
    }

    /**
     * 🚩【2024-05-30 08:59:05】仅用于内部设定，外部不会也无法修改
     *
     * @param currentConcept
     */
    protected void setCurrentConcept(Concept currentConcept) {
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
     *
     * * ️📝可空性：可空
     * * 📝可变性：可变 | 仅切换值，不修改内部 @ 切换信念/修正
     * * 📝所有权：具所有权
     *
     * * 🚩【2024-05-30 09:25:15】内部不被修改，同时「语句」允许被随意复制（内容固定，占用小）
     */
    private Sentence currentBelief;

    public Sentence getCurrentBelief() {
        return currentBelief;
    }

    /** 🆕实用方法：用于简化「推理规则分派」的代码 */
    public boolean hasCurrentBelief() {
        return currentBelief != null;
    }

    /**
     * 设置当前信念
     * * 📝仅在「直接推理」之前、「概念推理」切换概念时用到
     */
    protected void setCurrentBelief(Sentence currentBelief) {
        this.currentBelief = currentBelief;
    }

    // ! 📌删除「新时间戳」：只需在推理的最后「导出结论」时构造

    /** 🆕产生新时间戳 from 单前提 */
    protected Stamp generateNewStampSingle() {
        if (this.getCurrentTask().isJudgment() || !this.hasCurrentBelief()) {
            return new Stamp(this.getCurrentTask(), memory.getTime());
        } else { // to answer a question with negation in NAL-5 --- move to activated task?
            return new Stamp(this.getCurrentBelief(), memory.getTime());
        }
    }

    /** 🆕产生新时间戳 from 双前提 */
    protected Stamp generateNewStampDouble() {
        // * 🚩使用「当前任务」和「当前信念」产生新时间戳
        return this.hasCurrentBelief()
                // * 🚩具有「当前信念」⇒直接合并
                ? Stamp.uncheckedMerge( // ! 此前已在`getBelief`处检查
                        this.getCurrentTask(),
                        // * 📌此处的「时间戳」一定是「当前信念」的时间戳
                        // * 📄理由：最后返回的信念与「成功时比对的信念」一致（只隔着`clone`）
                        this.getCurrentBelief(),
                        this.getTime())
                : null;
    }

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
        this.silenceValue = memory.getSilenceValue().get();
        this.time = memory.getTime();
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
    public void activatedTask(final Budget budget, final Sentence sentence, final Sentence candidateBelief) {
        // * 🚩回答问题后，开始从「信念」中生成新任务：以「当前任务」为父任务，以「候选信念」为父信念
        final BudgetValue newBudget = BudgetValue.from(budget);
        final Task task = new TaskV1(sentence, newBudget, this.getCurrentTask(), sentence, candidateBelief);
        memory.getRecorder().append("!!! Activated: " + task.toString() + "\n");
        // * 🚩若为「问题」⇒输出显著的「导出结论」
        if (sentence.isQuestion()) {
            final float s = task.budgetSummary();
            // float minSilent = memory.getReasoner().getMainWindow().silentW.value() /
            // 100.0f;
            if (s > this.getSilencePercent()) { // only report significant derived Tasks
                report(task, ReportType.OUT);
            }
        }
        // * 🚩将新创建的「导出任务」添加到「新任务」中
        newTasks.add(task);
    }

    /**
     * Derived task comes from the inference rules.
     *
     * @param task the derived task
     */
    private void derivedTask(Task task) {
        // * 🚩判断「导出的新任务」是否有价值
        if (!task.budgetAboveThreshold()) {
            memory.getRecorder().append("!!! Ignored: " + task + "\n");
            return;
        }
        // * 🚩报告
        memory.getRecorder().append("!!! Derived: " + task + "\n");
        final float budget = task.budgetSummary();
        // final float minSilent = memory.getReasoner()
        // .getMainWindow().silentW.value() / 100.0f;
        if (budget > this.getSilencePercent()) { // only report significant derived Tasks
            report(task, ReportType.OUT);
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
    public void doublePremiseTask(Term newContent, Truth newTruth, Budget newBudget) {
        // * 🚩引入「当前任务」与「新时间戳」
        doublePremiseTask(this.getCurrentTask(), newContent, newTruth, newBudget, this.generateNewStampDouble());
    }

    /**
     * 🆕其直接调用来自组合规则
     * * 🎯避免对`currentTask`的赋值，解耦调用（并让`currentTask`不可变）
     * * 🎯避免对`newStamp`的复制，解耦调用（让「新时间戳」的赋值止步在「推理开始」之前）
     *
     * @param currentTask
     * @param newContent
     * @param newTruth
     * @param newBudget
     * @param newStamp
     */
    public void doublePremiseTask(
            final Task currentTask,
            final Term newContent,
            final Truth newTruth,
            final Budget newBudget,
            final Stamp newStamp) {
        if (newContent == null)
            return;
        // * 🚩仅在「任务内容」可用时构造
        final char newPunctuation = currentTask.getPunctuation();
        final Sentence newSentence = new SentenceV1(newContent, newPunctuation, newTruth, newStamp, true);
        final Task newTask = new TaskV1(newSentence, newBudget, this.getCurrentTask(), this.currentBelief);
        derivedTask(newTask);
    }

    /** 🆕重定向 */
    public void doublePremiseTask(Term newContent, Truth newTruth, Budget newBudget, boolean revisable) {
        doublePremiseTask(newContent, generateNewStampDouble(), newTruth, newBudget, revisable);
    }

    /**
     * Shared final operations by all double-premise rules, called from the
     * rules except StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth   The truth value of the sentence in task
     * @param newBudget  The budget value in task
     * @param newStamp   The stamp in sentence
     * @param revisable  Whether the sentence is revisable
     */

    private void doublePremiseTask(
            final Term newContent,
            final Stamp newStamp,
            final Truth newTruth,
            final Budget newBudget,
            final boolean revisable) {
        if (newContent == null)
            return;

        // * 🚩仅在「任务内容」可用时构造
        final Sentence taskSentence = this.getCurrentTask();
        final char newPunctuation = taskSentence.getPunctuation();
        final Sentence newSentence = new SentenceV1(newContent, newPunctuation, newTruth, newStamp, revisable);
        final Task newTask = new TaskV1(newSentence, newBudget, this.getCurrentTask(), currentBelief);
        derivedTask(newTask);
    }

    /**
     * Shared final operations by all single-premise rules, called in
     * StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth   The truth value of the sentence in task
     * @param newBudget  The budget value in task
     */
    public void singlePremiseTask(Term newContent, Truth newTruth, Budget newBudget) {
        singlePremiseTask(newContent, this.getCurrentTask().getPunctuation(), newTruth, newBudget);
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
    public void singlePremiseTask(Term newContent, char punctuation, Truth newTruth, Budget newBudget) {
        final Task parentTask = this.getCurrentTask().getParentTask();
        // * 🚩对于「结构转换」的单前提推理，若已有父任务且该任务与父任务相同⇒中止，避免重复推理
        if (parentTask != null && newContent.equals(parentTask.getContent()))
            return; // to avoid circular structural inference
        final Sentence taskSentence = this.getCurrentTask();
        // * 🚩构造新时间戳
        final Stamp newStamp = this.generateNewStampSingle();
        // * 🚩使用新内容构造新语句
        final Sentence newSentence = new SentenceV1(
                newContent, punctuation,
                newTruth, newStamp,
                taskSentence.getRevisable());
        // * 🚩构造新任务
        final Task newTask = new TaskV1(newSentence, newBudget, this.getCurrentTask(), null);
        // * 🚩导出
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
        // ! ⚠️由于「语句」和「任务」的扁平化（`.getSentence()`的消失），此处将直接打印作为「语句」的「任务」
        // * 💭思想：「任务」也是一种「语句」，只不过带了「物品」特性，可以被「袋」分派而已
        return type.toString() + ": " + sentence.toStringBrief();
    }

    /**
     * 让「记忆区」吸收「推理上下文」
     * * 🚩【2024-05-19 18:39:44】现在会在每次「准备上下文⇒推理」的过程中执行
     * * 🎯变量隔离，防止「上下文串线」与「重复使用」
     * * 📌传入所有权而非引用
     * * 🚩【2024-05-21 23:17:57】现在迁移到「推理上下文」处，以便进行方法分派
     */
    public void absorbedByMemory(final Memory memory) {
        // * 🚩销毁「当前信念」 | 变量值仅临时推理用
        this.currentBelief = null;
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
        // * 🚩清理上下文防串（同时清理「导出的新任务」与「导出字串」）
        this.getNewTasks().clear();
        this.getExportStrings().clear();
        // * 🚩销毁自身：在此处销毁相应变量
        drop(this.getNewTasks());
        drop(this.getExportStrings());
    }

    /**
     * 默认就是被「自身所属记忆区」吸收
     * * 📝【2024-05-30 08:48:15】此处的「记忆区」可变，因为要从「上下文」中获取结果
     * * 🚩【2024-05-30 08:48:29】此方法仅为分派需要，实际上要先将引用解耦
     */
    public void absorbedByMemory() {
        absorbedByMemory(this.mutMemory());
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
