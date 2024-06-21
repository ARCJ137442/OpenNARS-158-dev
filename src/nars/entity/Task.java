package nars.entity;

import nars.entity.Item.BagItem;
import nars.storage.Bag.MergeOrder;

/**
 * A task to be processed, consists of a Sentence and a BudgetValue
 */
public interface Task extends Sentence, ToKey {

    /**
     * Get the parent belief of a task
     * * 📝似乎只有一处
     * * 🚩只读
     *
     * @return The belief from which the task is derived
     */
    public Judgement getParentBelief();

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
    public Judgement getBestSolution();

    /**
     * Set the best-so-far solution for a Question or Goal, and report answer
     * for input question
     * * 📝【2024-05-30 17:59:59】仅在「本地规则」中调用
     * * 📌【2024-06-05 00:59:55】只在「用『判断』回答『疑问』」中使用
     *
     * @param judgment The solution to be remembered
     */
    public void setBestSolution(final Judgement judgment);

    /**
     * Check if a Task is a direct input
     *
     * @return Whether the Task is derived from another task
     */
    public default boolean isInput() {
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

    /**
     * Get a String representation of the Task
     *
     * @return The Task as a String
     */
    public default String taskToString(BagItem<Task> self) {
        final StringBuilder s = new StringBuilder();
        final String superString = self.budgetToString() + " " + self.getKey().toString();
        final Task task = self.getValue();
        s.append(superString).append(" ");
        s.append(task.stampToString());
        if (task.getParentTask() != null) {
            s.append("  \n from task: ").append(task.getParentTask().toStringBrief());
            if (task.getParentBelief() != null) {
                s.append("  \n from belief: ").append(task.getParentBelief().toStringBrief());
            }
        }
        if (task.getBestSolution() != null) {
            s.append("  \n solution: ").append(task.getBestSolution().toStringBrief());
        }
        return s.toString();
    }

    public default String taskToStringLong(BagItem<Task> self) {
        return taskToString(self);
    }

    /**
     * Return a String representation of the Item after simplification
     *
     * @return A simplified String representation of the content
     */
    public default String taskToStringBrief(BagItem<Task> self) {
        return self.budgetToStringBrief() + " " + self.getKey();
    }
}
