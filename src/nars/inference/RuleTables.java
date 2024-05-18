package nars.inference;

import nars.entity.*;
import nars.language.*;
import nars.storage.Memory;
import nars.io.Symbols;

/**
 * Table of inference rules, indexed by the TermLinks for the task and the
 * belief. Used in indirect processing of a task, to dispatch inference cases
 * to the relevant inference rules.
 */
public class RuleTables {

    /**
     * Entry point of the inference engine
     *
     * @param tLink  The selected TaskLink, which will provide a task
     * @param bLink  The selected TermLink, which may provide a belief
     * @param memory Reference to the memory
     */
    public static void reason(TaskLink tLink, TermLink bLink, Memory memory) {
        final Task task = memory.context.currentTask;
        final Sentence taskSentence = task.getSentence();
        final Term taskTerm = (Term) taskSentence.getContent().clone(); // cloning for substitution
        final Term beliefTerm = (Term) bLink.getTarget().clone(); // cloning for substitution
        final Concept beliefConcept = memory.termToConcept(beliefTerm);
        final Sentence belief = beliefConcept != null ? beliefConcept.getBelief(task) : null;
        memory.context.currentBelief = belief; // ! may be null
        if (belief != null) {
            LocalRules.match(memory);
        }
        if (!memory.noResult() && task.getSentence().isJudgment()) {
            return;
        }
        final short tIndex = tLink.getIndex(0);
        final short bIndex = bLink.getIndex(0);
        switch (tLink.getType()) { // dispatch first by TaskLink type
            case TermLink.SELF:
                switch (bLink.getType()) {
                    case TermLink.COMPONENT:
                        compoundAndSelf((CompoundTerm) taskTerm, beliefTerm, true, memory);
                        break;
                    case TermLink.COMPOUND:
                        compoundAndSelf((CompoundTerm) beliefTerm, taskTerm, false, memory);
                        break;
                    case TermLink.COMPONENT_STATEMENT:
                        if (belief != null) {
                            SyllogisticRules.detachment(task.getSentence(), belief, bIndex, memory);
                        }
                        break;
                    case TermLink.COMPOUND_STATEMENT:
                        if (belief != null) {
                            SyllogisticRules.detachment(belief, task.getSentence(), bIndex, memory);
                        }
                        break;
                    case TermLink.COMPONENT_CONDITION:
                        if (belief != null) {
                            final short bIndex2 = bLink.getIndex(1);
                            SyllogisticRules.conditionalDedInd((Implication) taskTerm, bIndex2, beliefTerm, tIndex,
                                    memory);
                        }
                        break;
                    case TermLink.COMPOUND_CONDITION:
                        if (belief != null) {
                            final short bIndex2 = bLink.getIndex(1);
                            SyllogisticRules.conditionalDedInd((Implication) beliefTerm, bIndex2, taskTerm, tIndex,
                                    memory);
                        }
                        break;
                }
                break;
            case TermLink.COMPOUND:
                switch (bLink.getType()) {
                    case TermLink.COMPOUND:
                        compoundAndCompound((CompoundTerm) taskTerm, (CompoundTerm) beliefTerm, memory);
                        break;
                    case TermLink.COMPOUND_STATEMENT:
                        compoundAndStatement((CompoundTerm) taskTerm, tIndex, (Statement) beliefTerm, bIndex,
                                beliefTerm, memory);
                        break;
                    case TermLink.COMPOUND_CONDITION:
                        if (belief != null) {
                            if (beliefTerm instanceof Implication) {
                                if (Variable.unify(Symbols.VAR_INDEPENDENT, ((Implication) beliefTerm).getSubject(),
                                        taskTerm, beliefTerm, taskTerm)) {
                                    detachmentWithVar(belief, taskSentence, bIndex, memory);
                                } else {
                                    SyllogisticRules.conditionalDedInd((Implication) beliefTerm, bIndex, taskTerm, -1,
                                            memory);
                                }
                            } else if (beliefTerm instanceof Equivalence) {
                                SyllogisticRules.conditionalAna((Equivalence) beliefTerm, bIndex, taskTerm, -1, memory);
                            }
                        }
                        break;
                }
                break;
            case TermLink.COMPOUND_STATEMENT:
                switch (bLink.getType()) {
                    case TermLink.COMPONENT:
                        componentAndStatement((CompoundTerm) memory.context.currentTerm, bIndex, (Statement) taskTerm,
                                tIndex,
                                memory);
                        break;
                    case TermLink.COMPOUND:
                        compoundAndStatement((CompoundTerm) beliefTerm, bIndex, (Statement) taskTerm, tIndex,
                                beliefTerm, memory);
                        break;
                    case TermLink.COMPOUND_STATEMENT:
                        if (belief != null) {
                            syllogisms(tLink, bLink, taskTerm, beliefTerm, memory);
                        }
                        break;
                    case TermLink.COMPOUND_CONDITION:
                        if (belief != null) {
                            final short bIndex2 = bLink.getIndex(1);
                            if (beliefTerm instanceof Implication) {
                                conditionalDedIndWithVar((Implication) beliefTerm, bIndex2, (Statement) taskTerm,
                                        tIndex, memory);
                            }
                        }
                        break;
                }
                break;
            case TermLink.COMPOUND_CONDITION:
                switch (bLink.getType()) {
                    case TermLink.COMPOUND:
                        if (belief != null) {
                            detachmentWithVar(taskSentence, belief, tIndex, memory);
                        }
                        break;
                    case TermLink.COMPOUND_STATEMENT:
                        if (belief != null) {
                            // TODO maybe put instanceof test within conditionalDedIndWithVar()
                            if (taskTerm instanceof Implication) {
                                Term subj = ((Implication) taskTerm).getSubject();
                                if (subj instanceof Negation) {
                                    if (task.getSentence().isJudgment()) {
                                        componentAndStatement((CompoundTerm) subj, bIndex, (Statement) taskTerm, tIndex,
                                                memory);
                                    } else {
                                        componentAndStatement((CompoundTerm) subj, tIndex, (Statement) beliefTerm,
                                                bIndex, memory);
                                    }
                                } else {
                                    conditionalDedIndWithVar((Implication) taskTerm, tIndex, (Statement) beliefTerm,
                                            bIndex, memory);
                                }
                            }
                            break;
                        }
                        break;
                }
        }
    }

    /* ----- syllogistic inferences ----- */
    /**
     * Meta-table of syllogistic rules, indexed by the content classes of the
     * taskSentence and the belief
     *
     * @param tLink      The link to task
     * @param bLink      The link to belief
     * @param taskTerm   The content of task
     * @param beliefTerm The content of belief
     * @param memory     Reference to the memory
     */
    private static void syllogisms(TaskLink tLink, TermLink bLink, Term taskTerm, Term beliefTerm, Memory memory) {
        final Sentence taskSentence = memory.context.currentTask.getSentence();
        final Sentence belief = memory.context.currentBelief;
        final int figure;
        if (taskTerm instanceof Inheritance) {
            if (beliefTerm instanceof Inheritance) {
                figure = indexToFigure(tLink, bLink);
                asymmetricAsymmetric(taskSentence, belief, figure, memory);
            } else if (beliefTerm instanceof Similarity) {
                figure = indexToFigure(tLink, bLink);
                asymmetricSymmetric(taskSentence, belief, figure, memory);
            } else {
                detachmentWithVar(belief, taskSentence, bLink.getIndex(0), memory);
            }
        } else if (taskTerm instanceof Similarity) {
            if (beliefTerm instanceof Inheritance) {
                figure = indexToFigure(bLink, tLink);
                asymmetricSymmetric(belief, taskSentence, figure, memory);
            } else if (beliefTerm instanceof Similarity) {
                figure = indexToFigure(bLink, tLink);
                symmetricSymmetric(belief, taskSentence, figure, memory);
            }
        } else if (taskTerm instanceof Implication) {
            if (beliefTerm instanceof Implication) {
                figure = indexToFigure(tLink, bLink);
                asymmetricAsymmetric(taskSentence, belief, figure, memory);
            } else if (beliefTerm instanceof Equivalence) {
                figure = indexToFigure(tLink, bLink);
                asymmetricSymmetric(taskSentence, belief, figure, memory);
            } else if (beliefTerm instanceof Inheritance) {
                detachmentWithVar(taskSentence, belief, tLink.getIndex(0), memory);
            }
        } else if (taskTerm instanceof Equivalence) {
            if (beliefTerm instanceof Implication) {
                figure = indexToFigure(bLink, tLink);
                asymmetricSymmetric(belief, taskSentence, figure, memory);
            } else if (beliefTerm instanceof Equivalence) {
                figure = indexToFigure(bLink, tLink);
                symmetricSymmetric(belief, taskSentence, figure, memory);
            } else if (beliefTerm instanceof Inheritance) {
                detachmentWithVar(taskSentence, belief, tLink.getIndex(0), memory);
            }
        }
    }

    /**
     * Decide the figure of syllogism according to the locations of the common
     * term in the premises
     *
     * @param link1 The link to the first premise
     * @param link2 The link to the second premise
     * @return The figure of the syllogism, one of the four: 11, 12, 21, or 22
     */
    private static int indexToFigure(TermLink link1, TermLink link2) {
        return (link1.getIndex(0) + 1) * 10 + (link2.getIndex(0) + 1);
    }

    /**
     * Syllogistic rules whose both premises are on the same asymmetric relation
     *
     * @param sentence The taskSentence in the task
     * @param belief   The judgment in the belief
     * @param figure   The location of the shared term
     * @param memory   Reference to the memory
     */
    private static void asymmetricAsymmetric(Sentence sentence, Sentence belief, int figure, Memory memory) {
        final Statement s1 = (Statement) sentence.cloneContent();
        final Statement s2 = (Statement) belief.cloneContent();
        final Term t1, t2;
        switch (figure) {
            case 11: // induction
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getSubject(), s2.getSubject(), s1, s2)) {
                    if (s1.equals(s2)) {
                        return;
                    }
                    t1 = s2.getPredicate();
                    t2 = s1.getPredicate();
                    CompositionalRules.composeCompound(s1, s2, 0, memory);
                    SyllogisticRules.abdIndCom(t1, t2, sentence, belief, figure, memory);
                }

                break;
            case 12: // deduction
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getSubject(), s2.getPredicate(), s1, s2)) {
                    if (s1.equals(s2)) {
                        return;
                    }
                    t1 = s2.getSubject();
                    t2 = s1.getPredicate();
                    if (Variable.unify(Symbols.VAR_QUERY, t1, t2, s1, s2)) {
                        LocalRules.matchReverse(memory);
                    } else {
                        SyllogisticRules.dedExe(t1, t2, sentence, belief, memory);
                    }
                }
                break;
            case 21: // exemplification
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getPredicate(), s2.getSubject(), s1, s2)) {
                    if (s1.equals(s2)) {
                        return;
                    }
                    t1 = s1.getSubject();
                    t2 = s2.getPredicate();
                    if (Variable.unify(Symbols.VAR_QUERY, t1, t2, s1, s2)) {
                        LocalRules.matchReverse(memory);
                    } else {
                        SyllogisticRules.dedExe(t1, t2, sentence, belief, memory);
                    }
                }
                break;
            case 22: // abduction
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getPredicate(), s2.getPredicate(), s1, s2)) {
                    if (s1.equals(s2)) {
                        return;
                    }
                    t1 = s1.getSubject();
                    t2 = s2.getSubject();
                    if (!SyllogisticRules.conditionalAbd(t1, t2, s1, s2, memory)) { // if conditional abduction, skip
                                                                                    // the following
                        CompositionalRules.composeCompound(s1, s2, 1, memory);
                        SyllogisticRules.abdIndCom(t1, t2, sentence, belief, figure, memory);
                    }
                }
                break;
            default:
        }
    }

    /**
     * Syllogistic rules whose first premise is on an asymmetric relation, and
     * the second on a symmetric relation
     *
     * @param asym   The asymmetric premise
     * @param sym    The symmetric premise
     * @param figure The location of the shared term
     * @param memory Reference to the memory
     */
    private static void asymmetricSymmetric(Sentence asym, Sentence sym, int figure, Memory memory) {
        final Statement asymSt = (Statement) asym.cloneContent();
        final Statement symSt = (Statement) sym.cloneContent();
        final Term t1, t2;
        switch (figure) {
            case 11:
                if (Variable.unify(Symbols.VAR_INDEPENDENT, asymSt.getSubject(), symSt.getSubject(), asymSt, symSt)) {
                    t1 = asymSt.getPredicate();
                    t2 = symSt.getPredicate();
                    if (Variable.unify(Symbols.VAR_QUERY, t1, t2, asymSt, symSt)) {
                        LocalRules.matchAsymSym(asym, sym, figure, memory);
                    } else {
                        SyllogisticRules.analogy(t2, t1, asym, sym, figure, memory);
                    }
                }
                break;
            case 12:
                if (Variable.unify(Symbols.VAR_INDEPENDENT, asymSt.getSubject(), symSt.getPredicate(), asymSt, symSt)) {
                    t1 = asymSt.getPredicate();
                    t2 = symSt.getSubject();
                    if (Variable.unify(Symbols.VAR_QUERY, t1, t2, asymSt, symSt)) {
                        LocalRules.matchAsymSym(asym, sym, figure, memory);
                    } else {
                        SyllogisticRules.analogy(t2, t1, asym, sym, figure, memory);
                    }
                }
                break;
            case 21:
                if (Variable.unify(Symbols.VAR_INDEPENDENT, asymSt.getPredicate(), symSt.getSubject(), asymSt, symSt)) {
                    t1 = asymSt.getSubject();
                    t2 = symSt.getPredicate();
                    if (Variable.unify(Symbols.VAR_QUERY, t1, t2, asymSt, symSt)) {
                        LocalRules.matchAsymSym(asym, sym, figure, memory);
                    } else {
                        SyllogisticRules.analogy(t1, t2, asym, sym, figure, memory);
                    }
                }
                break;
            case 22:
                if (Variable.unify(Symbols.VAR_INDEPENDENT, asymSt.getPredicate(), symSt.getPredicate(), asymSt,
                        symSt)) {
                    t1 = asymSt.getSubject();
                    t2 = symSt.getSubject();
                    if (Variable.unify(Symbols.VAR_QUERY, t1, t2, asymSt, symSt)) {
                        LocalRules.matchAsymSym(asym, sym, figure, memory);
                    } else {
                        SyllogisticRules.analogy(t1, t2, asym, sym, figure, memory);
                    }
                }
                break;
        }
    }

    /**
     * Syllogistic rules whose both premises are on the same symmetric relation
     *
     * @param belief       The premise that comes from a belief
     * @param taskSentence The premise that comes from a task
     * @param figure       The location of the shared term
     * @param memory       Reference to the memory
     */
    private static void symmetricSymmetric(Sentence belief, Sentence taskSentence, int figure, Memory memory) {
        final Statement s1 = (Statement) belief.cloneContent();
        final Statement s2 = (Statement) taskSentence.cloneContent();
        switch (figure) {
            case 11:
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getSubject(), s2.getSubject(), s1, s2)) {
                    SyllogisticRules.resemblance(s1.getPredicate(), s2.getPredicate(), belief, taskSentence, figure,
                            memory);
                }
                break;
            case 12:
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getSubject(), s2.getPredicate(), s1, s2)) {
                    SyllogisticRules.resemblance(s1.getPredicate(), s2.getSubject(), belief, taskSentence, figure,
                            memory);
                }
                break;
            case 21:
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getPredicate(), s2.getSubject(), s1, s2)) {
                    SyllogisticRules.resemblance(s1.getSubject(), s2.getPredicate(), belief, taskSentence, figure,
                            memory);
                }
                break;
            case 22:
                if (Variable.unify(Symbols.VAR_INDEPENDENT, s1.getPredicate(), s2.getPredicate(), s1, s2)) {
                    SyllogisticRules.resemblance(s1.getSubject(), s2.getSubject(), belief, taskSentence, figure,
                            memory);
                }
                break;
        }
    }

    /* ----- conditional inferences ----- */
    /**
     * The detachment rule, with variable unification
     *
     * @param originalMainSentence The premise that is an Implication or
     *                             Equivalence
     * @param subSentence          The premise that is the subject or predicate of
     *                             the
     *                             first one
     * @param index                The location of the second premise in the first
     * @param memory               Reference to the memory
     */
    private static void detachmentWithVar(Sentence originalMainSentence, Sentence subSentence, int index,
            Memory memory) {
        final Sentence mainSentence = (Sentence) originalMainSentence.clone(); // for substitution
        final Statement statement = (Statement) mainSentence.getContent();
        final Term component = statement.componentAt(index);
        final Term content = subSentence.getContent();
        if (((component instanceof Inheritance) || (component instanceof Negation))
                && (memory.context.currentBelief != null)) {
            if (component.isConstant()) {
                SyllogisticRules.detachment(mainSentence, subSentence, index, memory);
            } else if (Variable.unify(Symbols.VAR_INDEPENDENT, component, content, statement, content)) {
                SyllogisticRules.detachment(mainSentence, subSentence, index, memory);
            } else if ((statement instanceof Implication) && (statement.getPredicate() instanceof Statement)
                    && (memory.context.currentTask.getSentence().isJudgment())) {
                final Statement s2 = (Statement) statement.getPredicate();
                if (s2.getSubject().equals(((Statement) content).getSubject())) {
                    CompositionalRules.introVarInner((Statement) content, s2, statement, memory);
                }
                CompositionalRules.IntroVarSameSubjectOrPredicate(originalMainSentence, subSentence, component, content,
                        index, memory);
            } else if ((statement instanceof Equivalence) && (statement.getPredicate() instanceof Statement)
                    && (memory.context.currentTask.getSentence().isJudgment())) {
                CompositionalRules.IntroVarSameSubjectOrPredicate(originalMainSentence, subSentence, component, content,
                        index, memory);
            }
        }
    }

    /**
     * Conditional deduction or induction, with variable unification
     *
     * @param conditional The premise that is an Implication with a Conjunction
     *                    as condition
     * @param index       The location of the shared term in the condition
     * @param statement   The second premise that is a statement
     * @param side        The location of the shared term in the statement
     * @param memory      Reference to the memory
     */
    private static void conditionalDedIndWithVar(Implication conditional, short index, Statement statement, short side,
            Memory memory) {
        final CompoundTerm condition = (CompoundTerm) conditional.getSubject();
        final Term component = condition.componentAt(index);
        final Term component2;
        if (statement instanceof Inheritance) {
            component2 = statement;
            side = -1;
        } else if (statement instanceof Implication) {
            component2 = statement.componentAt(side);
        } else {
            component2 = null;
        }
        if (component2 != null) {
            boolean unifiable = Variable.unify(Symbols.VAR_INDEPENDENT, component, component2, conditional, statement);
            if (!unifiable) {
                unifiable = Variable.unify(Symbols.VAR_DEPENDENT, component, component2, conditional, statement);
            }
            if (unifiable) {
                SyllogisticRules.conditionalDedInd(conditional, index, statement, side, memory);
            }
        }
    }

    /* ----- structural inferences ----- */
    /**
     * Inference between a compound term and a component of it
     *
     * @param compound     The compound term
     * @param component    The component term
     * @param compoundTask Whether the compound comes from the task
     * @param memory       Reference to the memory
     */
    private static void compoundAndSelf(CompoundTerm compound, Term component, boolean compoundTask, Memory memory) {
        if ((compound instanceof Conjunction) || (compound instanceof Disjunction)) {
            if (memory.context.currentBelief != null) {
                CompositionalRules.decomposeStatement(compound, component, compoundTask, memory);
            } else if (compound.containComponent(component)) {
                StructuralRules.structuralCompound(compound, component, compoundTask, memory);
            }
            // } else if ((compound instanceof Negation) &&
            // !memory.context.currentTask.isStructural()) {
        } else if (compound instanceof Negation) {
            if (compoundTask) {
                StructuralRules.transformNegation(((Negation) compound).componentAt(0), memory);
            } else {
                StructuralRules.transformNegation(compound, memory);
            }
        }
    }

    /**
     * Inference between two compound terms
     *
     * @param taskTerm   The compound from the task
     * @param beliefTerm The compound from the belief
     * @param memory     Reference to the memory
     */
    private static void compoundAndCompound(CompoundTerm taskTerm, CompoundTerm beliefTerm, Memory memory) {
        if (taskTerm.getClass() == beliefTerm.getClass()) {
            if (taskTerm.size() > beliefTerm.size()) {
                compoundAndSelf(taskTerm, beliefTerm, true, memory);
            } else if (taskTerm.size() < beliefTerm.size()) {
                compoundAndSelf(beliefTerm, taskTerm, false, memory);
            }
        }
    }

    /**
     * Inference between a compound term and a statement
     *
     * @param compound   The compound term
     * @param index      The location of the current term in the compound
     * @param statement  The statement
     * @param side       The location of the current term in the statement
     * @param beliefTerm The content of the belief
     * @param memory     Reference to the memory
     */
    private static void compoundAndStatement(CompoundTerm compound, short index, Statement statement, short side,
            Term beliefTerm, Memory memory) {
        final Term component = compound.componentAt(index);
        final Task task = memory.context.currentTask;
        if (component.getClass() == statement.getClass()) {
            if ((compound instanceof Conjunction) && (memory.context.currentBelief != null)) {
                if (Variable.unify(Symbols.VAR_DEPENDENT, component, statement, compound, statement)) {
                    SyllogisticRules.eliminateVarDep(compound, component, statement.equals(beliefTerm), memory);
                } else if (task.getSentence().isJudgment()) { // && !compound.containComponent(component)) {
                    CompositionalRules.introVarInner(statement, (Statement) component, compound, memory);
                } else if (Variable.unify(Symbols.VAR_QUERY, component, statement, compound, statement)) {
                    CompositionalRules.decomposeStatement(compound, component, true, memory);
                }
            }
        } else {
            // if (!task.isStructural() && task.getSentence().isJudgment()) {
            if (task.getSentence().isJudgment()) {
                if (statement instanceof Inheritance) {
                    StructuralRules.structuralCompose1(compound, index, statement, memory);
                    // if (!(compound instanceof SetExt) && !(compound instanceof SetInt)) {
                    if (!(compound instanceof SetExt || compound instanceof SetInt || compound instanceof Negation)) {
                        StructuralRules.structuralCompose2(compound, index, statement, side, memory);
                    } // {A --> B, A @ (A&C)} |- (A&C) --> (B&C)
                } else if ((statement instanceof Similarity) && !(compound instanceof Conjunction)) {
                    StructuralRules.structuralCompose2(compound, index, statement, side, memory);
                } // {A <-> B, A @ (A&C)} |- (A&C) <-> (B&C)
            }
        }
    }

    /**
     * Inference between a component term (of the current term) and a statement
     *
     * @param compound  The compound term
     * @param index     The location of the current term in the compound
     * @param statement The statement
     * @param side      The location of the current term in the statement
     * @param memory    Reference to the memory
     */
    private static void componentAndStatement(CompoundTerm compound, short index, Statement statement, short side,
            Memory memory) {
        // if (!memory.context.currentTask.isStructural()) {
        if (statement instanceof Inheritance) {
            StructuralRules.structuralDecompose1(compound, index, statement, memory);
            if (!(compound instanceof SetExt) && !(compound instanceof SetInt)) {
                StructuralRules.structuralDecompose2(statement, index, memory); // {(C-B) --> (C-A), A @ (C-A)} |- A -->
                                                                                // B
            } else {
                StructuralRules.transformSetRelation(compound, statement, side, memory);
            }
        } else if (statement instanceof Similarity) {
            StructuralRules.structuralDecompose2(statement, index, memory); // {(C-B) --> (C-A), A @ (C-A)} |- A --> B
            if ((compound instanceof SetExt) || (compound instanceof SetInt)) {
                StructuralRules.transformSetRelation(compound, statement, side, memory);
            }
        } else if ((statement instanceof Implication) && (compound instanceof Negation)) {
            if (index == 0) {
                StructuralRules.contraposition(statement, memory.context.currentTask.getSentence(), memory);
            } else {
                StructuralRules.contraposition(statement, memory.context.currentBelief, memory);
            }
        }
        // }
    }

    /* ----- inference with one TaskLink only ----- */
    /**
     * The TaskLink is of type TRANSFORM, and the conclusion is an equivalent
     * transformation
     *
     * @param tLink  The task link
     * @param memory Reference to the memory
     */
    public static void transformTask(TaskLink tLink, Memory memory) {
        final CompoundTerm content = (CompoundTerm) memory.context.currentTask.getContent().clone();
        final short[] indices = tLink.getIndices();
        final Term inh;
        if ((indices.length == 2) || (content instanceof Inheritance)) { // <(*, term, #) --> #>
            inh = content;
        } else if (indices.length == 3) { // <<(*, term, #) --> #> ==> #>
            inh = content.componentAt(indices[0]);
        } else if (indices.length == 4) { // <(&&, <(*, term, #) --> #>, #) ==> #>
            Term component = content.componentAt(indices[0]);
            if ((component instanceof Conjunction)
                    && (((content instanceof Implication) && (indices[0] == 0)) || (content instanceof Equivalence))) {
                inh = ((CompoundTerm) component).componentAt(indices[1]);
            } else {
                return;
            }
        } else {
            inh = null;
        }
        if (inh instanceof Inheritance) {
            StructuralRules.transformProductImage((Inheritance) inh, content, indices, memory);
        }
    }
}
