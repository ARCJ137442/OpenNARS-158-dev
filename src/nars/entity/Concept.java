package nars.entity;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import nars.control.ConceptLinking;
import nars.io.ToStringBriefAndLong;
import nars.language.CompoundTerm;
import nars.language.Term;
import nars.main.NARS;
import nars.main.Parameters;
import nars.storage.ArrayBuffer;
import nars.storage.Bag;
import nars.storage.BagObserver;
import nars.storage.BeliefTable;
import nars.storage.Memory;
import nars.storage.NullBagObserver;

/**
 * A concept contains information associated with a term, including directly and
 * indirectly related tasks and beliefs.
 * <p>
 * To make sure the space will be released, the only allowed reference to a
 * concept are those in a ConceptBag. All other access go through the Term that
 * names the concept.
 */
public final class Concept implements Item, ToStringBriefAndLong {

    // struct Concept

    /**
     * 🆕Item令牌
     */
    private final Token token;

    /**
     * The term is the unique ID of the concept
     */
    private final Term term;
    /**
     * Task links for indirect processing
     */
    private final Bag<TaskLink> taskLinks;
    /**
     * Term links between the term and its components and compounds
     */
    private final Bag<TermLink> termLinks;
    /**
     * Link templates of TermLink, only in concepts with CompoundTerm
     * * 🎯用于「复合词项构建词项链」如「链接到任务」
     * * 📌【2024-06-04 20:14:09】目前确定为「所有『内部元素』链接到自身的可能情况」的模板集
     * * 📝只会创建「从内部元素链接到自身」（target=）
     * * 📝在{@link ConceptLinking#prepareTermLinkTemplates}中被准备，随后不再变化
     */
    private final ArrayList<TermLinkTemplate> linkTemplatesToSelf;
    /**
     * Question directly asked about the term
     */
    private final ArrayBuffer<Task> questions;
    /**
     * Sentences directly made about the term, with non-future tense
     */
    private final BeliefTable beliefs;
    // ! 🚩【2024-06-08 17:37:04】现在不再持有反向引用
    /**
     * The display window
     */
    private EntityObserver entityObserver = new NullEntityObserver();

    // impl Budget for Concept

    @Override
    public ShortFloat __priority() {
        return this.token.__priority();
    }

    @Override
    public ShortFloat __durability() {
        return this.token.__durability();
    }

    @Override
    public ShortFloat __quality() {
        return this.token.__quality();
    }

    // impl Item for Concept

    @Override
    public String getKey() {
        return token.getKey();
    }

    // impl ToStringBriefAndLong for Concept

    /**
     * 🆕是否在{@link Concept#toString}处显示更细致的内容
     * * 🎯与主类解耦
     */
    public static boolean detailedString = false;

    /**
     * Return a string representation of the concept, called in ConceptBag only
     *
     * @return The concept name, with taskBudget in the full version
     */
    @Override
    public String toString() { // called from concept bag
        if (detailedString) {
            final String superString = this.token.getBudgetValue().toString() + " " + getKey().toString();
            return (superString + " " + getKey());
        } else {
            return getKey();
        }
    }

    /**
     * called from {@link NARS}
     *
     * @return A string representation of the concept
     */
    @Override
    public String toStringLong() {
        String res = toStringBrief() + " " + getKey()
                + toStringIfNotNull(termLinks, "termLinks")
                + toStringIfNotNull(taskLinks, "taskLinks");
        res += toStringIfNotNull(null, "questions");
        for (Task t : questions) {
            res += t.toString();
        }
        // TODO other details?
        return res;
    }

    /**
     * 🆕原版没有，此处仅重定向
     */
    @Override
    public String toStringBrief() {
        return toString();
    }

    public String toStringIfNotNull(Object item, String title) {
        return item == null ? "" : "\n " + title + ":" + item.toString();
    }

    // impl Concept

    /* ---------- constructor and initialization ---------- */
    /**
     * Constructor, called in Memory.getConcept only
     *
     * @param term   A term corresponding to the concept
     * @param memory A reference to the memory
     */
    public Concept(Term term, Memory memory) {
        this(term, memory.getTaskForgettingRate(),
                memory.getBeliefForgettingRate());
    }

    public Concept(Term term, AtomicInteger taskLinkForgettingRate, AtomicInteger termLinkForgettingRate) {
        this.token = new Token(term.getName());
        this.term = term;
        this.questions = new ArrayBuffer<Task>(Parameters.MAXIMUM_QUESTIONS_LENGTH);
        this.beliefs = new BeliefTable();
        this.taskLinks = new Bag<TaskLink>(taskLinkForgettingRate, Parameters.TASK_LINK_BAG_SIZE);
        this.termLinks = new Bag<TermLink>(termLinkForgettingRate, Parameters.TERM_LINK_BAG_SIZE);
        if (term instanceof CompoundTerm) {
            // * 🚩只有「复合词项←其内元素」的链接模板
            // * 📝所有信息基于「内容包含」关系
            this.linkTemplatesToSelf = ConceptLinking.prepareTermLinkTemplates(((CompoundTerm) term));
        } else {
            this.linkTemplatesToSelf = null;
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
    public BeliefTable getBeliefs() {
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
    }

    /**
     * API方法 @ 链接建立
     *
     * Return the templates for TermLinks, only called in
     * Memory.continuedProcess
     *
     * @return The template get
     */
    public ArrayList<TermLinkTemplate> getLinkTemplatesToSelf() {
        return this.linkTemplatesToSelf;
    }

    /**
     * 🆕API方法 @ 链接建立
     */
    public void putInTermLink(TermLink termLink) {
        this.termLinks.putIn(termLink);
    }

    /**
     * 🆕API方法 @ 链接建立
     */
    public void putInTaskLink(TaskLink taskLink) {
        this.taskLinks.putIn(taskLink);
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
     * Recalculate the quality of the concept [to be refined to show
     * extension/intension balance]
     *
     * @return The quality value
     */
    public float termLinksAveragePriority() {
        return this.termLinks.averagePriority();
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
    public Judgement getBelief(Sentence taskSentence) {
        // * 🚩此处按「信念排名」从大到小遍历；第一个满足「证据基不重复」的信念将被抽取
        for (final Judgement belief : beliefs) {
            // * 📝在OpenNARS 3.0.4中会被覆盖：
            // * 📄`nal.setTheNewStamp(taskStamp, belief.stamp, currentTime);`
            // * ✅【2024-06-08 10:13:46】现在彻底删除newStamp字段，不再需要覆盖了
            if (!taskSentence.evidentialOverlap(belief)) {
                // * 🚩现在彻底删除内部memory字段
                // memory.getRecorder().append(" * Selected Belief: " + belief + "\n");
                final Judgement selected = (Judgement) belief.sentenceClone(); // will this mess up priority adjustment?
                return selected;
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
        return this.takeOutTermLinkFromTaskLink(currentTaskLink, time);
    }

    /**
     * Replace default to prevent repeated inference, by checking TaskLink
     * * 📌特殊的「根据任务链拿出词项链（信念链）」
     * * 🎯在「概念推理」的「准备待推理词项链」的过程中用到
     * * 🔗ProcessReason.chooseTermLinksToReason
     *
     * @param taskLink The selected TaskLink
     * @param time     The current time
     * @return The selected TermLink
     */
    private TermLink takeOutTermLinkFromTaskLink(TaskLink taskLink, long time) {
        for (int i = 0; i < Parameters.MAX_MATCHED_TERM_LINK; i++) {
            // * 🚩尝试拿出词项链 | 📝此间存在资源竞争
            final TermLink termLink = this.termLinks.takeOut();
            if (termLink == null)
                return null;
            // * 🚩任务链相对词项链「新近」⇒直接返回
            if (taskLink.novel(termLink, time))
                return termLink;
            // * 🚩当即放回
            this.termLinks.putBack(termLink);
        }
        return null;
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
