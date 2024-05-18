package nars.inference;

import java.util.HashMap;
import java.util.Random;

import nars.entity.Concept;
import nars.entity.Sentence;
import nars.entity.Stamp;
import nars.entity.Task;
import nars.entity.TaskLink;
import nars.entity.TermLink;
import nars.language.Term;
import nars.storage.Memory;

/**
 * 🆕新的「推理上下文」对象
 * * 📄仿自OpenNARS 3.1.0
 */
public class DerivationContext {

    /**
     * 对「记忆区」的反向引用
     * * 🚩【2024-05-18 17:00:12】目前需要访问其「输出」「概念」等功能
     */
    public Memory memory;

    /* ---------- Short-term workspace for a single cycle ---------- */
    /**
     * The selected Term
     */
    public Term currentTerm = null;
    /**
     * The selected Concept
     */
    public Concept currentConcept = null;
    /**
     * The selected TaskLink
     */
    public TaskLink currentTaskLink = null;
    /**
     * The selected Task
     */
    public Task currentTask = null;
    /**
     * The selected TermLink
     */
    public TermLink currentBeliefLink = null;
    /**
     * The selected belief
     */
    public Sentence currentBelief = null;
    /**
     * The new Stamp
     */
    public Stamp newStamp = null;
    /**
     * The substitution that unify the common term in the Task and the Belief
     * TODO unused
     */
    protected HashMap<Term, Term> substitute = null;

    /**
     * 用于「变量替换」中的「伪随机数生成器」
     */
    public static Random randomNumber = new Random(1);

    /**
     * 构造函数
     * * 🚩创建一个空的「推理上下文」
     *
     * @param memory 所反向引用的「记忆区」对象
     */
    public DerivationContext(final Memory memory) {
        this.memory = memory;
    }

    /**
     * 重置全局状态
     */
    public static void init() {
        randomNumber = new Random(1);
    }

    /**
     * 清理推导上下文
     * * 🎯便于断言性、学习性调试：各「推导上下文」字段的可空性、可变性
     */
    public void clear() {
        this.currentTerm = null;
        this.currentConcept = null;
        this.currentTaskLink = null;
        this.currentTask = null;
        this.currentBeliefLink = null;
        this.currentBelief = null;
        this.newStamp = null;
        this.substitute = null;
    }
}
