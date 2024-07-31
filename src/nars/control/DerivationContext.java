package nars.control;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

import nars.entity.Concept;
import nars.entity.Task;
import nars.language.Term;
import nars.storage.Memory;

/**
 * 🆕新的「推理上下文」对象
 * * 📄仿自OpenNARS 3.1.0
 */
@SuppressWarnings("unused")
public interface DerivationContext extends DerivationIn, DerivationOut {

    /**
     * 重置全局状态
     */
    public static void init() {
        DerivationContextCore.randomNumber = new Random(1);
    }

    /**
     * 让「推理器」吸收「推理上下文」
     * * 🚩【2024-05-19 18:39:44】现在会在每次「准备上下文⇒推理」的过程中执行
     * * 🎯变量隔离，防止「上下文串线」与「重复使用」
     * * 📌传入所有权而非引用
     * * 🚩【2024-05-21 23:17:57】现在迁移到「推理上下文」处，以便进行方法分派
     */
    public void absorbedByReasoner(final Reasoner reasoner);

    // /**
    // * 默认就是被「自身所属推理器」吸收
    // * * 📝【2024-05-30 08:48:15】此处的「推理器」可变，因为要从「上下文」中获取结果
    // * * 🚩【2024-05-30 08:48:29】此方法仅为分派需要，实际上要先将引用解耦
    // */
    // public void absorbedByReasoner() {
    // this.absorbedByReasoner(this.mutMemory());
    // }

    static void drop(Object any) {
    }

    /** 🆕内置公开结构体，用于公共读取 */
    public static final class DerivationContextCore {

        /**
         * 缓存的「当前时间」
         * * 🎯与「记忆区」解耦
         *
         * * ️📝可空性：非空
         * * 📝可变性：只读 | 仅构造时赋值
         * * 📝所有权：具所有权
         */
        final long time;

        /**
         * 缓存的「静默值」
         * * 🚩【2024-05-30 09:02:10】现仅在构造时赋值，其余情况不变
         * * ️📝可空性：非空
         * * 📝可变性：只读
         * * 📝所有权：具所有权
         */
        private final int silenceValue;

        /* ---------- Short-term workspace for a single cycle ---------- */
        /**
         * List of new tasks accumulated in one cycle, to be processed in the next
         * cycle
         * * 🚩【2024-05-18 17:29:40】在「记忆区」与「推理上下文」中各有一个，但语义不同
         * * 📌「记忆区」的跨越周期，而「推理上下文」仅用于存储
         * * ️📝可空性：非空
         * * 📝可变性：可变 | 单次推理的结果存放至此
         * * 📝所有权：具所有权
         */
        final LinkedList<Task> newTasks;

        /**
         * List of Strings or Tasks to be sent to the output channels
         * * 🚩【2024-05-18 17:29:40】在「记忆区」与「推理上下文」中各有一个，但语义不同
         * * 📌「记忆区」的跨越周期，而「推理上下文」仅用于存储
         * * ️📝可空性：非空
         * * 📝可变性：可变 | 单次推理的结果存放至此
         * * 📝所有权：具所有权
         */
        final ArrayList<String> exportStrings;

        /**
         * * 🆕用于在「被吸收」时加入「推理记录器」的字符串集合
         *
         * * ️📝可空性：非空
         * * 📝可变性：可变 | 单次推理的结果存放至此
         * * 📝所有权：具所有权
         */
        final ArrayList<String> stringsToRecord;

        /**
         * The selected Concept
         * * 🚩【2024-05-25 16:19:51】现在已经具备所有权
         *
         * * ️📝可空性：非空
         * * 📝可变性：可变 | 「链接到任务」等
         * * 📝所有权：具所有权
         */
        final Concept currentConcept;

        /**
         * 用于「变量替换」中的「伪随机数生成器」
         * * ️📝可空性：非空
         * * 📝可变性：可变 | 在「打乱集合」时被`shuffle`函数修改
         * * 📝所有权：具所有权
         */
        public static Random randomNumber = new Random(1);

        /**
         * 记录所有的「导出结果」
         * * ️📝可空性：非空
         * * 📝可变性：可变 | 在「打乱集合」时被`shuffle`函数修改
         * * 📝所有权：具所有权
         */
        public LinkedList<Derivation> derivations = new LinkedList<>();

        /**
         * 构造函数
         * * 🚩创建一个空的「推理上下文」，默认所有参数为空
         *
         * @param memory 所反向引用的「记忆区」对象
         */
        DerivationContextCore(final Reasoner reasoner, final Concept currentConcept) {
            this(reasoner, currentConcept, new LinkedList<>(), new ArrayList<>());
        }

        /**
         * 🆕带参初始化
         * * 🚩包含所有`final`变量，避免「创建后赋值」如「复制时」
         *
         * @param memory
         */
        private DerivationContextCore(
                final Reasoner reasoner,
                final Concept currentConcept,
                final LinkedList<Task> newTasks,
                final ArrayList<String> exportStrings) {
            // this.memory = reasoner.getMemory();
            this.currentConcept = currentConcept;
            this.silenceValue = reasoner.getSilenceValue().get();
            this.time = reasoner.getTime();
            this.newTasks = newTasks;
            this.exportStrings = exportStrings;
            this.stringsToRecord = new ArrayList<>();
        }

        /** 🆕共用的静态方法 */
        public void absorbedByReasoner(final Reasoner reasoner) {
            final Memory memory = reasoner.getMemory();
            // * 🚩将「当前概念」归还到「推理器」中
            memory.putBackConcept(this.currentConcept);
            // * 🚩将推理导出的「新任务」添加到自身新任务中（先进先出）
            for (final Task newTask : this.newTasks) {
                reasoner.addNewTask(newTask);
            }
            // * 🚩将推理导出的「导出字串」添加到自身「导出字串」中（先进先出）
            for (final String output : this.exportStrings) {
                reasoner.report(output);
            }
            // * 🚩将推理导出的「报告字串」添加到自身「报告字串」中（先进先出）
            for (final String message : this.stringsToRecord) {
                reasoner.getRecorder().append(message);
            }
            // * 🚩清理上下文防串（同时清理「导出的新任务」与「导出字串」）
            this.newTasks.clear();
            this.exportStrings.clear();
            // * 🚩销毁自身：在此处销毁相应变量
            drop(this.newTasks);
            drop(this.exportStrings);
        }

        /** 🆕对上层暴露的方法 */
        float getSilencePercent() {
            return this.silenceValue / 100.0f;
        }

        public void sendDerivation(Derivation derivation) {
            // // ! 不能用out打印：5.1、5.2测试失败
            // System.err.println("Derivation sent: " + derivation + " @ " + derivation.content);
            this.derivations.add(derivation);
        }
    }
}
