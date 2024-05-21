package nars.entity;

import java.util.ArrayList;

import nars.inference.BudgetFunctions;
import nars.inference.UtilityFunctions;
import nars.language.CompoundTerm;
import nars.language.Conjunction;
import nars.language.Equivalence;
import nars.language.ImageExt;
import nars.language.ImageInt;
import nars.language.Implication;
import nars.language.Negation;
import nars.language.Product;
import nars.language.Statement;
import nars.language.Term;
import nars.main_nogui.NARSBatch;
import nars.main_nogui.Parameters;
import nars.storage.BagObserver;
import nars.storage.Memory;
import nars.storage.NullBagObserver;
import nars.storage.TaskLinkBag;
import nars.storage.TermLinkBag;

/**
 * A concept contains information associated with a term, including directly and
 * indirectly related tasks and beliefs.
 * <p>
 * To make sure the space will be released, the only allowed reference to a
 * concept are those in a ConceptBag. All other access go through the Term that
 * names the concept.
 */
public final class Concept extends Item {

    /**
     * The term is the unique ID of the concept
     */
    private final Term term;
    /**
     * Task links for indirect processing
     */
    private final TaskLinkBag taskLinks;
    /**
     * Term links between the term and its components and compounds
     */
    private final TermLinkBag termLinks;
    /**
     * Link templates of TermLink, only in concepts with CompoundTerm
     * TODO(jmv) explain more
     */
    private final ArrayList<TermLink> termLinkTemplates;
    /**
     * Question directly asked about the term
     */
    private final ArrayList<Task> questions;
    /**
     * Sentences directly made about the term, with non-future tense
     */
    private final ArrayList<Sentence> beliefs;
    /**
     * Reference to the memory
     */
    final Memory memory;
    /**
     * The display window
     */
    private EntityObserver entityObserver = new NullEntityObserver();

    /* ---------- constructor and initialization ---------- */
    /**
     * Constructor, called in Memory.getConcept only
     *
     * @param tm     A term corresponding to the concept
     * @param memory A reference to the memory
     */
    public Concept(Term tm, Memory memory) {
        super(tm.getName());
        term = tm;
        this.memory = memory;
        this.questions = new ArrayList<>();
        this.beliefs = new ArrayList<>();
        this.taskLinks = new TaskLinkBag(memory);
        this.termLinks = new TermLinkBag(memory);
        if (tm instanceof CompoundTerm) {
            // * 🚩只有「复合词项→其内元素」的链接
            // * 📝「复合词项→其内元素」是有限的，而「元素→复合词项」是无限的
            this.termLinkTemplates = prepareComponentLinks(((CompoundTerm) tm));
        } else {
            this.termLinkTemplates = null;
        }
    }

    /* ----- link CompoundTerm and its components ----- */
    /**
     * Build TermLink templates to constant components and sub-components
     * <p>
     * The compound type determines the link type; the component type determines
     * whether to build the link.
     *
     * @return A list of TermLink templates
     */
    private static ArrayList<TermLink> prepareComponentLinks(CompoundTerm self) {
        final ArrayList<TermLink> componentLinks = new ArrayList<>();
        // * 🚩预备「默认类型」：自身为陈述⇒陈述，自身为复合⇒复合
        final short type = (self instanceof Statement) ? TermLink.COMPOUND_STATEMENT : TermLink.COMPOUND; // default
        // * 🚩建立连接：从自身到自身开始
        prepareComponentLinks(self, componentLinks, type, self);
        return componentLinks;
    }

    /**
     * Collect TermLink templates into a list, go down one level except in
     * special cases
     * <p>
     *
     * @param self           The CompoundTerm for which the links are built
     * @param componentLinks The list of TermLink templates built so far
     * @param type           The type of TermLink to be built
     * @param term           The CompoundTerm for which the links are built
     */
    private static void prepareComponentLinks(
            final CompoundTerm self,
            final ArrayList<TermLink> componentLinks,
            final short type,
            final CompoundTerm term) {
        // * 🚩从目标第一层元素出发
        for (int i = 0; i < term.size(); i++) { // first level components
            /** 第一层元素 */
            final Term inner1 = term.componentAt(i);
            // * 🚩「常量」词项⇒直接链接
            if (inner1.isConstant()) {
                componentLinks.add(TermLink.from(inner1, type, i));
                // * 📝【2024-05-15 18:21:25】案例笔记 概念="<(&&,A,B) ==> D>"：
                // * 📄self="<(&&,A,B) ==> D>" ~> "(&&,A,B)" [i=0]
                // * @ 4=COMPOUND_STATEMENT "At C, point to <C --> A>"
                // * 📄self="(&&,A,B)" ~> "A" [i=0]
                // * @ 6=COMPOUND_CONDITION "At C, point to <(&&, C, B) ==> A>"
                // * 📄self="(&&,A,B)" ~> "B" [i=1]
                // * @ 6=COMPOUND_CONDITION "At C, point to <(&&, C, B) ==> A>"
                // * 📄self="<(&&,A,B) ==> D>" ~> "D" [i=1]
                // * @ 4=COMPOUND_STATEMENT "At C, point to <C --> A>"
                // * 📄self="(&&,A,B)" ~> "A" [i=0]
                // * @ 2=COMPOUND "At C, point to (&&, A, C)"
                // * 📄self="(&&,A,B)" ~> "B" [i=1]
                // * @ 2=COMPOUND "At C, point to (&&, A, C)"
            }
            // * 🚩条件类链接⇒递归
            final boolean isConditionalCompound =
                    // * 📌自身和索引必须先是「蕴含の主词」或「等价」，如 <# ==> C> 或 <# <=> #>
                    (self instanceof Equivalence || (self instanceof Implication && i == 0));
            final boolean isConditionalComponent =
                    // * 🚩然后「内部词项」必须是「合取」或「否定」
                    (inner1 instanceof Conjunction || inner1 instanceof Negation);
            if (isConditionalCompound && isConditionalComponent)
                // * 📝递归深入，将作为「入口」的「自身向自身建立链接」缩小到「组分」区域
                prepareComponentLinks(
                        (CompoundTerm) inner1,
                        componentLinks,
                        TermLink.COMPOUND_CONDITION, // * 🚩改变「默认类型」为「复合条件」
                        (CompoundTerm) inner1);
            // * 🚩其它情况⇒若元素为复合词项，再度深入
            else if (inner1 instanceof CompoundTerm) {
                for (int j = 0; j < ((CompoundTerm) inner1).size(); j++) { // second level components
                    /** 第二层元素 */
                    final Term inner2 = ((CompoundTerm) inner1).componentAt(j);
                    // * 🚩NAL-4「转换」相关：第二层
                    if (inner2.isConstant()) {
                        final int[] indexes = type == TermLink.COMPOUND_CONDITION
                                // * 📝若背景的「链接类型」已经是「复合条件」⇒已经深入了一层，并且一定在「主项」位置
                                ? new int[] { 0, i, j }
                                // * 📝否则就还是第二层
                                : new int[] { i, j };
                        final short linkType =
                                // * 🚩内部是「乘积」「外延像」「内涵像」
                                inner1 instanceof Product
                                        || inner1 instanceof ImageExt
                                        || inner1 instanceof ImageInt
                                                // * 🚩⇒安排「转换」关系
                                                ? TermLink.TRANSFORM
                                                // * 🚩否则⇒按原有类型执行
                                                : type;
                        componentLinks.add(new TermLink(inner2, linkType, indexes));
                    }
                    // * 🚩NAL-4「转换」相关：第三层
                    if (inner2 instanceof Product
                            || inner2 instanceof ImageExt
                            || inner2 instanceof ImageInt) {
                        for (int k = 0; k < ((CompoundTerm) inner2).size(); k++) {
                            final Term inner3 = ((CompoundTerm) inner2).componentAt(k);
                            if (inner3.isConstant()) { // third level
                                final int[] indexes = type == TermLink.COMPOUND_CONDITION
                                        // * 📝此处若是「复合条件」即为最深第四层
                                        ? new int[] { 0, i, j, k }
                                        // * 📝否则仅第三层
                                        : new int[] { i, j, k };
                                componentLinks.add(new TermLink(inner3, TermLink.TRANSFORM, indexes));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 🆕对外接口：刷新「实体观察者」
     * * 🎯从「直接推理」而来
     */
    public void refreshObserver() {
        this.entityObserver.refresh(this.displayContent());
    }

    /**
     * 🆕对外接口：获取「当前信念表」
     * * 🎯从「直接推理」而来
     */
    public ArrayList<Sentence> getBeliefs() {
        return this.beliefs;
    }

    /**
     * 🆕对外接口：获取「当前信念表」
     * * 🎯从「直接推理」而来
     */
    public Iterable<Task> getQuestions() {
        return this.questions;
    }

    /**
     * 🆕对外接口：添加问题到「问题集」
     * * 🚩除了「添加」以外，还会实行「任务缓冲区」机制
     */
    public void addQuestion(final Task task) {
        // * 🚩不会添加重复的问题
        this.questions.add(task);
        // * 🚩问题缓冲区机制 | 📝断言：只有在「问题变动」时处理
        if (this.questions.size() > Parameters.MAXIMUM_QUESTIONS_LENGTH) {
            this.questions.remove(0); // FIFO
        }
    }

    /**
     * Link to a new task from all relevant concepts for continued processing in
     * the near future for unspecified time.
     * <p>
     * The only method that calls the TaskLink constructor.
     *
     * @param task    The task to be linked
     * @param content The content of the task
     */
    public void linkToTask(Task task) {
        final BudgetValue taskBudget = task.getBudget();
        final TaskLink taskLink = new TaskLink(task, null, taskBudget); // link type: SELF
        insertTaskLink(taskLink);
        if (!(term instanceof CompoundTerm && termLinkTemplates.size() > 0)) {
            return;
        }
        final BudgetValue subBudget = BudgetFunctions.distributeAmongLinks(taskBudget, termLinkTemplates.size());
        if (subBudget.aboveThreshold()) {
            for (TermLink termLink : termLinkTemplates) {
                // if (!(task.isStructural() && (termLink.getType() == TermLink.TRANSFORM))) {
                // // avoid circular transform
                final TaskLink tLink = new TaskLink(task, termLink, subBudget);
                final Term componentTerm = termLink.getTarget();
                final Concept componentConcept = memory.getConceptOrCreate(componentTerm);
                if (componentConcept != null) {
                    componentConcept.insertTaskLink(tLink);
                }
                // }
            }
            buildTermLinks(taskBudget); // recursively insert TermLink
        }
    }

    /**
     * Add a new belief (or goal) into the table Sort the beliefs/goals by rank,
     * and remove redundant or low rank one
     *
     * @param newSentence The judgment to be processed
     * @param table       The table to be revised
     * @param capacity    The capacity of the table
     */
    public static void addBeliefToTable(Sentence newSentence, ArrayList<Sentence> table, int capacity) {
        final float rank1 = BudgetFunctions.rankBelief(newSentence); // for the new isBelief
        int i;
        for (i = 0; i < table.size(); i++) {
            final Sentence judgment2 = table.get(i);
            final float rank2 = BudgetFunctions.rankBelief(judgment2);
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

    /* ---------- insert Links for indirect processing ---------- */
    /**
     * Insert a TaskLink into the TaskLink bag
     * <p>
     * called only from Memory.continuedProcess
     *
     * @param taskLink The termLink to be inserted
     */
    private void insertTaskLink(TaskLink taskLink) {
        final BudgetValue taskBudget = taskLink.getBudget();
        taskLinks.putIn(taskLink);
        memory.activateConcept(this, taskBudget);
    }

    /**
     * Recursively build TermLinks between a compound and its components
     * <p>
     * called only from Memory.continuedProcess
     *
     * @param taskBudget The BudgetValue of the task
     */
    private void buildTermLinks(BudgetValue taskBudget) {
        Term t;
        Concept concept;
        TermLink termLink1, termLink2;
        if (termLinkTemplates.size() > 0) {
            BudgetValue subBudget = BudgetFunctions.distributeAmongLinks(taskBudget, termLinkTemplates.size());
            if (subBudget.aboveThreshold()) {
                for (TermLink template : termLinkTemplates) {
                    if (template.getType() != TermLink.TRANSFORM) {
                        t = template.getTarget();
                        concept = memory.getConceptOrCreate(t);
                        if (concept != null) {
                            termLink1 = new TermLink(t, template, subBudget);
                            insertTermLink(termLink1); // this termLink to that
                            termLink2 = new TermLink(term, template, subBudget);
                            concept.insertTermLink(termLink2); // that termLink to this
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
     *
     * @param termLink The termLink to be inserted
     */
    private void insertTermLink(TermLink termLink) {
        termLinks.putIn(termLink);
    }

    /* ---------- access local information ---------- */
    /**
     * Return the associated term, called from Memory only
     *
     * @return The associated term
     */
    public Term getTerm() {
        return term;
    }

    /**
     * Return a string representation of the concept, called in ConceptBag only
     *
     * @return The concept name, with taskBudget in the full version
     */
    @Override
    public String toString() { // called from concept bag
        if (NARSBatch.isStandAlone()) {
            return (super.toStringBrief() + " " + key);
        } else {
            return key;
        }
    }

    /**
     * called from {@link NARSBatch}
     *
     * @return A string representation of the concept
     */
    @Override
    public String toStringLong() {
        String res = toStringBrief() + " " + key
                + toStringIfNotNull(termLinks, "termLinks")
                + toStringIfNotNull(taskLinks, "taskLinks");
        res += toStringIfNotNull(null, "questions");
        for (Task t : questions) {
            res += t.toString();
        }
        // TODO other details?
        return res;
    }

    public String toStringIfNotNull(Object item, String title) {
        return item == null ? "" : "\n " + title + ":" + item.toString();
    }

    /**
     * Recalculate the quality of the concept [to be refined to show
     * extension/intension balance]
     *
     * @return The quality value
     */
    @Override
    public float getQuality() {
        final float linkPriority = termLinks.averagePriority();
        final float termComplexityFactor = 1.0f / term.getComplexity();
        return UtilityFunctions.or(linkPriority, termComplexityFactor);
    }

    /**
     * Return the templates for TermLinks, only called in
     * Memory.continuedProcess
     *
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
     * * 📝⚠️实际上并不`only called in RuleTables.reason`
     * * 📄在「组合规则」的「回答带变量合取」时用到
     * * 🚩改：去除其中「设置当前时间戳」的副作用，将其迁移到调用者处
     *
     * @param taskSentence The selected task
     * @return The selected isBelief
     */
    public Sentence getBelief(Sentence taskSentence) {
        for (final Sentence belief : beliefs) {
            memory.getRecorder().append(" * Selected Belief: " + belief + "\n");
            // * 📝在OpenNARS 3.0.4中也会被覆盖：
            // * 📄`nal.setTheNewStamp(taskStamp, belief.stamp, currentTime);`
            // context.newStamp = Stamp.make(taskSentence.getStamp(),
            // belief.getStamp(), memory.getTime());
            if (!Stamp.haveOverlap(taskSentence.getStamp(), belief.getStamp())) {
                final Sentence belief2 = belief.clone(); // will this mess up priority adjustment?
                return belief2;
            }
        }
        return null;
    }

    /**
     * 🆕从「任务链袋」获取一个任务链
     * * 🚩仅用于从「记忆区」调用的{@link Memory#fireConcept}
     */
    public TaskLink __takeOutTaskLink() {
        return this.taskLinks.takeOut();
    }

    /**
     * 🆕从「词项链袋」获取一个词项链
     * * 🚩仅用于从「记忆区」调用的{@link Memory#fireConcept}
     */
    public TermLink __takeOutTermLink(TaskLink currentTaskLink, long time) {
        return this.termLinks.takeOut(currentTaskLink, time);
    }

    /**
     * 🆕将一个任务链放回「任务链袋」
     * * 🚩仅用于从「记忆区」调用的{@link Memory#fireConcept}
     */
    public boolean __putTaskLinkBack(TaskLink link) {
        return this.taskLinks.putBack(link);
    }

    /**
     * 🆕将一个词项链放回「词项链袋」
     * * 🚩仅用于从「记忆区」调用的{@link Memory#fireConcept}
     */
    public boolean __putTermLinkBack(TermLink link) {
        return this.termLinks.putBack(link);
    }

    /* ---------- display ---------- */
    /**
     * Start displaying contents and links, called from ConceptWindow,
     * TermWindow or Memory.processTask only
     *
     * same design as for {@link nars.storage.Bag} and
     * {@link nars.gui.BagWindow}; see
     * {@link nars.storage.Bag#addBagObserver(BagObserver, String)}
     *
     * @param entityObserver {@link EntityObserver} to set; TODO make it a real
     *                       observer pattern (i.e. with a plurality of observers)
     * @param showLinks      Whether to display the task links
     */
    @SuppressWarnings("unchecked")
    public void startPlay(EntityObserver entityObserver, boolean showLinks) {
        this.entityObserver = entityObserver;
        entityObserver.startPlay(this, showLinks);
        entityObserver.post(displayContent());
        if (showLinks) {
            taskLinks.addBagObserver(entityObserver.createBagObserver(), "Task Links in " + term);
            termLinks.addBagObserver(entityObserver.createBagObserver(), "Term Links in " + term);
        }
    }

    /**
     * Resume display, called from ConceptWindow only
     */
    public void play() {
        entityObserver.post(displayContent());
    }

    /**
     * Stop display, called from ConceptWindow only
     */
    public void stop() {
        entityObserver.stop();
    }

    /**
     * Collect direct isBelief, questions, and goals for display
     *
     * @return String representation of direct content
     */
    public String displayContent() {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("\n  Beliefs:\n");
        if (beliefs.size() > 0) {
            for (final Sentence s : beliefs) {
                buffer.append(s).append("\n");
            }
        }
        buffer.append("\n  Question:\n");
        if (questions.size() > 0) {
            for (final Task t : questions) {
                buffer.append(t).append("\n");
            }
        }
        return buffer.toString();
    }

    class NullEntityObserver implements EntityObserver {

        @Override
        public void post(String str) {
        }

        @Override
        public BagObserver<TermLink> createBagObserver() {
            return new NullBagObserver<>();
        }

        @Override
        public void startPlay(Concept concept, boolean showLinks) {
        }

        @Override
        public void stop() {
        }

        @Override
        public void refresh(String message) {
        }
    }
}
