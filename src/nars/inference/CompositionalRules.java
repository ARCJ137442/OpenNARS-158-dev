package nars.inference;

import java.util.*;

import nars.control.DerivationContextReason;
import nars.entity.*;
import nars.inference.TruthFunctions.TruthFDouble;
import nars.language.*;

import static nars.language.MakeTerm.*;

/**
 * Compound term composition and decomposition rules, with two premises.
 * <p>
 * Forward inference only, except the last group (dependent variable
 * introduction) can also be used backward.
 */
class CompositionalRules {
    /** 🆕用于下边switch分派 */
    private static final String negativeTruthS(Truth truth) {
        return truth.isNegative() ? "N" : "P";
    }

    /* -------------------- intersections and differences -------------------- */
    /**
     * {<S ==> M>, <P ==> M>} |- {
     * <(S|P) ==> M>, <(S&P) ==> M>,
     * <(S-P) ==> M>, <(P-S) ==> M>
     * }
     *
     * @param taskSentence The first premise
     * @param belief       The second premise
     * @param sharedTermI  The location of the shared term
     * @param context      Reference to the derivation context
     */
    static void composeCompound(
            Statement taskContent,
            Statement beliefContent,
            int sharedTermI,
            DerivationContextReason context) {
        // * 🚩前提：任务是判断句（前向推理）、任务与信念类型相同
        if (!context.getCurrentTask().isJudgement()
                || !taskContent.isSameType(beliefContent)) {
            return;
        }
        // * 🚩提取词项
        final int otherSideI = 1 - sharedTermI;
        final Term componentCommon = taskContent.componentAt(sharedTermI);
        final Term componentT = taskContent.componentAt(otherSideI);
        final Term componentB = beliefContent.componentAt(otherSideI);
        // * 🚩预判，分派到「解构」中
        if (componentT instanceof CompoundTerm && ((CompoundTerm) componentT).containAllComponents(componentB)) {
            // * 🚩「任务词项中的另一项」包含「信念词项的另一侧」的所有元素
            decomposeCompound((CompoundTerm) componentT, componentB, componentCommon, sharedTermI, true, context);
            return;
        } else if (componentB instanceof CompoundTerm && ((CompoundTerm) componentB).containAllComponents(componentT)) {
            // * 🚩「信念词项中的另一项」包含「任务词项的另一侧」的所有元素
            decomposeCompound((CompoundTerm) componentB, componentT, componentCommon, sharedTermI, false, context);
            return;
        }

        // * 🚩NAL-3规则：交并差
        composeAsSet(taskContent, sharedTermI, componentCommon, componentT, componentB, context);

        // * 🚩引入变量
        if (taskContent instanceof Inheritance) {
            introVarOuter(taskContent, beliefContent, sharedTermI, context);
            // introVarImage(taskContent, beliefContent, index);
        }
    }

    /** 🆕作为「集合」操作：交并差 */
    private static void composeAsSet(
            Statement taskContent, int sharedTermI,
            final Term componentCommon, final Term componentT, final Term componentB,
            DerivationContextReason context) {
        final Truth truthT = context.getCurrentTask().asJudgement();
        final Truth truthB = context.getCurrentBelief();
        final Truth truthOr = TruthFunctions.union(truthT, truthB);
        final Truth truthAnd = TruthFunctions.intersection(truthT, truthB);
        final Truth truthDif;
        final Term termOr;
        final Term termAnd;
        final Term termDif;

        // * 🚩根据「共有词项的位置」分派
        if (sharedTermI == 0) {
            // * 🚩共有在主项 ⇒ 内涵交，外延交，外延差
            // * 📄"<M ==> S>", "<M ==> P>"
            if (taskContent instanceof Inheritance) {
                // * 🚩「或」内涵交
                termOr = makeIntersectionInt(componentT, componentB);
                // * 🚩「与」外延交
                termAnd = makeIntersectionExt(componentT, componentB);
                // * 🚩根据「真值是否负面」决定「差」的真值
                switch (negativeTruthS(truthT) + negativeTruthS(truthB)) {
                    // * 🚩同正/同负 ⇒ 不予生成
                    case "P" + "P":
                    case "N" + "N":
                        termDif = null;
                        truthDif = null;
                        break;
                    // * 🚩任务正，信念负 ⇒ 词项="(任务-信念)"，真值=任务 ∩ ¬信念
                    case "P" + "N":
                        termDif = makeDifferenceExt(componentT, componentB);
                        truthDif = TruthFunctions.intersection(truthT, TruthFunctions.negation(truthB));
                        break;
                    // * 🚩任务负，信念正 ⇒ 词项="(信念-任务)"，真值=信念 ∩ ¬任务
                    case "N" + "P":
                        termDif = makeDifferenceExt(componentB, componentT);
                        truthDif = TruthFunctions.intersection(truthB, TruthFunctions.negation(truthT));
                        break;
                    // * 🚩其它⇒不可达
                    default:
                        throw new IllegalStateException("unreachable");
                }
            } else if (taskContent instanceof Implication) {
                termOr = makeDisjunction(componentT, componentB);
                termAnd = makeConjunction(componentT, componentB);
                termDif = null;
                truthDif = null;
            } else {
                termOr = null;
                termAnd = null;
                termDif = null;
                truthDif = null;
            }
            // * 🚩统一导出结论："<公共项 ==> 新词项>"
            processComposed(taskContent, componentCommon.clone(), termOr, truthOr, context);
            processComposed(taskContent, componentCommon.clone(), termAnd, truthAnd, context);
            processComposed(taskContent, componentCommon.clone(), termDif, truthDif, context);
        } else { // index == 1
            // * 🚩共有在谓项 ⇒ 内涵交，外延交，内涵差
            // * 📄"<S ==> M>", "<P ==> M>"
            if (taskContent instanceof Inheritance) {
                // * 🚩「或」外延交
                termOr = makeIntersectionExt(componentT, componentB);
                // * 🚩「与」内涵交
                termAnd = makeIntersectionInt(componentT, componentB);
                // * 🚩根据「真值是否负面」决定「差」的真值
                switch (negativeTruthS(truthT) + negativeTruthS(truthB)) {
                    // * 🚩同正/同负 ⇒ 不予生成
                    case "P" + "P":
                    case "N" + "N":
                        termDif = null;
                        truthDif = null;
                        break;
                    // * 🚩任务正，信念负 ⇒ 词项="(任务-信念)"，真值=任务 ∩ ¬信念
                    case "P" + "N":
                        termDif = makeDifferenceInt(componentT, componentB);
                        truthDif = TruthFunctions.intersection(truthT, TruthFunctions.negation(truthB));
                        break;
                    // * 🚩任务负，信念正 ⇒ 词项="(信念-任务)"，真值=信念 ∩ ¬任务
                    case "N" + "P":
                        termDif = makeDifferenceInt(componentB, componentT);
                        truthDif = TruthFunctions.intersection(truthB, TruthFunctions.negation(truthT));
                        break;
                    // * 🚩其它⇒不可达
                    default:
                        throw new IllegalStateException("unreachable");
                }
            } else if (taskContent instanceof Implication) {
                termOr = makeConjunction(componentT, componentB);
                termAnd = makeDisjunction(componentT, componentB);
                termDif = null;
                truthDif = null;
            } else {
                termOr = null;
                termAnd = null;
                termDif = null;
                truthDif = null;
            }
            // * 🚩统一导出结论："<新词项 ==> 公共项>"
            processComposed(taskContent, termOr, componentCommon.clone(), truthOr, context);
            processComposed(taskContent, termAnd, componentCommon.clone(), truthAnd, context);
            processComposed(taskContent, termDif, componentCommon.clone(), truthDif, context);
        }
    }

    /**
     * Finish composing implication term
     * * 📌根据主谓项、真值 创建新内容，并导出结论
     *
     * @param premise1  Type of the contentInd
     * @param subject   Subject of contentInd
     * @param predicate Predicate of contentInd
     * @param truth     Truth of the contentInd
     * @param context   Reference to the derivation context
     */
    private static void processComposed(
            Statement taskContent,
            Term subject, Term predicate, Truth truth,
            DerivationContextReason context) {
        // * 🚩跳过空值
        if (subject == null || predicate == null)
            return;

        // * 🚩词项：不能跟任务、信念 内容相同
        final Term content = makeStatement(taskContent, subject, predicate);
        final Term beliefContent = context.getCurrentBelief().getContent(); // ! 假定一定有「当前信念」
        if (content == null || content.equals(taskContent) || content.equals(beliefContent))
            return;

        // * 🚩预算：复合前向
        final Budget budget = BudgetInference.compoundForward(truth, content, context);
        // * 🚩结论
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<(S|P) ==> M>, <P ==> M>} |- <S ==> M>
     *
     * @param implication        The implication term to be decomposed
     * @param componentCommon    The part of the implication to be removed
     * @param term1              The other term in the contentInd
     * @param side               The location of the shared term: 0 for subject, 1
     *                           for predicate
     * @param isCompoundFromTask Whether the implication comes from the task
     * @param context            Reference to the derivation context
     */
    private static void decomposeCompound(
            CompoundTerm compound, Term component,
            Term term1, int side,
            boolean isCompoundFromTask, DerivationContextReason context) {
        // * 🚩「参考的复合词项」是 陈述/像 ⇒ 不解构
        if (compound instanceof Statement || compound instanceof ImageExt || compound instanceof ImageInt)
            return;

        // * 🚩将当前元素从复合词项中移除
        final Term term2 = reduceComponents(compound, component);
        if (term2 == null)
            return;

        final Task task = context.getCurrentTask();

        // * 🚩词项 * //
        final Statement oldTaskContent = (Statement) task.getContent();
        final Term content = side == 0
                // * 🚩共有前项
                ? makeStatement(oldTaskContent, term1, term2)
                // * 🚩共有后项
                : makeStatement(oldTaskContent, term2, term1);
        if (content == null)
            return;

        // * 🚩真值 * //
        if (!task.isJudgement())
            return; // ! 只能是判断句、正向推理
        final Judgement belief = context.getCurrentBelief();
        final Judgement taskJudgement = context.getCurrentTask().asJudgement();
        final Truth v1, v2;
        if (isCompoundFromTask) {
            v1 = taskJudgement;
            v2 = belief;
        } else {
            v1 = belief;
            v2 = taskJudgement;
        }

        // * 🚩根据各词项类型分派
        final Truth truth;
        if (side == 0) {
            // * 🚩共用主项
            if (oldTaskContent instanceof Inheritance)
                // * 🚩旧任务内容 <: 继承
                if (compound instanceof IntersectionExt)
                    // * 🚩外延交 ⇒ 合取
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                else if (compound instanceof IntersectionInt)
                    // * 🚩内涵交 ⇒ 析取
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                else if (compound instanceof SetInt && component instanceof SetInt)
                    // * 🚩内涵集-内涵集 ⇒ 合取
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                else if (compound instanceof SetExt && component instanceof SetExt)
                    // * 🚩外延集-外延集 ⇒ 析取
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                else if (compound instanceof DifferenceExt)
                    // * 🚩外延差
                    if (compound.componentAt(0).equals(component))
                        // * 🚩内容正好为被减项 ⇒ 析取（反向）
                        truth = TruthFunctions.reduceDisjunction(v2, v1);
                    else
                        // * 🚩其它 ⇒ 合取否定
                        truth = TruthFunctions.reduceConjunctionNeg(v1, v2);
                else
                    // * 🚩其它 ⇒ 否决
                    return;
            else if (oldTaskContent instanceof Implication)
                // * 🚩旧任务内容 <: 蕴含
                if (compound instanceof Conjunction)
                    // * 🚩合取 ⇒ 合取
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                else if (compound instanceof Disjunction)
                    // * 🚩析取 ⇒ 析取
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                else
                    // * 🚩其它 ⇒ 否决
                    return;
            else
                // * 🚩其它 ⇒ 否决
                return;
        } else {
            // * 🚩共用谓项
            if (oldTaskContent instanceof Inheritance)
                // * 🚩旧任务内容 <: 继承
                if (compound instanceof IntersectionInt)
                    // * 🚩内涵交 ⇒ 合取
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                else if (compound instanceof IntersectionExt)
                    // * 🚩外延交 ⇒ 析取
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                else if (compound instanceof SetExt && component instanceof SetExt)
                    // * 🚩外延集-外延集 ⇒ 合取
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                else if (compound instanceof SetInt && component instanceof SetInt)
                    // * 🚩内涵集-内涵集 ⇒ 析取
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                else if (compound instanceof DifferenceInt)
                    // * 🚩内涵差
                    if (compound.componentAt(1).equals(component))
                        // * 🚩内容正好为所减项 ⇒ 析取（反向）
                        truth = TruthFunctions.reduceDisjunction(v2, v1);
                    else
                        // * 🚩其它 ⇒ 合取否定
                        truth = TruthFunctions.reduceConjunctionNeg(v1, v2);
                else
                    return;
            else if (oldTaskContent instanceof Implication)
                // * 🚩旧任务内容 <: 蕴含
                if (compound instanceof Disjunction)
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                else if (compound instanceof Conjunction)
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                else
                    // * 🚩其它 ⇒ 否决
                    return;
            else
                // * 🚩其它 ⇒ 否决
                return;
        }

        // * 🚩预算 * //
        final Budget budget = BudgetInference.compoundForward(truth, content, context);

        // * 🚩结论 * //
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * {(||, S, P), P} |- S
     * {(&&, S, P), P} |- S
     *
     * @param implication        The implication term to be decomposed
     * @param componentCommon    The part of the implication to be removed
     * @param isCompoundFromTask Whether the implication comes from the task
     * @param context            Reference to the derivation context
     */
    static void decomposeStatement(
            CompoundTerm compound, Term component,
            boolean isCompoundFromTask,
            DerivationContextReason context) {
        final Task task = context.getCurrentTask();
        final Judgement belief = context.getCurrentBelief();
        final boolean backward = context.isBackward();
        // * 🚩删去指定的那个元素，用删去之后的剩余元素做结论
        final Term content = reduceComponents(compound, component);
        if (content == null)
            return;
        final Truth truth;
        final Budget budget;
        // * 🚩反向推理：尝试答问
        if (backward) {
            // * 📄(||,A,B)? + A. => B?
            // * 🚩先将剩余部分作为「问题」提出
            // ! 📄原版bug：当输入 (||,A,?1)? 时，因「弹出的变量复杂度为零」预算推理「除以零」爆炸
            if (!content.zeroComplexity()) {
                budget = BudgetInference.compoundBackward(content, context);
                context.doublePremiseTask(content, null, budget);
            }
            // * 🚩再将对应有「概念」与「信念」的内容作为新的「信念」放出
            // special inference to answer conjunctive questions with query variables
            if (!Variable.containVarQ(task.getContent()))
                return;
            // * 🚩只有在「回答合取问题」时，取出其中的项构建新任务
            final Concept contentConcept = context.termToConcept(content);
            if (contentConcept == null)
                return;
            // * 🚩只在「内容对应了概念」时，取出「概念」中的信念
            final Judgement contentBelief = contentConcept.getBelief(task);
            if (contentBelief == null)
                return;
            // * 🚩只在「概念中有信念」时，以这个信念作为「当前信念」构建新任务
            final Stamp newStamp = Stamp.uncheckedMerge(
                    task,
                    contentBelief, // * 🚩实际上就是需要与「已有信念」的证据基合并
                    context.getTime(),
                    context.getMaxEvidenceBaseLength());
            // * 🚩【2024-06-07 13:41:16】现在直接从「任务」构造新的「预算值」
            final Task contentTask = new Task(contentBelief, task);
            // ! 🚩【2024-05-19 20:29:17】现在移除：直接在「导出结论」处指定
            final Term conj = makeConjunction(component, content);
            // * ↓不会用到`context.getCurrentTask()`、`newStamp`
            truth = TruthFunctions.intersection(contentBelief, belief);
            // * ↓不会用到`context.getCurrentTask()`、`newStamp`
            final Budget budget1 = BudgetInference.compoundForward(truth, conj, context);
            // ! ⚠️↓会用到`context.getCurrentTask()`、`newStamp`：构建新结论时要用到
            // * ✅【2024-05-21 22:38:52】现在通过「参数传递」抵消了对`context.getCurrentTask`的访问
            context.doublePremiseTask(contentTask, conj, truth, budget1, newStamp);
        }
        // * 🚩前向推理：直接用于构造信念
        else {
            // * 🚩选取前提真值 | ⚠️前后件语义不同
            final Truth v1, v2;
            if (isCompoundFromTask) {
                v1 = task.asJudgement();
                v2 = belief;
            } else {
                v1 = belief;
                v2 = task.asJudgement();
            }
            // * 🚩选取真值函数
            final TruthFDouble truthF;
            if (compound instanceof Conjunction)
                truthF = TruthFunctions::reduceConjunction;
            else if (compound instanceof Disjunction)
                truthF = TruthFunctions::reduceDisjunction;
            else
                return;
            // * 🚩构造真值、预算值，双前提结论
            truth = truthF.call(v1, v2);
            budget = BudgetInference.compoundForward(truth, content, context);
            context.doublePremiseTask(content, truth, budget);
        }
    }

    /* --------------- rules used for variable introduction --------------- */

    /**
     * 🆕入口之一：变量引入
     * ! ⚠️【2024-07-23 12:20:18】逻辑未完全被测试覆盖，代码理解度低
     * * 📝【2024-07-23 12:04:33】OpenNARS 3.1.0仍然没有样例注释……
     * * 📄一例（平凡情况）：
     * * * originalMainSentence = "<<$1 --> swimmer> ==> <$1 --> bird>>"
     * * * subSentence = "<bird --> animal>"
     * * * component = "<$1 --> bird>"
     * * * subContent = "<bird --> animal>"
     * * * index = 1 @ originalMainSentence
     * * * => "<<$1 --> swimmer> ==> <$1 --> bird>>"
     */
    static void introVarSameSubjectOrPredicate(
            Judgement originalMainSentence, Judgement subSentence,
            Term component, CompoundTerm subContent,
            int index,
            DerivationContextReason context) {
        // * 🚩词项 * //
        final Sentence clonedMain = originalMainSentence.sentenceClone();
        final Term clonedMainT = clonedMain.getContent();
        // * 🚩仅对复合词项
        if (!(clonedMainT instanceof CompoundTerm) || !(subContent instanceof CompoundTerm))
            return;

        final CompoundTerm mainCompound = (CompoundTerm) clonedMainT;
        final CompoundTerm subCompound = subContent.clone();
        // * 🚩对内部内容，仅适用于「继承×继承」与「相似×相似」
        if (!((component instanceof Inheritance && subContent instanceof Inheritance) ||
                (component instanceof Similarity && subContent instanceof Similarity)))
            return;
        final Statement componentS = (Statement) component;
        final Statement subContentS = (Statement) subContent;
        // CompoundTerm result = mainCompound;
        if (componentS.equals(subContentS))
            // wouldn't make sense to create a conjunction here,
            // would contain a statement twice
            return;

        final Term content;
        if (componentS.getPredicate().equals(subContentS.getPredicate())
                && !(componentS.getPredicate() instanceof Variable)) {
            // ! ⚠️【2024-07-23 12:17:44】目前还没真正触发过此处逻辑
            // ! * 诸多尝试均被「变量分离规则」等 截胡
            /*
             * 📄已知如下输入无法触发：
             * <swam --> swimmer>.
             * <swam --> bird>.
             * <bird --> swimmer>.
             * <<$1 --> swimmer> ==> <$1 --> bird>>.
             * <<bird --> $1> ==> <swimmer --> $1>>.
             * 1000
             */
            final Variable V = makeVarD(mainCompound, subCompound); // * ✅不怕重名：现在始终是「最大词项的最大id+1」的模式
            final CompoundTerm zw = (CompoundTerm) mainCompound.componentAt(index).clone();
            final CompoundTerm zw2 = (CompoundTerm) setComponent(zw, 1, V);
            final CompoundTerm newSubCompound = (CompoundTerm) setComponent(subCompound, 1, V);
            if (zw2 == null || newSubCompound == null || zw2.equals(newSubCompound))
                return;
            final Conjunction res = (Conjunction) makeConjunction(zw, newSubCompound);
            content = (CompoundTerm) setComponent(mainCompound, index, res);
        } else if (componentS.getSubject().equals(subContentS.getSubject())
                && !(componentS.getSubject() instanceof Variable)) {
            // ! ⚠️【2024-07-23 12:17:44】目前还没真正触发过此处逻辑
            // ! * 诸多尝试均被「变量分离规则」等 截胡
            /*
             * 📄已知如下输入无法触发：
             * <swam --> swimmer>.
             * <swam --> bird>.
             * <bird --> swimmer>.
             * <<$1 --> swimmer> ==> <$1 --> bird>>.
             * <<bird --> $1> ==> <swimmer --> $1>>.
             * 1000
             */
            final Variable V = makeVarD(mainCompound, subCompound); // * ✅不怕重名：现在始终是「最大词项的最大id+1」的模式
            final CompoundTerm zw = (CompoundTerm) mainCompound.componentAt(index).clone();
            final CompoundTerm zw2 = (CompoundTerm) setComponent(zw, 0, V);
            final CompoundTerm newSubCompound = (CompoundTerm) setComponent(subCompound, 0, V);
            if (zw2 == null || newSubCompound == null || zw2.equals(newSubCompound))
                return;
            final Conjunction res = (Conjunction) makeConjunction(zw2, newSubCompound);
            content = (CompoundTerm) setComponent(mainCompound, index, res);
        } else {
            content = mainCompound; // ? 【2024-07-23 12:20:27】为何要重复得出结果
        }

        // * 🚩真值 * //
        final Truth truth = TruthFunctions.induction(originalMainSentence, subSentence);

        // * 🚩预算 * //
        final Budget budget = BudgetInference.compoundForward(truth, content, context);

        // * 🚩结论 * //
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * Introduce a dependent variable in an outer-layer conjunction
     * * 📝「变量外引入」系列规则
     *
     * * 📌导出结论：「正反似合」
     * * * 外延正传递（归因/归纳）
     * * * 外延反传递（归因/归纳）
     * * * 相似の传递（比较）
     * * * 因变量引入（合取）
     *
     * * 📄@主项: "<M --> S>" × "<M --> P>"
     * * * => "<<$1 --> S> ==> <$1 --> P>>"
     * * * => "<<$1 --> P> ==> <$1 --> S>>"
     * * * => "<<$1 --> S> <=> <$1 --> P>>"
     * * * => "(&&,<#1 --> S>,<#1 --> P>)"
     *
     * * 📄@谓项: "<S --> M>" × "<P --> M>"
     * * * => "<<S --> $1> ==> <P --> $1>>"
     * * * => "<<P --> $1> ==> <S --> $1>>"
     * * * => "<<P --> $1> <=> <S --> $1>>"
     * * * => "(&&,<P --> #1>,<S --> #1>)"
     *
     * @param taskContent   The first premise <M --> S>
     * @param beliefContent The second premise <M --> P>
     * @param index         The location of the shared term:
     *                      0 for subject, 1 for predicate
     * @param context       Reference to the derivation context
     */
    private static void introVarOuter(
            Statement taskContent,
            Statement beliefContent,
            int index,
            DerivationContextReason context) {
        // * 🚩任务/信念 的真值 | 仅适用于前向推理
        final Truth truthT = context.getCurrentTask().asJudgement();
        final Truth truthB = context.getCurrentBelief();

        // * 🚩词项初步：引入变量 * //
        final Statement[] statesInd = introVarStatesInd(taskContent, beliefContent, index);
        final Statement stateI1 = statesInd[0];
        final Statement stateI2 = statesInd[1];

        final Statement[] statesDep = introVarStatesDep(taskContent, beliefContent, index);
        final Statement stateD1 = statesDep[0];
        final Statement stateD2 = statesDep[1];

        // * 🚩继续分派：词项、真值、预算、结论 * //
        introVarOuter1(stateI1, stateI2, truthT, truthB, context);
        introVarOuter2(stateI1, stateI2, truthT, truthB, context);
        introVarOuter3(stateI1, stateI2, truthT, truthB, context);
        introVarOuter4(stateD1, stateD2, truthT, truthB, context);
    }

    /**
     * 🆕以「变量外引入」的内部词项，计算「引入状态」陈述
     * * 📌引入的是「独立变量/自变量」"$"
     * * 🎯产生的陈述（二元组）用于生成新结论内容
     */
    private static Statement[] introVarStatesInd(
            final Statement taskContent, final Statement beliefContent,
            final int index) {
        final Variable varInd = makeVarI(taskContent, beliefContent);
        final Term term11, term12, term21, term22;
        final Term needCommon1, needCommon2;
        // * 🚩根据索引决定「要组成新陈述的词项的位置」
        if (index == 0) {
            term11 = varInd;
            term21 = varInd;
            term12 = needCommon1 = taskContent.getPredicate();
            term22 = needCommon2 = beliefContent.getPredicate();
        } else { // index == 1
            term11 = needCommon1 = taskContent.getSubject();
            term21 = needCommon2 = beliefContent.getSubject();
            term12 = varInd;
            term22 = varInd;
        }
        // * 🚩寻找「第二个相同词项」并在内容中替换 | 对「外延像@0」「内涵像@1」的特殊处理
        /// * 📌【2024-07-23 13:19:30】此处原码与secondCommonTerm相同，故提取简并
        final Term secondCommonTerm = secondCommonTerm(needCommon1, needCommon2, index);
        if (secondCommonTerm != null) {
            // * 🚩产生一个新的独立变量，并以此替换
            final Variable varInd2 = makeVarI(taskContent, beliefContent, varInd);
            final HashMap<Term, Term> subs = new HashMap<>();
            subs.put(secondCommonTerm, varInd2);
            // ! ⚠️在此期间【修改】其【所指向】的词项
            VariableProcess.applySubstitute(needCommon1, subs);
            VariableProcess.applySubstitute(needCommon2, subs);
        }
        // * 🚩返回：从元素构造继承陈述
        return new Statement[] { makeInheritance(term11, term12), makeInheritance(term21, term22) };
    }

    /**
     * 🆕以「变量外引入」的内部词项，计算「引入状态」陈述
     * * 📌引入的是「非独变量/因变量」"#"
     * * 🎯产生的陈述（二元组）用于生成新结论内容
     */
    private static Statement[] introVarStatesDep(
            final Statement taskContent, final Statement beliefContent,
            final int index) {
        final Variable varDep = makeVarD(taskContent, beliefContent);
        final Statement state1, state2;
        if (index == 0) {
            state1 = makeInheritance(varDep, taskContent.getPredicate());
            state2 = makeInheritance(varDep, beliefContent.getPredicate());
        } else {
            state1 = makeInheritance(taskContent.getSubject(), varDep);
            state2 = makeInheritance(beliefContent.getSubject(), varDep);
        }
        return new Statement[] { state1, state2 };
    }

    /**
     * 「变量外引入」规则 结论1
     * * 📄"<bird --> animal>" × "<bird --> swimmer>"
     * * * => "<<$1 --> animal> ==> <$1 --> swimmer>>"
     * * 📄"<sport --> competition>" × "<chess --> competition>"
     * * * => "<<sport --> $1> ==> <chess --> $1>>"
     *
     * @param state1
     * @param state2
     * @param truthT
     * @param truthB
     * @param context
     */
    private static void introVarOuter1(
            Statement state1, Statement state2,
            Truth truthT, Truth truthB,
            DerivationContextReason context) {
        final Term content = makeImplication(state1, state2);
        if (content == null)
            return;
        final Truth truth = TruthFunctions.induction(truthT, truthB);
        final Budget budget = BudgetInference.compoundForward(truth, content, context);
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * 「变量外引入」规则 结论2
     * * 📄"<bird --> animal>" × "<bird --> swimmer>"
     * * * => "<<$1 --> swimmer> ==> <$1 --> animal>>"
     * * 📄"<sport --> competition>" × "<chess --> competition>"
     * * * => "<<chess --> $1> ==> <sport --> $1>>"
     *
     * @param state1
     * @param state2
     * @param truthT
     * @param truthB
     * @param context
     */
    private static void introVarOuter2(
            Statement state1, Statement state2,
            Truth truthT, Truth truthB,
            DerivationContextReason context) {
        final Term content = makeImplication(state2, state1);
        if (content == null)
            return;
        final Truth truth = TruthFunctions.induction(truthB, truthT);
        final Budget budget = BudgetInference.compoundForward(truth, content, context);
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * 「变量外引入」规则 结论3
     * * 📄"<bird --> animal>" × "<bird --> swimmer>"
     * * * => "<<$1 --> animal> <=> <$1 --> swimmer>>"
     * * 📄"<sport --> competition>" × "<chess --> competition>"
     * * * => "<<chess --> $1> <=> <sport --> $1>>"
     *
     * @param state1
     * @param state2
     * @param truthT
     * @param truthB
     * @param context
     */
    private static void introVarOuter3(
            final Statement state1, final Statement state2,
            final Truth truthT, final Truth truthB,
            DerivationContextReason context) throws AssertionError {
        final Term content = makeEquivalence(state1, state2);
        if (content == null)
            return;
        final Truth truth = TruthFunctions.comparison(truthT, truthB);
        final Budget budget = BudgetInference.compoundForward(truth, content, context);
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * 「变量外引入」规则 结论4
     * * 📄"<bird --> animal>" × "<bird --> swimmer>"
     * * * => "(&&,<#1 --> animal>,<#1 --> swimmer>)"
     * * 📄"<sport --> competition>" × "<chess --> competition>"
     * * * => "(&&,<chess --> #1>,<sport --> #1>)"
     *
     * @param state1
     * @param state2
     * @param truthT
     * @param truthB
     * @param context
     */
    private static void introVarOuter4(
            final Statement state1, final Statement state2,
            final Truth truthT, final Truth truthB,
            DerivationContextReason context) {
        final Term content = makeConjunction(state1, state2);
        if (content == null)
            return;
        final Truth truth = TruthFunctions.intersection(truthT, truthB);
        final Budget budget = BudgetInference.compoundForward(truth, content, context);
        context.doublePremiseTaskNotRevisable(content, truth, budget);
    }

    /**
     * {<M --> S>, <C ==> <M --> P>>} |- <(&&, <#x --> S>, C) ==> <#x --> P>>
     * {<M --> S>, (&&, C, <M --> P>)} |- (&&, C, <<#x --> S> ==> <#x --> P>>)
     *
     * @param taskContent   The first premise directly used in internal induction,
     *                      <M --> S>
     * @param beliefContent The componentCommon to be used as a premise in
     *                      internal induction, <M --> P>
     * @param oldCompound   The whole contentInd of the first premise, Implication
     *                      or Conjunction
     * @param context       Reference to the derivation context
     */
    static void introVarInner(
            Statement premise1, Statement premise2,
            CompoundTerm oldCompound,
            DerivationContextReason context) {
        final Task task = context.getCurrentTask();
        final Judgement belief = context.getCurrentBelief();
        // * 🚩仅适用于前向推理
        if (!task.isJudgement())
            return;
        // * 🚩前提1与前提2必须是相同类型，且「旧复合词项」不能包括前提1
        if (!premise1.isSameType(premise2) || oldCompound.containComponent(premise1))
            return;

        // * 🚩计算共有词项
        final Term[] commonTerms = introVarCommons(premise1, premise2);
        if (commonTerms == null)
            return;
        final Term commonTerm1 = commonTerms[0], commonTerm2 = commonTerms[1];

        // * 🚩继续向下分派
        introVarInner1(premise1, oldCompound, task, belief, commonTerm1, commonTerm2, context);
        introVarInner2(premise1, oldCompound, task, belief, commonTerm1, commonTerm2, context);
    }

    /**
     * 🆕以「变量内引入」的内部词项，计算「共有词项」
     * * 🎯产生的词项（二元组/空）用于生成新结论内容
     */
    private static Term[] introVarCommons(final Statement premise1, final Statement premise2) {
        final Term term11 = premise1.getSubject();
        final Term term21 = premise2.getSubject();
        final Term term12 = premise1.getPredicate();
        final Term term22 = premise2.getPredicate();
        // * 🚩轮流判等以决定所抽取的词项
        if (term11.equals(term21))
            // * 🚩共有主项 ⇒ 11→(12×22)
            return new Term[] { term11, secondCommonTerm(term12, term22, 0) };
        else if (term12.equals(term22))
            // * 🚩共有谓项 ⇒ 12→(11×21)
            return new Term[] { term12, secondCommonTerm(term11, term21, 0) };
        else
            // * 🚩无共有词项⇒空
            return null;
    }

    /**
     * 「变量内引入」规则 结论1
     * * 📝引入第二个变量，并在替换后产生一个合取
     *
     * * 📄"<{lock1} --> lock>" × "<{lock1} --> (/,open,$1,_)>"
     * * * @ "<<$1 --> key> ==> <{lock1} --> (/,open,$1,_)>>"
     * * * => "(&&,<#2 --> lock>,<<$1 --> key> ==> <#2 --> (/,open,$1,_)>>)"
     *
     * * 📄"<{Tweety} --> [chirping]>" × "<robin --> [chirping]>"
     * * * @ "(&&,<robin --> [chirping]>,<robin --> [with_wings]>)"
     * * * => "(&&,<robin --> #1>,<robin --> [with_wings]>,<{Tweety} --> #1>)"
     */
    private static void introVarInner1(
            Statement premise1, CompoundTerm oldCompound,
            final Task task, final Judgement belief,
            final Term commonTerm1, final Term commonTerm2,
            DerivationContextReason context) {
        // * 🚩词项 * //
        final CompoundTerm content = (CompoundTerm) makeConjunction(premise1, oldCompound);
        if (content == null)
            return;
        // * 🚩将「共有词项」替换成变量
        final HashMap<Term, Term> substitute = new HashMap<>();
        final Variable varD = makeVarD(content);
        substitute.put(commonTerm1, varD);
        VariableProcess.applySubstitute(content, substitute);

        // * 🚩真值 * //
        final Truth truth = TruthFunctions.intersection(task.asJudgement(), belief);

        // * 🚩预算 * //
        final Budget budget = BudgetInference.forward(truth, context);

        // * 🚩结论 * //
        context.doublePremiseTaskNotRevisable(content, truth, budget);
    }

    /**
     * 「变量内引入」规则 结论2
     * * 📝引入第二个变量，并在替换后产生一个蕴含
     *
     * * 📄"<{lock1} --> lock>" × "<{lock1} --> (/,open,$1,_)>"
     * * * @ "<<$1 --> key> ==> <{lock1} --> (/,open,$1,_)>>"
     * * * => "<(&&,<$1 --> key>,<$2 --> lock>) ==> <$2 --> (/,open,$1,_)>>"
     *
     * * 📄"<{Tweety} --> [chirping]>" × "<robin --> [chirping]>"
     * * * @ "(&&,<robin --> [chirping]>,<robin --> [with_wings]>)"
     * * * => "<<{Tweety} --> $1> ==> (&&,<robin --> $1>,<robin --> [with_wings]>)>"
     */
    private static void introVarInner2(
            Statement premise1, CompoundTerm oldCompound,
            final Task task, final Judgement belief,
            final Term commonTerm1, final Term commonTerm2,
            DerivationContextReason context) {
        // * 🚩词项 * //
        final Term content = makeImplication(premise1, oldCompound);
        if (content == null)
            return;
        // * 🚩将「共有词项」替换成变量
        final HashMap<Term, Term> substitute = new HashMap<>();
        final Variable varI = makeVarI(content);
        substitute.put(commonTerm1, varI);
        if (commonTerm2 != null) {
            final Variable varI2 = makeVarI(content, varI);
            substitute.put(commonTerm2, varI2);
        }
        VariableProcess.applySubstitute(content, substitute);

        // * 🚩真值 * //
        final Truth truth = premise1.equals(task.getContent())
                // * 🚩前提 == 任务 ⇒ 归纳 信念→任务
                ? TruthFunctions.induction(belief, task.asJudgement())
                // * 🚩前提 != 任务 ⇒ 归纳 任务→信念
                : TruthFunctions.induction(task.asJudgement(), belief);

        // * 🚩预算 * //
        final Budget budget = BudgetInference.forward(truth, context);

        // * 🚩结论 * //
        context.doublePremiseTask(content, truth, budget);
    }

    /**
     * Introduce a second independent variable into two terms with a common
     * component
     *
     * @param term1 The first term
     * @param term2 The second term
     * @param index The index of the terms in their statement
     */
    private static Term secondCommonTerm(final Term term1, final Term term2, final int index) {
        if (false
                // * 📄1: 都是主项，且均为外延像
                || (index == 0 && term1 instanceof ImageExt && term2 instanceof ImageExt)
                // * 📄2: 都是谓项，且均为内涵像
                || (index == 1 && term1 instanceof ImageInt && term2 instanceof ImageInt)) {
            final Image image1 = (Image) term1;
            final Image image2 = (Image) term2;
            // * 🚩先试第一个
            Term commonTerm = image1.getTheOtherComponent();
            // * 🚩尝试不到？考虑第二个/用第二个覆盖
            if (commonTerm == null || !image2.containTerm(commonTerm)) {
                // * 🚩再试第二个
                commonTerm = image2.getTheOtherComponent();
                // * 🚩尝试不到就是尝试不到
                if (commonTerm == null || !image1.containTerm(commonTerm)) {
                    commonTerm = null;
                }
            }
            // * 🚩根据中间条件多次覆盖，最终拿到一个引用
            return commonTerm;
        }
        return null;
    }

    /**
     * {(&&, <#x() --> S>, <#x() --> P>), <M --> P>} |- <M --> S>
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
        // * 🚩提取参数 * //
        final Task task = context.getCurrentTask();
        final Judgement belief = context.getCurrentBelief();
        final boolean backward = context.isBackward();

        // * 🚩词项 * //
        final Term content = reduceComponents(compound, component);
        if ((content == null) || ((content instanceof Statement) && ((Statement) content).invalid()))
            return;

        // * 🚩真值 * //
        final Truth truth = backward ? null
                // * 🚩复合词项来自任务 ⇒ 任务，信念
                : isCompoundFromTask ? TruthFunctions.anonymousAnalogy(task.asJudgement(), belief)
                        // * 🚩否则 ⇒ 信念，任务
                        : TruthFunctions.anonymousAnalogy(belief, task.asJudgement());

        // * 🚩预算 * //
        final Budget budget = backward
                ? (isCompoundFromTask
                        // * 🚩复合词项来自任务 ⇒ 反向
                        ? BudgetInference.backward(belief, context)
                        // * 🚩其它 ⇒ 反向弱推理
                        : BudgetInference.backwardWeak(belief, context))
                // * 🚩前向推理
                : BudgetInference.compoundForward(truth, content, context);

        // * 🚩结论 * //
        context.doublePremiseTask(content, truth, budget);
    }
}
