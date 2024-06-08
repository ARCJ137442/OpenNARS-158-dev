package nars.inference;

import nars.entity.*;
import nars.language.*;
import nars.io.Symbols;
import static nars.control.MakeTerm.*;
import static nars.io.Symbols.*;

import nars.control.DerivationContext;
import nars.control.DerivationContextReason;
import nars.control.ReportType;

/**
 * Directly process a task by a oldBelief, with only two Terms in both. In
 * matching, the new task is compared with all existing direct Tasks in that
 * Concept, to carry out:
 * <p>
 * revision: between judgments on non-overlapping evidence; revision: between
 * judgments; satisfy: between a Sentence and a Question/Goal; merge: between
 * items of the same type and stamp; conversion: between different inheritance
 * relations.
 */
public class LocalRules {

    /* -------------------- same contents -------------------- */
    /**
     * The task and belief have the same content
     * <p>
     * called in RuleTables.reason
     * TODO: 【2024-06-08 09:18:23】预计将所有「本地规则」均迁移到「直接推理」中
     *
     * @param task    The task
     * @param belief  The belief
     * @param context Reference to the derivation context
     */
    public static void matchTaskAndBelief(DerivationContextReason context) {
        // * 📝【2024-05-18 14:35:35】自调用者溯源：此处的`task`一定是`context.currentTask`
        final Task currentTask = context.getCurrentTask();
        // * 📝【2024-05-18 14:35:35】自调用者溯源：此处的`belief`一定是`context.currentBelief`
        final Judgement belief = context.getCurrentBelief();

        // * 🚩按照标点分派
        switch (currentTask.getPunctuation()) {
            // * 🚩判断⇒尝试修正
            case JUDGMENT_MARK:
                if (revisable(currentTask.asJudgement(), belief))
                    revision(currentTask.asJudgement(), belief, context);
                return;
            // * 🚩问题⇒尝试回答「特殊疑问」（此处用「变量替换」解决查询变量）
            case QUESTION_MARK:
                // * 🚩查看是否可以替换「查询变量」，具体替换从「特殊疑问」转变为「一般疑问」
                // * 📄Task :: SentenceV1@49 "<{?1} --> murder>? {105 : 6} "
                // * & Belief: SentenceV1@39 "<{tom} --> murder>. %1.0000;0.7290% {147 : 3;4;2}"
                // * ⇒ Unified SentenceV1@23 "<{tom} --> murder>? {105 : 6} "
                final boolean hasUnified = Variable.hasSubstitute(
                        Symbols.VAR_QUERY,
                        currentTask.getContent().clone(),
                        belief.getContent().clone());
                // * ⚠️只针对「特殊疑问」：传入的只有「带变量问题」，因为「一般疑问」通过直接推理就完成了
                if (hasUnified)
                    // * 🚩此时「当前任务」「当前信念」仍然没变
                    trySolution(belief, currentTask, context);
                return;
            // * 🚩其它
            default:
                System.err.println("未知的语句类型：" + currentTask);
                return;
        }
    }

    /**
     * Check whether two sentences can be used in revision
     * * 📝【2024-05-19 13:09:40】这里的`s1`、`s2`必定是「判断」类型
     * * 🚩只有两个「判断句」才有可能「被用于修正」
     *
     * @param newBelief  The first sentence
     * @param baseBelief The second sentence
     * @return If revision is possible between the two sentences
     */
    public static boolean revisable(Judgement newBelief, Judgement baseBelief) {
        // * 🚩如果两个「判断句」的「内容」相同，并且新的「判断句」是可（参与）修正的，那么第二个「判断句」可以修正第一个「判断句」
        final boolean contentEq = newBelief.getContent().equals(baseBelief.getContent());
        final boolean baseRevisable = newBelief.getRevisable();
        return contentEq && baseRevisable;
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
    public static void revision(Judgement newBelief, Judgement oldBelief, DerivationContext context) {
        // * 🚩计算真值/预算值
        final Truth truth = TruthFunctions.revision(newBelief, oldBelief);
        final Budget budget = BudgetFunctions.revise(newBelief, oldBelief, truth, context);
        final Term content = newBelief.getContent();
        // * 🚩创建并导入结果：双前提 | 📝仅在此处用到「当前信念」作为「导出信念」
        // * 🚩【2024-06-06 08:52:56】现场构建「新时间戳」
        final Stamp newStamp = Stamp.uncheckedMerge(newBelief, oldBelief, context.getTime());
        context.doublePremiseTask(context.getCurrentTask(), content, truth, budget, newStamp);
    }

    /**
     * Check if a Sentence provide a better answer to a Question or Goal
     *
     * @param belief       The proposed answer
     * @param questionTask The task to be processed
     * @param context      Reference to the derivation context
     */
    public static void trySolution(Judgement belief, Task questionTask, DerivationContext context) {
        // * 🚩预设&断言
        final Judgement oldBest = questionTask.getBestSolution();
        if (belief == null)
            throw new AssertionError("将解答的必须是「判断」");
        if (questionTask == null || !questionTask.isQuestion())
            throw new AssertionError("要解决的必须是「问题」");

        // * 🚩验证这个信念是否为「解决问题的最优解」
        final float newQ = BudgetFunctions.solutionQuality(questionTask, belief);
        if (oldBest != null) {
            final float oldQ = BudgetFunctions.solutionQuality(questionTask, oldBest);
            // * 🚩新解比旧解还差⇒驳回
            if (oldQ >= newQ)
                return;
        }

        // * 🚩若比先前「最优解」还优，那就确立新的「最优解」
        questionTask.setBestSolution(belief);
        if (questionTask.isInput()) { // moved from Sentence
            // * 🚩同时在此确立「回答」：只在回应「输入的任务」时反映
            context.report(belief, ReportType.ANSWER);
        }
        // * 🚩计算新预算值
        final Question problem = questionTask.asQuestion();
        final Budget budget = BudgetFunctions.solutionEval(problem, belief, questionTask);
        // * 🚩更新「问题任务」的预算值
        final float solutionQuality = BudgetFunctions.solutionQuality(problem, belief);
        final float updatedQuestionPriority = Math.min(
                UtilityFunctions.not(solutionQuality),
                questionTask.getPriority());
        questionTask.setPriority(updatedQuestionPriority);

        // * 🚩尝试「激活任务」
        if (budget == null)
            throw new AssertionError("【2024-06-09 00:45:04】计算出的新预算值不可能为空");
        if (budget.budgetAboveThreshold()) {
            // * 🚩激活任务 | 在此过程中将「当前任务」添加回「新任务」
            context.activatedTask(budget, belief, questionTask.getParentBelief());
        }
    }

    /* -------------------- same terms, difference relations -------------------- */
    /**
     * The task and belief match reversely
     * * 📄<A --> B> + <B --> A>
     *
     * @param context Reference to the derivation context
     */
    static void matchReverse(DerivationContextReason context) {
        // TODO: 过程笔记注释
        // 📄TaskV1@21 "$0.9913;0.1369;0.1447$ <<cup --> $1> ==> <toothbrush --> $1>>.
        // %1.00;0.45% {503 : 38;37}
        // 📄JudgementV1@43 "<<toothbrush --> $1> ==> <cup --> $1>>. %1.0000;0.4475%
        // {483 : 36;39} "
        final Task task = context.getCurrentTask();
        final Judgement belief = context.getCurrentBelief();
        switch (task.getPunctuation()) {
            // * 🚩判断句⇒尝试合并成对称形式（继承⇒相似，蕴含⇒等价）
            case JUDGMENT_MARK:
                inferToSym(task.asJudgement(), belief, context);
                return;
            // * 🚩疑问句⇒尝试执行转换规则
            case QUESTION_MARK:
                conversion(context);
                return;
            default:
                throw new Error("Unknown punctuation of task: " + task.toStringLong());
        }
    }

    /**
     * Inheritance/Implication matches Similarity/Equivalence
     *
     * @param asym    A Inheritance/Implication sentence
     * @param sym     A Similarity/Equivalence sentence
     * @param figure  location of the shared term
     * @param context Reference to the derivation context
     */
    static void matchAsymSym(Sentence asym, Sentence sym, int figure, DerivationContextReason context) {
        final Task task = context.getCurrentTask();
        switch (task.getPunctuation()) {
            // * 🚩判断句⇒尝试合并成对称形式（继承⇒相似，蕴含⇒等价）
            case JUDGMENT_MARK:
                // * 🚩若「当前任务」是「判断」，则两个都会是「判断」
                inferToAsym(asym.asJudgement(), sym.asJudgement(), context);
                return;
            // * 🚩疑问句⇒尝试「继承⇄相似」「蕴含⇄等价」
            case QUESTION_MARK:
                convertRelation(context);
                return;
            default:
                throw new Error("Unknown punctuation of task: " + task.toStringLong());
        }
    }

    /* -------------------- two-premise inference rules -------------------- */
    /**
     * {<S --> P>, <P --> S} |- <S <-> p>
     * Produce Similarity/Equivalence from a pair of reversed
     * Inheritance/Implication
     *
     * @param judgment1 The first premise
     * @param judgment2 The second premise
     * @param context   Reference to the derivation context
     */
    private static void inferToSym(Judgement judgment1, Judgement judgment2, DerivationContextReason context) {
        // TODO: 过程笔记注释
        final Statement s1 = (Statement) judgment1.getContent();
        final Term t1 = s1.getSubject();
        final Term t2 = s1.getPredicate();
        final Term content;
        if (s1 instanceof Inheritance) {
            content = makeSimilarity(t1, t2);
        } else {
            content = makeEquivalence(t1, t2);
        }
        final Truth value1 = judgment1;
        final Truth value2 = judgment2;
        final Truth truth = TruthFunctions.intersection(value1, value2);
        final Budget budget = BudgetFunctions.forward(truth, context);
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<S <-> P>, <P --> S>} |- <S --> P> Produce an Inheritance/Implication
     * from a Similarity/Equivalence and a reversed Inheritance/Implication
     *
     * @param asym    The asymmetric premise
     * @param sym     The symmetric premise
     * @param context Reference to the derivation context
     */
    private static void inferToAsym(Judgement asym, Judgement sym, DerivationContextReason context) {
        // TODO: 过程笔记注释
        final Statement statement = (Statement) asym.getContent();
        final Term sub = statement.getPredicate();
        final Term pre = statement.getSubject();
        final Statement content = makeStatement(statement, sub, pre);
        final Truth truth = TruthFunctions.reduceConjunction(sym, asym);
        final Budget budget = BudgetFunctions.forward(truth, context);
        context.doublePremiseTask(content, truth, budget);
    }

    /* -------------------- one-premise inference rules -------------------- */
    /**
     * {<P --> S>} |- <S --> P> Produce an Inheritance/Implication from a
     * reversed Inheritance/Implication
     *
     * @param context Reference to the derivation context
     */
    private static void conversion(DerivationContextReason context) {
        // TODO: 过程笔记注释
        final Truth truth = TruthFunctions.conversion(context.getCurrentBelief());
        final Budget budget = BudgetFunctions.forward(truth, context);
        convertedJudgment(truth, budget, context);
    }

    /**
     * {<S --> P>} |- <S <-> P>
     * {<S <-> P>} |- <S --> P> Switch between
     * Inheritance/Implication and Similarity/Equivalence
     *
     * @param context Reference to the derivation context
     */
    private static void convertRelation(DerivationContextReason context) {
        // TODO: 过程笔记注释
        final Truth truth = context.getCurrentBelief();
        final Truth newTruth;
        if (((Statement) context.getCurrentTask().getContent()).isCommutative()) {
            newTruth = TruthFunctions.abduction(truth, 1.0f);
        } else {
            newTruth = TruthFunctions.deduction(truth, 1.0f);
        }
        final Budget budget = BudgetFunctions.forward(newTruth, context);
        convertedJudgment(newTruth, budget, context);
    }

    /**
     * Convert judgment into different relation
     * <p>
     * called in MatchingRules
     *
     * @param budget  The budget value of the new task
     * @param truth   The truth value of the new task
     * @param context Reference to the derivation context
     */
    private static void convertedJudgment(Truth newTruth, Budget newBudget, DerivationContext context) {
        // TODO: 过程笔记注释
        Statement content = (Statement) context.getCurrentTask().getContent();
        final Statement beliefContent = (Statement) context.getCurrentBelief().getContent();
        final Term subjT = content.getSubject();
        final Term predT = content.getPredicate();
        final Term subjB = beliefContent.getSubject();
        final Term predB = beliefContent.getPredicate();
        Term otherTerm;
        if (Variable.containVarQ(subjT)) {
            otherTerm = (predT.equals(subjB)) ? predB : subjB;
            content = makeStatement(content, otherTerm, predT);
        }
        if (Variable.containVarQ(predT)) {
            otherTerm = (subjT.equals(subjB)) ? predB : subjB;
            content = makeStatement(content, subjT, otherTerm);
        }
        // * 🚩导出任务
        context.singlePremiseTask(content, Symbols.JUDGMENT_MARK, newTruth, newBudget);
    }
}
