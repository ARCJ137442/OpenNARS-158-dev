package nars.control;

import nars.entity.BudgetValue;
import nars.entity.Evidential;
import nars.entity.Judgement;
import nars.entity.Sentence;
import nars.entity.SentenceV1;
import nars.entity.Stamp;
import nars.entity.Task;
import nars.entity.TruthValue;
import nars.inference.Budget;
import nars.inference.Truth;
import nars.inference.BudgetInference.BudgetInferenceTask;
import nars.language.Term;

/**
 * 🆕推理导出的结果
 * * 🎯用于「输入输出功能分别定义」
 *
 * TODO: 【2024-07-02 14:25:45】后续需要挪走其中的继承关系——消去「导出任务」中的「curentTask」
 */
public interface DerivationOut extends DerivationIn {

    public static class Derivation {

        /**
         * 所基于的「当前任务」
         * * 🎯用于`decomposeStatement`
         * * 🚩若为空，则自动补全为「当前任务」
         */
        public final Task currentTask;

        /** 新产生的任务词项（非空） */
        public final Term content;

        /** 新产生的任务真值（可空@反向推理） */
        public final Truth truth;

        /** 需要处理的「预算推理任务」 */
        public final BudgetInferenceTask budget;

        /**
         * 新产生的时间戳
         * * 🚩若为空，则根据上下文自动补全
         */
        public final Stamp stamp;

        public Derivation(Task currentTask, Term content, Truth truth, BudgetInferenceTask budget, Evidential stamp) {
            this.currentTask = currentTask;
            this.content = content;
            this.truth = TruthValue.from(truth); // 拷贝以分离所有权
            this.budget = budget;
            this.stamp = Stamp.from(stamp); // 拷贝以分离所有权
        }

        public Derivation(Term content, Truth truth, BudgetInferenceTask budget) {
            this(null, content, truth, budget, null);
        }

        public Derivation(Term content, Truth truth, BudgetInferenceTask budget, Evidential newStamp) {
            this(null, content, truth, budget, newStamp);
        }
    }

    /**
     * Actually means that there are no new Tasks
     * * 🚩【2024-05-21 11:50:51】现在从「记忆区」迁移而来
     * * ❓【2024-05-21 12:04:35】尚未实装：若靠「局部是否有结果」则会改变推理结果
     * * ️📝可空性：非空
     * * 📝可变性：只读
     * * 📝所有权：仅引用
     */
    public boolean noNewTask();

    /** 获取「新任务」的数量 */
    public int numNewTasks();

    /** 添加「新任务」 */
    public void addNewTask(Task newTask);

    public void addExportString(String exportedString);

    public void addStringToRecord(String stringToRecord);

    /**
     * Activated task called in MatchingRules.trySolution and
     * Concept.processGoal
     * * 📝仅被「答问」调用
     *
     * @param budget          The budget value of the new Task
     * @param newTask         The content of the new Task
     * @param candidateBelief The belief to be used in future inference, for
     *                        forward/backward correspondence
     */
    public default void activatedTask(final Budget budget, final Judgement newTask, final Judgement candidateBelief) {
        // * 🚩回答问题后，开始从「信念」中生成新任务：以「当前任务」为父任务，以「候选信念」为父信念
        final BudgetValue newBudget = BudgetValue.from(budget);
        final Task task = new Task(newTask, newBudget, this.getCurrentTask(), newTask, candidateBelief);
        this.addStringToRecord("!!! Activated: " + task.toString() + "\n");
        // * 🚩若为「问题」⇒输出显著的「导出结论」
        // * ❓【2024-06-26 20:14:00】貌似此处永不发生，禁用之
        if (newTask.isQuestion())
            throw new AssertionError("【2024-06-26 20:14:19】目前只有「判断句」会参与「任务激活」");
        // if (newTask.isQuestion()) {
        // final float s = task.budgetSummary();
        // if (s > this.getSilencePercent()) { // only report significant derived Tasks
        // report(task, ReportType.OUT);
        // }
        // }
        // * 🚩将新创建的「导出任务」添加到「新任务」中
        this.addNewTask(task);
    }

    /* --------------- new task building --------------- */

    /**
     * Derived task comes from the inference rules.
     *
     * @param task the derived task
     */
    default void derivedTask(Task task) {
        // * 🚩判断「导出的新任务」是否有价值
        if (!task.budgetAboveThreshold()) {
            this.addStringToRecord("!!! Ignored: " + task + "\n");
            return;
        }
        // * 🚩报告
        this.addStringToRecord("!!! Derived: " + task + "\n");
        final float budget = task.budgetSummary();
        if (budget > this.getSilencePercent()) { // only report significant derived Tasks
            report(task, ReportType.OUT);
        }
        // * 🚩将「导出的新任务」添加到「新任务表」中
        this.addNewTask(task);
    }

    /** 🆕仅源自「修正规则」调用，没有「父信念」 */
    public default void doublePremiseTaskRevision(
            final Term newContent,
            final Truth newTruth,
            final Budget newBudget,
            final Stamp newStamp) {
        if (newContent == null)
            return;
        // * 🚩仅在「任务内容」可用时构造
        final Task currentTask = this.getCurrentTask();
        final char newPunctuation = currentTask.getPunctuation();
        final Sentence newSentence = SentenceV1.newSentenceFromPunctuation(
                newContent,
                newPunctuation, newTruth,
                newStamp, true);
        final Task newTask = new Task(newSentence, newBudget, this.getCurrentTask(), null);
        derivedTask(newTask);
    }

    /**
     * 🆕此处「报告」与记忆区的「报告」不同
     * * 🚩记忆区在「吸收上下文」时产生记忆区的「报告」
     * * 📌原则：此处不应涉及有关「记忆区」的内容
     */
    public default void report(Sentence sentence, ReportType type) {
        final String s = generateReportString(sentence, type);
        this.addExportString(s);
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

    public void sendDerivation(Derivation derivation);

    /**
     * 过程：导出结论⇒各类`XXXPremiseTask`
     * * 📌在「具体推理规则」运行后执行
     */
    public void handleDerivation(Derivation derivation);
}
