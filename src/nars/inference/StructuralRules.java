package nars.inference;

import java.util.ArrayList;

import nars.entity.*;
import nars.io.Symbols;
import nars.language.*;
import nars.main_nogui.Parameters;

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
     * @param compound       The compound term
     * @param index          The location of the indicated term in the compound
     * @param statement      The premise
     * @param side           The location of the indicated term in the premise
     * @param context.memory Reference to the context.memory
     */
    static void structuralCompose2(CompoundTerm compound, short index, Statement statement, short side,
            DerivationContext context) {
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
                pred = CompoundTerm.make(compound, components, context.memory);
            }
        } else {
            if (components.contains(pred)) {
                components.set(index, sub);
                sub = CompoundTerm.make(compound, components, context.memory);
                pred = compound;
            }
        }
        if ((sub == null) || (pred == null)) {
            return;
        }
        final Term content;
        if (switchOrder(compound, index)) {
            content = Statement.make(statement, pred, sub, context.memory);
        } else {
            content = Statement.make(statement, sub, pred, context.memory);
        }
        if (content == null) {
            return;
        }
        final Task task = context.currentTask;
        final Sentence sentence = task.getSentence();
        TruthValue truth = sentence.getTruth();
        final BudgetValue budget;
        if (sentence.isQuestion()) {
            budget = BudgetFunctions.compoundBackwardWeak(content, context.memory);
        } else {
            if (compound.size() > 1) {
                if (sentence.isJudgment()) {
                    truth = TruthFunctions.deduction(truth, RELIANCE);
                } else {
                    return;
                }
            }
            budget = BudgetFunctions.compoundForward(truth, content, context.memory);
        }
        context.singlePremiseTask(content, truth, budget);
    }

    /**
     * {<(S&T) --> (P&T)>, S@(S&T)} |- <S --> P>
     *
     * @param statement      The premise
     * @param context.memory Reference to the context.memory
     */
    static void structuralDecompose2(Statement statement, int index, DerivationContext context) {
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
            content = Statement.make(statement, t2, t1, context.memory);
        } else {
            content = Statement.make(statement, t1, t2, context.memory);
        }
        if (content == null) {
            return;
        }
        final Task task = context.currentTask;
        final Sentence sentence = task.getSentence();
        final TruthValue truth = sentence.getTruth();
        final BudgetValue budget;
        if (sentence.isQuestion()) {
            budget = BudgetFunctions.compoundBackward(content, context.memory);
        } else {
            if (!(sub instanceof Product) && (sub.size() > 1) && (sentence.isJudgment())) {
                return;
            }
            budget = BudgetFunctions.compoundForward(truth, content, context.memory);
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
     * @param compound       The compound term
     * @param index          The location of the indicated term in the compound
     * @param statement      The premise
     * @param context.memory Reference to the context.memory
     */
    static void structuralCompose1(CompoundTerm compound, short index, Statement statement, DerivationContext context) {
        if (!context.currentTask.getSentence().isJudgment()) {
            return;
        }
        final Term component = compound.componentAt(index);
        final Task task = context.currentTask;
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
     * @param compound       The compound term
     * @param index          The location of the indicated term in the compound
     * @param statement      The premise
     * @param context.memory Reference to the context.memory
     */
    static void structuralDecompose1(CompoundTerm compound, short index, Statement statement,
            DerivationContext context) {
        if (!context.currentTask.getSentence().isJudgment()) {
            return;
        }
        final Term component = compound.componentAt(index);
        final Task task = context.currentTask;
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
                structuralStatement(SetExt.make(component, context.memory), pred, truthDed, context);
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
                structuralStatement(subj, SetInt.make(component, context.memory), truthDed, context);
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
     * @param subject        The subject of the new task
     * @param predicate      The predicate of the new task
     * @param truth          The truth value of the new task
     * @param context.memory Reference to the context.memory
     */
    private static void structuralStatement(Term subject, Term predicate, TruthValue truth, DerivationContext context) {
        final Task task = context.currentTask;
        final Term oldContent = task.getContent();
        if (oldContent instanceof Statement) {
            final Term content = Statement.make((Statement) oldContent, subject, predicate, context.memory);
            if (content != null) {
                final BudgetValue budget = BudgetFunctions.compoundForward(truth, content, context.memory);
                context.singlePremiseTask(content, truth, budget);
            }
        }
    }

    /* -------------------- set transform -------------------- */
    /**
     * {<S --> {P}>} |- <S <-> {P}>
     *
     * @param compound       The set compound
     * @param statement      The premise
     * @param side           The location of the indicated term in the premise
     * @param context.memory Reference to the context.memory
     */
    static void transformSetRelation(CompoundTerm compound, Statement statement, short side,
            DerivationContext context) {
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
            content = Similarity.make(sub, pre, context.memory);
        } else {
            if (((compound instanceof SetExt) && (side == 0)) || ((compound instanceof SetInt) && (side == 1))) {
                content = Inheritance.make(pre, sub, context.memory);
            } else {
                content = Inheritance.make(sub, pre, context.memory);
            }
        }
        if (content == null) {
            return;
        }
        final Task task = context.currentTask;
        final Sentence sentence = task.getSentence();
        final TruthValue truth = sentence.getTruth();
        final BudgetValue budget;
        if (sentence.isQuestion()) {
            budget = BudgetFunctions.compoundBackward(content, context.memory);
        } else {
            budget = BudgetFunctions.compoundForward(truth, content, context.memory);
        }
        context.singlePremiseTask(content, truth, budget);
    }

    /* -------------------- products and images transform -------------------- */
    /**
     * Equivalent transformation between products and images
     * {<(*, S, M) --> P>, S@(*, S, M)} |- <S --> (/, P, _, M)>
     * {<S --> (/, P, _, M)>, P@(/, P, _, M)} |- <(*, S, M) --> P>
     * {<S --> (/, P, _, M)>, M@(/, P, _, M)} |- <M --> (/, P, S, _)>
     *
     * @param inh            An Inheritance statement
     * @param oldContent     The whole content
     * @param indices        The indices of the TaskLink
     * @param task           The task
     * @param context.memory Reference to the context.memory
     */
    static void transformProductImage(Inheritance inh, CompoundTerm oldContent, short[] indices,
            DerivationContext context) {
        Term subject = inh.getSubject();
        Term predicate = inh.getPredicate();
        if (inh.equals(oldContent)) {
            if (subject instanceof CompoundTerm) {
                transformSubjectPI((CompoundTerm) subject, predicate, context);
            }
            if (predicate instanceof CompoundTerm) {
                transformPredicatePI(subject, (CompoundTerm) predicate, context);
            }
            return;
        }
        final short index = indices[indices.length - 1];
        final short side = indices[indices.length - 2];
        final CompoundTerm comp = (CompoundTerm) inh.componentAt(side);
        if (comp instanceof Product) {
            if (side == 0) {
                subject = comp.componentAt(index);
                predicate = ImageExt.make((Product) comp, inh.getPredicate(), index, context.memory);
            } else {
                subject = ImageInt.make((Product) comp, inh.getSubject(), index, context.memory);
                predicate = comp.componentAt(index);
            }
        } else if ((comp instanceof ImageExt) && (side == 1)) {
            if (index == ((ImageExt) comp).getRelationIndex()) {
                subject = Product.make(comp, inh.getSubject(), index, context.memory);
                predicate = comp.componentAt(index);
            } else {
                subject = comp.componentAt(index);
                predicate = ImageExt.make((ImageExt) comp, inh.getSubject(), index, context.memory);
            }
        } else if ((comp instanceof ImageInt) && (side == 0)) {
            if (index == ((ImageInt) comp).getRelationIndex()) {
                subject = comp.componentAt(index);
                predicate = Product.make(comp, inh.getPredicate(), index, context.memory);
            } else {
                subject = ImageInt.make((ImageInt) comp, inh.getPredicate(), index, context.memory);
                predicate = comp.componentAt(index);
            }
        } else {
            return;
        }
        final Inheritance newInh = Inheritance.make(subject, predicate, context.memory);
        Term content = null;
        if (indices.length == 2) {
            content = newInh;
        } else if ((oldContent instanceof Statement) && (indices[0] == 1)) {
            content = Statement.make((Statement) oldContent, oldContent.componentAt(0), newInh, context.memory);
        } else {
            ArrayList<Term> componentList;
            Term condition = oldContent.componentAt(0);
            if (((oldContent instanceof Implication) || (oldContent instanceof Equivalence))
                    && (condition instanceof Conjunction)) {
                componentList = ((CompoundTerm) condition).cloneComponents();
                componentList.set(indices[1], newInh);
                Term newCond = CompoundTerm.make((CompoundTerm) condition, componentList, context.memory);
                content = Statement.make((Statement) oldContent, newCond, ((Statement) oldContent).getPredicate(),
                        context.memory);
            } else {
                componentList = oldContent.cloneComponents();
                componentList.set(indices[0], newInh);
                if (oldContent instanceof Conjunction) {
                    content = CompoundTerm.make(oldContent, componentList, context.memory);
                } else if ((oldContent instanceof Implication) || (oldContent instanceof Equivalence)) {
                    content = Statement.make((Statement) oldContent, componentList.get(0), componentList.get(1),
                            context.memory);
                }
            }
        }
        if (content == null) {
            return;
        }
        final Sentence sentence = context.currentTask.getSentence();
        final TruthValue truth = sentence.getTruth();
        final BudgetValue budget;
        if (sentence.isQuestion()) {
            budget = BudgetFunctions.compoundBackward(content, context.memory);
        } else {
            budget = BudgetFunctions.compoundForward(truth, content, context.memory);
        }
        context.singlePremiseTask(content, truth, budget);
    }

    /**
     * Equivalent transformation between products and images when the subject is a
     * compound
     * {<(*, S, M) --> P>, S@(*, S, M)} |- <S --> (/, P, _, M)>
     * {<S --> (/, P, _, M)>, P@(/, P, _, M)} |- <(*, S, M) --> P>
     * {<S --> (/, P, _, M)>, M@(/, P, _, M)} |- <M --> (/, P, S, _)>
     *
     * @param subject        The subject term
     * @param predicate      The predicate term
     * @param context.memory Reference to the context.memory
     */
    private static void transformSubjectPI(CompoundTerm subject, Term predicate, DerivationContext context) {
        final TruthValue truth = context.currentTask.getSentence().getTruth();
        BudgetValue budget;
        Inheritance inheritance;
        Term newSubj, newPred;
        if (subject instanceof Product) {
            Product product = (Product) subject;
            for (short i = 0; i < product.size(); i++) {
                newSubj = product.componentAt(i);
                newPred = ImageExt.make(product, predicate, i, context.memory);
                inheritance = Inheritance.make(newSubj, newPred, context.memory);
                if (inheritance != null) {
                    if (truth == null) {
                        budget = BudgetFunctions.compoundBackward(inheritance, context.memory);
                    } else {
                        budget = BudgetFunctions.compoundForward(truth, inheritance, context.memory);
                    }
                    context.singlePremiseTask(inheritance, truth, budget);
                }
            }
        } else if (subject instanceof ImageInt) {
            final ImageInt image = (ImageInt) subject;
            final int relationIndex = image.getRelationIndex();
            for (short i = 0; i < image.size(); i++) {
                if (i == relationIndex) {
                    newSubj = image.componentAt(relationIndex);
                    newPred = Product.make(image, predicate, relationIndex, context.memory);
                } else {
                    newSubj = ImageInt.make((ImageInt) image, predicate, i, context.memory);
                    newPred = image.componentAt(i);
                }
                inheritance = Inheritance.make(newSubj, newPred, context.memory);
                if (inheritance != null) {
                    if (truth == null) {
                        budget = BudgetFunctions.compoundBackward(inheritance, context.memory);
                    } else {
                        budget = BudgetFunctions.compoundForward(truth, inheritance, context.memory);
                    }
                    context.singlePremiseTask(inheritance, truth, budget);
                }
            }
        }
    }

    /**
     * Equivalent transformation between products and images when the predicate is a
     * compound
     * {<(*, S, M) --> P>, S@(*, S, M)} |- <S --> (/, P, _, M)>
     * {<S --> (/, P, _, M)>, P@(/, P, _, M)} |- <(*, S, M) --> P>
     * {<S --> (/, P, _, M)>, M@(/, P, _, M)} |- <M --> (/, P, S, _)>
     *
     * @param subject        The subject term
     * @param predicate      The predicate term
     * @param context.memory Reference to the context.memory
     */
    private static void transformPredicatePI(Term subject, CompoundTerm predicate, DerivationContext context) {
        final TruthValue truth = context.currentTask.getSentence().getTruth();
        BudgetValue budget;
        Inheritance inheritance;
        Term newSubj, newPred;
        if (predicate instanceof Product) {
            final Product product = (Product) predicate;
            for (short i = 0; i < product.size(); i++) {
                newSubj = ImageInt.make(product, subject, i, context.memory);
                newPred = product.componentAt(i);
                inheritance = Inheritance.make(newSubj, newPred, context.memory);
                if (inheritance != null) {
                    if (truth == null) {
                        budget = BudgetFunctions.compoundBackward(inheritance, context.memory);
                    } else {
                        budget = BudgetFunctions.compoundForward(truth, inheritance, context.memory);
                    }
                    context.singlePremiseTask(inheritance, truth, budget);
                }
            }
        } else if (predicate instanceof ImageExt) {
            final ImageExt image = (ImageExt) predicate;
            final int relationIndex = image.getRelationIndex();
            for (short i = 0; i < image.size(); i++) {
                if (i == relationIndex) {
                    newSubj = Product.make(image, subject, relationIndex, context.memory);
                    newPred = image.componentAt(relationIndex);
                } else {
                    newSubj = image.componentAt(i);
                    newPred = ImageExt.make((ImageExt) image, subject, i, context.memory);
                }
                inheritance = Inheritance.make(newSubj, newPred, context.memory);
                if (inheritance != null) { // jmv <<<<<
                    if (truth == null) {
                        budget = BudgetFunctions.compoundBackward(inheritance, context.memory);
                    } else {
                        budget = BudgetFunctions.compoundForward(truth, inheritance, context.memory);
                    }
                    context.singlePremiseTask(inheritance, truth, budget);
                }
            }
        }
    }

    /* --------------- Disjunction and Conjunction transform --------------- */
    /**
     * {(&&, A, B), A@(&&, A, B)} |- A, or answer (&&, A, B)? using A
     * {(||, A, B), A@(||, A, B)} |- A, or answer (||, A, B)? using A
     *
     * @param compound       The premise
     * @param component      The recognized component in the premise
     * @param compoundTask   Whether the compound comes from the task
     * @param context.memory Reference to the context.memory
     */
    static void structuralCompound(CompoundTerm compound, Term component, boolean compoundTask,
            DerivationContext context) {
        if (!component.isConstant()) {
            return;
        }
        final Term content = (compoundTask ? component : compound);
        final Task task = context.currentTask;
        final Sentence sentence = task.getSentence();
        TruthValue truth = sentence.getTruth();
        final BudgetValue budget;
        if (sentence.isQuestion()) {
            budget = BudgetFunctions.compoundBackward(content, context.memory);
        } else {
            if ((sentence.isJudgment()) == (compoundTask == (compound instanceof Conjunction))) {
                truth = TruthFunctions.deduction(truth, RELIANCE);
            } else {
                TruthValue v1, v2;
                v1 = TruthFunctions.negation(truth);
                v2 = TruthFunctions.deduction(v1, RELIANCE);
                truth = TruthFunctions.negation(v2);
            }
            budget = BudgetFunctions.forward(truth, context.memory);
        }
        context.singlePremiseTask(content, truth, budget);
    }

    /* --------------- Negation related rules --------------- */
    /**
     * {A, A@(--, A)} |- (--, A)
     *
     * @param content        The premise
     * @param context.memory Reference to the context.memory
     */
    public static void transformNegation(Term content, DerivationContext context) {
        final Task task = context.currentTask;
        final Sentence sentence = task.getSentence();
        TruthValue truth = sentence.getTruth();
        if (sentence.isJudgment()) {
            truth = TruthFunctions.negation(truth);
        }
        final BudgetValue budget;
        if (sentence.isQuestion()) {
            budget = BudgetFunctions.compoundBackward(content, context.memory);
        } else {
            budget = BudgetFunctions.compoundForward(truth, content, context.memory);
        }
        context.singlePremiseTask(content, truth, budget);
    }

    /**
     * {<A ==> B>, A@(--, A)} |- <(--, B) ==> (--, A)>
     *
     * @param statement      The premise
     * @param context.memory Reference to the context.memory
     */
    static void contraposition(Statement statement, Sentence sentence, DerivationContext context) {
        final Term subj = statement.getSubject();
        final Term pred = statement.getPredicate();
        final Term content = Statement.make(statement, Negation.make(pred, context.memory),
                Negation.make(subj, context.memory),
                context.memory);
        TruthValue truth = sentence.getTruth();
        final BudgetValue budget;
        if (sentence.isQuestion()) {
            if (content instanceof Implication) {
                budget = BudgetFunctions.compoundBackwardWeak(content, context.memory);
            } else {
                budget = BudgetFunctions.compoundBackward(content, context.memory);
            }
            context.singlePremiseTask(content, Symbols.QUESTION_MARK, truth, budget);
        } else {
            if (content instanceof Implication) {
                truth = TruthFunctions.contraposition(truth);
            }
            budget = BudgetFunctions.compoundForward(truth, content, context.memory);
            context.singlePremiseTask(content, Symbols.JUDGMENT_MARK, truth, budget);
        }
    }
}
