/*
 * SyllogisticRules.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.inference;

import java.util.*;

import nars.entity.*;
import nars.language.*;
import nars.io.Symbols;
import nars.main.Memory;

/**
 * Syllogisms: Inference rules based on the transitivity of the relation.
 */
public final class SyllogisticRules {

    /* --------------- rules used in both first-tense inference and higher-tense inference --------------- */
    /**
     * {<S ==> M>, <M ==> P>} |- {<S ==> P>, <P ==> S>}
     * @param term1 Subject of the first new task
     * @param term2 Predicate of the first new task
     * @param sentence The first premise
     * @param belief The second premise
     */
    static void dedExe(Term term1, Term term2, Sentence sentence, Judgment belief) {
        if (Statement.invalidStatement(term1, term2)) {
            return;
        }
        TruthValue value1 = sentence.getTruth();
        TruthValue value2 = belief.getTruth();
        TruthValue truth1 = null;
        TruthValue truth2 = null;
        BudgetValue budget1, budget2;
        if (sentence instanceof Question) {
            budget1 = BudgetFunctions.backwardWeak(value2);
            budget2 = BudgetFunctions.backwardWeak(value2);
        } else {
            if (sentence instanceof Goal) {
                truth1 = TruthFunctions.desireWeak(value1, value2);
                truth2 = TruthFunctions.desireWeak(value1, value2);
            } else {
                truth1 = TruthFunctions.deduction(value1, value2);
                truth2 = TruthFunctions.exemplification(value1, value2);
            }
            budget1 = BudgetFunctions.forward(truth1);
            budget2 = BudgetFunctions.forward(truth2);
        }
        TemporalRules.Relation order1 = sentence.getContent().getTemporalOrder();
        TemporalRules.Relation order2 = belief.getContent().getTemporalOrder();
        TemporalRules.Relation order = TemporalRules.temporalInference(order1, order2);
        if (order == null) {
            order = TemporalRules.Relation.WHEN;
            truth1 = TruthFunctions.temporalInduction(truth1);
            truth2 = TruthFunctions.temporalInduction(truth2);
        }
        Statement content1 = Statement.make((Statement) sentence.getContent(), term1, term2, order);
        Statement content2 = Statement.make((Statement) sentence.getContent(), term2, term1, TemporalRules.reverse(order));
        Memory.doublePremiseTask(budget1, content1, truth1);
        Memory.doublePremiseTask(budget2, content2, truth2);
    }

    /**
     * {<M ==> S>, <M ==> P>} |- {<S ==> P>, <P ==> S>, <S <=> P>}
     * @param term1 Subject of the first new task
     * @param term2 Predicate of the first new task
     * @param taskSentence The first premise
     * @param belief The second premise
     * @param figure Locations of the shared term in premises
     */
    static void abdIndCom(Term term1, Term term2, Sentence taskSentence, Judgment belief, int figure) {
        if (Statement.invalidStatement(term1, term2)) {
            return;
        }
        Statement st1 = (Statement) taskSentence.getContent();
        Statement st2 = (Statement) belief.getContent();
        TruthValue truth1 = null;
        TruthValue truth2 = null;
        TruthValue truth3 = null;
        BudgetValue budget1, budget2, budget3;
        TruthValue value1 = taskSentence.getTruth();
        TruthValue value2 = belief.getTruth();
        if (taskSentence instanceof Question) {
            budget1 = BudgetFunctions.backward(value2);
            budget2 = BudgetFunctions.backwardWeak(value2);
            budget3 = BudgetFunctions.backward(value2);
        } else {
            if (taskSentence instanceof Goal) {
                truth1 = TruthFunctions.desireStrong(value1, value2);
                truth2 = TruthFunctions.desireWeak(value2, value1);
                truth3 = TruthFunctions.desireStrong(value1, value2);
            } else {
                truth1 = TruthFunctions.abduction(value1, value2);
                truth2 = TruthFunctions.abduction(value2, value1);
                truth3 = TruthFunctions.comparison(value1, value2);
            }
            budget1 = BudgetFunctions.forward(truth1);
            budget2 = BudgetFunctions.forward(truth2);
            budget3 = BudgetFunctions.forward(truth3);
        }
        TemporalRules.Relation order1 = st1.getTemporalOrder();
        TemporalRules.Relation order2 = st2.getTemporalOrder();
        TemporalRules.Relation order = TemporalRules.temporalSyllogism(order1, order2, figure);
        if (order == null) {
            order = TemporalRules.Relation.WHEN;
            truth1 = TruthFunctions.temporalInduction(truth1);
            truth2 = TruthFunctions.temporalInduction(truth2);
            truth3 = TruthFunctions.temporalInduction(truth3);
        }
        Statement statement1 = Statement.make(st1, term1, term2, order);
        Statement statement2 = Statement.make(st1, term2, term1, TemporalRules.reverse(order));
        Statement statement3 = Statement.makeSym(st1, term1, term2, order);
        Memory.doublePremiseTask(budget1, statement1, truth1);
        Memory.doublePremiseTask(budget2, statement2, truth2);
        Memory.doublePremiseTask(budget3, statement3, truth3);
        if (statement1.isConstant()) {
            Memory.doublePremiseTask(budget1, introVarInd(belief, taskSentence, figure, true), truth1);
            Memory.doublePremiseTask(budget2, introVarInd(taskSentence, belief, figure, true), truth2);
            Memory.doublePremiseTask(budget3, introVarInd(taskSentence, belief, figure, false), truth3);
        }
    }

    /**
     * {<S ==> P>, <M <=> P>} |- <S ==> P>
     * @param term1 Subject of the new task
     * @param term2 Predicate of the new task
     * @param asym The asymmetric premise
     * @param sym The symmetric premise
     * @param figure Locations of the shared term in premises
     */
    static void analogy(Term term1, Term term2, Sentence asym, Sentence sym, int figure) {
        if (Statement.invalidStatement(term1, term2)) {
            return;
        }
        Statement asymSt = (Statement) asym.getContent();
        Statement symSt = (Statement) sym.getContent();
        TruthValue truth = null;
        BudgetValue budget;
        Sentence sentence = Memory.currentTask.getSentence();
        CompoundTerm taskTerm = (CompoundTerm) sentence.getContent();
        if (sentence instanceof Question) {
            if (taskTerm.isCommutative()) {
                budget = BudgetFunctions.backwardWeak(asym.getTruth());
            } else {
                budget = BudgetFunctions.backward(sym.getTruth());
            }
        } else {
            if (sentence instanceof Goal) {
                if (taskTerm.isCommutative()) {
                    truth = TruthFunctions.desireWeak(asym.getTruth(), sym.getTruth());
                } else {
                    truth = TruthFunctions.desireStrong(asym.getTruth(), sym.getTruth());
                }
            } else {
                truth = TruthFunctions.analogy(asym.getTruth(), sym.getTruth());
            }
            budget = BudgetFunctions.forward(truth);
        }
        TemporalRules.Relation order1 = asymSt.getTemporalOrder();
        TemporalRules.Relation order2 = symSt.getTemporalOrder();
        TemporalRules.Relation order;
        switch (figure) {
            case 11:
            case 12:
                order = TemporalRules.temporalSyllogism(order2, order1, figure);
                break;
            case 21:
            case 22:
                order = TemporalRules.temporalSyllogism(order1, order2, figure);
                break;
            default:
                return;
        }
        if (order == null) {
            order = TemporalRules.Relation.WHEN;
            truth = TruthFunctions.temporalInduction(truth);
        }
        Term content = Statement.make(asymSt, term1, term2, order);
        Memory.doublePremiseTask(budget, content, truth);
    }

    /**
     * {<S <=> M>, <M <=> P>} |- <S <=> P>
     * @param term1 Subject of the new task
     * @param term2 Predicate of the new task
     * @param belief The first premise
     * @param sentence The second premise
     * @param figure Locations of the shared term in premises
     */
    static void resemblance(Term term1, Term term2, Judgment belief, Sentence sentence, int figure) {
        if (Statement.invalidStatement(term1, term2)) {
            return;
        }
        Statement st1 = (Statement) belief.getContent();
        Statement st2 = (Statement) sentence.getContent();
        TruthValue truth = null;
        BudgetValue budget;
        Task task = Memory.currentTask;
        if (sentence instanceof Question) {
            budget = BudgetFunctions.backward(belief.getTruth());
        } else {
            if (sentence instanceof Goal) {
                truth = TruthFunctions.desireStrong(sentence.getTruth(), belief.getTruth());
            } else {
                truth = TruthFunctions.resemblance(belief.getTruth(), sentence.getTruth());
            }
            budget = BudgetFunctions.forward(truth);
        }
        TemporalRules.Relation order1 = st1.getTemporalOrder();
        TemporalRules.Relation order2 = st2.getTemporalOrder();
        TemporalRules.Relation order = TemporalRules.temporalSyllogism(order1, order2, figure);
        if (order == null) {
            order = TemporalRules.Relation.WHEN;
            truth = TruthFunctions.temporalInduction(truth);
        }
        Term statement = Statement.make(st1, term1, term2, order);
        Memory.doublePremiseTask(budget, statement, truth);
    }

    /* --------------- rules used only in conditional inference --------------- */
    /**
     * {<<M --> S> ==> <M --> P>>, <M --> S>} |- <M --> P>
     * {<<M --> S> ==> <M --> P>>, <M --> P>} |- <M --> S>
     * @param mainSentence The implication/equivalence premise
     * @param subSentence The premise on part of s1
     * @param s The location of s2 in s1
     */
    static void detachment(Sentence mainSentence, Sentence subSentence, int s) {
        Statement statement = (Statement) mainSentence.getContent();
        Term subject = statement.getSubject();
        Term predicate = statement.getPredicate();
        Term term = subSentence.getContent();
        Term content;
        int side;
        if (term.equals(subject)) {
            side = 0;
            content = predicate;
        } else if (term.equals(predicate)) {
            side = 1;
            content = subject;
        } else {
            return;
        }
        if ((content instanceof Statement) && ((Statement) content).invalid())
            return;
        Sentence taskSentence = Memory.currentTask.getSentence();
        Sentence beliefSentence = Memory.currentBelief;
        TruthValue beliefTruth = beliefSentence.getTruth();
        TruthValue truth1 = mainSentence.getTruth();
        TruthValue truth2 = subSentence.getTruth();
        TruthValue truth = null;
        BudgetValue budget;
        if (taskSentence instanceof Question) {
            if (statement instanceof Equivalence) {
                budget = BudgetFunctions.backward(beliefTruth);
            } else if (side == 0) {
                budget = BudgetFunctions.backwardWeak(beliefTruth);
            } else {
                budget = BudgetFunctions.backward(beliefTruth);
            }
        } else {
            if (taskSentence instanceof Goal) {
                if (statement instanceof Equivalence) {
                    truth = TruthFunctions.desireStrong(truth1, truth2);
                } else if (side == 0) {
                    truth = TruthFunctions.desireInd(truth1, truth2);
                } else {
                    truth = TruthFunctions.desireDed(truth1, truth2);
                }
            } else {
                if (statement instanceof Equivalence) {
                    truth = TruthFunctions.analogy(truth1, truth2);
                } else if (side == 0) {
                    truth = TruthFunctions.deduction(truth1, truth2);
                } else {
                    truth = TruthFunctions.abduction(truth2, truth1);
                }
            }
            budget = BudgetFunctions.forward(truth);
        }
        TemporalRules.Relation order1 = subSentence.getTense();
        TemporalRules.Relation order2 = statement.getTemporalOrder();
        TemporalRules.Relation order;
        if (side == 0) {
            order = TemporalRules.temporalInference(order1, order2);
        } else {
            order = TemporalRules.temporalInference(order1, TemporalRules.reverse(order2));
        }
        if (order == null) {
            order = TemporalRules.Relation.WHEN;
//            truth = TruthFunctions.temporalInduction(truth);
        }
        Memory.currentTense = order;
        Memory.doublePremiseTask(budget, content, truth);
    }

    /**
     * {<(&&, S1, S2, S3) ==> P>, S1} |- <(&&, S2, S3) ==> P>
     * {<(&&, S2, S3) ==> P>, <S1 ==> S2>} |- <(&&, S1, S3) ==> P>
     * {<(&&, S1, S3) ==> P>, <S1 ==> S2>} |- <(&&, S2, S3) ==> P>
     * @param premise1 The conditional premise
     * @param index The location of the shared term in the condition of premise1
     * @param premise2 The premise which, or part of which, appears in the condition of premise1
     * @param side The location of the shared term in premise2: 0 for subject, 1 for predicate, -1 for the whole term
     */
    static void conditionalDedInd(Implication premise1, short index, Term premise2, int side) {
        Task task = Memory.currentTask;
        Sentence taskSentence = task.getSentence();
        Judgment belief = Memory.currentBelief;
        boolean deduction = (side != 0);
        HashMap substitute = Variable.findSubstitute(Variable.VarType.ALL, premise2, belief.getContent());
        boolean conditionalTask = (substitute != null);
        TemporalRules.Relation tense1 = (conditionalTask ? taskSentence.getTense() : belief.getTense());
        TemporalRules.Relation tense2 = (conditionalTask ? belief.getTense() : taskSentence.getTense());
        TemporalRules.Relation order1 = premise1.getTemporalOrder();
        TemporalRules.Relation order2 = premise2.getTemporalOrder();
        if ((side == -1) && (tense2 == TemporalRules.Relation.AFTER)) {
            return;
        }
        if ((side == 0) && (order2 == TemporalRules.Relation.AFTER)) {
            return;
        }
        if ((side == 1) && (order2 == TemporalRules.Relation.BEFORE)) {
            return;
        }
        TemporalRules.Relation tense = TemporalRules.temporalInference(tense1, tense2);
        if (tense != TemporalRules.Relation.NONE) {
            tense = TemporalRules.Relation.WHEN;
        }
        Term commonComponent;
        Term newComponent = null;
        if (side == 0) {
            commonComponent = ((Statement) premise2).getSubject();
            newComponent = ((Statement) premise2).getPredicate();
        } else if (side == 1) {
            commonComponent = ((Statement) premise2).getPredicate();
            newComponent = ((Statement) premise2).getSubject();
        } else {
            commonComponent = premise2;
        }
        Conjunction oldCondition = (Conjunction) premise1.getSubject();
        boolean match = Variable.unify(Variable.VarType.INDEPENDENT, oldCondition.componentAt(index), commonComponent, premise1, premise2);
        if (!match && (commonComponent.getClass() == oldCondition.getClass())) {
            match = Variable.unify(Variable.VarType.INDEPENDENT, oldCondition.componentAt(index), ((CompoundTerm) commonComponent).componentAt(index), premise1, premise2);
        }
        if (!match) {
            return;
        }
        Term newCondition;
        if (oldCondition.equals(commonComponent)) {
            newCondition = null;
        } else {
            newCondition = CompoundTerm.replaceComponent(oldCondition, index, newComponent);
        }
        Term content;
        if (newCondition != null) {
            content = Statement.make(premise1, newCondition, premise1.getPredicate(), order1);
        } else {
            content = premise1.getPredicate();
        }
        if (content == null) {
            return;
        }
        TruthValue truth1 = taskSentence.getTruth();
        TruthValue truth2 = belief.getTruth();
        TruthValue truth = null;
        BudgetValue budget;
        if (taskSentence instanceof Question) {
            budget = BudgetFunctions.backwardWeak(truth2);
        } else {
            if (taskSentence instanceof Goal) {
                if (conditionalTask) {
                    truth = TruthFunctions.desireWeak(truth1, truth2);
                } else if (deduction) {
                    truth = TruthFunctions.desireInd(truth1, truth2);
                } else {
                    truth = TruthFunctions.desireDed(truth1, truth2);
                }
                budget = BudgetFunctions.forward(truth);
            } else {
                if (deduction) {
                    truth = TruthFunctions.deduction(truth1, truth2);
                } else if (conditionalTask) {
                    truth = TruthFunctions.induction(truth2, truth1);
                } else {
                    truth = TruthFunctions.induction(truth1, truth2);
                }
            }
            budget = BudgetFunctions.forward(truth);
        }
        Memory.currentTense = tense;
        Memory.doublePremiseTask(budget, content, truth);
    }

    /**
     * {<(&&, S2, S3) ==> P>, <(&&, S1, S3) ==> P>} |- <S1 ==> S2>
     * @param cond1 The condition of the first premise
     * @param cond2 The condition of the second premise
     * @param st1 The first premise
     * @param st2 The second premise
     * @return Whether there are derived tasks
     */
    static boolean conditionalAbd(Term cond1, Term cond2, Statement st1, Statement st2) {
        if (!(st1 instanceof Implication) || !(st2 instanceof Implication)) {
            return false;
        }
        if (!(cond1 instanceof Conjunction) && !(cond2 instanceof Conjunction)) {
            return false;
        }
        TemporalRules.Relation order1 = st1.getTemporalOrder();
        TemporalRules.Relation order2 = st2.getTemporalOrder();
        if (order1 != order2) {
            return false;
        }
        Term term1 = null;
        Term term2 = null;
        if (cond1 instanceof Conjunction) {
            term1 = CompoundTerm.reduceComponents((Conjunction) cond1, cond2);
        }
        if (cond2 instanceof Conjunction) {
            term2 = CompoundTerm.reduceComponents((Conjunction) cond2, cond1);
        }
        if ((term1 == null) && (term2 == null)) {
            return false;
        }
        Task task = Memory.currentTask;
        Sentence sentence = task.getSentence();
        Judgment belief = Memory.currentBelief;
        TruthValue value1 = sentence.getTruth();
        TruthValue value2 = belief.getTruth();
        boolean keepOrder = (Variable.findSubstitute(Variable.VarType.INDEPENDENT, st1, task.getContent()) != null);
        Term content;
        TruthValue truth = null;
        BudgetValue budget;
        if (term1 != null) {
            if (term2 != null) {
                content = Statement.make(st2, term2, term1, order2);
            } else {
                content = term1;
            }
            if (sentence instanceof Question) {
                budget = BudgetFunctions.backwardWeak(value2);
            } else {
                if (sentence instanceof Goal) {
                    if (keepOrder) {
                        truth = TruthFunctions.desireDed(value1, value2);
                    } else {
                        truth = TruthFunctions.desireInd(value1, value2);
                    }
                } else {
                    truth = TruthFunctions.abduction(value2, value1);
                }
                budget = BudgetFunctions.forward(truth);
            }
            Memory.doublePremiseTask(budget, content, truth);
        }
        if (term2 != null) {
            if (term1 != null) {
                content = Statement.make(st1, term1, term2, order1);
            } else {
                content = term2;
            }
            if (sentence instanceof Question) {
                budget = BudgetFunctions.backwardWeak(value2);
            } else {
                if (sentence instanceof Goal) {
                    if (keepOrder) {
                        truth = TruthFunctions.desireDed(value1, value2);
                    } else {
                        truth = TruthFunctions.desireInd(value1, value2);
                    }
                } else {
                    truth = TruthFunctions.abduction(value1, value2);
                }
                budget = BudgetFunctions.forward(truth);
            }
            Memory.currentTense = TemporalRules.Relation.NONE;
            Memory.doublePremiseTask(budget, content, truth);
        }
        return true;
    }

    /* --------------- rules used for independent variable introduction --------------- */
    /**
     * {<M --> S>, <M --> P>} |- <<#x --> S> ==> <#x --> P>>
     * {<M --> S>, <M --> P>} |- <<#x --> S> <=> <#x --> P>>
     * @param sentence1 The first premise <M --> S>
     * @param sentence2 The second premise <M --> P>
     * @param figure The figure indicating the location of the shared term
     * @param isImplication The conclusion is Implication, not Equivalence
     */
    private static Statement introVarInd(Sentence sentence1, Sentence sentence2, int figure, boolean isImplication) {
        Statement premise1 = (Statement) sentence1.getContent();
        Statement premise2 = (Statement) sentence2.getContent();
        Statement state1, state2;
        Variable v1 = new Variable(Symbols.VARIABLE_TAG + "0");
        Variable v2 = new Variable(Symbols.VARIABLE_TAG + "0");
        if (figure == 11) {
            state1 = Statement.make(premise1, v1, premise1.getPredicate());
            state2 = Statement.make(premise2, v2, premise2.getPredicate());
        } else {
            state1 = Statement.make(premise1, premise1.getSubject(), v1);
            state2 = Statement.make(premise2, premise2.getSubject(), v2);
        }
        TemporalRules.Relation tense1 = sentence1.getTense();
        TemporalRules.Relation tense2 = sentence2.getTense();
        TemporalRules.Relation tense = TemporalRules.tenseInference(tense1, tense2);
        if (tense == null) {
            return null;
        }
        Statement content;
        if (isImplication) {
            content = Implication.make(state1, state2, tense);
        } else {
            content = Equivalence.make(state1, state2, tense);
        }
        return content;
    }

    /**
     * {<M --> S>, <C ==> <M --> P>>} |- <(&&, <#x --> S>, C) ==> <#x --> P>>
     * {<M --> S>, (&&, C, <M --> P>)} |- (&&, C, <<#x --> S> ==> <#x --> P>>)
     * @param premise1 The first premise directly used in internal induction, <M --> S>
     * @param premise2 The component to be used as a premise in internal induction, <M --> P>
     * @param oldCompound The whole content of the first premise, Implication or Conjunction
     */
    static void introVarIndInner(Statement premise1, Statement premise2, CompoundTerm oldCompound) {
        Task task = Memory.currentTask;
        Sentence taskSentence = task.getSentence();
        if (!taskSentence.isJudgment()) {
            return;
        }
        if (premise1.getClass() != premise2.getClass()) {
            return;
        }
        Variable var1 = new Variable(Symbols.VARIABLE_TAG + "0");
        Variable var2 = new Variable(Symbols.VARIABLE_TAG + "0");
        Statement state1, state2;
        if (premise1.getSubject().equals(premise2.getSubject())) {
            state1 = Statement.make(premise1, var1, premise1.getPredicate());
            state2 = Statement.make(premise2, var2, premise2.getPredicate());
        } else if (premise1.getPredicate().equals(premise2.getPredicate())) {
            state1 = Statement.make(premise1, premise1.getSubject(), var1);
            state2 = Statement.make(premise2, premise2.getSubject(), var2);
        } else {
            return;
        }
        Sentence belief = Memory.currentBelief;
        Term compound, content;
        TemporalRules.Relation tense;
        TruthValue truth;
        if (premise1.equals(taskSentence.getContent())) {
            truth = TruthFunctions.abduction(taskSentence.getTruth(), belief.getTruth());
            tense = TemporalRules.tenseInference(taskSentence.getTense(), belief.getTense());
        } else {
            truth = TruthFunctions.abduction(belief.getTruth(), taskSentence.getTruth());
            tense = TemporalRules.tenseInference(belief.getTense(), taskSentence.getTense());
        }
        if (tense == null) {
            return;
        }
        if ((oldCompound instanceof Implication) && (tense == oldCompound.getTemporalOrder())) {
            compound = Statement.make((Statement) oldCompound, oldCompound.componentAt(0), state2);
            content = Statement.make((Statement) oldCompound, state1, compound);
        } else if (oldCompound instanceof Conjunction) {
            compound = Implication.make(state1, state2, tense);
            content = CompoundTerm.replaceComponent(oldCompound, premise2, compound);
        } else {
            return;
        }
        BudgetValue budget = BudgetFunctions.forward(truth);
        Memory.doublePremiseTask(budget, content, truth);
    }

    /**
     * {<M ==> S>, <M ==> P>} |- {<S ==> P>, <P ==> S>, <S <=> P>}
     * @param task1 The first premise
     * @param task2 The second premise
     * @param order Temporal order of the premises
     */
    static void temporalIndCom(Task task1, Task task2, TemporalRules.Relation order) {
        Judgment judg1 = (Judgment) task1.getSentence();
        Judgment judg2 = (Judgment) task2.getSentence();
        Stamp stamp = Stamp.make(judg1.getStamp(), judg2.getStamp());
        if (stamp == null) {
            return;
        }
        Memory.currentStamp = stamp;
        Term term1 = judg1.getContent();
        Term term2 = judg2.getContent();
        if ((term1 instanceof Inheritance) && (term2 instanceof Inheritance)) {
            Statement s1 = (Statement) term1;
            Statement s2 = (Statement) term2;
            Variable var1 = new Variable(Symbols.VARIABLE_TAG + "0");
            Variable var2 = new Variable(Symbols.VARIABLE_TAG + "0");
            if (s1.getSubject().equals(s2.getSubject())) {
                term1 = Statement.make(s1, var1, s1.getPredicate());
                term2 = Statement.make(s2, var2, s2.getPredicate());
            } else if (s1.getPredicate().equals(s2.getPredicate())) {
                term1 = Statement.make(s1, s1.getSubject(), var1);
                term2 = Statement.make(s2, s2.getSubject(), var2);
            }
        } else { // to generalize
            Term condition;
            if ((term1 instanceof Implication) && (term2 instanceof Inheritance)) {
                condition = ((Implication) term1).getSubject();
                if (condition.equals(term2)) {
                    return;
                }
                if ((condition instanceof Conjunction) && ((Conjunction) condition).containComponent(term2)) {
                    return;
                }
            } else if ((term1 instanceof Inheritance) && (term2 instanceof Implication)) {
                condition = ((Implication) term2).getSubject();
                if (condition.equals(term1)) {
                    return;
                }
                if ((condition instanceof Conjunction) && ((Conjunction) condition).containComponent(term1)) {
                    return;
                }
            }
        }
        Statement statement1 = Implication.make(term1, term2, order);
        Statement statement2 = Implication.make(term2, term1, TemporalRules.reverse(order));
        Statement statement3 = Equivalence.make(term1, term2, order);
        TruthValue value1 = judg1.getTruth();
        TruthValue value2 = judg2.getTruth();
        TruthValue truth1 = TruthFunctions.induction(value1, value2);
        TruthValue truth2 = TruthFunctions.induction(value2, value1);
        TruthValue truth3 = TruthFunctions.comparison(value1, value2);
        BudgetValue budget1 = BudgetFunctions.temporalIndCom(task1.getBudget(), task2.getBudget(), truth1);
        BudgetValue budget2 = BudgetFunctions.temporalIndCom(task1.getBudget(), task2.getBudget(), truth2);
        BudgetValue budget3 = BudgetFunctions.temporalIndCom(task1.getBudget(), task2.getBudget(), truth3);
        Memory.currentTense = TemporalRules.Relation.WHEN;
        Memory.doublePremiseTask(budget1, statement1, truth1);
        Memory.doublePremiseTask(budget2, statement2, truth2);
        Memory.doublePremiseTask(budget3, statement3, truth3);
    }
}
