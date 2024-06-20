package nars.inference;

import nars.entity.*;
import nars.inference.TruthFunctions.TruthFSingleReliance;
import nars.language.*;
import nars.language.VariableProcess.AppliedCompounds;
import nars.language.VariableProcess.Unification;
import nars.io.Symbols;

import static nars.io.Symbols.JUDGMENT_MARK;
import static nars.io.Symbols.QUESTION_MARK;
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
     * @param term1    Subject of the first new task
     * @param term2    Predicate of the first new task
     * @param sentence The first premise
     * @param belief   The second premise
     * @param context  Reference to the derivation context
     */
    static void dedExe(Term term1, Term term2, Sentence sentence, Judgement belief,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        if (Statement.invalidStatement(term1, term2)) {
            return;
        }
        final Statement content = (Statement) sentence.getContent();
        final Statement content1 = makeStatement(content, term1, term2);
        final Statement content2 = makeStatement(content, term2, term1);
        Truth truth1 = null;
        Truth truth2 = null;
        final Budget budget1, budget2;
        if (sentence.isQuestion()) {
            budget1 = BudgetInference.backwardWeak(belief, context);
            budget2 = BudgetInference.backwardWeak(belief, context);
        } else {
            final Truth value1 = sentence.asJudgement();
            truth1 = TruthFunctions.deduction(value1, belief);
            truth2 = TruthFunctions.exemplification(value1, belief);
            budget1 = BudgetInference.forward(truth1, context);
            budget2 = BudgetInference.forward(truth2, context);
        }
        context.doublePremiseTask(content1, truth1, budget1);
        context.doublePremiseTask(content2, truth2, budget2);
    }

    /**
     * {<M ==> S>, <M ==> P>} |- {<S ==> P>, <P ==> S>, <S <=> P>}
     *
     * @param term1        Subject of the first new task
     * @param term2        Predicate of the first new task
     * @param taskSentence The first premise
     * @param belief       The second premise
     * @param context      Reference to the derivation context
     */
    static void abdIndCom(Term term1, Term term2, Sentence taskSentence, Judgement belief,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        if (Statement.invalidStatement(term1, term2) || Statement.invalidPair(term1.getName(), term2.getName())) {
            return;
        }
        final Statement taskContent = (Statement) taskSentence.getContent();
        Truth truth1 = null;
        Truth truth2 = null;
        Truth truth3 = null;
        final Budget budget1, budget2, budget3;
        if (taskSentence.isQuestion()) {
            budget1 = BudgetInference.backward(belief, context);
            budget2 = BudgetInference.backwardWeak(belief, context);
            budget3 = BudgetInference.backward(belief, context);
        } else {
            final Truth value1 = taskSentence.asJudgement();
            truth1 = TruthFunctions.abduction(value1, belief);
            truth2 = TruthFunctions.abduction(belief, value1);
            truth3 = TruthFunctions.comparison(value1, belief);
            budget1 = BudgetInference.forward(truth1, context);
            budget2 = BudgetInference.forward(truth2, context);
            budget3 = BudgetInference.forward(truth3, context);
        }
        final Statement statement1 = makeStatement(taskContent, term1, term2);
        final Statement statement2 = makeStatement(taskContent, term2, term1);
        final Statement statement3 = makeStatementSymmetric(taskContent, term1, term2);
        context.doublePremiseTask(statement1, truth1, budget1);
        context.doublePremiseTask(statement2, truth2, budget2);
        context.doublePremiseTask(statement3, truth3, budget3);
    }

    /**
     * {<S ==> P>, <M <=> P>} |- <S ==> P>
     *
     * @param subj       Subject of the new task
     * @param pred       Predicate of the new task
     * @param asymmetric The asymmetric premise
     * @param symmetric  The symmetric premise
     * @param context    Reference to the derivation context
     */
    static void analogy(Term subj, Term pred, Sentence asymmetric, Sentence symmetric,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        if (Statement.invalidStatement(subj, pred)) {
            return;
        }
        final Statement st = (Statement) asymmetric.getContent();
        final Truth truth;
        final Budget budget;
        final Sentence sentence = context.getCurrentTask();
        final CompoundTerm taskTerm = (CompoundTerm) sentence.getContent();
        if (sentence.isQuestion()) {
            truth = null;
            if (taskTerm.isCommutative()) {
                budget = BudgetInference.backwardWeak(asymmetric.asJudgement(), context);
            } else {
                budget = BudgetInference.backward(symmetric.asJudgement(), context);
            }
        } else {
            truth = TruthFunctions.analogy(asymmetric.asJudgement(), symmetric.asJudgement());
            budget = BudgetInference.forward(truth, context);
        }
        Term content = makeStatement(st, subj, pred);
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<S <=> M>, <M <=> P>} |- <S <=> P>
     *
     * @param term1    Subject of the new task
     * @param term2    Predicate of the new task
     * @param belief   The first premise
     * @param sentence The second premise
     * @param context  Reference to the derivation context
     */
    static void resemblance(Term term1, Term term2, Judgement belief, Sentence sentence,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        if (Statement.invalidStatement(term1, term2)) {
            return;
        }
        final Statement st = (Statement) belief.getContent();
        final Truth truth;
        final Budget budget;
        if (sentence.isQuestion()) {
            truth = null;
            budget = BudgetInference.backward(belief, context);
        } else {
            truth = TruthFunctions.resemblance(belief, sentence.asJudgement());
            budget = BudgetInference.forward(truth, context);
        }
        final Term statement = makeStatement(st, term1, term2);
        context.doublePremiseTask(statement, truth, budget);
    }

    /* --------------- rules used only in conditional inference --------------- */
    /**
     * {<<M --> S> ==> <M --> P>>, <M --> S>} |- <M --> P>
     * {<<M --> S> ==> <M --> P>>, <M --> P>} |- <M --> S>
     * {<<M --> S> <=> <M --> P>>, <M --> S>} |- <M --> P>
     * {<<M --> S> <=> <M --> P>>, <M --> P>} |- <M --> S>
     *
     * @param mainSentence The implication/equivalence premise
     * @param subSentence  The premise on part of s1
     * @param side         The location of s2 in s1
     * @param context      Reference to the derivation context
     */
    static void detachment(Sentence mainSentence, Sentence subSentence, int side,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        final Statement statement = (Statement) mainSentence.getContent();
        if (!(statement instanceof Implication) && !(statement instanceof Equivalence)) {
            return;
        }
        final Term subject = statement.getSubject();
        final Term predicate = statement.getPredicate();
        final Term content;
        final Term term = subSentence.getContent();
        if ((side == 0) && term.equals(subject)) {
            content = predicate;
        } else if ((side == 1) && term.equals(predicate)) {
            content = subject;
        } else {
            return;
        }
        // * 📄【2024-06-15 11:39:40】可能存在「变量统一」后词项无效的情况
        // * * main"<<bird --> bird> ==> <bird --> swimmer>>"
        // * * content"<bird --> bird>"
        // * * sub"<bird --> swimmer>"
        if ((content instanceof Statement) && ((Statement) content).invalid()) {
            return;
        }
        final Task task = context.getCurrentTask();
        final Judgement belief = context.getCurrentBelief();
        final Truth truth;
        final Budget budget;
        if (task.isQuestion()) {
            truth = null;
            if (statement instanceof Equivalence) {
                budget = BudgetInference.backward(belief, context);
            } else if (side == 0) {
                budget = BudgetInference.backwardWeak(belief, context);
            } else {
                budget = BudgetInference.backward(belief, context);
            }
        } else {
            if (statement instanceof Equivalence) {
                truth = TruthFunctions.analogy(subSentence.asJudgement(), mainSentence.asJudgement());
            } else if (side == 0) {
                truth = TruthFunctions.deduction(mainSentence.asJudgement(), subSentence.asJudgement());
            } else {
                truth = TruthFunctions.abduction(subSentence.asJudgement(), mainSentence.asJudgement());
            }
            budget = BudgetInference.forward(truth, context);
        }
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<(&&, S1, S2, S3) ==> P>, S1} |- <(&&, S2, S3) ==> P>
     * {<(&&, S2, S3) ==> P>, <S1 ==> S2>} |- <(&&, S1, S3) ==> P>
     * {<(&&, S1, S3) ==> P>, <S1 ==> S2>} |- <(&&, S2, S3) ==> P>
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
            Implication conditional, short index,
            Term premise2, int side,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        final Task task = context.getCurrentTask();
        final Judgement belief = context.getCurrentBelief();
        final boolean deduction = (side != 0);
        final boolean conditionalTask = VariableProcess.hasUnificationI(
                premise2, belief.getContent());
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
        final Statement conditionalUnified; // 经过（潜在的）「变量统一」之后的「前提1」
        if (index2 >= 0) {
            index = (short) index2;
            conditionalUnified = conditional.clone();
        } else {
            // * 🚩尝试数次匹配，将其中的变量归一化
            final Term conditionToUnify = oldCondition.componentAt(index);
            final Unification unification1 = VariableProcess.unifyFindI(conditionToUnify, commonComponent);
            if (unification1.hasUnification()) {
                final AppliedCompounds appliedCompounds = VariableProcess.unifyApplied(
                        conditional, (CompoundTerm) premise2,
                        unification1);
                conditionalUnified = (Statement) appliedCompounds.extractApplied1();
            } else {
                if (commonComponent.isSameType(oldCondition)) {
                    final Term commonComponentComponent = ((CompoundTerm) commonComponent).componentAt(index);
                    final Unification unification2 = VariableProcess.unifyFindI(
                            conditionToUnify, commonComponentComponent);
                    if (unification2.hasUnification()) {
                        final AppliedCompounds appliedCompounds = VariableProcess.unifyApplied(
                                conditional, (CompoundTerm) premise2,
                                unification2);
                        conditionalUnified = (Statement) appliedCompounds.extractApplied1();
                    } else
                        return;
                } else
                    return;
            }
            // boolean hasMatch = VariableProcess.unifyI(
            // oldCondition.componentAt(index), commonComponent,
            // premise1, premise2);
            // if (!hasMatch && (commonComponent.isSameType(oldCondition))) {
            // hasMatch = VariableProcess.unifyI(
            // oldCondition.componentAt(index), ((CompoundTerm)
            // commonComponent).componentAt(index),
            // premise1, premise2);
            // }
            // if (!hasMatch) {
            // return;
            // }
        }
        // * 🚩构造「新条件」
        final Term newCondition;
        if (oldCondition.equals(commonComponent)) {
            newCondition = null;
        } else {
            newCondition = setComponent(oldCondition, index, newComponent);
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
        final Truth truth;
        final Budget budget;
        if (task.isQuestion()) {
            truth = null;
            budget = BudgetInference.backwardWeak(belief, context);
        } else {
            final Truth truth1 = task.asJudgement();
            if (deduction) {
                truth = TruthFunctions.deduction(truth1, belief);
            } else if (conditionalTask) {
                truth = TruthFunctions.induction(belief, truth1);
            } else {
                truth = TruthFunctions.induction(truth1, belief);
            }
            budget = BudgetInference.forward(truth, context);
        }
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<(&&, S1, S2) <=> P>, (&&, S1, S2)} |- P
     *
     * @param premise1 The equivalence premise
     * @param index    The location of the shared term in the condition of
     *                 premise1
     * @param premise2 The premise which, or part of which, appears in the
     *                 condition of premise1
     * @param side     The location of the shared term in premise2: 0 for
     *                 subject, 1
     *                 for predicate, -1 for the whole term
     * @param context  Reference to the derivation context
     */
    static void conditionalAna(
            Equivalence premise1, short index,
            Implication premise2, int side,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        final Task task = context.getCurrentTask();
        final Judgement belief = context.getCurrentBelief();
        final boolean conditionalTask = VariableProcess.hasUnificationI(
                premise2, belief.getContent());
        final Term commonComponent;
        final Term newComponent;
        if (side == 0) {
            commonComponent = ((Statement) premise2).getSubject();
            newComponent = ((Statement) premise2).getPredicate();
        } else if (side == 1) {
            commonComponent = ((Statement) premise2).getPredicate();
            newComponent = ((Statement) premise2).getSubject();
        } else {
            commonComponent = premise2;
            newComponent = null;
        }
        final Term tm = premise1.getSubject();
        if (!(tm instanceof Conjunction))
            return;
        final Conjunction oldCondition = (Conjunction) tm;
        boolean match = VariableProcess.unifyD(
                oldCondition.componentAt(index), commonComponent,
                premise1, premise2);
        if (!match && (commonComponent.isSameType(oldCondition))) {
            match = VariableProcess.unifyD(
                    oldCondition.componentAt(index), ((CompoundTerm) commonComponent).componentAt(index),
                    premise1, premise2);
        }
        if (!match) {
            return;
        }
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
        final Truth truth;
        final Budget budget;
        if (task.isQuestion()) {
            truth = null;
            budget = BudgetInference.backwardWeak(belief, context);
        } else {
            final Truth truth1 = task.asJudgement();
            if (conditionalTask) {
                truth = TruthFunctions.comparison(truth1, belief);
            } else {
                truth = TruthFunctions.analogy(truth1, belief);
            }
            budget = BudgetInference.forward(truth, context);
        }
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<(&&, S2, S3) ==> P>, <(&&, S1, S3) ==> P>} |- <S1 ==> S2>
     *
     * @param cond1   The condition of the first premise
     * @param cond2   The condition of the second premise
     * @param st1     The first premise
     * @param st2     The second premise
     * @param context Reference to the derivation context
     * @return Whether there are derived tasks
     */
    static boolean conditionalAbd(Term cond1, Term cond2, Statement st1, Statement st2,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        if (!(st1 instanceof Implication) || !(st2 instanceof Implication)) {
            return false;
        }
        if (!(cond1 instanceof Conjunction) && !(cond2 instanceof Conjunction)) {
            return false;
        }
        final Term term1;
        final Term term2;
        // if ((cond1 instanceof Conjunction) &&
        // !Variable.containVarDep(cond1.getName())) {
        if (cond1 instanceof Conjunction) {
            term1 = reduceComponents((Conjunction) cond1, cond2);
        } else {
            term1 = null;
        }
        // if ((cond2 instanceof Conjunction) &&
        // !Variable.containVarDep(cond2.getName())) {
        if (cond2 instanceof Conjunction) {
            term2 = reduceComponents((Conjunction) cond2, cond1);
        } else {
            term2 = null;
        }
        if ((term1 == null) && (term2 == null)) {
            return false;
        }
        final Task task = context.getCurrentTask();
        final Judgement belief = context.getCurrentBelief();
        if (term1 != null) {
            final Term content;
            final Truth truth;
            final Budget budget;
            if (term2 != null) {
                content = makeStatement(st2, term2, term1);
            } else {
                content = term1;
            }
            if (task.isQuestion()) {
                truth = null;
                budget = BudgetInference.backwardWeak(belief, context);
            } else {
                truth = TruthFunctions.abduction(belief, task.asJudgement());
                budget = BudgetInference.forward(truth, context);
            }
            context.doublePremiseTask(content, truth, budget);
        }
        if (term2 != null) {
            final Term content2;
            final Truth truth2;
            final Budget budget2;
            if (term1 != null) {
                content2 = makeStatement(st1, term1, term2);
            } else {
                content2 = term2;
            }
            if (task.isQuestion()) {
                truth2 = null;
                budget2 = BudgetInference.backwardWeak(belief, context);
            } else {
                truth2 = TruthFunctions.abduction(task.asJudgement(), belief);
                budget2 = BudgetInference.forward(truth2, context);
            }
            context.doublePremiseTask(content2, truth2, budget2);
        }
        return true;
    }

    /**
     * {(&&, <#x() --> S>, <#x() --> P>>, <M --> P>} |- <M --> S>
     *
     * @param compound           The compound term to be decomposed
     * @param component          The part of the compound to be removed
     * @param isCompoundFromTask Whether the compound comes from the task
     * @param context            Reference to the derivation context
     */
    static void eliminateVarDep(
            CompoundTerm compound, Term component,
            boolean isCompoundFromTask,
            DerivationContextReason context) {
        // TODO: 过程笔记注释
        final Term content = reduceComponents(compound, component);
        if ((content == null) || ((content instanceof Statement) && ((Statement) content).invalid()))
            return;
        final Task task = context.getCurrentTask();
        final Judgement belief = context.getCurrentBelief();
        final Truth truth;
        final Budget budget;
        if (task.isQuestion()) {
            truth = null;
            budget = (isCompoundFromTask ? BudgetInference.backward(belief, context)
                    : BudgetInference.backwardWeak(belief, context));
        } else {
            final Truth v1 = task.asJudgement();
            truth = (isCompoundFromTask ? TruthFunctions.anonymousAnalogy(v1, belief)
                    : TruthFunctions.anonymousAnalogy(belief, v1));
            budget = BudgetInference.compoundForward(truth, content, context);
        }
        context.doublePremiseTask(content, truth, budget);
    }

    // * 📝【2024-06-10 15:25:14】以下函数最初处在「本地规则」，后来迁移到「匹配规则」，现在放置于「三段论规则」

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
    static void matchAsymSym(Sentence asym, Sentence sym, DerivationContextReason context) {
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
        // * 🚩提取内容
        final Statement statement1 = (Statement) judgment1.getContent();
        final Term term1 = statement1.getSubject();
        final Term term2 = statement1.getPredicate();
        // * 🚩构建内容 | 📝直接使用「制作对称」方法
        final Term content = makeStatementSymmetric(statement1, term1, term2);
        // * 🚩计算真值&预算
        final Truth truth = TruthFunctions.intersection(judgment1, judgment2);
        final Budget budget = BudgetInference.forward(truth, context);
        // * 🚩双前提结论
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
        final Budget budget = BudgetInference.forward(truth, context);
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
        final Budget budget = BudgetInference.forward(truth, context);
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
