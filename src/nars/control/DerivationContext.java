package nars.control;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

import nars.entity.BudgetValue;
import nars.entity.Concept;
import nars.entity.Judgement;
import nars.entity.Sentence;
import nars.entity.SentenceV1;
import nars.entity.Stamp;
import nars.entity.Task;
import nars.entity.TaskV1;
import nars.inference.Budget;
import nars.inference.Truth;
import nars.language.Term;
import nars.main.Reasoner;
import nars.storage.Memory;

/**
 * 🆕新的「推理上下文」对象
 * * 📄仿自OpenNARS 3.1.0
 */
public interface DerivationContext {

    /** 🆕内置公开结构体，用于公共读取 */
    public static final class DerivationContextCore {

        /**
         * 缓存的「当前时间」
         * * 🎯与「记忆区」解耦
         *
         * * ️📝可空性：非空
         * * 📝可变性：只读 | 仅构造时赋值
         * * 📝所有权：具所有权
         */
        final long time;

        /**
         * 缓存的「静默值」
         * * 🚩【2024-05-30 09:02:10】现仅在构造时赋值，其余情况不变
         * * ️📝可空性：非空
         * * 📝可变性：只读
         * * 📝所有权：具所有权
         */
        private final int silenceValue;

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
        final LinkedList<Task> newTasks;

        /**
         * List of Strings or Tasks to be sent to the output channels
         * * 🚩【2024-05-18 17:29:40】在「记忆区」与「推理上下文」中各有一个，但语义不同
         * * 📌「记忆区」的跨越周期，而「推理上下文」仅用于存储
         * * ️📝可空性：非空
         * * 📝可变性：可变 | 单次推理的结果存放至此
         * * 📝所有权：具所有权
         */
        final ArrayList<String> exportStrings;

        /**
         * * 🆕用于在「被吸收」时加入「推理记录器」的字符串集合
         *
         * * ️📝可空性：非空
         * * 📝可变性：可变 | 单次推理的结果存放至此
         * * 📝所有权：具所有权
         */
        final ArrayList<String> stringsToRecord;

        /**
         * The selected Concept
         * * 🚩【2024-05-25 16:19:51】现在已经具备所有权
         *
         * * ️📝可空性：非空
         * * 📝可变性：可变 | 「链接到任务」等
         * * 📝所有权：具所有权
         */
        final Concept currentConcept;

        /**
         * 用于「变量替换」中的「伪随机数生成器」
         * * ️📝可空性：非空
         * * 📝可变性：可变 | 在「打乱集合」时被`shuffle`函数修改
         * * 📝所有权：具所有权
         */
        public static Random randomNumber = new Random(1);

        /**
         * 构造函数
         * * 🚩创建一个空的「推理上下文」，默认所有参数为空
         *
         * @param memory 所反向引用的「记忆区」对象
         */
        DerivationContextCore(final Reasoner reasoner, final Concept currentConcept) {
            this(reasoner, currentConcept, new LinkedList<>(), new ArrayList<>());
        }

        /**
         * 🆕带参初始化
         * * 🚩包含所有`final`变量，避免「创建后赋值」如「复制时」
         *
         * @param memory
         */
        DerivationContextCore(
                final Reasoner reasoner,
                final Concept currentConcept,
                final LinkedList<Task> newTasks,
                final ArrayList<String> exportStrings) {
            // this.memory = reasoner.getMemory();
            this.currentConcept = currentConcept;
            this.silenceValue = reasoner.getSilenceValue().get();
            this.time = reasoner.getTime();
            this.newTasks = newTasks;
            this.exportStrings = exportStrings;
            this.stringsToRecord = new ArrayList<>();
        }

        /** 🆕共用的静态方法 */
        public void absorbedByReasoner(final Reasoner reasoner) {
            final Memory memory = reasoner.getMemory();
            // * 🚩将「当前概念」归还到「推理器」中
            memory.putBackConcept(this.currentConcept);
            // * 🚩将推理导出的「新任务」添加到自身新任务中（先进先出）
            for (final Task newTask : this.newTasks) {
                reasoner.mut_newTasks().add(newTask);
            }
            // * 🚩将推理导出的「导出字串」添加到自身「导出字串」中（先进先出）
            for (final String output : this.exportStrings) {
                reasoner.report(output);
            }
            // * 🚩将推理导出的「报告字串」添加到自身「报告字串」中（先进先出）
            for (final String message : this.stringsToRecord) {
                reasoner.getRecorder().append(message);
            }
            // * 🚩清理上下文防串（同时清理「导出的新任务」与「导出字串」）
            this.newTasks.clear();
            this.exportStrings.clear();
            // * 🚩销毁自身：在此处销毁相应变量
            drop(this.newTasks);
            drop(this.exportStrings);
        }

        /** 🆕对上层暴露的方法 */
        float getSilencePercent() {
            return this.silenceValue / 100.0f;
        }

    }

    /**
     * 🆕获取记忆区（不可变引用）
     */
    public Memory getMemory();

    /**
     * 🆕访问「当前时间」
     * * 🎯用于在推理过程中构建「新时间戳」
     * * ️📝可空性：非空
     * * 📝可变性：只读
     */
    public long getTime();

    /**
     * 获取「静默值」
     * * 🎯在「推理上下文」中无需获取「推理器」`getReasoner`
     * * ️📝可空性：非空
     * * 📝可变性：只读
     *
     * @return 静默值
     */
    public float getSilencePercent();

    /**
     * Actually means that there are no new Tasks
     * * 🚩【2024-05-21 11:50:51】现在从「记忆区」迁移而来
     * * ❓【2024-05-21 12:04:35】尚未实装：若靠「局部是否有结果」则会改变推理结果
     * * ️📝可空性：非空
     * * 📝可变性：只读
     * * 📝所有权：仅引用
     */
    public default boolean noResult() {
        return getNewTasks().isEmpty();
    }

    public LinkedList<Task> getNewTasks();

    public ArrayList<String> getExportStrings();

    public ArrayList<String> getStringsToRecord();

    public Concept getCurrentConcept();

    /**
     * * 📝在所有使用场景中，均为「当前概念要处理的词项」且只读
     * * 🚩【2024-05-20 09:15:59】故此处仅保留getter，并且不留存多余字段（减少共享引用）
     * * ️📝可空性：非空
     * * 📝可变性：只读 | 完全依赖「当前概念」而定，且「当前概念」永不变更词项
     * * 📝所有权：仅引用
     */
    public default Term getCurrentTerm() {
        // ! 🚩需要假定`this.getCurrentConcept() != null`
        return this.getCurrentConcept().getTerm();
    }

    /**
     * The selected task
     * * 🚩【2024-05-21 22:40:21】现在改为抽象方法：不同实现有不同的用法
     * * 📄「直接推理上下文」将其作为字段，而「转换推理上下文」「概念推理上下文」均只用作「当前任务链的目标」
     */
    public abstract Task getCurrentTask();

    /**
     * 重置全局状态
     */
    public static void init() {
        DerivationContextCore.randomNumber = new Random(1);
    }

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
        final Task task = new TaskV1(newTask, newBudget, this.getCurrentTask(), newTask, candidateBelief);
        this.getStringsToRecord().add("!!! Activated: " + task.toString() + "\n");
        // * 🚩若为「问题」⇒输出显著的「导出结论」
        if (newTask.isQuestion()) {
            final float s = task.budgetSummary();
            if (s > this.getSilencePercent()) { // only report significant derived Tasks
                report(task, ReportType.OUT);
            }
        }
        // * 🚩将新创建的「导出任务」添加到「新任务」中
        this.getNewTasks().add(task);
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
            this.getStringsToRecord().add("!!! Ignored: " + task + "\n");
            return;
        }
        // * 🚩报告
        this.getStringsToRecord().add("!!! Derived: " + task + "\n");
        final float budget = task.budgetSummary();
        if (budget > this.getSilencePercent()) { // only report significant derived Tasks
            report(task, ReportType.OUT);
        }
        // * 🚩将「导出的新任务」添加到「新任务表」中
        this.getNewTasks().add(task);
    }

    /** 🆕仅源自「修正规则」调用，没有「父信念」 */
    public default void doublePremiseTaskRevision(
            final Task currentTask,
            final Term newContent,
            final Truth newTruth,
            final Budget newBudget,
            final Stamp newStamp) {
        if (newContent == null)
            return;
        // * 🚩仅在「任务内容」可用时构造
        final char newPunctuation = currentTask.getPunctuation();
        final Sentence newSentence = SentenceV1.newSentenceFromPunctuation(newContent, newPunctuation, newTruth,
                newStamp, true);
        final Task newTask = new TaskV1(newSentence, newBudget, this.getCurrentTask(), null);
        derivedTask(newTask);
    }

    /**
     * 🆕此处「报告」与记忆区的「报告」不同
     * * 🚩记忆区在「吸收上下文」时产生记忆区的「报告」
     * * 📌原则：此处不应涉及有关「记忆区」的内容
     */
    public default void report(Sentence sentence, ReportType type) {
        final String s = generateReportString(sentence, type);
        this.getExportStrings().add(s);
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
     * 让「推理器」吸收「推理上下文」
     * * 🚩【2024-05-19 18:39:44】现在会在每次「准备上下文⇒推理」的过程中执行
     * * 🎯变量隔离，防止「上下文串线」与「重复使用」
     * * 📌传入所有权而非引用
     * * 🚩【2024-05-21 23:17:57】现在迁移到「推理上下文」处，以便进行方法分派
     */
    public void absorbedByReasoner(final Reasoner reasoner);

    // /**
    // * 默认就是被「自身所属推理器」吸收
    // * * 📝【2024-05-30 08:48:15】此处的「推理器」可变，因为要从「上下文」中获取结果
    // * * 🚩【2024-05-30 08:48:29】此方法仅为分派需要，实际上要先将引用解耦
    // */
    // public void absorbedByReasoner() {
    // this.absorbedByReasoner(this.mutMemory());
    // }

    static void drop(Object any) {
    }

    /**
     * 获取「已存在的概念」
     * * 🎯让「概念推理」可以在「拿出概念」的时候运行，同时不影响具体推理过程
     * * 🚩先与「当前概念」做匹配，若没有再在记忆区中寻找
     * * 📌【2024-05-24 22:07:42】目前专供「推理规则」调用
     */
    public default Concept termToConcept(Term term) {
        if (term.equals(this.getCurrentTerm()))
            return this.getCurrentConcept();
        else
            return this.getMemory().termToConcept(term);
    }
}
