package nars.entity;

import nars.inference.Budget;
import nars.language.Term;
import nars.storage.Bag.MergeOrder;

/**
 * A task to be processed, consists of a Sentence and a BudgetValue
 */
public class Task implements Sentence, Item {

    // struct Task

    /**
     * The sentence of the Task
     * * 📝任务的「内容」
     *
     * * ️📝可空性：非空
     * * 📝可变性：不变 | 仅构造时，无需可变，「语句」类型可随意复制
     * * 📝所有权：具所有权
     */
    private final Sentence sentence;

    /**
     * 🆕Item令牌
     */
    private final Token token;

    /**
     * Task from which the Task is derived, or null if input
     *
     * * ️📝可空性：可空
     * * 📝可变性：不变 | 仅构造时，无需可变，「语句」类型可随意复制
     * * 📝所有权：共享引用
     */
    private final Task parentTask;

    /**
     * Belief from which the Task is derived, or null if derived from a theorem
     *
     * * ️📝可空性：可空
     * * 📝可变性：不变 | 仅构造时，无需可变，「语句」类型可随意复制
     * * 📝所有权：具所有权
     */
    private final Judgement parentBelief;

    /**
     * For Question and Goal: best solution found so far
     * * 📝一旦设定值后，永不回到空值状态
     *
     * * ️📝可空性：可空 | 可能暂无「最优解」
     * * 📝可变性：可变
     * * 📝所有权：具所有权 | 「语句」类型
     */
    private Judgement bestSolution;

    // impl Task

    /**
     * 完全构造函数
     * Constructor for an activated task
     *
     * @param sentence     The sentence
     * @param budget       The budget
     * @param parentTask   The task from which this new task is derived
     * @param parentBelief The belief from which this new task is derived
     * @param solution     The best solution found so far
     */
    public Task(Sentence sentence, Budget budget, Task parentTask, Judgement parentBelief, Judgement solution) {
        // * 🚩【2024-06-21 23:35:53】不要「太信得过外界所传入的对象」被共享引用：全部用clone隔绝所有权
        this.token = new Token(sentence.toKey(), budget); // * ✅此处的「预算」也是「零信任」 | change to toKey()
        this.sentence = sentence.sentenceClone();
        // this.key = this.sentence.toKey(); // * ❌无需使用：s.toKey()与此相通
        this.parentTask = parentTask; // * 🚩除了此处：共享所有权
        this.parentBelief = parentBelief == null ? null : (Judgement) parentBelief.sentenceClone();
        this.bestSolution = solution == null ? null : (Judgement) solution.sentenceClone();
    }

    /**
     * Constructor for input task
     *
     * @param sentence The sentence
     * @param budget   The budget
     */
    public Task(Sentence sentence, Budget budget) {
        this(sentence, budget, null, null, null);
    }

    /**
     * Constructor for a derived task
     *
     * @param sentence     The sentence
     * @param budget       The budget
     * @param parentTask   The task from which this new task is derived
     * @param parentBelief The belief from which this new task is derived
     */
    public Task(Sentence sentence, Budget budget, Task parentTask, Judgement parentBelief) {
        this(sentence, budget, parentTask, parentBelief, null);
    }

    // impl Task

    public Task getParentTask() {
        return this.parentTask;
    }

    public Judgement getParentBelief() {
        return this.parentBelief;
    }

    public Judgement getBestSolution() {
        return this.bestSolution;
    }

    public void setBestSolution(Judgement judgment) {
        if (!this.isQuestion())
            throw new AssertionError(this + " is not question");
        if (judgment == null)
            throw new AssertionError("judgment == null");
        // * 🚩【2024-06-01 16:37:47】遵照原意，不复制
        this.bestSolution = judgment;
        // this.bestSolution = judgment.cloneSentence();
    }

    public boolean isInput() {
        return this.getParentTask() == null;
    }

    /**
     * 决定两个「任务」之间的「合并顺序」
     * * 🚩 true ⇒ 改变顺序(this <- that)，并入that
     * * 🚩false ⇒ 维持原样(that <- this)，并入this
     *
     * @param that
     * @return
     */
    public static MergeOrder mergeOrder(final Task self, final Task that) {
        /*
         * 旧源码 @ Bag.java：
         * newItem.mergeBudget(oldItem);
         * * ⇒ this = newItem，此处传入的 this 在袋中相当于「新进入的任务」
         * * ⇒ that = oldItem，此处传入的 that 在袋中相当于「要移出的任务」
         */
        /*
         * 旧源码 @ Task.java：
         * // * 🚩均为「任务」⇒按照「发生时间」决定「谁并入谁」
         * if (getCreationTime() >= ((Task) that).getCreationTime())
         * // * ⚠️改成接口后无法使用`super.method`调用默认方法
         * // * 🚩【2024-06-05 00:25:49】现在可直接使用「获取预算」而无需强制要求基于「Token」
         * // * 🚩【2024-06-07 13:52:15】目前直接内联接口的默认方法
         * // * 📝自身「创建时间」晚于「要移出的任务」 ⇒ 将「要移出的任务」并入自身 ⇒ 旧任务并入新任务
         * // * 📝自身「创建时间」早于「要移出的任务」 ⇒ 将「要移出的任务」并入自身 ⇒ 新任务并入旧任务
         * BudgetInference.merge(this, that);
         * else
         * BudgetInference.merge(that, this);
         */
        return self.getCreationTime() < that.getCreationTime()
                // * 📝自身「创建时间」早于「要移出的任务」 ⇒ 将「要移出的任务」并入自身 ⇒ 新任务并入旧任务
                ? MergeOrder.NewToOld
                // * 📝自身「创建时间」晚于「要移出的任务」 ⇒ 将「要移出的任务」并入自身 ⇒ 旧任务并入新任务
                : MergeOrder.OldToNew;
    }

    // impl Budget for Task

    @Override
    public ShortFloat __priority() {
        return this.token.__priority();
    }

    @Override
    public ShortFloat __durability() {
        return this.token.__durability();
    }

    @Override
    public ShortFloat __quality() {
        return this.token.__quality();
    }

    // impl Item for Task

    @Override
    public String getKey() {
        return token.getKey();
    }

    // impl Evidential for Task

    @Override
    public long[] __evidentialBase() {
        return this.sentence.__evidentialBase();
    }

    @Override
    public long __creationTime() {
        return this.sentence.__creationTime();
    }

    // impl ToStringBriefAndLong for Task

    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder();
        final String superString = this.budgetToString() + " " + this.getKey().toString();
        s.append(superString).append(" ");
        s.append(this.stampToString());
        if (this.getParentTask() != null) {
            s.append("  \n from task: ").append(this.getParentTask().toStringBrief());
            if (this.getParentBelief() != null) {
                s.append("  \n from belief: ").append(this.getParentBelief().toStringBrief());
            }
        }
        if (this.getBestSolution() != null) {
            s.append("  \n solution: ").append(this.getBestSolution().toStringBrief());
        }
        return s.toString();
    }

    @Override
    public String toStringBrief() {
        return this.budgetToStringBrief() + " " + this.getKey();
    }

    @Override
    public String toStringLong() {
        return this.toString();
    }

    // impl Sentence for Task

    @Override
    public Term getContent() {
        return this.sentence.getContent();
    }

    @Override
    public char getPunctuation() {
        return this.sentence.getPunctuation();
    }

    @Override
    public Sentence sentenceClone() {
        return this.sentence.sentenceClone();
    }

    @Override
    public String toKey() {
        return this.sentence.toKey();
    }

    @Override
    public String sentenceToString() {
        return this.sentence.sentenceToString();
    }

    @Override
    public boolean isJudgement() {
        return this.sentence.isJudgement();
    }

    @Override
    public Judgement asJudgement() {
        return this.sentence.asJudgement();
    }

    @Override
    public boolean isQuestion() {
        return this.sentence.isQuestion();
    }

    @Override
    public Question asQuestion() {
        return this.sentence.asQuestion();
    }
}
