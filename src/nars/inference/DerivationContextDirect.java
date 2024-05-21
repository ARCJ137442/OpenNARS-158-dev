package nars.inference;

import java.util.ArrayList;
import java.util.LinkedList;

import nars.entity.Task;
import nars.storage.Memory;

/**
 * 🆕新的「直接推理上下文」对象
 * * 📄从「推理上下文」中派生，用于「概念-任务」的「直接推理」
 */
public class DerivationContextDirect extends DerivationContext {

    /* ---------- Short-term workspace for a single cycle ---------- */

    /**
     * 构造函数
     * * 🚩创建一个空的「直接推理上下文」，默认所有参数为空
     *
     * @param memory 所反向引用的「记忆区」对象
     */
    public DerivationContextDirect(final Memory memory) {
        super(memory);
    }

    /**
     * 🆕带参初始化
     * * 🚩包含所有`final`变量，避免「创建后赋值」如「复制时」
     *
     * @param memory
     */
    private DerivationContextDirect(final Memory memory,
            final LinkedList<Task> newTasks,
            final ArrayList<String> exportStrings) {
        super(memory, newTasks, exportStrings);
    }

    /**
     * 「复制」推导上下文
     * * 🚩只搬迁引用，并不更改所有权
     */
    public DerivationContextDirect clone() {
        // * 🚩创建新上下文，并随之迁移`final`变量
        final DerivationContextDirect self = new DerivationContextDirect(
                this.getMemory(),
                this.getNewTasks(),
                this.getExportStrings());
        // * 🚩搬迁独有引用
        // * 🚩返回新上下文
        return self;
    }

    /**
     * 清理上下文
     * * 🎯便于断言性、学习性调试：各「推导上下文」字段的可空性、可变性
     */
    public void clear() {
        super.clear();
        // * 🚩清理独有变量
        // this.currentTerm = null;
        // this.currentConcept = null;
        // this.currentTaskLink = null;
        // this.currentTask = null;
        // this.currentBeliefLink = null;
        // this.currentBelief = null;
        // this.newStamp = null;
        // this.substitute = null;
    }

}
