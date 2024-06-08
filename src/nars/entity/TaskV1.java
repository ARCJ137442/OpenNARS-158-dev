package nars.entity;

import nars.inference.Budget;
import nars.language.Term;

/**
 * A task to be processed, consists of a Sentence and a BudgetValue
 */
public class TaskV1 implements Task {

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

    // impl Truth for SentenceV1

    @Override
    public ShortFloat __frequency() {
        return this.sentence.__frequency();
    }

    @Override
    public ShortFloat __confidence() {
        return this.sentence.__confidence();
    }

    @Override
    public boolean __isAnalytic() {
        return this.sentence.__isAnalytic();
    }

    // impl Budget for TaskV1

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

    // impl Item for TaskV1

    @Override
    public String getKey() {
        return token.getKey();
    }

    // impl Sentence for TaskV1

    @Override
    public Term getContent() {
        return this.sentence.getContent();
    }

    @Override
    public char getPunctuation() {
        return this.sentence.getPunctuation();
    }

    @Override
    public boolean hasTruth() {
        return this.sentence.hasTruth();
    }

    @Override
    public Sentence sentenceClone() {
        return this.sentence.sentenceClone();
    }

    @Override
    public boolean __revisable() {
        return this.sentence.__revisable();
    }

    // impl Stamp for TaskV1

    @Override
    public long[] __evidentialBase() {
        return this.sentence.__evidentialBase();
    }

    @Override
    public long __creationTime() {
        return this.sentence.__creationTime();
    }

    // impl Task for TaskV1

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
    private final Sentence parentBelief;

    /**
     * For Question and Goal: best solution found so far
     * * 📝一旦设定值后，永不回到空值状态
     *
     * * ️📝可空性：可空 | 可能暂无「最优解」
     * * 📝可变性：可变
     * * 📝所有权：具所有权 | 「语句」类型
     */
    private Sentence bestSolution;

    @Override
    public Task getParentTask() {
        return this.parentTask;
    }

    @Override
    public Sentence getParentBelief() {
        return this.parentBelief;
    }

    @Override
    public Sentence getBestSolution() {
        return this.bestSolution;
    }

    @Override
    public void setBestSolution(Sentence judgment) {
        if (!this.isQuestion())
            throw new IllegalArgumentException(this + " is not question");
        if (judgment == null)
            throw new NullPointerException("judgment == null");
        if (!judgment.isJudgment())
            throw new IllegalArgumentException(judgment + " is not judgment");
        // * 🚩【2024-06-01 16:37:47】遵照原意，不复制
        this.bestSolution = judgment;
        // this.bestSolution = judgment.cloneSentence();
    }

    // other

    /**
     * 完全构造函数
     * Constructor for an activated task
     *
     * @param sentence     The sentence
     * @param budget       The budget
     * @param parentTask   The task from which this new task is derived
     * @param parentBelief The belief from which this new task is derived
     */
    public TaskV1(Sentence sentence, Budget budget, Task parentTask, Sentence parentBelief, Sentence solution) {
        this.token = new Token(sentence.toKey(), budget); // change to toKey()
        this.sentence = sentence;
        // this.key = this.sentence.toKey(); // * ❌无需使用：s.toKey()与此相通
        this.parentTask = parentTask;
        if (parentBelief != null && parentBelief.isQuestion())
            throw new IllegalArgumentException("父信念只能是「判断句」");
        this.parentBelief = parentBelief;
        this.bestSolution = solution;
    }

    /**
     * Constructor for input task
     *
     * @param sentence The sentence
     * @param budget   The budget
     */
    public TaskV1(Sentence sentence, Budget budget) {
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
    public TaskV1(Sentence sentence, Budget budget, Task parentTask, Sentence parentBelief) {
        this(sentence, budget, parentTask, parentBelief, null);
    }

    /**
     * Return a String representation of the Item after simplification
     *
     * @return A simplified String representation of the content
     */
    @Override
    public String toStringBrief() {
        return this.token.getBudgetValue().toStringBrief() + " " + getKey();
    }

    /**
     * Get a String representation of the Task
     *
     * @return The Task as a String
     */
    @Override
    public String toString() {
        return this.taskToString();
    }

    @Override
    public String toStringLong() {
        return this.taskToStringLong();
    }
}
