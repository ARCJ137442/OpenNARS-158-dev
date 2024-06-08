package nars.control;

import java.util.LinkedList;

import nars.entity.Concept;
import nars.entity.Judgement;
import nars.entity.Sentence;
import nars.entity.Task;
import nars.inference.LocalRules;
import nars.io.Symbols;
import nars.language.Term;
import nars.main.Parameters;
import nars.storage.Memory;

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
        final LinkedList<Task> tasksToProcess = loadFromNewTasks(self);
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
        final LinkedList<Task> tasksToProcess = loadFromNovelTasks(self);
        // * 🚩处理新近任务
        final boolean noResult = immediateProcess(self, tasksToProcess);
        // * 🚩清理收尾
        tasksToProcess.clear();
        return noResult;
    }

    /**
     * 🆕获取「要处理的新任务」列表
     */
    private static LinkedList<Task> loadFromNewTasks(final Memory self) {
        // * 🚩处理新输入：立刻处理 or 加入「新近任务」 or 忽略
        final LinkedList<Task> tasksToProcess = new LinkedList<>();
        final LinkedList<Task> mut_newTasks = self.mut_newTasks();
        // don't include new tasks produced in the current workCycle
        // * 🚩处理「新任务缓冲区」中的所有任务
        while (!mut_newTasks.isEmpty()) {
            // * 🚩拿出第一个
            final Task task = mut_newTasks.removeFirst();
            // * 🚩是输入 或 已有对应概念 ⇒ 将参与「直接推理」
            if (task.isInput() || self.hasConcept(task.getContent())) {
                tasksToProcess.add(task); // new input or existing concept
            }
            // * 🚩否则：继续筛选以放进「新近任务」
            else {
                // * 🚩筛选
                final boolean shouldAddToNovelTasks;
                if (task.isJudgment()) {
                    // * 🚩判断句⇒看期望，期望满足⇒放进「新近任务」
                    final double exp = task.asJudgement().getExpectation();
                    shouldAddToNovelTasks = exp > Parameters.DEFAULT_CREATION_EXPECTATION;
                } else
                    shouldAddToNovelTasks = false;
                // * 🚩添加
                if (shouldAddToNovelTasks)
                    self.mut_novelTasks().putIn(task);
                else
                    // * 🚩忽略
                    self.getRecorder().append("!!! Neglected: " + task + "\n");
            }
        }
        return tasksToProcess;
    }

    /**
     * 🆕获取「要处理的新近任务」列表
     */
    private static LinkedList<Task> loadFromNovelTasks(final Memory self) {
        final LinkedList<Task> tasksToProcess = new LinkedList<>();
        // select a task from novelTasks
        // one of the two places where this variable is set
        // * 🚩从「新近任务袋」中拿出一个任务，若有⇒添加进列表
        final Task task = self.mut_novelTasks().takeOut();
        if (task != null)
            tasksToProcess.add(task);
        return tasksToProcess;
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

        // * 🚩构建「实际上下文」并断言可空性
        final DerivationContextDirect context = prepareDirectProcessContext(
                self,
                taskInput);

        // * 🚩上下文准备完毕⇒开始
        if (context != null) {
            // * 🚩调整概念的预算值
            self.activateConcept(context.getCurrentConcept(), taskInput);
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
     * * 📌捕获`currentConcept`的所有权
     * * ⚠️不在其中修改实体（预算值 等）
     *
     * @param currentTask
     * @return 直接推理上下文 / 空
     */
    private static DerivationContextDirect prepareDirectProcessContext(
            final Memory self,
            final Task currentTask) {
        // * 🚩准备上下文
        // one of the two places where this variable is set
        final Concept taskConcept = self.getConceptOrCreate(currentTask.getContent());
        if (taskConcept != null) {
            // final Concept currentConcept = taskConcept;
            final Concept currentConcept = self.pickOutConcept(taskConcept.getKey());
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
    public static void directProcess(final DerivationContextDirect context) {
        // * 🚩断言原先传入的「任务」就是「推理上下文」的「当前任务」
        // * 📝在其被唯一使用的地方，传入的`task`只有可能是`context.currentTask`
        // * 🚩断言所基于的「当前概念」就是「推理上下文」的「当前概念」
        // * 📝在其被唯一使用的地方，传入的`task`只有可能是`context.currentConcept`
        // * 📝相比于「概念推理」仅少了「当前词项链」与「当前任务链」，其它基本通用
        final Task task = context.getCurrentTask();

        // * 🚩先根据类型分派推理
        switch (task.getPunctuation()) {
            case Symbols.JUDGMENT_MARK:
                processJudgment(context);
                break;
            case Symbols.QUESTION_MARK:
                processQuestion(context);
                break;
            default:
                throw new Error("Unknown punctuation of task: " + task.toStringLong());
        }

        // * 🚩在推理后做链接 | 若预算值够就链接，若预算值不够就丢掉
        if (task.budgetAboveThreshold()) { // still need to be processed
            ConceptLinking.linkConceptToTask(context);
        }
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
        // * 🚩断言传入任务的「语句」一定是「判断」
        if (!task.isJudgment())
            throw new Error("task " + task + "is not a judgment");
        final Judgement judgment = task.sentenceClone().asJudgement(); // ? 此处是否要将「任务」直接作为「信念」存储
        // * 🚩找到旧信念，并尝试修正
        final Judgement oldBelief = evaluation(judgment, self.getBeliefs());
        if (oldBelief != null) {
            if (judgment.evidentialEqual(oldBelief)) {
                // * 🚩时间戳上重复⇒优先级沉底，避免重复推理
                if (task.getParentTask().isJudgment()) {
                    task.decPriority(0); // duplicated task
                } // else: activated belief
                return;
            }
            // * 🚩不重复 && 可修正 ⇒ 修正
            else if (LocalRules.revisable(judgment, oldBelief)) {
                // * 🚩现在将「当前信念」「新时间戳」移入「修正」调用中
                final boolean hasOverlap = judgment.evidentialOverlap(oldBelief);
                if (!hasOverlap) {
                    // * 📌【2024-06-07 11:38:02】现在由于「新时间戳」的内置，经检查不再需要设置「当前信念」
                    // * 📌此处的「当前信念」直接取`oldBelief`，并以此构造时间戳
                    LocalRules.revision(judgment, oldBelief, context);
                }
            }
        }
        // * 🚩尝试用新的信念解决旧有问题
        // * 📄如：先输入`A?`再输入`A.`
        if (task.budgetAboveThreshold()) {
            // * 🚩开始尝试解决「问题表」中的所有问题
            for (final Task existedQuestion : self.getQuestions()) {
                LocalRules.trySolution(judgment, existedQuestion, context);
            }
            // * 🚩将信念追加至「信念表」
            addBelief(self, judgment);
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
        final Task questionTask = context.getCurrentTask();
        // * 🚩断言传入任务的「语句」一定是「问题」
        if (!questionTask.isQuestion())
            throw new Error("task " + questionTask + "is not a judgment");
        // * 🚩断言所基于的「当前概念」就是「推理上下文」的「当前概念」
        // * 📝在其被唯一使用的地方，传入的`task`只有可能是`context.currentConcept`
        final Concept self = context.getCurrentConcept();

        // * 🚩尝试寻找已有问题，若已有相同问题则直接处理已有问题
        final Task existedQuestion = findExistedQuestion(self, questionTask.getContent());
        final boolean newQuestion = existedQuestion == null;
        final Sentence question = newQuestion ? questionTask : existedQuestion;

        // * 🚩实际上「先找答案，再新增『问题任务』」区别不大——找答案的时候，不会用到「问题任务」
        final Judgement newAnswer = evaluation(question, self.getBeliefs());
        if (newAnswer != null) {
            // LocalRules.trySolution(ques, newAnswer, task, memory);
            LocalRules.trySolution(newAnswer, questionTask, context);
        }
        // * 🚩新增问题
        if (newQuestion) {
            self.addQuestion(questionTask);
        }
    }

    /**
     * Add a new belief (or goal) into the table Sort the beliefs/goals by rank,
     * and remove redundant or low rank one
     * * 🚩添加到固定容量的缓冲区，并返回溢出的那个（溢出==所添加 ⇒ 添加失败）
     *
     * @param newBelief The judgment to be processed
     * @param table     The table to be revised
     * @param capacity  The capacity of the table
     */
    public static Judgement addBelief(
            final Concept self,
            final Judgement newBelief) {
        return self.getBeliefs().add(newBelief);
    }

    /**
     * 🆕根据输入的任务，寻找并尝试返回已有的问题
     * * ⚠️输出可空，且此时具有含义：概念中并没有「已有问题」
     * * 🚩经上游确认，此处的`task`只可能是`context.currentTask`
     *
     * @param taskContent 要在「自身所有问题」中查找相似的「问题」任务
     * @return 已有的问题，或为空
     */
    private static Task findExistedQuestion(final Concept self, final Term taskContent) {
        // TODO: 过程笔记注释
        final Iterable<Task> questions = self.getQuestions();
        if (questions == null)
            return null;
        for (final Task existedQuestion : questions) {
            final Term questionTerm = existedQuestion.getContent();
            if (questionTerm.equals(taskContent))
                return existedQuestion;
        }
        return null;
    }

    /**
     * Evaluate a query against beliefs (and desires in the future)
     *
     * @param query The question to be processed
     * @param list  The list of beliefs to be used
     * @return The best candidate belief selected
     */
    private static Judgement evaluation(final Sentence query, final Iterable<Judgement> list) {
        // TODO: 过程笔记注释
        if (list == null)
            return null;
        float currentBest = 0;
        float beliefQuality;
        Judgement candidate = null;
        for (final Judgement judgment : list) {
            beliefQuality = LocalRules.solutionQuality(query, judgment);
            if (beliefQuality > currentBest) {
                currentBest = beliefQuality;
                candidate = judgment;
            }
        }
        return candidate;
    }
}
