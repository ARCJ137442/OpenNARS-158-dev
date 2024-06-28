package nars.inference;

import nars.control.DerivationContextDirect;
import nars.entity.Concept;
import nars.entity.Judgement;
import nars.entity.Sentence;
import nars.entity.Stamp;
import nars.entity.Task;
import nars.io.Symbols;
import nars.language.Term;

/**
 * 本地推理
 * * 🎯承载原先「直接推理」的部分
 * * 📝其中包含「修订规则」等
 */
final class LocalInference {
    /**
     * 入口
     *
     * @param context
     */
    static void process(DerivationContextDirect context) {
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
        if (!task.isJudgement())
            throw new AssertionError("task " + task + "is not a judgment");
        final Judgement judgment = task.sentenceClone().asJudgement(); // ? 此处是否要将「任务」直接作为「信念」存储
        // * 🚩找到旧信念，并尝试修正
        final Judgement oldBelief = evaluation(
                judgment, self.getBeliefs(),
                BudgetFunctions::solutionQuality);
        if (oldBelief != null) {
            if (judgment.evidentialEqual(oldBelief)) {
                // * 🚩时间戳上重复⇒优先级沉底，避免重复推理
                if (task.getParentTask().isJudgement()) {
                    task.setPriority(0); // duplicated task
                } // else: activated belief
                return;
            }
            // * 🚩不重复 && 可修正 ⇒ 修正
            else if (judgment.revisable(oldBelief)) {
                // * 🚩现在将「当前信念」「新时间戳」移入「修正」调用中
                final boolean hasOverlap = judgment.evidentialOverlap(oldBelief);
                if (!hasOverlap) {
                    // * 📌【2024-06-07 11:38:02】现在由于「新时间戳」的内置，经检查不再需要设置「当前信念」
                    // * 📌此处的「当前信念」直接取`oldBelief`，并以此构造时间戳
                    revisionDirect(judgment, oldBelief, context);
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
            self.addBelief(judgment);
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
            throw new AssertionError("task " + questionTask + "is not a judgment");
        // * 🚩断言所基于的「当前概念」就是「推理上下文」的「当前概念」
        // * 📝在其被唯一使用的地方，传入的`task`只有可能是`context.currentConcept`
        final Concept self = context.getCurrentConcept();

        // * 🚩尝试寻找已有问题，若已有相同问题则直接处理已有问题
        final Task existedQuestion = findExistedQuestion(self, questionTask.getContent());
        final boolean newQuestion = existedQuestion == null;
        final Sentence question = newQuestion ? questionTask : existedQuestion;

        // * 🚩实际上「先找答案，再新增『问题任务』」区别不大——找答案的时候，不会用到「问题任务」
        final Judgement newAnswer = evaluation(
                question, self.getBeliefs(),
                BudgetFunctions::solutionQuality);
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
     * Belief revision
     * <p>
     * called from Concept.reviseTable and match
     *
     * @param newBelief       The new belief in task
     * @param oldBelief       The previous belief with the same content
     * @param feedbackToLinks Whether to send feedback to the links
     * @param context         Reference to the derivation context
     */
    private static void revisionDirect(Judgement newBelief, Judgement oldBelief, DerivationContextDirect context) {
        // * 🚩计算真值/预算值
        final Truth revisedTruth = TruthFunctions.revision(newBelief, oldBelief);
        final Budget budget = BudgetInference.revise(newBelief, oldBelief, revisedTruth, context.getCurrentTask());
        final Term content = newBelief.getContent();
        // * 🚩创建并导入结果：双前提
        // * 📝仅在此处用到「当前信念」作为「导出信念」
        // * 📝此处用不到「当前信念」（旧信念）
        // * 🚩【2024-06-06 08:52:56】现场构建「新时间戳」
        final Stamp newStamp = Stamp.uncheckedMerge(
                newBelief, oldBelief,
                context.getTime(),
                context.getMaxEvidenceBaseLength());
        context.doublePremiseTaskRevision(
                content,
                revisedTruth, budget,
                newStamp);
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
        // * 🚩遍历所有已知问题：任意一个问题「词项相等」就返回
        for (final Task existedQuestion : self.getQuestions()) {
            final Term questionTerm = existedQuestion.getContent();
            if (questionTerm.equals(taskContent))
                // * 🚩词项相等⇒返回
                return existedQuestion;
        }
        return null;
    }

    @FunctionalInterface
    private interface EvaluateSolutionQuality {
        float call(Sentence query, Judgement judgment);
    }

    /**
     * Evaluate a query against beliefs (and desires in the future)
     * * 📌返回值可空
     *
     * @param query           The question to be processed
     * @param list            The list of beliefs to be used
     * @param solutionQuality the way to calculate the quality of the solution
     * @return The best candidate belief selected
     */
    private static Judgement evaluation(
            final Sentence query,
            final Iterable<Judgement> list,
            final EvaluateSolutionQuality solutionQuality) {
        if (list == null)
            throw new AssertionError("传入的表不可能为空");
        // * 🚩筛选出其中排行最前的回答
        float currentBest = 0;
        float beliefQuality;
        Judgement candidate = null;
        for (final Judgement judgment : list) {
            beliefQuality = solutionQuality.call(query, judgment);
            // * 🚩排行大于⇒更新
            if (beliefQuality > currentBest) {
                currentBest = beliefQuality;
                candidate = judgment;
            }
        }
        return candidate;
    }
}
