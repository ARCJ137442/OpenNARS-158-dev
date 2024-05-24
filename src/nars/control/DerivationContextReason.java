package nars.control;

import java.util.LinkedList;

import nars.entity.Concept;
import nars.entity.Sentence;
import nars.entity.Stamp;
import nars.entity.Task;
import nars.entity.TaskLink;
import nars.entity.TermLink;
import nars.inference.RuleTables;
import nars.language.Term;
import nars.storage.Memory;

/**
 * 🆕新的「概念推理上下文」对象
 * * 📄从「推理上下文」中派生，用于「概念-任务链-信念链」的「概念推理」
 * * 📌类名源自入口函数{@link RuleTables#reason}
 */
public class DerivationContextReason extends DerivationContextTransform {

    /**
     * 用于构建「直接推理上下文」对象
     */
    public static final void verify(DerivationContextReason self) {
        // * 🚩系列断言与赋值（实际使用中可删）
        /*
         * 📝有效字段：{
         * currentTerm
         * currentConcept
         * currentTask
         * currentTaskLink
         * currentBelief?
         * currentBeliefLink
         * newStamp?
         * }
         */
        if (self.getCurrentTask() == null)
            throw new Error("currentTask: 不符预期的可空情况");
        if (self.getCurrentTerm() == null)
            throw new Error("currentTerm: 不符预期的可空情况");
        if (self.getCurrentConcept() == null)
            throw new Error("currentConcept: 不符预期的可空情况");
        if (self.getCurrentBelief() == null && self.getCurrentBelief() != null) // * 📝可空
            throw new Error("currentBelief: 不符预期的可空情况");
        if (self.getCurrentBeliefLink() == null)
            throw new Error("currentBeliefLink: 不符预期的可空情况");
        if (self.getCurrentTaskLink() == null)
            throw new Error("currentTaskLink: 不符预期的可空情况");
        if (self.getNewStamp() != null && self.getNewStamp() == null)
            // * 📝溯源其在这之前被赋值的场所：getBelief⇒processConcept
            throw new Error("newStamp: 不符预期的可空情况");
        if (self.getSubstitute() != null)
            throw new Error("substitute: 不符预期的可空情况");
        if (self.getTermLinksToReason().isEmpty() && !self.getTermLinksToReason().isEmpty()) // * 📝可空：有可能只有一个词项链
            throw new Error("termLinksToReason: 不符预期的可空情况");
    }

    /**
     * 🆕带参初始化
     * * 🚩包含所有`final`变量，避免「创建后赋值」如「复制时」
     *
     * @param memory
     */
    public DerivationContextReason(
            final Memory memory,
            final Concept currentConcept,
            final Task currentTask,
            final TaskLink currentTaskLink,
            final TermLink currentBeliefLink,
            final LinkedList<TermLink> toReasonLinks) {
        // * 🚩从基类构造，并预先检验
        super(memory, currentConcept, currentTaskLink);
        // * 🚩赋值
        this.setCurrentBeliefLink(currentBeliefLink);
        this.termLinksToReason = toReasonLinks;
        // * 🚩从「当前信念链」出发，尝试获取并更新「当前信念」「新时间戳」
        updateCurrentBeliefAndNewStamp();
        // * 🚩检验
        verify(this);
    }

    /**
     * 切换到新的信念（与信念链）
     * * 📌【2024-05-21 10:26:59】现在是「概念推理上下文」独有
     * * 🚩【2024-05-21 22:51:09】只在自身内部搬迁所有权：从「待推理词项链表」中取出一个「词项链」替代原有词项链
     * * 🚩能取出⇒返回旧词项链，已空⇒返回`null`
     * * ✅【2024-05-21 23:13:10】内存安全：整个过程中`currentBeliefLink`不可能为空
     * * ✅每行代码后加`verify`都不会有事
     */
    public TermLink nextBelief() {
        // * 🚩先尝试拿出下一个词项链，若拿不出则返回空值
        final TermLink oldTermLink = this.getCurrentBeliefLink();
        final TermLink currentBeliefLink = this.termLinksToReason.poll();

        // * 🚩若没有更多词项链了⇒返回空表示「已结束」
        if (currentBeliefLink == null)
            return null;

        // * 🚩更新「当前信念链」 | 此举保证「信念链」永不为空
        this.setCurrentBeliefLink(currentBeliefLink);

        // * 🚩从「当前信念链」出发，尝试获取并更新「当前信念」「新时间戳」
        updateCurrentBeliefAndNewStamp();

        // * ♻️回收弹出的旧词项链（所有权转移）
        this.getCurrentConcept().__putTermLinkBack(oldTermLink);

        // * 🚩收尾：返回被替换下来的「旧词项链」
        return oldTermLink;
    }

    /**
     * 通过设置好的（非空的）「当前信念链」更新「当前信念」与「新时间戳」
     * * ❓是否要考虑「归还信念链」？此处使用的是值还是引用？所有权如何变更？
     */
    protected void updateCurrentBeliefAndNewStamp() {
        // * 🚩背景变量
        final TermLink newBeliefLink = this.currentBeliefLink;
        final Sentence newBelief;
        final Stamp newStamp;
        // * 🚩尝试从「当前信念链的目标」获取「当前信念」所对应的概念
        final Term beliefTerm = newBeliefLink.getTarget();
        final Concept beliefConcept = this.termToConcept(beliefTerm);
        if (beliefConcept != null) {
            newBelief = beliefConcept.getBelief(this.getCurrentTask().getSentence()); // ! may be null
            if (newBelief != null) {
                newStamp = Stamp.uncheckedMerge( // ! 此前已在`getBelief`处检查
                        this.getCurrentTask().getSentence().getStamp(),
                        // * 📌此处的「时间戳」一定是「当前信念」的时间戳
                        // * 📄理由：最后返回的信念与「成功时比对的信念」一致（只隔着`clone`）
                        newBelief.getStamp(),
                        this.getTime());
            } else {
                newStamp = null;
            }
        } else {
            newBelief = null;
            newStamp = null;
        }
        // * 🚩最后设置二者的值（可空性相对独立）
        this.setCurrentBelief(newBelief);
        this.setNewStamp(newStamp);
    }

    /* ---------- Short-term workspace for a single cycle ---------- */

    /**
     * The selected TermLink
     */
    private TermLink currentBeliefLink = null;

    public TermLink getCurrentBeliefLink() {
        return currentBeliefLink;
    }

    /**
     * 🆕所有要参与「概念推理」的词项链（信念链）
     * * 🎯装载「准备好的词项链（信念链）」，简化「概念推理准备阶段」的传参
     * * 📌Java没有像元组那样方便的「规范化临时结构」类型，对函数返回值的灵活性限制颇多
     * * 🚩目前对于「第一个要准备的词项链」会直接存储在「当前词项链（信念链）」中
     * * 📌类似Rust所有权规则：始终只有一处持有「完全独占引用（所有权）」
     */
    private LinkedList<TermLink> termLinksToReason = new LinkedList<>();

    public LinkedList<TermLink> getTermLinksToReason() {
        return termLinksToReason;
    }

    /**
     * 设置当前任务链
     * * 📝仅在「开始推理」之前设置，并且只在「概念推理」中出现（构建推理上下文）
     */
    public void setCurrentBeliefLink(TermLink currentBeliefLink) {
        this.currentBeliefLink = currentBeliefLink;
    }

    @Override
    public void absorbedByMemory(Memory memory) {
        // * 🚩将最后一个「当前信念链」归还给「当前信念」（所有权转移）
        this.getCurrentConcept().__putTermLinkBack(currentBeliefLink);
        // * 🚩从基类方法继续
        super.absorbedByMemory(memory);
    }
}
