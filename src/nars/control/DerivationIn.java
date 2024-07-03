package nars.control;

import nars.entity.Concept;
import nars.entity.Task;
import nars.language.Term;
import nars.storage.Memory;

public interface DerivationIn {

    /**
     * 🆕获取记忆区（不可变引用）
     */
    public Memory getMemory();

    /**
     * 🆕访问「当前时间」
     * * 🎯用于在推理过程中构建「新时间戳」
     * * ️📝可空性：非空
     * * 📝可变性：只读
     */
    public long getTime();

    // /**
    // * 🆕访问「当前超参数」
    // * * 🎯用于在推理过程中构建「新时间戳」（作为「最大长度」参数）
    // * * ️📝可空性：非空
    // * * 📝可变性：只读
    // */
    // public Parameters getParameters();
    public default int getMaxEvidenceBaseLength() {
        return Parameters.MAXIMUM_STAMP_LENGTH;
    }

    /**
     * 获取「静默值」
     * * 🎯在「推理上下文」中无需获取「推理器」`getReasoner`
     * * ️📝可空性：非空
     * * 📝可变性：只读
     *
     * @return 静默值
     */
    public float getSilencePercent();

    /**
     * 获取「当前概念」
     * * ️📝可空性：非空
     * * 📝可变性：内部可变
     * * 📝所有权：临时所有（推理结束时归还）
     */
    public Concept getCurrentConcept();

    /**
     * * 📝在所有使用场景中，均为「当前概念要处理的词项」且只读
     * * 🚩【2024-05-20 09:15:59】故此处仅保留getter，并且不留存多余字段（减少共享引用）
     * * ️📝可空性：非空
     * * 📝可变性：只读 | 完全依赖「当前概念」而定，且「当前概念」永不变更词项
     * * 📝所有权：仅引用
     */
    public default Term getCurrentTerm() {
        // ! 🚩需要假定`this.getCurrentConcept() != null`
        return this.getCurrentConcept().getTerm();
    }

    /**
     * The selected task
     * * 🚩【2024-05-21 22:40:21】现在改为抽象方法：不同实现有不同的用法
     * * 📄「直接推理上下文」将其作为字段，而「转换推理上下文」「概念推理上下文」均只用作「当前任务链的目标」
     */
    public abstract Task getCurrentTask();

    /**
     * 🆕判断当前上下文是否为「反向推理」
     * * 🚩实质上就是判断「当前任务」是否为「疑问句」
     * * * 📝疑问⇒追问⇒反向
     */
    public default boolean isBackward() {
        return this.getCurrentTask().isQuestion();
    }

    /**
     * 获取「已存在的概念」
     * * 🎯让「概念推理」可以在「拿出概念」的时候运行，同时不影响具体推理过程
     * * 🚩先与「当前概念」做匹配，若没有再在记忆区中寻找
     * * 📌【2024-05-24 22:07:42】目前专供「推理规则」调用
     * * 📝【2024-06-26 20:45:59】目前所有逻辑纯只读：最多为「获取其中的信念」
     */
    public default Concept termToConcept(Term term) {
        if (term.equals(this.getCurrentTerm()))
            return this.getCurrentConcept();
        else
            return this.getMemory().termToConcept(term);
    }
}
