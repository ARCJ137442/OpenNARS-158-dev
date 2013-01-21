/*
 * Concept.java
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
package nars.entity;

import java.util.ArrayList;

import nars.gui.ConceptWindow;
import nars.inference.*;
import nars.io.Symbols;
import nars.language.*;
import nars.main.*;
import nars.storage.*;

/**
 * A concept contains information associated with a term, including directly 
 * and indirectly related tasks and beliefs.
 * <p>
 * To make sure the space will be released, the only allowed reference to a concept are
 * those in a ConceptBag. All other access go through the Term that names the concept.
 */
public final class Concept extends Item {

    /** The term is the unique ID of the concept */
    private Term term;
    /** Task links for indirect processing */
    private TaskLinkBag taskLinks;
    /** Term links between the term and its components and compounds */
    private TermLinkBag termLinks;
    /** Link templates of TermLink, only in concepts with CompoundTerm */
    private ArrayList<TermLink> termLinkTemplates;
    /** Question directly asked about the term */
    private ArrayList<Task> questions;
    /** Sentences directly made about the term, with non-future tense */
    private ArrayList<Sentence> beliefs;
    /** Whether the content of the concept is being displayed */
    private boolean showing = false;
    /** The display window */
    private ConceptWindow window = null;
    /** Reference to the memory */
    Memory memory;


    /* ---------- constructor and intialization ---------- */
    /**
     * Constructor, called in Memory.getConcept only
     * @param tm A term corresponding to the concept
     * @param memory A reference to the memory
     */
    public Concept(Term tm, Memory memory) {
        super(tm.getName());
        term = tm;
        this.memory = memory;
        questions = new ArrayList<Task>();
        beliefs = new ArrayList<Sentence>();
        taskLinks = new TaskLinkBag(memory);
        termLinks = new TermLinkBag(memory);
        if (tm instanceof CompoundTerm) {
            termLinkTemplates = ((CompoundTerm) tm).prepareComponentLinks();
        }
    }

    /* ---------- direct processing of tasks ---------- */
    /**
     * Directly process a new task. Called exactly once on each task.
     * Using local information and finishing in a constant time.
     * Provide feedback in the taskBudget value of the task.
     * <p>
     * called in Memory.immediateProcess only
     * @param task The task to be processed
     */
    public void directProcess(Task task) {
        if (task.getSentence().isJudgment()) {
            processJudgment(task);
        } else {
            processQuestion(task);
        }
        if (task.getBudget().aboveThreshold()) {    // still need to be processed
            linkToTask(task);
        }
        if (showing) {
            window.post(displayContent());
        }
    }

    /**
     * To accept a new judgment as isBelief, and check for revisions and solutions
     * @param judg The judgment to be accepted
     * @param task The task to be processed
     * @return Whether to continue the processing of the task
     */
    private void processJudgment(Task task) {
        Sentence judg = task.getSentence();
        Sentence oldBelief = evaluation(judg, beliefs);
        if (oldBelief != null) {
            memory.newStamp = Stamp.make(judg.getStamp(), oldBelief.getStamp(), memory.getTime());
            if ((memory.newStamp != null) && Variable.unify(Symbols.VAR_INDEPENDENT, judg.getContent(), oldBelief.getContent())) {
                memory.currentBelief = oldBelief;
                LocalRules.revision(judg, oldBelief, false, memory);
            }
        }
        if (task.getBudget().aboveThreshold()) {
            for (Task ques : questions) {
                LocalRules.trySolution(ques.getSentence(), judg, ques, memory);
            }
            addToTable(judg, beliefs, Parameters.MAXMUM_BELIEF_LENGTH);
        }
    }

    /**
     * To answer a question by existing beliefs
     * @param task The task to be processed
     * @return Whether to continue the processing of the task
     */
    public float processQuestion(Task task) {
        Sentence ques = task.getSentence();
        boolean newQuestion = true;
        if (questions != null) {
            for (Task t : questions) {
                Sentence q = t.getSentence();
                if (q.getContent().equals(ques.getContent())) {
                    ques = q;
                    newQuestion = false;
                    break;
                }
            }
        }
        if (newQuestion) {
            questions.add(task);
        }
        if (questions.size() > Parameters.MAXMUM_QUESTIONS_LENGTH) {
            questions.remove(0);    // FIFO
        }
        Sentence newAnswer = evaluation(ques, beliefs);
        if (newAnswer != null) {
            LocalRules.trySolution(ques, newAnswer, task, memory);
            return newAnswer.getTruth().getExpectation();
        } else {
            return 0.5f;
        }
    }

    /**
     * Link to a new task from all relevant concepts for continued processing in
     * the near future for unspecified time.
     * <p>
     * The only method that calls the TaskLink constructor.
     * @param task The task to be linked
     * @param content The content of the task
     */
    private void linkToTask(Task task) {
        BudgetValue taskBudget = task.getBudget();
        TaskLink taskLink = new TaskLink(task, null, taskBudget);   // link type: SELF
        insertTaskLink(taskLink);
        if (term instanceof CompoundTerm) {
            if (termLinkTemplates.size() > 0) {
                BudgetValue subBudget = BudgetFunctions.distributeAmongLinks(taskBudget, termLinkTemplates.size());
                if (subBudget.aboveThreshold()) {
                    Term componentTerm;
                    Concept componentConcept;
                    for (TermLink termLink : termLinkTemplates) {
                        if (!(task.isStructural() && (termLink.getType() == TermLink.TRANSFORM))) { // avoid circular transform
                            taskLink = new TaskLink(task, termLink, subBudget);
                            componentTerm = termLink.getTarget();
                            componentConcept = memory.getConcept(componentTerm);
                            if (componentConcept != null) {
                                componentConcept.insertTaskLink(taskLink);
                            }
                        }
                    }
                    buildTermLinks(taskBudget);  // recursively insert TermLink
                }
            }
        }
    }

    /**
     * Add a new belief (or goal) into the table
     * Sort the beliefs/goals by rank, and remove redundant or low rank one
     * @param newSentence The judgment to be processed
     * @param table The table to be revised
     * @param capacity The capacity of the table
     */
    private void addToTable(Sentence newSentence, ArrayList<Sentence> table, int capacity) {
        float rank1 = BudgetFunctions.rankBelief(newSentence);    // for the new isBelief
        Sentence judgment2;
        float rank2;
        int i;
        for (i = 0; i < table.size(); i++) {
            judgment2 = (Sentence) table.get(i);
            rank2 = BudgetFunctions.rankBelief(judgment2);
            if (rank1 >= rank2) {
                if (newSentence.equivalentTo(judgment2)) {
                    return;
                }
                table.add(i, newSentence);
                break;
            }
        }
        if (table.size() >= capacity) {
            while (table.size() > capacity) {
                table.remove(table.size() - 1);
            }
        } else if (i == table.size()) {
            table.add(newSentence);
        }
    }

    /**
     * Evaluate a query against beliefs (and desires in the future)
     * @param query The question to be processed
     * @param list The list of beliefs to be used
     * @return The best candidate belief selected
     */
    private Sentence evaluation(Sentence query, ArrayList<Sentence> list) {
        if (list == null) {
            return null;
        }
        float currentBest = 0;
        float beliefQuality;
        Sentence candidate = null;
        for (Sentence judg : list) {
            beliefQuality = LocalRules.solutionQuality(query, judg);
            if (beliefQuality > currentBest) {
                currentBest = beliefQuality;
                candidate = judg;
            }
        }
        return candidate;
    }

    /* ---------- insert Links for indirect processing ---------- */
    /**
     * Insert a TaskLink into the TaskLink bag
     * <p>
     * called only from Memory.continuedProcess
     * @param taskLink The termLink to be inserted
     */
    public void insertTaskLink(TaskLink taskLink) {
        BudgetValue taskBudget = taskLink.getBudget();
        taskLinks.putIn(taskLink);
        memory.activateConcept(this, taskBudget);
    }

    /**
     * Recursively build TermLinks between a compound and its components
     * <p>
     * called only from Memory.continuedProcess
     * @param taskBudget The BudgetValue of the task
     */
    public void buildTermLinks(BudgetValue taskBudget) {
        Term t;
        Concept concept;
        TermLink termLink1, termLink2;
        if (termLinkTemplates.size() > 0) {
            BudgetValue subBudget = BudgetFunctions.distributeAmongLinks(taskBudget, termLinkTemplates.size());
            if (subBudget.aboveThreshold()) {
                for (TermLink template : termLinkTemplates) {
                    if (template.getType() != TermLink.TRANSFORM) {
                        t = template.getTarget();
                        concept = memory.getConcept(t);
                        if (concept != null) {
                            termLink1 = new TermLink(t, template, subBudget);
                            insertTermLink(termLink1);   // this termLink to that
                            termLink2 = new TermLink(term, template, subBudget);
                            concept.insertTermLink(termLink2);   // that termLink to this
                            if (t instanceof CompoundTerm) {
                                concept.buildTermLinks(subBudget);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Insert a TermLink into the TermLink bag
     * <p>
     * called from buildTermLinks only
     * @param termLink The termLink to be inserted
     */
    public void insertTermLink(TermLink termLink) {
        termLinks.putIn(termLink);
    }

    /* ---------- access local information ---------- */
    /**
     * Return the assocated term, called from Memory only
     * @return The assocated term
     */
    public Term getTerm() {
        return term;
    }

    /**
     * Return a string representation of the concept, called in ConceptBag only
     * @return The concept name, with taskBudget in the full version
     */
    @Override
    public String toString() {  // called from concept bag
        if (NARS.isStandAlone()) {
            return (super.toStringBrief() + " " + key);
        } else {
            return key;
        }
    }

    /**
     * Recalculate the quality of the concept [to be refined to show extension/intension balance]
     * @return The quality value
     */
    @Override
    public float getQuality() {
        float linkPriority = termLinks.averagePriority();
        float termComplexityFactor = 1.0f / term.getComplexity();
        return UtilityFunctions.or(linkPriority, termComplexityFactor);
    }

    /**
     * Return the templates for TermLinks, only called in Memory.continuedProcess
     * @return The template get
     */
    public ArrayList<TermLink> getTermLinkTemplates() {
        return termLinkTemplates;
    }

    /**
     * Select a isBelief to interact with the given task in inference
     * <p>
     * get the first qualified one
     * <p>
     * only called in RuleTables.reason
     * @param task The selected task
     * @return The selected isBelief
     */
    public Sentence getBelief(Task task) {
        Sentence taskSentence = task.getSentence();
        Sentence belief;
        for (int i = 0; i < beliefs.size(); i++) {
            belief = beliefs.get(i);
            memory.getRecorder().append(" * Selected Belief: " + belief + "\n");
            memory.newStamp = Stamp.make(taskSentence.getStamp(), belief.getStamp(), memory.getTime());
            if (memory.newStamp != null) {
                Sentence belief2 = (Sentence) belief.clone();   // will this mess up priority adjustment?
                return belief2;
            }
        }
        return null;
    }

    /* ---------- main loop ---------- */
    /**
     * An atomic step in a concept, only called in Memory.processConcept
     */
    public void fire() {
        TaskLink tLink = taskLinks.takeOut();
        if (tLink == null) {
            return;
        }
        memory.currentTaskLink = tLink;
        memory.currentBeliefLink = null;
        memory.getRecorder().append(" * Selected TaskLink: " + tLink + "\n");
        Task task = tLink.getTargetTask();
        memory.currentTask = task;  // one of the two places where this variable is set
        if (tLink.getType() == TermLink.TRANSFORM) {
            RuleTables.transformTask(tLink, memory);  // to turn this into structural inference as below?
        }
        int termLinkCount = Parameters.MAX_REASONED_TERM_LINK;
        while (memory.noResult() && (termLinkCount > 0)) {
            TermLink termLink = termLinks.takeOut(tLink, memory.getTime());
            if (termLink != null) {
                memory.getRecorder().append(" * Selected TermLink: " + termLink + "\n");
                memory.currentBeliefLink = termLink;
                RuleTables.reason(tLink, termLink, memory);
                termLinks.putBack(termLink);
                termLinkCount--;
            } else {
                termLinkCount = 0;
            }
        }
        taskLinks.putBack(tLink);
    }

    /* ---------- display ---------- */
    /**
     * Start displaying contents and links, called from ConceptWindow or Memory.processTask only
     * @param showLinks Whether to display the task links
     */
    public void startPlay(boolean showLinks) {
        if (window != null && window.isVisible()) {
            window.detachFromConcept();
        }
        window = new ConceptWindow(this);
        showing = true;
        window.post(displayContent());
        if (showLinks) {
            taskLinks.startPlay("Task Links in " + term);
            termLinks.startPlay("Term Links in " + term);
        }
    }

    /**
     * Resume display, called from ConceptWindow only
     */
    public void play() {
        showing = true;
        window.post(displayContent());
    }

    /**
     * Stop display, called from ConceptWindow only
     */
    public void stop() {
        showing = false;
    }

    /**
     * Collect direct isBelief, questions, and goals for display
     * @return String representation of direct content
     */
    public String displayContent() {
        StringBuffer buffer = new StringBuffer();
        if (beliefs.size() > 0) {
            buffer.append("\n  Beliefs:\n");
            for (Sentence s : beliefs) {
                buffer.append(s + "\n");
            }
        }
        if (questions.size() > 0) {
            buffer.append("\n  Question:\n");
            for (Task t : questions) {
                buffer.append(t + "\n");
            }
        }
        return buffer.toString();
    }
}

