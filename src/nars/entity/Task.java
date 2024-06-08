package nars.entity;

import nars.inference.Budget;
import nars.inference.BudgetFunctions;

/**
 * A task to be processed, consists of a Sentence and a BudgetValue
 */
public interface Task extends Sentence, Item {

    /**
     * Get the parent belief of a task
     * * 📝似乎只有一处
     * * 🚩只读
     *
     * @return The belief from which the task is derived
     */
    public Sentence getParentBelief();

    /**
     * Get the parent task of a task
     * * 🚩只读
     *
     * @return The task from which the task is derived
     */
    public Task getParentTask();

    /**
     * Get the best-so-far solution for a Question or Goal
     * * 🚩只读（若作为字段，则为读写）
     *
     * @return The stored Sentence or null
     */
    public Sentence getBestSolution();

    /**
     * Set the best-so-far solution for a Question or Goal, and report answer
     * for input question
     * * 📝【2024-05-30 17:59:59】仅在「本地规则」中调用
     * * 📌【2024-06-05 00:59:55】只在「用『判断』回答『疑问』」中使用
     *
     * @param judgment The solution to be remembered
     */
    public void setBestSolution(final Sentence judgment);

    /**
     * Check if a Task is a direct input
     *
     * @return Whether the Task is derived from another task
     */
    public default boolean isInput() {
        return this.getParentTask() == null;
    }

    /**
     * Merge one Task into another
     *
     * @param that The other Task
     */
    @Override
    public default void mergeBudget(final Budget that) {
        if (!(that instanceof Task))
            throw new IllegalArgumentException(that + " isn't a Task");
        // * 🚩均为「任务」⇒按照「发生时间」决定「谁并入谁」
        if (getCreationTime() >= ((Task) that).getCreationTime())
            // * ⚠️改成接口后无法使用`super.method`调用默认方法
            // * 🚩【2024-06-05 00:25:49】现在可直接使用「获取预算」而无需强制要求基于「Token」
            // * 🚩【2024-06-07 13:52:15】目前直接内联接口的默认方法
            BudgetFunctions.merge(this, that);
        else
            BudgetFunctions.merge(that, this);
    }

    public default String taskToString() {
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

    public default String taskToStringLong() {
        return taskToString();
    }
}
