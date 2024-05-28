package nars.inference;

import java.util.ArrayList;

import nars.control.DerivationContextReason;
import nars.entity.*;
import nars.io.Symbols;
import nars.language.*;
import nars.main_nogui.Parameters;
import static nars.control.MakeTerm.*;

/**
 * Single-premise inference rules involving compound terms. Input are one
 * sentence (the premise) and one TermLink (indicating a component)
 */
public final class StructuralRules {

    private static final float RELIANCE = Parameters.RELIANCE;

    /*
     * -------------------- transform between compounds and components
     * --------------------
     */
    /**
     * {<S --> P>, S@(S&T)} |- <(S&T) --> (P&T)>
     * {<S --> P>, S@(M-S)} |- <(M-P) --> (M-S)>
     *
     * @param compound  The compound term
     * @param index     The location of the indicated term in the compound
     * @param statement The premise
     * @param side      The location of the indicated term in the premise
     * @param context   Reference to the derivation context
     */
    static void structuralCompose2(CompoundTerm compound, short index, Statement statement, short side,
            DerivationContextReason context) {
        if (compound.equals(statement.componentAt(side))) {
            return;
        }
        Term sub = statement.getSubject();
        Term pred = statement.getPredicate();
        final ArrayList<Term> components = compound.cloneComponents();
        if (((side == 0) && components.contains(pred)) || ((side == 1) && components.contains(sub))) {
            return;
        }
        if (side == 0) {
            if (components.contains(sub)) {
                sub = compound;
                components.set(index, pred);
                pred = makeCompoundTerm(compound, components, context.getMemory());
            }
        } else {
            if (components.contains(pred)) {
                components.set(index, sub);
                sub = makeCompoundTerm(compound, components, context.getMemory());
                pred = compound;
            }
        }
        if ((sub == null) || (pred == null)) {
            return;
        }
        final Term content;
        if (switchOrder(compound, index)) {
            content = makeStatement(statement, pred, sub, context.getMemory());
        } else {
            content = makeStatement(statement, sub, pred, context.getMemory());
        }
        if (content == null) {
            return;
        }
        final Task task = context.getCurrentTask();
        final Sentence sentence = task.getSentence();
        TruthValue truth = sentence.getTruth();
        final BudgetValue budget;
        if (sentence.isQuestion()) {
            budget = BudgetFunctions.compoundBackwardWeak(content, context);
        } else {
            if (compound.size() > 1) {
                if (sentence.isJudgment()) {
                    truth = TruthFunctions.deduction(truth, RELIANCE);
                } else {
                    return;
                }
            }
            budget = BudgetFunctions.compoundForward(truth, content, context);
        }
        context.singlePremiseTask(content, truth, budget);
    }

    /**
     * {<(S&T) --> (P&T)>, S@(S&T)} |- <S --> P>
     *
     * @param statement The premise
     * @param context   Reference to the derivation context
     */
    static void structuralDecompose2(Statement statement, int index, DerivationContextReason context) {
        final Term subj = statement.getSubject();
        final Term pred = statement.getPredicate();
        if (subj.getClass() != pred.getClass()) {
            return;
        }
        final CompoundTerm sub = (CompoundTerm) subj;
        final CompoundTerm pre = (CompoundTerm) pred;
        if (sub.size() != pre.size() || sub.size() <= index) {
            return;
        }
        final Term t1 = sub.componentAt(index);
        final Term t2 = pre.componentAt(index);
        final Term content;
        if (switchOrder(sub, (short) index)) {
            content = makeStatement(statement, t2, t1, context.getMemory());
        } else {
            content = makeStatement(statement, t1, t2, context.getMemory());
        }
        if (content == null) {
            return;
        }
        final Task task = context.getCurrentTask();
        final Sentence sentence = task.getSentence();
        final TruthValue truth = sentence.getTruth();
        final BudgetValue budget;
        if (sentence.isQuestion()) {
            budget = BudgetFunctions.compoundBackward(content, context);
        } else {
            if (!(sub instanceof Product) && (sub.size() > 1) && (sentence.isJudgment())) {
                return;
            }
            budget = BudgetFunctions.compoundForward(truth, content, context);
        }
        context.singlePremiseTask(content, truth, budget);
    }

    /**
     * List the cases where the direction of inheritance is revised in
     * conclusion
     *
     * @param compound The compound term
     * @param index    The location of focus in the compound
     * @return Whether the direction of inheritance should be revised
     */
    private static boolean switchOrder(CompoundTerm compound, short index) {
        return ((((compound instanceof DifferenceExt) || (compound instanceof DifferenceInt)) && (index == 1))
                || ((compound instanceof ImageExt) && (index != ((ImageExt) compound).getRelationIndex()))
                || ((compound instanceof ImageInt) && (index != ((ImageInt) compound).getRelationIndex())));
    }

    /**
     * {<S --> P>, P@(P&Q)} |- <S --> (P&Q)>
     *
     * @param compound  The compound term
     * @param index     The location of the indicated term in the compound
     * @param statement The premise
     * @param context   Reference to the derivation context
     */
    static void structuralCompose1(CompoundTerm compound, short index, Statement statement,
            DerivationContextReason context) {
        if (!context.getCurrentTask().getSentence().isJudgment()) {
            return;
        }
        final Term component = compound.componentAt(index);
        final Task task = context.getCurrentTask();
        final Sentence sentence = task.getSentence();
        final TruthValue truth = sentence.getTruth();
        final TruthValue truthDed = TruthFunctions.deduction(truth, RELIANCE);
        final TruthValue truthNDed = TruthFunctions.negation(TruthFunctions.deduction(truth, RELIANCE));
        final Term subj = statement.getSubject();
        final Term pred = statement.getPredicate();
        if (component.equals(subj)) {
            if (compound instanceof IntersectionExt) {
                structuralStatement(compound, pred, truthDed, context);
            } else if (compound instanceof IntersectionInt) {
            } else if ((compound instanceof DifferenceExt) && (index == 0)) {
                structuralStatement(compound, pred, truthDed, context);
            } else if (compound instanceof DifferenceInt) {
                if (index == 0) {
                } else {
                    structuralStatement(compound, pred, truthNDed, context);
                }
            }
        } else if (component.equals(pred)) {
            if (compound instanceof IntersectionExt) {
            } else if (compound instanceof IntersectionInt) {
                structuralStatement(subj, compound, truthDed, context);
            } else if (compound instanceof DifferenceExt) {
                if (index == 0) {
                } else {
                    structuralStatement(subj, compound, truthNDed, context);
                }
            } else if ((compound instanceof DifferenceInt) && (index == 0)) {
                structuralStatement(subj, compound, truthDed, context);
            }
        }
    }

    /**
     * {<(S&T) --> P>, S@(S&T)} |- <S --> P>
     *
     * @param compound  The compound term
     * @param index     The location of the indicated term in the compound
     * @param statement The premise
     * @param context   Reference to the derivation context
     */
    static void structuralDecompose1(CompoundTerm compound, short index, Statement statement,
            DerivationContextReason context) {
        if (!context.getCurrentTask().getSentence().isJudgment()) {
            return;
        }
        final Term component = compound.componentAt(index);
        final Task task = context.getCurrentTask();
        final Sentence sentence = task.getSentence();
        final TruthValue truth = sentence.getTruth();
        final TruthValue truthDed = TruthFunctions.deduction(truth, RELIANCE);
        final TruthValue truthNDed = TruthFunctions.negation(TruthFunctions.deduction(truth, RELIANCE));
        final Term subj = statement.getSubject();
        final Term pred = statement.getPredicate();
        if (compound.equals(subj)) {
            if (compound instanceof IntersectionInt) {
                structuralStatement(component, pred, truthDed, context);
            } else if ((compound instanceof SetExt) && (compound.size() > 1)) {
                structuralStatement(makeSetExt(component, context.getMemory()), pred, truthDed, context);
            } else if (compound instanceof DifferenceInt) {
                if (index == 0) {
                    structuralStatement(component, pred, truthDed, context);
                } else {
                    structuralStatement(component, pred, truthNDed, context);
                }
            }
        } else if (compound.equals(pred)) {
            if (compound instanceof IntersectionExt) {
                structuralStatement(subj, component, truthDed, context);
            } else if ((compound instanceof SetInt) && (compound.size() > 1)) {
                structuralStatement(subj, makeSetInt(component, context.getMemory()), truthDed, context);
            } else if (compound instanceof DifferenceExt) {
                if (index == 0) {
                    structuralStatement(subj, component, truthDed, context);
                } else {
                    structuralStatement(subj, component, truthNDed, context);
                }
            }
        }
    }

    /**
     * Common final operations of the above two methods
     *
     * @param subject   The subject of the new task
     * @param predicate The predicate of the new task
     * @param truth     The truth value of the new task
     * @param context   Reference to the derivation context
     */
    private static void structuralStatement(Term subject, Term predicate, TruthValue truth,
            DerivationContextReason context) {
        final Task task = context.getCurrentTask();
        final Term oldContent = task.getContent();
        if (oldContent instanceof Statement) {
            final Term content = makeStatement((Statement) oldContent, subject, predicate, context.getMemory());
            if (content != null) {
                final BudgetValue budget = BudgetFunctions.compoundForward(truth, content, context);
                context.singlePremiseTask(content, truth, budget);
            }
        }
    }

    /* -------------------- set transform -------------------- */
    /**
     * {<S --> {P}>} |- <S <-> {P}>
     *
     * @param compound  The set compound
     * @param statement The premise
     * @param side      The location of the indicated term in the premise
     * @param context   Reference to the derivation context
     */
    static void transformSetRelation(CompoundTerm compound, Statement statement, short side,
            DerivationContextReason context) {
        if (compound.size() > 1) {
            return;
        }
        if (statement instanceof Inheritance) {
            if (((compound instanceof SetExt) && (side == 0)) || ((compound instanceof SetInt) && (side == 1))) {
                return;
            }
        }
        final Term sub = statement.getSubject();
        final Term pre = statement.getPredicate();
        final Term content;
        if (statement instanceof Inheritance) {
            content = makeSimilarity(sub, pre, context.getMemory());
        } else {
            if (((compound instanceof SetExt) && (side == 0)) || ((compound instanceof SetInt) && (side == 1))) {
                content = makeInheritance(pre, sub, context.getMemory());
            } else {
                content = makeInheritance(sub, pre, context.getMemory());
            }
        }
        if (content == null) {
            return;
        }
        final Task task = context.getCurrentTask();
        final Sentence sentence = task.getSentence();
        final TruthValue truth = sentence.getTruth();
        final BudgetValue budget;
        if (sentence.isQuestion()) {
            budget = BudgetFunctions.compoundBackward(content, context);
        } else {
            budget = BudgetFunctions.compoundForward(truth, content, context);
        }
        context.singlePremiseTask(content, truth, budget);
    }

    /* --------------- Disjunction and Conjunction transform --------------- */
    /**
     * {(&&, A, B), A@(&&, A, B)} |- A, or answer (&&, A, B)? using A
     * {(||, A, B), A@(||, A, B)} |- A, or answer (||, A, B)? using A
     *
     * @param compound     The premise
     * @param component    The recognized component in the premise
     * @param compoundTask Whether the compound comes from the task
     * @param context      Reference to the derivation context
     */
    static void structuralCompound(CompoundTerm compound, Term component, boolean compoundTask,
            DerivationContextReason context) {
        if (!component.isConstant()) {
            return;
        }
        final Term content = (compoundTask ? component : compound);
        final Task task = context.getCurrentTask();
        final Sentence sentence = task.getSentence();
        TruthValue truth = sentence.getTruth();
        final BudgetValue budget;
        if (sentence.isQuestion()) {
            budget = BudgetFunctions.compoundBackward(content, context);
        } else {
            if ((sentence.isJudgment()) == (compoundTask == (compound instanceof Conjunction))) {
                truth = TruthFunctions.deduction(truth, RELIANCE);
            } else {
                TruthValue v1, v2;
                v1 = TruthFunctions.negation(truth);
                v2 = TruthFunctions.deduction(v1, RELIANCE);
                truth = TruthFunctions.negation(v2);
            }
            budget = BudgetFunctions.forward(truth, context);
        }
        context.singlePremiseTask(content, truth, budget);
    }

    /* --------------- Negation related rules --------------- */
    /**
     * {A, A@(--, A)} |- (--, A)
     *
     * @param content The premise
     * @param context Reference to the derivation context
     */
    public static void transformNegation(Term content, DerivationContextReason context) {
        final Task task = context.getCurrentTask();
        final Sentence sentence = task.getSentence();
        TruthValue truth = sentence.getTruth();
        if (sentence.isJudgment()) {
            truth = TruthFunctions.negation(truth);
        }
        final BudgetValue budget;
        if (sentence.isQuestion()) {
            budget = BudgetFunctions.compoundBackward(content, context);
        } else {
            budget = BudgetFunctions.compoundForward(truth, content, context);
        }
        context.singlePremiseTask(content, truth, budget);
    }

    /**
     * {<A ==> B>, A@(--, A)} |- <(--, B) ==> (--, A)>
     *
     * @param statement The premise
     * @param context   Reference to the derivation context
     */
    static void contraposition(Statement statement, Sentence sentence, DerivationContextReason context) {
        final Term subj = statement.getSubject();
        final Term pred = statement.getPredicate();
        final Term content = makeStatement(statement, makeNegation(pred, context.getMemory()),
                makeNegation(subj, context.getMemory()),
                context.getMemory());
        TruthValue truth = sentence.getTruth();
        final BudgetValue budget;
        if (sentence.isQuestion()) {
            if (content instanceof Implication) {
                budget = BudgetFunctions.compoundBackwardWeak(content, context);
            } else {
                budget = BudgetFunctions.compoundBackward(content, context);
            }
            context.singlePremiseTask(content, Symbols.QUESTION_MARK, truth, budget);
        } else {
            if (content instanceof Implication) {
                truth = TruthFunctions.contraposition(truth);
            }
            budget = BudgetFunctions.compoundForward(truth, content, context);
            context.singlePremiseTask(content, Symbols.JUDGMENT_MARK, truth, budget);
        }
    }
}
