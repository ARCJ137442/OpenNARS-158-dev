package nars.inference;

import static nars.control.MakeTerm.*;
import static nars.io.Symbols.*;

import nars.control.DerivationContextReason;
import nars.control.VariableInference;
import nars.entity.Judgement;
import nars.entity.Question;
import nars.entity.Sentence;
import nars.entity.Stamp;
import nars.entity.Task;
import nars.inference.TruthFunctions.TruthFSingleReliance;
import nars.io.Symbols;
import nars.language.Inheritance;
import nars.language.Statement;
import nars.language.Term;
import nars.language.Variable;

/**
 * 🆕重新创建「匹配规则」
 * * 🎯用于在「概念推理」中【匹配】内容相近的语句
 * * 📄继承⇄相似
 * * 📄继承+继承→相似
 */
public abstract class MatchingRules {

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
                // * 🚩判断「当前任务」是否能与「当前信念」做修正
                if (currentTask.asJudgement().revisable(belief))
                    revision(currentTask.asJudgement(), belief, context);
                return;
            // * 🚩问题⇒尝试回答「特殊疑问」（此处用「变量替换」解决查询变量）
            case QUESTION_MARK:
                // * 🚩查看是否可以替换「查询变量」，具体替换从「特殊疑问」转变为「一般疑问」
                // * 📄Task :: SentenceV1@49 "<{?1} --> murder>? {105 : 6} "
                // * & Belief: SentenceV1@39 "<{tom} --> murder>. %1.0000;0.7290% {147 : 3;4;2}"
                // * ⇒ Unified SentenceV1@23 "<{tom} --> murder>? {105 : 6} "
                final boolean hasUnified = VariableInference.hasUnification(
                        Symbols.VAR_QUERY,
                        currentTask.getContent().clone(),
                        belief.getContent().clone());
                // * ⚠️只针对「特殊疑问」：传入的只有「带变量问题」，因为「一般疑问」通过直接推理就完成了
                if (hasUnified)
                    // * 🚩此时「当前任务」「当前信念」仍然没变
                    LocalRules.trySolution(belief, currentTask, context);
                return;
            // * 🚩其它
            default:
                System.err.println("未知的语句类型：" + currentTask);
                return;
        }
    }

    /**
     * 🆕基于「概念推理」的「修正」规则
     * * 📝和「直接推理」的唯一区别：有「当前信念」（会作为「父信念」使用 ）
     * * 💭【2024-06-09 01:35:41】需要合并逻辑
     */
    private static void revision(Judgement newBelief, Judgement oldBelief, DerivationContextReason context) {
        // * 🚩计算真值/预算值
        final Truth truth = TruthFunctions.revision(newBelief, oldBelief);
        final Budget budget = BudgetFunctions.revise(newBelief, oldBelief, truth, context);
        final Term content = newBelief.getContent();
        // * 🚩创建并导入结果：双前提 | 📝仅在此处用到「当前信念」作为「导出信念」
        // * 🚩【2024-06-06 08:52:56】现场构建「新时间戳」
        final Stamp newStamp = Stamp.uncheckedMerge(
                newBelief, oldBelief,
                context.getTime());
        context.doublePremiseTask(
                context.getCurrentTask(),
                content,
                truth, budget,
                newStamp);
    }

    /* -------------------- same terms, difference relations -------------------- */
    /**
     * The task and belief match reversely
     * * 📄<A --> B> + <B --> A>
     * * <A --> B>. => <A <-> B>.
     * * <A --> B>? => <A --> B>.
     *
     * @param context Reference to the derivation context
     */
    static void matchReverse(DerivationContextReason context) {
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
                conversion(task.asQuestion(), belief, context);
                return;
            // * 🚩其它⇒报错
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
            // * 🚩判断句⇒尝试合并到非对称形式（相似⇒继承，等价⇒蕴含）
            case JUDGMENT_MARK:
                // * 🚩若「当前任务」是「判断」，则两个都会是「判断」
                inferToAsym(asym.asJudgement(), sym.asJudgement(), context);
                return;
            // * 🚩疑问句⇒尝试「继承⇄相似」「蕴含⇄等价」
            case QUESTION_MARK:
                convertRelation(task.asQuestion(), context);
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
        // * 🚩提取 | 📄<S --> P> => S, P
        final Statement asymStatement = (Statement) asym.getContent();
        // * 🚩构建新的相反陈述 | 📄S, P => <P --> S>
        final Term newSubject = asymStatement.getPredicate();
        final Term newPredicate = asymStatement.getSubject();
        final Statement content = makeStatement(asymStatement, newSubject, newPredicate);
        // * 🚩构建真值，更新预算
        // TODO: 后续可使用函数指针延迟计算
        final Truth truth = TruthFunctions.reduceConjunction(sym, asym);
        final Budget budget = BudgetFunctions.forward(truth, context);
        // * 🚩双前提结论
        context.doublePremiseTask(content, truth, budget);
    }

    /* -------------------- one-premise inference rules -------------------- */
    /**
     * {<P --> S>} |- <S --> P> Produce an Inheritance/Implication from a
     * reversed Inheritance/Implication
     *
     * @param context Reference to the derivation context
     */
    private static void conversion(Question taskQuestion, Judgement belief, DerivationContextReason context) {
        // * 🚩构建真值和预算值
        final Truth truth = TruthFunctions.conversion(context.getCurrentBelief());
        final Budget budget = BudgetFunctions.forward(truth, context);
        // * 🚩转发到统一的逻辑
        convertedJudgment(truth, budget, context);
    }

    /**
     * {<S --> P>} |- <S <-> P>
     * {<S <-> P>} |- <S --> P> Switch between
     * Inheritance/Implication and Similarity/Equivalence
     *
     * @param context Reference to the derivation context
     */
    private static void convertRelation(Question taskQuestion, DerivationContextReason context) {
        // * 🚩根据「可交换性」分派真值函数
        final TruthFSingleReliance truthF = ((Statement) taskQuestion.getContent()).isCommutative()
                // * 🚩可交换（相似/等价）⇒归纳
                ? TruthFunctions::analyticAbduction
                // * 🚩不可交换（继承/蕴含）⇒演绎
                : TruthFunctions::analyticDeduction;
        final Truth newTruth = truthF.call(
                // * 🚩基于「当前信念」
                context.getCurrentBelief(),
                1.0f);
        // * 🚩分派预算值
        final Budget budget = BudgetFunctions.forward(newTruth, context);
        // * 🚩继续向下分派函数
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
    private static void convertedJudgment(Truth newTruth, Budget newBudget, DerivationContextReason context) {
        // * 🚩提取内容
        final Statement taskContent = (Statement) context.getCurrentTask().getContent();
        final Statement beliefContent = (Statement) context.getCurrentBelief().getContent();
        final Term subjT = taskContent.getSubject();
        final Term predT = taskContent.getPredicate();
        final Term subjB = beliefContent.getSubject();
        final Term predB = beliefContent.getPredicate();
        // * 🚩创建内容 | ✅【2024-06-10 10:26:14】已通过「长期稳定性」验证与原先逻辑的稳定
        final Term newSubject, newPredicate;
        if (Variable.containVarQ(predT)) {
            // * 🚩谓词有查询变量⇒用「信念主词/信念谓词」替换
            newSubject = subjT;
            newPredicate = subjT.equals(subjB) ? predB : subjB;
        } else if (Variable.containVarQ(subjT)) {
            // * 🚩主词有查询变量⇒用「信念主词/信念谓词」替换
            newSubject = predT.equals(subjB) ? predB : subjB;
            newPredicate = predT;
        } else {
            // * 🚩否则：直接用「任务主词&任务谓词」替换
            newSubject = subjT;
            newPredicate = predT;
        }
        final Term newContent = makeStatement(taskContent, newSubject, newPredicate);
        // * 🚩导出任务
        context.singlePremiseTask(newContent, Symbols.JUDGMENT_MARK, newTruth, newBudget);
    }
}
