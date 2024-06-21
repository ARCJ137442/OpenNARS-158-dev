package nars.entity;

import nars.inference.Budget;
import nars.inference.BudgetInference;
import nars.language.Term;

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
     */
    public Task(Sentence sentence, Budget budget, Task parentTask, Judgement parentBelief, Judgement solution) {
        this.token = new Token(sentence.toKey(), budget); // change to toKey()
        this.sentence = sentence;
        // this.key = this.sentence.toKey(); // * ❌无需使用：s.toKey()与此相通
        this.parentTask = parentTask;
        this.parentBelief = parentBelief;
        this.bestSolution = solution;
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

    // impl OptionalTruth for SentenceV1

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

    // impl Stamp for Task

    @Override
    public long[] __evidentialBase() {
        return this.sentence.__evidentialBase();
    }

    @Override
    public long __creationTime() {
        return this.sentence.__creationTime();
    }

    // impl Task for Task

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
        if (!judgment.isJudgement())
            throw new AssertionError(judgment + " is not judgment");
        // * 🚩【2024-06-01 16:37:47】遵照原意，不复制
        this.bestSolution = judgment;
        // this.bestSolution = judgment.cloneSentence();
    }

    // impl ToStringBriefAndLong for Task

    @Override
    public String toStringBrief() {
        return this.taskToStringBrief();
    }

    @Override
    public String toString() {
        return this.taskToString();
    }

    @Override
    public String toStringLong() {
        return this.taskToStringLong();
    }

    // impl Sentence for Task

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

    public boolean isInput() {
        return this.getParentTask() == null;
    }

    @Override
    public void mergeBudget(Budget that) {
        final Budget that1 = that;
        if (!(that1 instanceof Task))
            throw new AssertionError(that1 + " isn't a Task");
        // * 🚩均为「任务」⇒按照「发生时间」决定「谁并入谁」
        if (getCreationTime() >= ((Task) that1).getCreationTime())
            // * ⚠️改成接口后无法使用`super.method`调用默认方法
            // * 🚩【2024-06-05 00:25:49】现在可直接使用「获取预算」而无需强制要求基于「Token」
            // * 🚩【2024-06-07 13:52:15】目前直接内联接口的默认方法
            BudgetInference.merge(this, that1);
        else
            BudgetInference.merge(that1, this);
    }

    public String taskToString() {
        final StringBuilder s = new StringBuilder();
        final String superString = this.budgetToString() + " " + getKey().toString();
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

    public String taskToStringBrief() {
        return this.budgetToStringBrief() + " " + getKey();
    }

    public String taskToStringLong() {
        return taskToString();
    }
}
