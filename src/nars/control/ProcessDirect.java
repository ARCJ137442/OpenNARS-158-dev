package nars.control;

import java.util.ArrayList;
import java.util.LinkedList;

import nars.entity.*;
import nars.inference.BudgetFunctions;
import nars.inference.LocalRules;
import nars.io.Symbols;
import nars.main_nogui.*;
import nars.storage.*;

public abstract class ProcessDirect {

    /**
     * 🆕本地直接推理
     * * 🚩最终只和「本地规则」与{@link Concept#directProcess}有关
     */
    public static boolean processDirect(final Memory self) {
        // * 🚩处理已有任务（新任务/新近任务）
        boolean noResult = processNewTask(self);
        // * 📝`processNewTask`可能会产生新任务，此举将影响到`noResult`的值
        if (noResult) { // necessary?
            // ! ❌【2024-05-19 22:51:03】不能内联逻辑：后边的「处理任务」受到前边任务处理条件的制约
            // * 🚩【2024-05-19 22:51:22】故不能同义实现「统一获取任务，统一立即处理」的机制
            final boolean noResultNovel = processNovelTask(self);
            if (!noResultNovel)
                noResult = false;
        }
        // * 🚩推理结束
        return noResult;
    }

    /**
     * Process the newTasks accumulated in the previous workCycle, accept input
     * ones and those that corresponding to existing concepts, plus one from the
     * buffer.
     */
    private static boolean processNewTask(final Memory self) {
        // * 🚩获取新任务
        final LinkedList<Task> tasksToProcess = Memory.getNewTasks(self);
        // * 🚩处理新任务
        final boolean noResult = immediateProcess(self, tasksToProcess);
        // * 🚩清理收尾
        tasksToProcess.clear();
        return noResult;
    }

    /**
     * Select a novel task to process.
     */
    private static boolean processNovelTask(final Memory self) {
        // * 🚩获取新近任务
        final LinkedList<Task> tasksToProcess = self.getNovelTasks();
        // * 🚩处理新近任务
        final boolean noResult = immediateProcess(self, tasksToProcess);
        // * 🚩清理收尾
        tasksToProcess.clear();
        return noResult;
    }

    /* ---------- task processing ---------- */
    /**
     * Immediate processing of a new task, in constant time Local processing, in
     * one concept only
     *
     * @param taskInput the task to be accepted (owned)
     */
    private static boolean immediateProcess(final Memory self, final Task taskInput) {
        self.getRecorder().append("!!! Insert: " + taskInput + "\n");

        // * 🚩准备上下文
        final DerivationContextDirect context = prepareDirectProcessContext(self, taskInput);

        // * 🚩上下文准备完毕⇒开始
        if (context != null) {
            // * 🚩调整概念的预算值
            self.activateConcept(context.getCurrentConcept(), taskInput.getBudget());
            // * 🔥开始「直接处理」
            directProcess(context);
        }

        final boolean noResult = context.noResult();

        // * 🚩吸收并清空上下文
        self.absorbContext(context);
        return noResult;
    }

    private static boolean immediateProcess(final Memory self, final Iterable<Task> tasksToProcess) {
        boolean noResult = true;
        for (final Task task : tasksToProcess) {
            // final BudgetValue oldBudgetValue = task.getBudget().clone();
            final boolean noResultSingle = immediateProcess(self, task);
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
     *
     * @param taskInput
     * @return 直接推理上下文 / 空
     */
    private static DerivationContextDirect prepareDirectProcessContext(final Memory self, final Task taskInput) {
        // * 🚩强制清空上下文防串
        final DerivationContextDirect context = new DerivationContextDirect(self);
        // * 🚩准备上下文
        // one of the two places where this variable is set
        context.setCurrentTask(taskInput);
        context.setCurrentConcept(self.getConceptOrCreate(taskInput.getContent()));
        if (context.getCurrentConcept() != null) {
            // * ✅【2024-05-20 08:52:34】↓不再需要：自始至终都是「当前概念」所对应的词项
            // context.setCurrentTerm(context.getCurrentConcept().getTerm());
            return context; // * 📌准备就绪
        }
        return null; // * 📌准备失败：没有可供推理的概念
    }

    // from Concept

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
    public static void directProcess(final DerivationContextDirect context) {
        // * 🚩断言原先传入的「任务」就是「推理上下文」的「当前任务」
        // * 📝在其被唯一使用的地方，传入的`task`只有可能是`context.currentTask`
        // * 🚩断言所基于的「当前概念」就是「推理上下文」的「当前概念」
        // * 📝在其被唯一使用的地方，传入的`task`只有可能是`context.currentConcept`
        // * 📝相比于「概念推理」仅少了「当前词项链」与「当前任务链」，其它基本通用
        final Concept self = context.getCurrentConcept();
        /*
         * 📝有效字段：{
         * currentTerm
         * currentConcept
         * currentTask
         *
         * currentBelief? | 用于中途推理
         * newStamp? | 用于中途推理
         * }
         */
        // * 🚩系列断言与赋值（实际使用中可删）
        if (context.getCurrentTask() == null) {
            throw new Error("currentTask: 不符预期的可空情况");
        }
        if (context.getCurrentTerm() == null) {
            throw new Error("currentTerm: 不符预期的可空情况");
        }
        if (context.getCurrentConcept() != self) { // ! 不仅非空，而且等于自身
            throw new Error("currentConcept: 不符预期的可空情况");
        }
        if (context.getCurrentBelief() != null) {
            throw new Error("currentBelief: 不符预期的可空情况");
        }
        // if (context.getCurrentBeliefLink() != null) {
        // throw new Error("currentBeliefLink: 不符预期的可空情况");
        // }
        // if (context.getCurrentTaskLink() != null) {
        // throw new Error("currentTaskLink: 不符预期的可空情况");
        // }
        if (context.getNewStamp() != null) {
            throw new Error("newStamp: 不符预期的可空情况");
        }
        if (context.getSubstitute() != null) {
            throw new Error("substitute: 不符预期的可空情况");
        }
        final Task task = context.getCurrentTask();

        // * 🚩先根据类型分派推理
        switch (task.getSentence().getPunctuation()) {
            case Symbols.JUDGMENT_MARK:
                processJudgment(context);
                break;
            case Symbols.QUESTION_MARK:
                processQuestion(context);
                break;
            default:
                throw new Error("Unknown punctuation of task: " + task.toStringLong());
        }
        // * 🚩在推理后做链接
        if (task.getBudget().aboveThreshold()) { // still need to be processed
            self.linkToTask(task);
        }
        self.refreshObserver();
    }

    /**
     * To accept a new judgment as isBelief, and check for revisions and
     * solutions
     *
     * @param task The judgment to be accepted
     * @param task The task to be processed
     * @return Whether to continue the processing of the task
     */
    private static void processJudgment(final DerivationContextDirect context) {
        // * 🚩断言所基于的「当前概念」就是「推理上下文」的「当前概念」
        // * 📝在其被唯一使用的地方，传入的`task`只有可能是`context.currentConcept`
        final Concept self = context.getCurrentConcept();
        // * 📝【2024-05-18 14:32:20】根据上游调用，此处「传入」的`task`只可能是`context.currentTask`
        final Task task = context.getCurrentTask();
        final Sentence judgment = task.getSentence();
        // * 🚩找到旧信念，并尝试修正
        final Sentence oldBelief = evaluation(judgment, self.getBeliefs());
        if (oldBelief != null) {
            final Stamp currentStamp = judgment.getStamp();
            final Stamp oldStamp = oldBelief.getStamp();
            if (currentStamp.equals(oldStamp)) {
                // * 🚩时间戳上重复⇒优先级沉底，避免重复推理
                if (task.getParentTask().getSentence().isJudgment()) {
                    task.getBudget().decPriority(0); // duplicated task
                } // else: activated belief
                return;
            }
            // * 🚩不重复 && 可修正 ⇒ 修正
            else if (LocalRules.revisable(judgment, oldBelief)) {
                // * 📝OpenNARS 3.0.4亦有覆盖：
                // * 📄`nal.setTheNewStamp(newStamp, oldStamp, nal.time.time());`
                final Stamp newStamp = Stamp.make(currentStamp, oldStamp, context.getTime());
                context.setNewStamp(newStamp);
                if (newStamp != null) {
                    // ! 📝【2024-05-19 21:35:45】此处导致`currentBelief`不能只读
                    context.setCurrentBelief(oldBelief);
                    // TODO: 后续要将此处「修正」分开成「概念推理用修正」与「直接推理用修正」
                    // TODO: 🎯去掉上边的`setCurrentBelief`，断言「『直接推理』不会使用『当前信念』」
                    // ! ⚠️会用到`currentBelief` @ LocalRules.revision/doublePremiseTask
                    LocalRules.revision(judgment, oldBelief, context);
                }
            }
        }
        // * 🚩尝试用新的信念解决旧有问题
        // * 📄如：先输入`A?`再输入`A.`
        if (task.getBudget().aboveThreshold()) {
            for (final Task existedQuestion : self.getQuestions()) {
                // LocalRules.trySolution(ques.getSentence(), judgment, ques, memory);
                LocalRules.trySolution(judgment, existedQuestion, context);
            }
            addBeliefToTable(judgment, self.getBeliefs(), Parameters.MAXIMUM_BELIEF_LENGTH);
        }
    }

    /**
     * Add a new belief (or goal) into the table Sort the beliefs/goals by rank,
     * and remove redundant or low rank one
     *
     * @param newSentence The judgment to be processed
     * @param table       The table to be revised
     * @param capacity    The capacity of the table
     */
    public static void addBeliefToTable(Sentence newSentence, ArrayList<Sentence> table, int capacity) {
        final float rank1 = BudgetFunctions.rankBelief(newSentence); // for the new isBelief
        int i;
        for (i = 0; i < table.size(); i++) {
            final Sentence judgment2 = table.get(i);
            final float rank2 = BudgetFunctions.rankBelief(judgment2);
            if (rank1 >= rank2) {
                if (newSentence.equivalentTo(judgment2)) {
                    return;
                }
                table.add(i, newSentence);
                break;
            }
        }
        if (table.size() >= capacity) {
            while (table.size() > capacity) {
                table.remove(table.size() - 1);
            }
        } else if (i == table.size()) {
            table.add(newSentence);
        }
    }

    /**
     * To answer a question by existing beliefs
     * * 🚩【2024-05-18 15:39:46】根据OpenNARS 3.1.0、3.1.2 与 PyNARS，均不会返回浮点数
     * * 📄其它OpenNARS版本中均不返回值，或返回的值并不使用
     * * 📄PyNARS在`Memory._solve_question`
     *
     * @param task The task to be processed
     * @return Whether to continue the processing of the task
     */
    private static void processQuestion(final DerivationContextDirect context) {
        // * 📝【2024-05-18 14:32:20】根据上游调用，此处「传入」的`task`只可能是`context.currentTask`
        final Task task = context.getCurrentTask();
        // * 🚩断言所基于的「当前概念」就是「推理上下文」的「当前概念」
        // * 📝在其被唯一使用的地方，传入的`task`只有可能是`context.currentConcept`
        final Concept self = context.getCurrentConcept();

        // * 🚩尝试寻找已有问题，若已有相同问题则直接处理已有问题
        final Task existedQuestion = self.findExistedQuestion(task.getContent());
        final boolean newQuestion = existedQuestion == null;
        final Sentence question = newQuestion ? task.getSentence() : existedQuestion.getSentence();

        // * 🚩实际上「先找答案，再新增『问题任务』」区别不大——找答案的时候，不会用到「问题任务」
        final Sentence newAnswer = evaluation(question, self.getBeliefs());
        if (newAnswer != null) {
            // LocalRules.trySolution(ques, newAnswer, task, memory);
            LocalRules.trySolution(newAnswer, task, context);
        }

        if (newQuestion) {
            // * 🚩不会添加重复的问题
            self.getQuestions().add(task);
            // * 🚩问题缓冲区机制 | 📝断言：只有在「问题变动」时处理
            if (self.getQuestions().size() > Parameters.MAXIMUM_QUESTIONS_LENGTH) {
                self.getQuestions().remove(0); // FIFO
            }
        }
    }

    /**
     * Evaluate a query against beliefs (and desires in the future)
     *
     * @param query The question to be processed
     * @param list  The list of beliefs to be used
     * @return The best candidate belief selected
     */
    private static Sentence evaluation(Sentence query, ArrayList<Sentence> list) {
        if (list == null) {
            return null;
        }
        float currentBest = 0;
        float beliefQuality;
        Sentence candidate = null;
        for (Sentence judgment : list) {
            beliefQuality = LocalRules.solutionQuality(query, judgment);
            if (beliefQuality > currentBest) {
                currentBest = beliefQuality;
                candidate = judgment;
            }
        }
        return candidate;
    }
}
