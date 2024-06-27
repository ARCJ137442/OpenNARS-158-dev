package nars.control;

import java.util.LinkedList;

import nars.entity.Concept;
import nars.entity.Task;
import nars.inference.InferenceEngine;
import nars.storage.Memory;

public abstract class ProcessDirect {

    /**
     * 🆕本地直接推理
     * * 🚩最终只和「本地规则」与{@link Concept#directProcess}有关
     * * 📝`processNewTask`可能会产生新任务，此举将影响到`noResult`的值
     * ! ❌【2024-05-19 22:51:03】不能内联逻辑：后边的「处理任务」受到前边任务处理条件的制约
     * * 🚩【2024-05-19 22:51:22】故不能同义实现「统一获取任务，统一立即处理」的机制
     * * 🔬【2024-06-27 21:29:28】再次尝试内联逻辑：「长期稳定性」测试通过
     * * 🚩【2024-06-27 21:44:49】非破坏性重构：允许同时处理「新任务」与「新近任务」
     * * * 📝在NARS的「直接推理」过程中，本身就有可能要处理多个任务，无论来源
     */
    public static boolean processDirect(final Reasoner self, InferenceEngine inferenceEngine) {
        // * 🚩加载任务 | 新任务/新近任务
        final LinkedList<Task> tasksToProcess = loadFromTasks(self);

        // * 🚩处理新任务
        final boolean noResult = immediateProcess(self, tasksToProcess, inferenceEngine);

        // * 🚩推理结束，清理收尾
        tasksToProcess.clear();
        return noResult;
    }

    private static LinkedList<Task> loadFromTasks(final Reasoner self) {
        // * 🚩创建并装载「将要处理的任务」
        final LinkedList<Task> tasksToProcess = new LinkedList<>();
        loadFromNewTasks(self, tasksToProcess);
        loadFromNovelTasks(self, tasksToProcess);
        // * 🚩【2024-06-27 22:58:33】现在合并逻辑，一个个处理
        // * 📝逻辑上不影响：
        // * 1. 「直接推理」的过程中不会用到「新任务」与「新近任务」
        // * 2. 仍然保留了「在『从新任务获取将处理任务』时，将部分任务放入『新近任务袋』」的逻辑
        return tasksToProcess;
    }

    /**
     * 🆕获取「要处理的新任务」列表
     *
     * Process the newTasks accumulated in the previous workCycle, accept input
     * ones and those that corresponding to existing concepts, plus one from the
     * buffer.
     */
    private static void loadFromNewTasks(final Reasoner self, LinkedList<Task> tasksToProcess) {
        // * 🚩处理新输入：立刻处理 or 加入「新近任务」 or 忽略
        final Memory memory = self.getMemory();
        // don't include new tasks produced in the current workCycle
        // * 🚩处理「新任务缓冲区」中的所有任务
        while (self.hasNewTask()) {
            // * 🚩拿出第一个
            final Task task = self.takeANewTask();
            // * 🚩是输入 或 已有对应概念 ⇒ 将参与「直接推理」
            if (task.isInput() || memory.hasConcept(task.getContent())) {
                tasksToProcess.add(task); // new input or existing concept
            }
            // * 🚩否则：继续筛选以放进「新近任务」
            else {
                // * 🚩筛选
                final boolean shouldAddToNovelTasks;
                if (task.isJudgement()) {
                    // * 🚩判断句⇒看期望，期望满足⇒放进「新近任务」
                    final double exp = task.asJudgement().getExpectation();
                    shouldAddToNovelTasks = exp > Parameters.DEFAULT_CREATION_EXPECTATION;
                } else
                    shouldAddToNovelTasks = false;
                // * 🚩添加
                if (shouldAddToNovelTasks)
                    self.putInNovelTasks(task);
                else
                    // * 🚩忽略
                    self.getRecorder().append("!!! Neglected: " + task + "\n");
            }
        }
    }

    /**
     * 🆕获取「要处理的新近任务」列表
     *
     * Select a novel task to process.
     */
    private static void loadFromNovelTasks(final Reasoner self, LinkedList<Task> tasksToProcess) {
        // select a task from novelTasks
        // one of the two places where this variable is set
        // * 🚩从「新近任务袋」中拿出一个任务，若有⇒添加进列表
        final Task task = self.takeANovelTask();
        if (task != null)
            tasksToProcess.add(task);
    }

    /* ---------- task processing ---------- */
    /**
     * Immediate processing of a new task, in constant time Local processing, in
     * one concept only
     *
     * @param taskInput the task to be accepted (owned)
     */
    private static boolean immediateProcess(
            final Reasoner self,
            final Task taskInput,
            final InferenceEngine inferenceEngine) {
        self.getRecorder().append("!!! Insert: " + taskInput + "\n");

        // * 🚩构建「实际上下文」并断言可空性
        final DerivationContextDirect context = prepareDirectProcessContext(
                self,
                taskInput);

        // * 🚩未准备上下文⇒直接结束
        if (context == null)
            return true;

        // * 🚩上下文准备完毕⇒开始

        // * 🚩调整概念的预算值
        // * 📌断言：此处一定是「概念在记忆区之外」
        self.getMemory().activateConceptOuter(context.getCurrentConcept(), context.getCurrentTask());

        // * 🔥开始「直接推理」
        directProcess(context, inferenceEngine);

        final boolean noResult = context.noNewTask();

        // * 🚩吸收并清空上下文
        self.absorbContext(context);
        return noResult;
    }

    private static boolean immediateProcess(final Reasoner self, final Iterable<Task> tasksToProcess,
            InferenceEngine inferenceEngine) {
        boolean noResult = true;
        for (final Task task : tasksToProcess) {
            // final BudgetValue oldBudgetValue = task.getBudget().clone();
            final boolean noResultSingle = immediateProcess(self, task, inferenceEngine);
            if (!noResultSingle)
                noResult = false;
            // ! 📝处理之后预算值可能改变，不能让整个函数与`processNovelTask`合并
            // * ⚠️需要「边处理（修改预算）边加入『新近任务』」
            // if (!task.getBudget().equals(oldBudgetValue)) {
            // recorder.append("!!! Budget changed: " + task + "\n");
            // }
        }
        return noResult;
    }

    /**
     * 🆕准备「直接推理」的推理上下文
     * * 🚩这其中不对「推理上下文」「记忆区」外的变量进行任何修改
     * * 📌捕获`taskInput`的所有权
     * * 📌捕获`currentConcept`的所有权
     * * ⚠️不在其中修改实体（预算值 等）
     *
     * @param currentTask
     * @return 直接推理上下文 / 空
     */
    private static DerivationContextDirect prepareDirectProcessContext(
            final Reasoner self,
            final Task currentTask) {
        // * 🚩准备上下文
        // one of the two places where this variable is set
        final Concept taskConcept = self.getMemory().getConceptOrCreate(currentTask.getContent());
        if (taskConcept != null) {
            // final Concept currentConcept = taskConcept;
            final Concept currentConcept = self.getMemory().pickOutConcept(taskConcept.getKey());
            return new DerivationContextDirect(self, currentTask, currentConcept); // * 📌准备就绪
        }
        return null; // * 📌准备失败：没有可供推理的概念
    }

    /* ---------- direct processing of tasks ---------- */
    /**
     * Directly process a new task. Called exactly once on each task. Using
     * local information and finishing in a constant time. Provide feedback in
     * the taskBudget value of the task.
     * <p>
     * called in Memory.immediateProcess only
     *
     * @param task The task to be processed
     */
    private static void directProcess(final DerivationContextDirect context, final InferenceEngine inferenceEngine) {
        // * 🚩断言原先传入的「任务」就是「推理上下文」的「当前任务」
        // * 📝在其被唯一使用的地方，传入的`task`只有可能是`context.currentTask`
        // * 🚩断言所基于的「当前概念」就是「推理上下文」的「当前概念」
        // * 📝在其被唯一使用的地方，传入的`task`只有可能是`context.currentConcept`
        // * 📝相比于「概念推理」仅少了「当前词项链」与「当前任务链」，其它基本通用
        inferenceEngine.directProcess(context);

        final Task task = context.getCurrentTask();
        // * 🚩在推理后做链接 | 若预算值够就链接，若预算值不够就丢掉
        if (task.budgetAboveThreshold()) { // still need to be processed
            ConceptLinking.linkConceptToTask(context);
        }
    }
}
