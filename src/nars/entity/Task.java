package nars.entity;

/**
 * A task to be processed, consists of a Sentence and a BudgetValue
 */
public interface Task extends Sentence, Item {

    /**
     * Task from which the Task is derived, or null if input
     *
     * * ️📝可空性：可空
     * * 📝可变性：不变 | 仅构造时，无需可变，「语句」类型可随意复制
     * * 📝所有权：共享引用
     */
    Task __parentTask();

    /**
     * Belief from which the Task is derived, or null if derived from a theorem
     *
     * * ️📝可空性：可空
     * * 📝可变性：不变 | 仅构造时，无需可变，「语句」类型可随意复制
     * * 📝所有权：具所有权
     */
    Sentence __parentBelief();

    /**
     * For Question and Goal: best solution found so far
     * * 📝一旦设定值后，永不回到空值状态
     *
     * * ️📝可空性：可空 | 可能暂无「最优解」
     * * 📝可变性：可变
     * * 📝所有权：具所有权 | 「语句」类型
     */
    Sentence __bestSolution();

    void __bestSolution_set(Sentence sentence);

    /**
     * Directly get the creation time of the sentence
     *
     * @return The creation time of the sentence
     */
    public default long getCreationTime() {
        return this.getStamp().getCreationTime();
    }

    /**
     * Check if a Task is a direct input
     *
     * @return Whether the Task is derived from another task
     */
    public default boolean isInput() {
        return this.__parentTask() == null;
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
    public default void merge(final Item that) {
        if (getCreationTime() >= ((Task) that).getCreationTime())
            // * 📝此处需要对内部令牌执行「合并」，以便调用默认方法
            // * ⚠️改成接口后无法使用`super.method`调用默认方法
            // * 🚩【2024-06-05 00:25:49】现在可直接使用「获取预算」而无需强制要求基于「Token」
            this.getBudget().merge(that.getBudget());
        else
            that.merge(this);
    }

    /**
     * Get the best-so-far solution for a Question or Goal
     *
     * @return The stored Sentence or null
     */
    public default Sentence getBestSolution() {
        return this.__bestSolution();
    }

    /**
     * Set the best-so-far solution for a Question or Goal, and report answer
     * for input question
     * * 📝【2024-05-30 17:59:59】仅在「本地规则」中调用
     * * 📌【2024-06-05 00:59:55】只在「用『判断』回答『疑问』」中使用
     *
     * @param judgment The solution to be remembered
     */
    public default void setBestSolution(final Sentence judgment) {
        if (!this.isQuestion())
            throw new IllegalArgumentException(this + " is not question");
        if (judgment == null)
            throw new NullPointerException("judgment == null");
        if (!judgment.isJudgment())
            throw new IllegalArgumentException(judgment + " is not judgment");
        // this.bestSolution = judgment;
        this.__bestSolution_set(judgment);
    }

    /**
     * Get the parent belief of a task
     * * 📝似乎只有一处
     *
     * @return The belief from which the task is derived
     */
    public default Sentence getParentBelief() {
        return this.__parentBelief();
    }

    /**
     * Get the parent task of a task
     *
     * @return The task from which the task is derived
     */
    public default Task getParentTask() {
        return this.__parentTask();
    }
}
