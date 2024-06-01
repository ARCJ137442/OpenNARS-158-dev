package nars.entity;

import nars.language.Term;

/**
 * A task to be processed, consists of a Sentence and a BudgetValue
 */
public class Task implements Sentence, Item {

    /**
     * 🆕Item令牌
     */
    private final Token token;

    @Override
    public String getKey() {
        return token.getKey();
    }

    @Override
    public BudgetValue getBudget() {
        return token.getBudget();
    }

    /**
     * The sentence of the Task
     * * 📝任务的「内容」
     *
     * * ️📝可空性：非空
     * * 📝可变性：不变 | 仅构造时，无需可变，「语句」类型可随意复制
     * * 📝所有权：具所有权
     */
    private final Sentence sentence;

    @Override
    public Term __content() {
        return sentence.__content();
    }

    @Override
    public char __punctuation() {
        return sentence.__punctuation();
    }

    @Override
    public TruthValue __truth() {
        return sentence.__truth();
    }

    @Override
    public Stamp __stamp() {
        return sentence.__stamp();
    }

    @Override
    public boolean __revisable() {
        return sentence.__revisable();
    }

    @Override
    public Sentence cloneSentence() {
        return this.sentence.cloneSentence();
    }

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

    /**
     * 完全构造函数
     * Constructor for an activated task
     *
     * @param s            The sentence
     * @param b            The budget
     * @param parentTask   The task from which this new task is derived
     * @param parentBelief The belief from which this new task is derived
     */
    public Task(Sentence s, BudgetValue b, Task parentTask, Sentence parentBelief, Sentence solution) {
        this.token = new Token(s.toKey(), b); // change to toKey()
        this.sentence = s;
        // this.key = this.sentence.toKey(); // * ❌无需使用：s.toKey()与此相通
        this.parentTask = parentTask;
        this.parentBelief = parentBelief;
        this.bestSolution = solution;
    }

    /**
     * Constructor for input task
     *
     * @param s The sentence
     * @param b The budget
     */
    public Task(Sentence s, BudgetValue b) {
        this(s, b, null, null, null);
    }

    /**
     * Constructor for a derived task
     *
     * @param s            The sentence
     * @param b            The budget
     * @param parentTask   The task from which this new task is derived
     * @param parentBelief The belief from which this new task is derived
     */
    public Task(Sentence s, BudgetValue b, Task parentTask, Sentence parentBelief) {
        this(s, b, parentTask, parentBelief, null);
    }

    /**
     * Get the sentence
     *
     * @return The sentence
     */
    public Sentence getSentence() {
        return this.sentence;
    }

    /**
     * Directly get the content of the sentence
     *
     * @return The content of the sentence
     */
    public Term getContent() {
        return this.sentence.getContent();
    }

    /**
     * Directly get the creation time of the sentence
     *
     * @return The creation time of the sentence
     */
    public long getCreationTime() {
        return this.sentence.getStamp().getCreationTime();
    }

    /**
     * Check if a Task is a direct input
     *
     * @return Whether the Task is derived from another task
     */
    public boolean isInput() {
        return this.parentTask == null;
    }

    /**
     * Check if a Task is derived by a StructuralRule
     *
     * @return Whether the Task is derived by a StructuralRule
     */
    // public boolean isStructural() {
    // return (parentBelief == null) && (parentTask != null);
    // }
    /**
     * Merge one Task into another
     *
     * @param that The other Task
     */
    @Override
    public void merge(final Item that) {
        if (getCreationTime() >= ((Task) that).getCreationTime())
            // * 📝此处需要对内部令牌执行「合并」，以便调用默认方法
            // * ⚠️改成接口后无法使用`super.method`调用默认方法
            this.token.merge(that);
        else
            that.merge(this);
    }

    /**
     * Get the best-so-far solution for a Question or Goal
     *
     * @return The stored Sentence or null
     */
    public Sentence getBestSolution() {
        return this.bestSolution;
    }

    /**
     * Set the best-so-far solution for a Question or Goal, and report answer
     * for input question
     * * 📝【2024-05-30 17:59:59】仅在「本地规则」中调用
     *
     * @param judgment The solution to be remembered
     */
    public void setBestSolution(final Sentence judgment) {
        if (judgment == null)
            throw new NullPointerException("judgment == null");
        if (!judgment.isJudgment())
            throw new IllegalArgumentException(judgment + " is not judgment");
        this.bestSolution = judgment;
    }

    /**
     * Get the parent belief of a task
     * * 📝似乎只有一处
     *
     * @return The belief from which the task is derived
     */
    public Sentence getParentBelief() {
        return this.parentBelief;
    }

    /**
     * Get the parent task of a task
     *
     * @return The task from which the task is derived
     */
    public Task getParentTask() {
        return this.parentTask;
    }

    /**
     * Get a String representation of the Task
     *
     * @return The Task as a String
     */
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder();
        s.append(super.toString()).append(" ");
        s.append(getSentence().getStamp());
        if (parentTask != null) {
            s.append("  \n from task: ").append(parentTask.toStringBrief());
            if (parentBelief != null) {
                s.append("  \n from belief: ").append(parentBelief.toStringBrief());
            }
        }
        if (bestSolution != null) {
            s.append("  \n solution: ").append(bestSolution.toStringBrief());
        }
        return s.toString();
    }

    /**
     * Get a String representation of the sentence, with 2-digit accuracy
     *
     * @return The String
     */
    @Override
    public String toStringBrief() {
        return toString();
    }
}
