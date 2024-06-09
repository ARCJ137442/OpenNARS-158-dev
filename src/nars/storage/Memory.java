package nars.storage;

import java.util.concurrent.atomic.AtomicInteger;

import nars.entity.Concept;
import nars.inference.Budget;
import nars.inference.BudgetFunctions;
import nars.language.Term;
import nars.main.Parameters;

/**
 * The memory of the system.
 */
public class Memory {

    // struct Memory

    /* ---------- Long-term storage for multiple cycles ---------- */
    /**
     * Concept bag. Containing all Concepts of the system
     *
     * * 📝可空性：非空
     * * 📝可变性：可变 | 需要内部修改
     * * 📝所有权：具所有权
     */
    private final Bag<Concept> concepts;

    // 各超参数
    /**
     * 概念遗忘速率
     *
     * * 📝可空性：非空
     * * 📝可变性：可变 | 需要内部修改
     * * 📝所有权：共享引用 | 用于外部GUI修改
     */
    private final AtomicInteger conceptForgettingRate;
    /**
     * 信念遗忘速率
     *
     * * 📝可空性：非空
     * * 📝可变性：可变 | 需要内部修改
     * * 📝所有权：共享引用 | 用于外部GUI修改
     */
    private final AtomicInteger beliefForgettingRate;
    /**
     * 任务遗忘速率
     *
     * * 📝可空性：非空
     * * 📝可变性：可变 | 需要内部修改
     * * 📝所有权：共享引用 | 用于外部GUI修改
     */
    private final AtomicInteger taskForgettingRate;

    // impl Memory

    /**
     * 获取概念遗忘速率
     * * 🎯用于「GUI更新」与「概念构造」
     *
     * @return
     */
    public AtomicInteger getConceptForgettingRate() {
        return this.conceptForgettingRate;
    }

    /**
     * 获取任务遗忘速率
     * * 🎯用于「GUI更新」与「概念构造」
     *
     * @return
     */
    public AtomicInteger getTaskForgettingRate() {
        return taskForgettingRate;
    }

    /**
     * 获取信念遗忘速率
     * * 🎯用于「GUI更新」与「概念构造」
     *
     * @return
     */
    public AtomicInteger getBeliefForgettingRate() {
        return beliefForgettingRate;
    }

    /* ---------- Constructor ---------- */
    /**
     * Create a new memory
     * <p>
     * * 🚩仅在记忆区的构造函数中使用
     */
    public Memory() {
        // * 🚩各参数
        this.conceptForgettingRate = new AtomicInteger(Parameters.CONCEPT_FORGETTING_CYCLE);
        this.beliefForgettingRate = new AtomicInteger(Parameters.TERM_LINK_FORGETTING_CYCLE);
        this.taskForgettingRate = new AtomicInteger(Parameters.TASK_LINK_FORGETTING_CYCLE);
        // * 🚩概念袋
        this.concepts = new Bag<Concept>(this.conceptForgettingRate, Parameters.CONCEPT_BAG_SIZE);
    }

    /**
     * 初始化记忆区
     * * 🚩初始化「概念袋」
     */
    public void init() {
        concepts.init();
    }

    /* ---------- conversion utilities ---------- */
    /**
     * Get an existing Concept for a given name
     * <p>
     * called from Term and ConceptWindow.
     *
     * @param name the name of a concept
     * @return a Concept or null
     */
    public Concept nameToConcept(String name) {
        return concepts.get(name);
    }

    /**
     * Get a Term for a given name of a Concept or Operator
     * <p>
     * called in StringParser and the make methods of compound terms.
     *
     * @param name the name of a concept or operator
     * @return a Term or null (if no Concept/Operator has this name)
     */
    public Term nameToListedTerm(String name) {
        final Concept concept = concepts.get(name);
        return concept == null ? null : concept.getTerm();
    }

    /**
     * Get an existing Concept for a given Term.
     *
     * @param term The Term naming a concept
     * @return a Concept or null
     */
    public Concept termToConcept(Term term) {
        // * ✅【2024-05-24 22:09:35】现在不会在推理规则中被调用了
        return nameToConcept(term.getName());
    }

    /**
     * 🆕判断「记忆区中是否已有概念」
     * * 🚩Check if a Term has a Concept.
     *
     * @param term The Term naming a concept
     * @return true if the Term has a Concept in the memory
     */
    public boolean hasConcept(Term term) {
        return termToConcept(term) != null;
    }

    /**
     * Get the Concept associated to a Term, or create it.
     *
     * @param term indicating the concept
     * @return an existing Concept, or a new one, or null ( bad smell ? )
     */
    public Concept getConceptOrCreate(Term term) {
        // * 🚩不给「非常量词项」新建概念 | 「非常量词项」也不可能作为一个「概念」被放进「记忆区」中
        if (!term.isConstant())
            return null;
        // * 🚩尝试从概念袋中获取「已有概念」，否则尝试创建概念
        final Concept concept = termToConcept(term);
        return concept == null ? makeNewConcept(term) : concept;
    }

    /**
     * 🆕新建一个概念
     * * 📌概念只可能由此被创建
     *
     * @param term 概念对应的词项
     * @return 已经被置入「概念袋」的概念 | 创建失败时返回`null`
     */
    private Concept makeNewConcept(Term term) {
        final Concept concept = new Concept(term, this); // the only place to make a new Concept
        final boolean created = concepts.putIn(concept);
        return created ? concept : null;
    }

    /* ---------- adjustment functions ---------- */
    /**
     * Adjust the activation level of a Concept
     * <p>
     * called in Concept.insertTaskLink only
     * * 🚩实际上也被「直接推理」调用
     *
     * @param c the concept to be adjusted
     * @param b the new BudgetValue
     */
    public void activateConcept(final Concept c, final Budget b) {
        // * 🚩存在性检查
        final boolean hasC = concepts.contains(c);
        // * 🚩若已有⇒拿出→放回 | 会改变「概念」的优先级，因此可能会调整位置
        if (hasC) {
            concepts.pickOut(c.getKey());
            BudgetFunctions.activate(c, b);
            concepts.putBack(c);
        }
        // * 🚩若没有⇒放回→拿出
        else {
            BudgetFunctions.activate(c, b);
            concepts.forget(c); // * 📝此方法将改变「概念」的预算值，需要保证顺序一致
        }
    }

    /**
     * 🆕对外接口：从「概念袋」中拿出一个概念
     *
     * @return 拿出的一个概念 / 空
     */
    public final Concept takeOutConcept() {
        return this.concepts.takeOut();
    }

    /**
     * 🆕对外接口：从「概念袋」中挑出一个概念
     * * 🚩用于「直接推理」中的「拿出概念」
     *
     * @return 拿出的一个概念 / 空
     */
    public final Concept pickOutConcept(String key) {
        return concepts.pickOut(key);
    }

    /**
     * 🆕对外接口：往「概念袋」放回一个概念
     *
     * @return 拿出的一个概念 / 空
     */
    public final void putBackConcept(Concept concept) {
        this.concepts.putBack(concept);
    }

    /**
     * 🆕对外接口：获取「概念袋」
     * * 🎯显示用
     */
    public final Bag<Concept> getConceptBagForDisplay() {
        return this.concepts;
    }
}
