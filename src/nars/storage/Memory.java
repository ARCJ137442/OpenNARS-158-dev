package nars.storage;

import java.util.concurrent.atomic.AtomicInteger;

import nars.control.ConceptLinking;
import nars.control.Parameters;
import nars.entity.BudgetValue;
import nars.entity.Concept;
import nars.inference.Budget;
import nars.inference.BudgetFunctions;
import nars.language.Term;

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
     *
     * @return []
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
     *
     * @param &m-this
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
     * @param &this
     * @param name  [&] the name of a concept
     * @return [&] a Concept or null
     */
    public Concept nameToConcept(String name) {
        return concepts.get(name);
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
        // the only place to make a new Concept
        final Concept concept = new Concept(
                term,
                this.getTaskForgettingRate(),
                this.getBeliefForgettingRate(),
                initialConceptBudget(),
                ConceptLinking.prepareTermLinkTemplates(term));
        final boolean created = concepts.putIn(concept);
        return created ? concept : null;
    }

    /**
     * 🆕计算新「概念」的「初始预算值」
     * * 📝OpenNARS原版仅此一处有「无预算值初始化」
     * * 🚩【2024-06-24 19:32:29】故将其提取为「超参数」处理
     */
    private static final BudgetValue initialConceptBudget() {
        return new BudgetValue(
                Parameters.CONCEPT_INITIAL_PRIORITY,
                Parameters.CONCEPT_INITIAL_DURABILITY,
                Parameters.CONCEPT_INITIAL_QUALITY);
    }

    /* ---------- adjustment functions ---------- */
    /**
     * Adjust the activation level of a Concept
     * <p>
     * called in Concept.insertTaskLink only
     * * 🚩实际上也被「直接推理」调用
     * * 🔬出于「借用明确」目的，此处需要拆分看待
     *
     * @param concept      the concept to be adjusted
     * @param incomeBudget the new BudgetValue
     */
    public void activateConcept(final Concept concept, final Budget incomeBudget) {
        // * 🚩存在性检查
        final boolean hasConcept = this.concepts.contains(concept);
        // * 🚩若已有⇒拿出→放回 | 会改变「概念」的优先级，因此可能会调整位置
        if (hasConcept)
            activateConceptInner(concept, incomeBudget);
        // * 🚩若没有⇒放回→拿出
        else
            activateConceptOuter(concept, incomeBudget);
    }

    public void activateConceptInner(final Concept concept, final Budget incomeBudget) {
        // * 🚩存在性检查
        final boolean hasConcept = this.concepts.contains(concept);
        // * 🚩若已有⇒拿出→放回 | 会改变「概念」的优先级，因此可能会调整位置
        if (hasConcept) {
            this.concepts.pickOut(concept.getKey());
            activateConceptBudget(concept, incomeBudget);
            this.concepts.putBack(concept);
        } else
            throw new AssertionError("激活「内部的概念」需要已有概念！");
    }

    public void activateConceptOuter(final Concept concept, final Budget incomeBudget) {
        // * 🚩存在性检查
        final boolean hasConcept = this.concepts.contains(concept);
        // * 🚩若已有⇒拿出→放回 | 会改变「概念」的优先级，因此可能会调整位置
        if (hasConcept)
            throw new AssertionError("激活「外部的概念」需要概念不在！");
        // * 🚩若没有⇒放回→拿出
        else {
            activateConceptBudget(concept, incomeBudget);
            this.concepts.forget(concept); // * 📝此方法将改变「概念」的预算值，需要保证顺序一致
        }
    }

    /**
     * 🆕单独更新预算值
     *
     * @param c [&m]
     * @param b [&]
     */
    public static void activateConceptBudget(final Concept c, final Budget b) {
        final Budget newBudget = BudgetFunctions.activate(c, b);
        c.copyBudgetFrom(newBudget);
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
