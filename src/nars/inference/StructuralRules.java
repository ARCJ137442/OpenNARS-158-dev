package nars.inference;

import java.util.ArrayList;

import nars.control.DerivationContextReason;
import nars.control.Parameters;
import nars.entity.*;
import static nars.language.MakeTerm.*;

import nars.language.*;

/**
 * Single-premise inference rules involving compound terms. Input are one
 * sentence (the premise) and one TermLink (indicating a component)
 */
final class StructuralRules {

    /** 单前提推理的依赖度 */
    private static final float RELIANCE = Parameters.RELIANCE;

    /*
     * --------------------
     * transform between compounds and components
     * --------------------
     */

    /**
     * {<S --> P>, S@(S&T)} |- <(S&T) --> (P&T)>
     * {<S --> P>, S@(M-S)} |- <(M-P) --> (M-S)>
     * * 📝双侧建构
     *
     * @param compound  The compound term
     * @param index     The location of the indicated term in the compound
     * @param statement The premise
     * @param side      The location of the indicated term in the premise
     * @param context   Reference to the derivation context
     */
    static void structuralComposeBoth(
            CompoundTerm compound, short index,
            Statement statement, short side,
            DerivationContextReason context) {
        // * 🚩预筛 * //
        final Term indicated = statement.componentAt(side);
        if (compound.equals(indicated))
            // * 📄compound="(&,glasses,[black])" @ 0 = "glasses"
            // * * statement="<sunglasses --> (&,glasses,[black])>" @ 1 = compound
            // * * ⇒不处理（❓为何如此）
            return;

        // * 🚩词项 * //
        final Term statementSubject = statement.getSubject();
        final Term statementPredicate = statement.getPredicate();
        final ArrayList<Term> components = compound.cloneComponents();
        if (side == 0 && components.contains(statementPredicate)
                || side == 1 && components.contains(statementSubject))
            // * 📄compound = "(*,{tom},(&,glasses,[black]))" @ 1 => "(&,glasses,[black])"
            // * * statement = "<(&,glasses,sunglasses) --> (&,glasses,[black])>" @ 0
            // * * components = ["{tom}", "(&,glasses,[black])"]
            // * * ⇒不处理（❓为何如此）
            return;

        final Term subj;
        final Term pred;
        if (side == 0 && components.contains(statementSubject)) {
            // * 🚩主项：原来的复合词项
            subj = compound;
            // * 🚩谓项：替换后的复合词项
            components.set(index, statementPredicate);
            pred = makeCompoundTerm(compound, components);
        } else if (side == 1 && components.contains(statementPredicate)) {
            // * 🚩主项：替换后的复合词项
            components.set(index, statementSubject);
            subj = makeCompoundTerm(compound, components);
            // * 🚩谓项：原来的复合词项
            pred = compound;
        } else {
            subj = statementSubject;
            pred = statementPredicate;
        }
        if (subj == null || pred == null)
            // * 📄compound = "(&,[yellow],{Birdie})" @ 0 => "[yellow]"
            // * * statement = "<{Tweety} --> [yellow]>" @ 1
            // * * components = ["{Tweety}", "{Birdie}"]
            // * * subj = "(&,{Tweety},{Birdie})" = null | 空集
            // * * pred = "(&,[yellow],{Birdie})"
            // * * ⇒制作失败
            return;

        final Term content = switchOrder(compound, index)
                // * 🚩根据「复合词项&索引」决定是否要「调换关系」
                ? makeStatement(statement, pred, subj)
                : makeStatement(statement, subj, pred);
        if (content == null) // * 🚩制作失败⇒返回
            return;

        final Task task = context.getCurrentTask();
        final boolean backward = context.isBackward();

        // * 🚩真值 * //
        final Truth truth = backward
                // * 🚩反向推理⇒空
                ? null
                // * 🚩前向推理
                : (compound.size() > 1
                        // * 🚩任务项多于一个元素⇒分析性演绎
                        ? TruthFunctions.analyticDeduction(task.asJudgement(), RELIANCE)
                        // * 🚩其它⇒恒等@当前任务
                        : TruthFunctions.identity(task.asJudgement()));

        // * 🚩预算 * //
        final Budget budget = backward
                // * 🚩反向推理⇒复合反向弱
                ? BudgetInference.compoundBackwardWeak(content, context)
                // * 🚩前向推理⇒复合前向
                : BudgetInference.compoundForward(truth, content, context);

        // * 🚩结论 * //
        context.singlePremiseTaskStructural(content, truth, budget);
    }

    /**
     * {<(S&T) --> (P&T)>, S@(S&T)} |- <S --> P>
     * * 📝双侧解构
     *
     * @param statement The premise
     * @param context   Reference to the derivation context
     */
    static void structuralDecomposeBoth(
            Statement statement, int index,
            DerivationContextReason context) {
        // * 🚩词项 * //

        final Term subj = statement.getSubject();
        final Term pred = statement.getPredicate();
        // * 📌必须是「同类复合词项」才有可能解构
        if (!subj.isSameType(pred))
            return;

        final CompoundTerm sub = (CompoundTerm) subj;
        final CompoundTerm pre = (CompoundTerm) pred;
        // * 📌必须是「同尺寸复合词项」且「索引在界内」
        if (sub.size() != pre.size() || sub.size() <= index)
            return;

        // * 🚩取其中索引所在的词项，按顺序制作相同系词的陈述
        final Term subInner = sub.componentAt(index);
        final Term preInner = pre.componentAt(index);
        final Term content = switchOrder(sub, (short) index)
                // * 🚩调换顺序
                ? makeStatement(statement, preInner, subInner)
                // * 🚩保持顺序
                : makeStatement(statement, subInner, preInner);
        if (content == null)
            return;

        // * 🚩预筛
        final boolean backward = context.isBackward();
        if (!backward && (!(sub instanceof Product) && (sub.size() > 1) && (context.getCurrentTask().isJudgement()))) {
            return;
        }

        // * 🚩真值 * //
        final Truth truth = backward
                // * 🚩反向推理⇒空
                ? null
                // * 🚩前向推理⇒直接用任务的真值
                : TruthFunctions.identity(context.getCurrentTask().asJudgement());

        // * 🚩预算 * //
        final Budget budget = backward
                // * 🚩反向推理⇒复合反向
                ? BudgetInference.compoundBackward(content, context)
                // * 🚩前向推理⇒复合前向
                : BudgetInference.compoundForward(truth, content, context);

        // * 🚩结论 * //
        context.singlePremiseTaskStructural(content, truth, budget);
    }

    /**
     * List the cases where the direction of inheritance is revised in conclusion
     * * 📝根据复合词项与索引，确定「是否在构建时交换」
     *
     * @param compound The compound term
     * @param index    The location of focus in the compound
     * @return Whether the direction of inheritance should be revised
     */
    private static boolean switchOrder(final CompoundTerm compound, final short index) {
        return (false
                // * 🚩外延差/内涵差 且 索引【在右侧】
                // * 📝原理：减法的性质
                // * 📄"<A --> B>" => "<(~, C, B) --> (~, C, A)>"
                // * 💭"<A --> B>" => "<(~, A, C) --> (~, B, C)>"
                // * ✅【2024-07-22 14:51:00】上述例子均以ANSWER验证
                || ((compound instanceof DifferenceExt || compound instanceof DifferenceInt) && index == 1)
                // * 🚩外延像/内涵像 且 索引【不在占位符上】
                // * 📄"<A --> B>" => "<(/, R, _, B) --> (/, R, _, A)>"
                // * 💭"<A --> B>" => "<(/, A, _, C) --> (/, B, _, C)>"
                // * ✅【2024-07-22 14:49:59】上述例子均以ANSWER验证
                || (compound instanceof ImageExt && index != ((ImageExt) compound).getRelationIndex())
                || (compound instanceof ImageInt && index != ((ImageInt) compound).getRelationIndex()));
    }

    /**
     * {<S --> P>, P@(P&Q)} |- <S --> (P&Q)>
     * * 📝单侧建构
     *
     * @param compound  The compound term
     * @param index     The location of the indicated term in the compound
     * @param statement The premise
     * @param context   Reference to the derivation context
     */
    static void structuralComposeOne(
            CompoundTerm compound, short index, // 只有复合词项有索引
            Statement statement,
            DerivationContextReason context) {
        final boolean backward = context.isBackward();

        if (backward) // ! 📝此推理只适用于前向推理（目标推理亦不行，refer@304）
            return;

        // * 🚩预先计算真值
        final Judgement taskJudgement = context.getCurrentTask().asJudgement();
        final Truth truthDed = TruthFunctions.analyticDeduction(taskJudgement, RELIANCE);
        final Truth truthNDed = TruthFunctions.negation(truthDed);

        // * 🚩部分计算词项，并向下分派
        // * * 📄"P@(P&Q)" => "P"
        // * * 📄"<S --> P>" => subj="S", pred="P"
        final Term component = compound.componentAt(index);
        final Term subj = statement.getSubject();
        final Term pred = statement.getPredicate();
        if (component.equals(subj)) {
            // * 📄"S"@"(S&T)" × "<S --> P>"
            if (compound instanceof IntersectionExt) {
                // * 🚩外延交
                // * 📄"S"@"(S&T)" × "<S --> P>"
                // * * component=subj="S"
                // * * compound="(S&T)"
                // * * pred="P"
                // * * => "<(S&T) --> P>"
                structuralStatement(compound, pred, truthDed, context);
            } else if (compound instanceof DifferenceExt && index == 0) {
                // * 🚩外延差@主项 ⇒ "<(S-T) --> P>"
                // * 📄"S"@"(S-T)" × "<S --> P>"
                // * * component=subj="S"
                // * * compound="(S-T)"
                // * * pred="P"
                // * * => "<(S-T) --> P>"
                structuralStatement(compound, pred, truthDed, context);
            } else if (compound instanceof DifferenceInt && index == 1) {
                // * 🚩内涵差@谓项 ⇒ "<(T~S) --> P>"
                // * 📄"S"@"(T~S)" × "<S --> P>"
                // * * component=subj="S"
                // * * compound="(T~S)"
                // * * pred="P"
                // * * => "<(T~S) --> P>"
                // * 📝真值取【否定】
                structuralStatement(compound, pred, truthNDed, context);
            }
        } else if (component.equals(pred)) {
            // * 📄"P"@"(P&Q)" × "<S --> P>"
            if (compound instanceof IntersectionInt) {
                // * 🚩内涵交
                // * 📄"P"@"(P|Q)" × "<S --> P>"
                // * * component=pred="P"
                // * * compound="(P|Q)"
                // * * subj="S"
                // * * => "<S --> (P|Q)>"
                structuralStatement(subj, compound, truthDed, context);
            } else if (compound instanceof DifferenceExt && index == 1) {
                // * 🚩外延差@谓项
                // * 📄"P"@"(Q-P)" × "<S --> P>"
                // * * component=pred="P"
                // * * compound="(Q-P)"
                // * * subj="S"
                // * * => "<S --> (Q-P)>"
                // * 📝真值取【否定】
                structuralStatement(subj, compound, truthNDed, context);
            } else if (compound instanceof DifferenceInt && index == 0) {
                // * 🚩内涵差@主项
                // * 📄"P"@"(P~Q)" × "<S --> P>"
                // * * component=pred="P"
                // * * compound="(P~Q)"
                // * * subj="S"
                // * * => "<S --> (P~Q)>"
                structuralStatement(subj, compound, truthDed, context);
            }
        }
    }

    /**
     * {<(S&T) --> P>, S@(S&T)} |- <S --> P>
     * * 📝单侧解构
     *
     * @param compound  The compound term
     * @param index     The location of the indicated term in the compound
     * @param statement The premise
     * @param context   Reference to the derivation context
     */
    static void structuralDecomposeOne(
            CompoundTerm compound, short index,
            Statement statement,
            DerivationContextReason context) {
        final boolean backward = context.isBackward();

        if (backward) // ! 📝此推理只适用于前向推理（目标推理亦不行，refer@304）
            return;

        // * 🚩预先计算真值
        final Judgement taskJudgement = context.getCurrentTask().asJudgement();
        final Truth truthDed = TruthFunctions.analyticDeduction(taskJudgement, RELIANCE);
        final Truth truthNDed = TruthFunctions.negation(truthDed);

        // * 🚩部分计算词项，并向下分派
        // * * 📄"S@(S&T)" => "S"
        // * * 📄"<(S&T) --> P>" => subj="(S&T)", pred="P"
        final Term component = compound.componentAt(index);
        final Term subj = statement.getSubject();
        final Term pred = statement.getPredicate();
        if (compound.equals(subj)) {
            // * 🚩复合词项是主项
            // * 📄"P"@"(P&Q)" × "<(P&Q) --> S>"
            if (compound instanceof IntersectionInt) {
                // * 🚩内涵交
                // * 📄"S"@"(S|T)" × "<(S|T) --> P>"
                // * * compound=subj="(S|T)"
                // * * component="S"
                // * * pred="P"
                // * * => "<S --> P>"
                structuralStatement(component, pred, truthDed, context);
            } else if (compound instanceof SetExt && compound.size() > 1) {
                // * 🚩多元外延集
                // * 📄"S"@"{S,T}" × "<{S,T} --> P>"
                // * * compound=subj="{S,T}"
                // * * component="S"
                // * * pred="P"
                // * * => "<{S} --> P>"
                // * 📌【2024-07-22 16:01:42】此处`makeSet`不会失败（结果非空）
                structuralStatement(makeSetExt(component), pred, truthDed, context);
            } else if (compound instanceof DifferenceInt) {
                // * 🚩内涵差
                // * 📄"S"@"(S~T)" × "<(S~T) --> P>"
                // * * compound=subj="(S~T)"/"(T~S)"
                // * * component="S"
                // * * pred="P"
                // * * => "<S --> P>"
                // * 📝真值函数方面：若为「减掉的项」则【取否定】处理
                structuralStatement(component, pred, index == 0 ? truthDed : truthNDed, context);
            }
        } else if (compound.equals(pred)) {
            // * 🚩复合词项是谓项
            // * 📄"P"@"(P&Q)" × "<S --> (P&Q)>"
            if (compound instanceof IntersectionExt) {
                // * 🚩外延交
                // * 📄"S"@"(S&T)" × "<(S&T) --> P>"
                // * * compound=subj="(S&T)"
                // * * component="S"
                // * * pred="P"
                // * * => "<S --> P>"
                structuralStatement(subj, component, truthDed, context);
            } else if (compound instanceof SetInt && compound.size() > 1) {
                // * 🚩多元内涵集
                // * 📄"P"@"[P,Q]" × "<S --> [P,Q]>"
                // * * compound=subj="[S,T]"
                // * * component="S"
                // * * pred="P"
                // * * => "<S --> [P]>"
                // * 📌【2024-07-22 16:01:42】此处`makeSet`不会失败（结果非空）
                structuralStatement(subj, makeSetInt(component), truthDed, context);
            } else if (compound instanceof DifferenceExt) {
                // * 🚩外延差
                // * 📄"P"@"(P-Q)" × "<S --> (P-Q)>"
                // * * compound=pred="(P-Q)"/"(Q-P)"
                // * * component="P"
                // * * subj="S"
                // * * => "<S --> P>"
                // * 📝真值函数方面：若为「减掉的项」则【取否定】处理
                structuralStatement(subj, component, index == 0 ? truthDed : truthNDed, context);
            }
        }
    }

    /**
     * Common final operations of the above two methods
     * * 📝共用函数：根据给定的主项、谓项、任务内容（as模板） 构造新任务
     *
     * @param subject   The subject of the new task
     * @param predicate The predicate of the new task
     * @param truth     The truth value of the new task
     * @param context   Reference to the derivation context
     */
    private static void structuralStatement(
            Term subject, Term predicate, Truth truth,
            DerivationContextReason context) {
        // * 🚩获取旧任务的陈述内容
        final Task task = context.getCurrentTask();
        final Term oldContent = task.getContent();
        if (!(oldContent instanceof Statement))
            return;

        // * 🚩构造新陈述
        final Term content = makeStatement((Statement) oldContent, subject, predicate);
        if (content == null)
            return;

        // * 🚩预算 * //
        final Budget budget = BudgetInference.compoundForward(truth, content, context);

        // * 🚩结论 * //
        context.singlePremiseTaskStructural(content, truth, budget);
    }

    /* -------------------- set transform -------------------- */
    /**
     * {<S --> {P}>} |- <S <-> {P}>
     * {<[S] --> P>} |- <[S] <-> P>
     * * 📝外延集、内涵集的「定理」
     * * 📝NAL中「外延集」「内涵集」可以理解为「概念继承关系的上界/下界」，整个继承关系可类比性地构造出Hasse图
     *
     * @param compound  The set compound
     * @param statement The premise
     * @param side      The location of the indicated term in the premise
     * @param context   Reference to the derivation context
     */
    static void transformSetRelation(
            CompoundTerm compound,
            Statement statement, short side,
            DerivationContextReason context) {

        // * 🚩预筛 * //
        // * 🚩仅一元集
        if (compound.size() > 1)
            return;

        // * 🚩不处理其它「继承」的情况
        if (statement instanceof Inheritance)
            // * 📄"<{S} --> X>"
            if (compound instanceof SetExt && side == 0)
                return;
            // * 📄"<X --> [P]>"
            else if (compound instanceof SetInt && side == 1)
                return;

        // * 🚩词项 * //
        final Term sub = statement.getSubject();
        final Term pre = statement.getPredicate();
        final Term content = statement instanceof Inheritance
                // * 📄"<S --> {P}>" => "<S <-> {P}>"
                // * 📄"<[S] --> P>" => "<[S] <-> P>"
                ? makeSimilarity(sub, pre)
                : compound instanceof SetExt && side == 0 || compound instanceof SetInt && side == 1
                        // * 📄"<{S} <-> P>" => "<P --> {S}>"
                        // * 📄"<S <-> [P]>" => "<[P] --> S>"
                        ? makeInheritance(pre, sub)
                        // * 📄"<S <-> {P}>" => "<S --> {P}>"
                        // * 📄"<[S] <-> P>" => "<[S] --> P>"
                        : makeInheritance(sub, pre);
        if (content == null)
            return;

        final Task task = context.getCurrentTask();
        final boolean backward = context.isBackward();

        // * 🚩真值 * //
        final Truth truth = backward
                // * 🚩反向 ⇒ 空
                ? null
                // * 🚩前向 ⇒ 恒等
                : TruthFunctions.identity(task.asJudgement());

        // * 🚩预算 * //
        final Budget budget = backward
                // * 🚩反向⇒复合反向
                ? BudgetInference.compoundBackward(content, context)
                // * 🚩前向⇒复合前向
                : BudgetInference.compoundForward(task.asJudgement(), content, context);

        // * 🚩结论 * //
        context.singlePremiseTaskStructural(content, truth, budget);
    }

    /* --------------- Disjunction and Conjunction transform --------------- */
    /**
     * {(&&, A, B), A@(&&, A, B)} |- A, or answer (&&, A, B)? using A
     * {(||, A, B), A@(||, A, B)} |- A, or answer (||, A, B)? using A
     *
     * @param compound           The premise
     * @param component          The recognized component in the premise
     * @param isCompoundFromTask Whether the compound comes from the task
     * @param context            Reference to the derivation context
     */
    static void structuralCompound(
            CompoundTerm compound, Term component,
            boolean isCompoundFromTask,
            DerivationContextReason context) {
        // * 🚩仅「常量词项」
        if (!component.isConstant())
            return;

        final Task task = context.getCurrentTask();
        final boolean backward = context.isBackward();

        // * 🚩词项 * //
        final Term content = isCompoundFromTask
                // * 🚩复合词项从任务中来 ⇒ 元素
                ? component
                // * 🚩其它 ⇒ 整体
                : compound;
        // * 🚩真值 * //
        final Truth truth = backward ? null
                // * 🚩前向推理⇒根据「判断句 == (复合词项从任务中来 == 复合词项是合取)」决策
                : (task.isJudgement() == (isCompoundFromTask == compound instanceof Conjunction))
                        // * 🚩满足⇒分析性演绎
                        ? TruthFunctions.analyticDeduction(task.asJudgement(), RELIANCE)
                        // * 🚩满足⇒分析性反演（非⇒演绎⇒非）
                        : TruthFunctions.negation(
                                TruthFunctions.analyticDeduction(TruthFunctions.negation(task.asJudgement()),
                                        RELIANCE));
        // * 🚩预算 * //
        final Budget budget = backward
                // * 🚩反向推理
                ? BudgetInference.compoundBackward(content, context)
                // * 🚩前向推理
                : BudgetInference.forward(truth, context);

        // * 🚩结论 * //
        context.singlePremiseTaskStructural(content, truth, budget);
    }

    /* --------------- Negation related rules --------------- */
    /**
     * {A, A@(--, A)} |- (--, A)
     * * 📝转换「否定」：反推否定、双重否定等
     *
     * @param content The premise
     * @param context Reference to the derivation context
     */
    static void transformNegation(Negation negation, boolean isCompoundFromTask, DerivationContextReason context) {
        final Task task = context.getCurrentTask();
        final boolean backward = context.isBackward();

        // * 🚩词项 * //
        final Term content = isCompoundFromTask
                // * 🚩从「当前任务」来⇒被否定的值
                // * 📝双重否定⇒肯定
                // * 📄【2024-06-10 19:57:15】一例：
                // * compound="(--,(--,A))"
                // * component="(--,A)"
                // * conceptTerm="(--,(--,A))"
                // * currentTask=Task@807 "$0.8000;0.8000;0.9500$ (--,(--,A)). %1.00;0.90%"
                // * => "(--,A)"
                ? negation.getTheComponent()
                // * 🚩其它⇒转换整个否定
                // * 📄【2024-07-22 17:52:09】一例：
                // * compound="(--,A)"
                // * component="A"
                // * conceptTerm="A"
                // * currentTask=Task@386 "$1.0000;0.0971;0.1341$ A. %0.90;0.09%"
                // * => "(--,A)"
                : negation;

        // ? 💭【2024-07-22 17:42:55】具体后续是「先『真值→预算』再『判断→问题』」还是「先『判断→问题』再『真值→预算』」，可以进一步探讨

        // * 🚩真值 * //
        final Truth truth = backward ? null
                // * 🚩前向推理⇒否定
                : TruthFunctions.negation(task.asJudgement());

        // * 🚩预算 * //
        final Budget budget = backward
                // * 🚩反向推理⇒复合反向
                ? BudgetInference.compoundBackward(content, context)
                // * 🚩前向推理⇒复合前向
                : BudgetInference.compoundForward(task.asJudgement(), content, context);

        // * 🚩结论 * //
        context.singlePremiseTaskStructural(content, truth, budget);
    }

    /**
     * {<A ==> B>, A@(--, A)} |- <(--, B) ==> (--, A)>
     * * 📝逆否
     *
     * @param statement The premise
     * @param context   Reference to the derivation context
     */
    static void contraposition(Statement statement, Sentence sentence, DerivationContextReason context) {
        final boolean backward = context.isBackward();

        // * 🚩词项 * //
        final Term subject = statement.getSubject();
        final Term predicate = statement.getPredicate();

        // * 🚩生成新内容
        final Term content = makeStatement(
                statement, // 相同系词
                makeNegation(predicate), // 否定 @ 相反位置
                makeNegation(subject));

        // * 🚩真值 * //
        final Truth truth = backward ? null
                : content instanceof Implication
                        // * 🚩蕴含⇒双重否定
                        ? TruthFunctions.contraposition(sentence.asJudgement())
                        // * 🚩其它⇒恒等
                        : TruthFunctions.identity(sentence.asJudgement());

        // * 🚩预算 * //
        final Budget budget = backward
                // * 🚩反向⇒按照「是否为蕴含」分派
                ? (content instanceof Implication
                        // * 🚩蕴含⇒弱推理
                        ? BudgetInference.compoundBackwardWeak(content, context)
                        : BudgetInference.compoundBackward(content, context))
                // * 🚩前向⇒复合前向
                : BudgetInference.compoundForward(truth, content, context);

        // * 🚩结论 * //
        context.singlePremiseTask(content, sentence.getPunctuation(), truth, budget);
    }
}
