package nars.inference;

import nars.entity.*;
import nars.inference.TruthFunctions.TruthFAnalytic;
import nars.language.*;
import nars.language.VariableProcess.AppliedCompounds;
import nars.language.VariableProcess.Unification;
import nars.io.Symbols;

import static nars.language.MakeTerm.*;

import nars.control.DerivationContextReason;

/**
 * Syllogisms: Inference rules based on the transitivity of the relation.
 */
final class SyllogisticRules {

    /*
     * --------------- rules used in both
     * first-tense inference and higher-tense
     * inference ---------------
     */
    /**
     * <pre>
     * {<S ==> M>, <M ==> P>} |- {<S ==> P>, <P ==> S>}
     * </pre>
     *
     * * 🚩演绎 & 举例
     * * * 📝一个强推理，一个弱推理
     *
     * @param sub     Subject of the first new task
     * @param pre     Predicate of the first new task
     * @param task    The first premise
     * @param belief  The second premise
     * @param context Reference to the derivation context
     */
    static void dedExe(
            Term sub, Term pre,
            Sentence task, Judgement belief,
            DerivationContextReason context) {
        // * 🚩陈述有效才行
        if (Statement.invalidStatement(sub, pre))
            return;
        // * 🚩后续根据「是否反向推理」安排真值和预算值
        final boolean backward = context.isBackward();
        final Statement oldContent = (Statement) task.getContent();

        // * 🚩演绎 & 举例
        deduction(sub, pre, task, belief, context, backward, oldContent);
        exemplification(sub, pre, task, belief, context, backward, oldContent);
    }

    /** 🆕演绎规则 */
    private static void deduction(
            Term sub, Term pre,
            Sentence task, Judgement belief,
            DerivationContextReason context,
            final boolean backward, final Statement oldContent) {
        // * 🚩词项
        final Statement content = makeStatement(oldContent, sub, pre);
        // * 🚩真值
        final Truth truth = backward ? null : TruthFunctions.deduction(task.asJudgement(), belief);
        // * 🚩预算
        final Budget budget = backward ? BudgetInference.backwardWeak(belief, context)
                : BudgetInference.forward(truth, context);
        // * 🚩结论
        context.doublePremiseTask(content, truth, budget);
    }

    /** 🆕举例规则 */
    private static void exemplification(
            Term sub, Term pre,
            Sentence task, Judgement belief,
            DerivationContextReason context,
            final boolean backward, final Statement oldContent) {
        // * 🚩词项
        final Statement content = makeStatement(oldContent, pre, sub);
        // * 🚩真值
        final Truth truth = backward ? null : TruthFunctions.exemplification(task.asJudgement(), belief);
        // * 🚩预算
        final Budget budget = backward ? BudgetInference.backwardWeak(belief, context)
                : BudgetInference.forward(truth, context);
        // * 🚩结论
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<M ==> S>, <M ==> P>} |- {<S ==> P>, <P ==> S>, <S <=> P>}
     * * 📝归因 & 归纳 & 比较
     *
     * @param sub     Subject of the first new task
     * @param pre     Predicate of the first new task
     * @param task    The first premise
     * @param belief  The second premise
     * @param context Reference to the derivation context
     */
    static void abdIndCom(
            Term sub, Term pre,
            Sentence task, Judgement belief,
            DerivationContextReason context) {
        // * 🚩判断结论合法性
        if (Statement.invalidStatement(sub, pre) || Statement.invalidPair(sub.getName(), pre.getName()))
            return;
        // * 🚩提取信息
        final Statement taskContent = (Statement) task.getContent();
        final boolean backward = context.isBackward();

        // * 🚩归因 & 归纳 & 比较
        abduction(sub, pre, task, belief, context, taskContent, backward);
        induction(sub, pre, task, belief, context, taskContent, backward);
        comparison(sub, pre, task, belief, context, taskContent, backward);

    }

    /** 🆕归因 */
    private static void abduction(
            Term sub, Term pre,
            Sentence task, Judgement belief,
            DerivationContextReason context,
            final Statement taskContent, final boolean backward) {
        // * 🚩词项
        final Statement statement = makeStatement(taskContent, sub, pre);
        // * 🚩真值
        final Truth truth = backward ? null : TruthFunctions.abduction(task.asJudgement(), belief);
        // * 🚩预算
        final Budget budget = backward ? BudgetInference.backward(belief, context)
                : BudgetInference.forward(truth, context);
        // * 🚩结论
        context.doublePremiseTask(statement, truth, budget);
    }

    /** 🆕归纳 */
    private static void induction(
            Term sub, Term pre,
            Sentence task, Judgement belief,
            DerivationContextReason context,
            final Statement taskContent, final boolean backward) {
        // * 🚩词项
        final Statement statement = makeStatement(taskContent, pre, sub);
        // * 🚩真值
        final Truth truth = backward ? null : TruthFunctions.induction(task.asJudgement(), belief);
        // * 🚩预算
        final Budget budget = backward ? BudgetInference.backwardWeak(belief, context)
                : BudgetInference.forward(truth, context);
        // * 🚩结论
        context.doublePremiseTask(statement, truth, budget);
    }

    /** 🆕比较 */
    private static void comparison(
            Term sub, Term pre,
            Sentence task, Judgement belief,
            DerivationContextReason context,
            final Statement taskContent, final boolean backward) {
        // * 🚩词项
        final Statement statement = makeStatementSymmetric(taskContent, sub, pre);
        // * 🚩真值
        final Truth truth = backward ? null : TruthFunctions.comparison(task.asJudgement(), belief);
        // * 🚩预算
        final Budget budget = backward ? BudgetInference.backward(belief, context)
                : BudgetInference.forward(truth, context);
        // * 🚩结论
        context.doublePremiseTask(statement, truth, budget);
    }

    /**
     * {<S ==> P>, <M <=> P>} |- <S ==> P>
     * * 📌类比
     * * 📝【2024-07-02 13:27:22】弱推理🆚强推理、前向推理🆚反向推理 不是一个事儿
     *
     * @param subj       Subject of the new task
     * @param pred       Predicate of the new task
     * @param asymmetric The asymmetric premise
     * @param symmetric  The symmetric premise
     * @param context    Reference to the derivation context
     */
    static void analogy(
            Term subj, Term pred,
            Sentence asymmetric, Sentence symmetric,
            DerivationContextReason context) {
        // * 🚩验明合法性
        if (Statement.invalidStatement(subj, pred))
            return;
        // * 🚩提取参数
        final Sentence sentence = context.getCurrentTask();
        final boolean backward = sentence.isQuestion();
        final CompoundTerm task = (CompoundTerm) sentence.getContent();
        // * 🚩词项
        // * 📝取「反对称」那个词项的系词
        final Statement asym = (Statement) asymmetric.getContent();
        final Term content = makeStatement(asym, subj, pred);
        // * 🚩真值
        final Truth truth = backward ? null : TruthFunctions.analogy(asymmetric.asJudgement(), symmetric.asJudgement());
        // * 🚩预算
        final Budget budget = backward
                ? (task.isCommutative()
                        // * 🚩可交换⇒弱推理
                        ? BudgetInference.backwardWeak(asymmetric.asJudgement(), context)
                        // * 🚩不可交换⇒强推理
                        : BudgetInference.backward(symmetric.asJudgement(), context))
                : BudgetInference.forward(truth, context);
        // * 🚩结论
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<S <=> M>, <M <=> P>} |- <S <=> P>
     *
     * @param subject   Subject of the new task
     * @param predicate Predicate of the new task
     * @param belief    The first premise
     * @param task      The second premise
     * @param context   Reference to the derivation context
     */
    static void resemblance(
            Term subject, Term predicate,
            Judgement belief, Sentence task,
            DerivationContextReason context) {
        // * 🚩合法性
        if (Statement.invalidStatement(subject, predicate))
            return;
        // * 🚩提取参数
        final boolean backward = context.isBackward();
        final Statement st = (Statement) belief.getContent();
        // * 🚩词项
        final Term statement = makeStatement(st, subject, predicate);
        // * 🚩真值
        final Truth truth = backward ? null : TruthFunctions.resemblance(belief, task.asJudgement());
        // * 🚩预算
        final Budget budget = backward ? BudgetInference.backward(belief, context)
                : BudgetInference.forward(truth, context);
        // * 🚩结论
        context.doublePremiseTask(statement, truth, budget);
    }

    /* --------------- rules used only in conditional inference --------------- */
    /**
     * <pre>
     * {<<M --> S> ==> <M --> P>>, <M --> S>} |- <M --> P>
     * {<<M --> S> ==> <M --> P>>, <M --> P>} |- <M --> S>
     * {<<M --> S> <=> <M --> P>>, <M --> S>} |- <M --> P>
     * {<<M --> S> <=> <M --> P>>, <M --> P>} |- <M --> S>
     * </pre>
     *
     * * 📝分离规则
     *
     * @param mainSentence The implication/equivalence premise
     * @param subSentence  The premise on part of mainSentence
     * @param side         The location of subSentence in mainSentence
     * @param context      Reference to the derivation context
     */
    static void detachment(
            Sentence mainSentence, Sentence subSentence, int side,
            DerivationContextReason context) {
        // * 🚩合法性
        if (!(mainSentence.getContent() instanceof Implication)
                && !(mainSentence.getContent() instanceof Equivalence)) {
            return;
        }

        // * 🚩提取参数
        final Statement statement = (Statement) mainSentence.getContent();
        final Term subject = statement.getSubject();
        final Term predicate = statement.getPredicate();
        final Judgement belief = context.getCurrentBelief();
        final boolean backward = context.isBackward();

        // * 🚩词项
        final Term term = subSentence.getContent();
        final Term content; // * 【2024-07-02 13:47:18】💭此处在Rust中能改成match，但因为有return，反而不好改写
        if (side == 0 && term.equals(subject)) {
            content = predicate;
        } else if (side == 1 && term.equals(predicate)) {
            content = subject;
        } else {
            return;
        }
        if (content instanceof Statement && ((Statement) content).invalid()) {
            // * 📄【2024-06-15 11:39:40】可能存在「变量统一」后词项无效的情况
            // * * main"<<bird --> bird> ==> <bird --> swimmer>>"
            // * * content"<bird --> bird>"
            // * * sub"<bird --> swimmer>"
            return;
        }

        // * 🚩真值
        final Truth truth = backward
                // * 🚩反向推理⇒空
                ? null
                : statement instanceof Equivalence
                        // * 🚩等价⇒类比
                        ? TruthFunctions.analogy(subSentence.asJudgement(), mainSentence.asJudgement())
                        : side == 0
                                // * 🚩非对称 & 主词 ⇒ 演绎
                                ? TruthFunctions.deduction(mainSentence.asJudgement(), subSentence.asJudgement())
                                // * 🚩其它 ⇒ 归纳
                                : TruthFunctions.abduction(subSentence.asJudgement(), mainSentence.asJudgement());

        // * 🚩预算
        final Budget budget = backward
                ? (
                // * 🚩等价 ⇒ 反向
                statement instanceof Equivalence ? BudgetInference.backward(belief, context)
                        // * 🚩非对称 & 主词 ⇒ 反向弱
                        : side == 0 ? BudgetInference.backwardWeak(belief, context)
                                // * 🚩其它 ⇒ 反向
                                : BudgetInference.backward(belief, context))
                : BudgetInference.forward(truth, context);

        // * 🚩结论
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * <pre>
     * {<(&&, S1, S2, S3) ==> P>, S1} |- <(&&, S2, S3) ==> P>
     * {<(&&, S2, S3) ==> P>, <S1 ==> S2>} |- <(&&, S1, S3) ==> P>
     * {<(&&, S1, S3) ==> P>, <S1 ==> S2>} |- <(&&, S2, S3) ==> P>
     * </pre>
     *
     * * 📝条件演绎/条件归纳
     *
     * @param conditional The conditional premise
     * @param index       The location of the shared term in the condition of
     *                    premise1
     * @param premise2    The premise which, or part of which, appears in the
     *                    condition of premise1
     * @param side        The location of the shared term in premise2:
     *                    0 for subject, 1 for predicate, -1 for the whole term
     * @param context     Reference to the derivation context
     */
    static void conditionalDedInd(
            final Implication conditional, final short indexInCondition,
            final Term premise2, final int side,
            final DerivationContextReason context) {
        // * 🚩提取参数 * //
        final Task task = context.getCurrentTask();
        final Judgement belief = context.getCurrentBelief();
        final boolean conditionalTask = VariableProcess.hasUnificationI(
                premise2, belief.getContent());
        final boolean backward = context.isBackward();
        final boolean deduction = side != 0;

        // * 🚩词项 * //
        // * 🚩获取公共项
        final Term commonComponent;
        final Term newComponent;
        if (side == 0) { // * 在主项
            commonComponent = ((Statement) premise2).getSubject();
            newComponent = ((Statement) premise2).getPredicate();
        } else if (side == 1) { // * 在谓项
            commonComponent = ((Statement) premise2).getPredicate();
            newComponent = ((Statement) premise2).getSubject();
        } else { // * 整个词项
            commonComponent = premise2;
            newComponent = null;
        }
        // * 🚩获取「条件句」的条件
        final Term subj = conditional.getSubject();
        if (!(subj instanceof Conjunction)) {
            return;
        }
        // * 🚩根据「旧条件」选取元素（或应用「变量统一」）
        final Conjunction oldCondition = (Conjunction) subj;
        final int index2 = oldCondition.indexOfComponent(commonComponent);
        final short indexInOldCondition;
        final Statement conditionalUnified; // 经过（潜在的）「变量统一」之后的「前提1」
        if (index2 >= 0) {
            indexInOldCondition = (short) index2;
            conditionalUnified = conditional.clone();
        } else {
            // * 🚩尝试数次匹配，将其中的变量归一化
            // * 📝两次尝试的变量类型相同，但应用的位置不同
            indexInOldCondition = indexInCondition;
            final Term conditionToUnify = oldCondition.componentAt(indexInOldCondition);
            final Unification unification1 = VariableProcess.unifyFindI(conditionToUnify, commonComponent);
            if (unification1.hasUnification()) {
                final AppliedCompounds appliedCompounds = VariableProcess.unifyApplied(
                        conditional, (CompoundTerm) premise2,
                        unification1);
                conditionalUnified = (Statement) appliedCompounds.extractApplied1(); // 📝实际上只需用到一个映射表
            } else if (commonComponent.isSameType(oldCondition)) {
                final Term commonComponentComponent = ((CompoundTerm) commonComponent).componentAt(indexInOldCondition);
                // * 🚩尝试寻找并应用变量归一化 @ 共同子项
                final Unification unification2 = VariableProcess.unifyFindI(
                        conditionToUnify, commonComponentComponent);
                if (unification2.hasUnification()) {
                    final AppliedCompounds appliedCompounds = VariableProcess.unifyApplied(
                            conditional, (CompoundTerm) premise2,
                            unification2);
                    conditionalUnified = (Statement) appliedCompounds.extractApplied1(); // 📝实际上只需用到一个映射表
                } else
                    return;
            } else
                return;
        }
        // * 🚩构造「新条件」
        final Term newCondition;
        if (oldCondition.equals(commonComponent)) {
            newCondition = null;
        } else {
            newCondition = setComponent(oldCondition, indexInOldCondition, newComponent);
        }
        // * 🚩根据「新条件」构造新词项
        final Term content;
        if (newCondition != null) {
            content = makeStatement(conditionalUnified, newCondition, conditionalUnified.getPredicate());
        } else {
            content = conditionalUnified.getPredicate();
        }
        if (content == null) {
            return;
        }

        // * 🚩真值 * //
        final Truth truth = backward ? null
                // * 🚩演绎 ⇒ 演绎
                : deduction ? TruthFunctions.deduction(task.asJudgement(), belief)
                        // * 🚩任务是条件句 ⇒ 归纳（任务→信念，就是反过来的归因）
                        : conditionalTask ? TruthFunctions.induction(belief, task.asJudgement())
                                // * 🚩其它 ⇒ 归纳（信念⇒任务）
                                : TruthFunctions.induction(task.asJudgement(), belief);

        // * 🚩预算 * //
        final Budget budget = backward
                // * 🚩反向⇒弱推理
                ? BudgetInference.backwardWeak(belief, context)
                // * 🚩其它⇒前向
                : BudgetInference.forward(truth, context);

        // * 🚩结论 * //
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<(&&, S1, S2) <=> P>, (&&, S1, S2)} |- P
     * * 📝条件类比
     * * 💭【2024-07-09 18:18:41】实际上是死代码
     * * * 📄禁用「等价⇒复合条件」后，「等价」不再能自`reason_compoundAndCompoundCondition`分派
     *
     * @param premise1 The equivalence premise
     * @param index    The location of the shared term in the condition of
     *                 premise1
     * @param premise2 The premise which, or part of which, appears in the
     *                 condition of premise1
     * @param side     The location of the shared term in premise2:
     *                 0 for subject, 1 for predicate, -1 for the whole term
     * @param context  Reference to the derivation context
     */
    static void conditionalAna(
            Equivalence premise1, short index,
            Implication premise2, int side,
            DerivationContextReason context) {
        // * 🚩提取参数 * //
        final Task task = context.getCurrentTask();
        final Judgement belief = context.getCurrentBelief();
        final boolean conditionalTask = VariableProcess.hasUnificationI(
                premise2, belief.getContent());
        final boolean backward = context.isBackward();

        // * 🚩词项 * //
        final Term commonComponent;
        final Term newComponent;
        if (side == 0) { // * 主项
            commonComponent = ((Statement) premise2).getSubject();
            newComponent = ((Statement) premise2).getPredicate();
        } else if (side == 1) { // * 谓项
            commonComponent = ((Statement) premise2).getPredicate();
            newComponent = ((Statement) premise2).getSubject();
        } else { // 整个词项
            commonComponent = premise2;
            newComponent = null;
        }

        // * 🚩尝试消解条件中的变量，匹配数次未果则返回
        final Term oldConjunction = premise1.getSubject();
        if (!(oldConjunction instanceof Conjunction))
            return;
        final Conjunction oldCondition = (Conjunction) oldConjunction;

        // * 📌【2024-07-09 18:20:33】已尝试「函数式化」但无法验证有效性
        boolean match = VariableProcess.unifyFindD(oldCondition.componentAt(index), commonComponent)
                .applyTo(premise1, premise2);
        if (!match && commonComponent.isSameType(oldCondition)) {
            match = VariableProcess
                    .unifyFindD(oldCondition.componentAt(index), ((CompoundTerm) commonComponent).componentAt(index))
                    .applyTo(premise1, premise2);
        }
        if (!match)
            return;
        final Term newCondition;
        if (oldCondition.equals(commonComponent)) {
            newCondition = null;
        } else {
            newCondition = setComponent(oldCondition, index, newComponent);
        }
        final Term content;
        if (newCondition != null) {
            content = makeStatement(premise1, newCondition, premise1.getPredicate());
        } else {
            content = premise1.getPredicate();
        }
        if (content == null) {
            return;
        }

        // * 🚩真值 * //
        final Truth truth = backward ? null
                // * 🚩条件性任务 ⇒ 比较
                : conditionalTask ? TruthFunctions.comparison(task.asJudgement(), belief)
                        // * 🚩其它 ⇒ 类比
                        : TruthFunctions.analogy(task.asJudgement(), belief);

        // * 🚩预算 * //
        final Budget budget = backward
                // * 🚩反向 ⇒ 弱推理
                ? BudgetInference.backwardWeak(belief, context)
                // * 🚩其它 ⇒ 前向推理
                : BudgetInference.forward(truth, context);

        // * 🚩结论 * //
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<(&&, S2, S3) ==> P>, <(&&, S1, S3) ==> P>} |- <S1 ==> S2>
     * * 📝条件归因，消去S3、P，可能构造<S1 ==> S2>也可能构造<S2 ==> S1>
     * * 🚩返回「是否应用成功」，用于规则表分派
     *
     * @param cond1   The condition of the first premise
     * @param cond2   The condition of the second premise
     * @param st1     The first premise
     * @param st2     The second premise
     * @param context Reference to the derivation context
     * @return Whether there are derived tasks
     */
    static boolean conditionalAbd(
            Term cond1, Term cond2,
            Statement st1, Statement st2,
            DerivationContextReason context) {
        // * 🚩检验合法性 * //
        if (!(st1 instanceof Implication) || !(st2 instanceof Implication)) // 📝都要是蕴含
            return false;
        if (!(cond1 instanceof Conjunction) && !(cond2 instanceof Conjunction)) // 📝必须其中一个是合取
            return false;

        // * 🚩提取参数 * //
        final Task task = context.getCurrentTask();
        final Judgement belief = context.getCurrentBelief();
        final boolean backward = context.isBackward();

        // * 🚩预置词项：分别消去彼此间的「内含条件」
        final Term term1 =
                // if ((cond1 instanceof Conjunction) &&
                // !Variable.containVarDep(cond1.getName())) {
                cond1 instanceof Conjunction
                        // * 🚩合取⇒消去另一半的元素
                        ? reduceComponents((Conjunction) cond1, cond2)
                        // * 🚩其它⇒空
                        : null;
        final Term term2 =
                // if ((cond2 instanceof Conjunction) &&
                // !Variable.containVarDep(cond2.getName())) {
                cond2 instanceof Conjunction
                        // * 🚩合取⇒消去另一半的元素
                        ? reduceComponents((Conjunction) cond2, cond1)
                        // * 🚩其它⇒空
                        : null;

        final Truth truth1 = task.asJudgement();
        final Truth truth2 = belief;
        conditionalAbdDerive(context, belief, backward, st2, term2, term1, truth2, truth1); // 任务→信念
        conditionalAbdDerive(context, belief, backward, st1, term1, term2, truth1, truth2); // 信念→任务
        // * 🚩匹配成功
        return true;
    }

    /** 从「条件归纳」中提取出的「导出」模块 */
    private static boolean conditionalAbdDerive(
            DerivationContextReason context, final Judgement belief, final boolean backward,
            Statement otherStatement,
            final Term otherTerm, final Term selfTerm, final Truth otherTruth, final Truth selfTruth) {
        if (selfTerm == null)
            return false;

        // * 🚩词项 * //
        final Term content = otherTerm != null
                // * 🚩仍然是条件句
                ? makeStatement(otherStatement, otherTerm, selfTerm)
                // * 🚩只剩下条件
                : selfTerm;
        if (content == null)
            return false;

        // * 🚩真值 * //
        final Truth truth = backward ? null
                // * 🚩类比
                : TruthFunctions.abduction(otherTruth, selfTruth);

        // * 🚩预算 * //
        final Budget budget = backward
                // * 🚩反向 ⇒ 弱
                ? BudgetInference.backwardWeak(belief, context)
                // * 🚩其它 ⇒ 前向
                : BudgetInference.forward(truth, context);

        // * 🚩结论 * //
        context.doublePremiseTask(content, truth, budget);

        return true;
    }

    /* -------------------- two-premise inference rules -------------------- */
    /**
     * {<S --> P>, <P --> S} |- <S <-> p>
     * Produce Similarity/Equivalence from a pair of reversed
     * Inheritance/Implication
     * * 📝非对称⇒对称（前向推理）
     *
     * @param judgment1 The first premise
     * @param judgment2 The second premise
     * @param context   Reference to the derivation context
     */
    static void inferToSym(Judgement judgment1, Judgement judgment2, DerivationContextReason context) {
        // * 🚩词项 * //
        final Statement statement1 = (Statement) judgment1.getContent();
        final Term term1 = statement1.getSubject();
        final Term term2 = statement1.getPredicate();
        final Term content = makeStatementSymmetric(statement1, term1, term2);

        // * 🚩真值 * //
        final Truth truth = TruthFunctions.intersection(judgment1, judgment2);

        // * 🚩预算 * //
        final Budget budget = BudgetInference.forward(truth, context);

        // * 🚩结论 * //
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<S <-> P>, <P --> S>} |- <S --> P> Produce an Inheritance/Implication
     * from a Similarity/Equivalence and a reversed Inheritance/Implication
     * * 📝对称⇒非对称（前向推理）
     *
     * @param asym    The asymmetric premise
     * @param sym     The symmetric premise
     * @param context Reference to the derivation context
     */
    static void inferToAsym(Judgement asym, Judgement sym, DerivationContextReason context) {
        // * 🚩词项 * //
        // * 🚩提取 | 📄<S --> P> => S, P
        final Statement asymStatement = (Statement) asym.getContent();
        // * 🚩构建新的相反陈述 | 📄S, P => <P --> S>
        final Term newSubject = asymStatement.getPredicate();
        final Term newPredicate = asymStatement.getSubject();
        final Statement content = makeStatement(asymStatement, newSubject, newPredicate);

        // * 🚩真值 * //
        final Truth truth = TruthFunctions.reduceConjunction(sym, asym);

        // * 🚩预算 * //
        final Budget budget = BudgetInference.forward(truth, context);
        // TODO: 后续可使用函数指针延迟计算

        // * 🚩结论 * //
        context.doublePremiseTask(content, truth, budget);
    }

    /* -------------------- one-premise inference rules -------------------- */
    /**
     * {<P --> S>} |- <S --> P> Produce an Inheritance/Implication from a
     * reversed Inheritance/Implication
     * * 📝转换（反向推理，但使用前向预算值）
     *
     * @param context Reference to the derivation context
     */
    static void conversion(Question taskQuestion, Judgement belief, DerivationContextReason context) {
        // * 🚩真值 * //
        final Truth truth = TruthFunctions.conversion(context.getCurrentBelief());
        // * 🚩预算 * //
        final Budget budget = BudgetInference.forward(truth, context);
        // * 🚩转发到统一的逻辑
        convertedJudgment(truth, budget, context);
    }

    /**
     * {<S --> P>} |- <S <-> P>
     * {<S <-> P>} |- <S --> P> Switch between
     * Inheritance/Implication and Similarity/Equivalence
     * * 📝非对称⇔对称
     *
     * @param context Reference to the derivation context
     */
    static void convertRelation(Question taskQuestion, DerivationContextReason context) {
        // * 🚩真值 * //
        final TruthFAnalytic truthF = ((Statement) taskQuestion.getContent()).isCommutative()
                // * 🚩可交换（相似/等价）⇒归纳
                ? TruthFunctions::analyticAbduction
                // * 🚩不可交换（继承/蕴含）⇒演绎
                : TruthFunctions::analyticDeduction;
        final Truth newTruth = truthF.call(
                // * 🚩基于「当前信念」
                context.getCurrentBelief(),
                1.0f);

        // * 🚩预算 * //
        final Budget budget = BudgetInference.forward(newTruth, context);

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
        // * 🚩词项 * //
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

        // * 🚩结论 * //
        context.singlePremiseTask(newContent, Symbols.JUDGMENT_MARK, newTruth, newBudget);
    }
}
