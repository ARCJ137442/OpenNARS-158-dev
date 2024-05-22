package nars.control;

import java.util.LinkedList;

import nars.entity.Concept;
import nars.entity.Task;
import nars.entity.TaskLink;
import nars.entity.TermLink;
import nars.inference.RuleTables;
import nars.inference.TransformRules;
import nars.main_nogui.Parameters;
import nars.storage.Memory;

public abstract class ProcessReason {

    /**
     * 🆕「概念推理」控制机制的入口函数
     */
    public static void processReason(final Memory self, final boolean noResult) {
        // * 🚩从「直接推理」到「概念推理」过渡 阶段 * //
        // * 🚩选择概念、选择任务链、选择词项链（中间亦有推理）⇒构建「概念推理上下文」
        final DerivationContextReason context = ProcessReason.preprocessConcept(
                self,
                noResult);
        if (context == null)
            return;

        // * 🚩内部概念高级推理 阶段 * //
        ProcessReason.processConcept(context);
    }

    /**
     * Select a concept to fire.
     */
    public static void processConcept(final DerivationContextReason context) {
        // * 🚩开始推理；【2024-05-17 17:50:05】此处代码分离仅为更好演示其逻辑
        // * 📝【2024-05-19 18:40:54】目前将这类「仅修改一个变量的推理」视作一组推理，共用一个上下文
        // * 📌【2024-05-21 16:33:56】在运行到此处时，「推理上下文」的「当前信念」不在「待推理词项链表」中，但需要「被聚焦」
        for (;;) {
            // * 🚩实际上就是「当前信念」「当前信念链」更改后的「新上下文」
            // this.context.currentBelief = newBelief;
            // this.context.currentBeliefLink = newBeliefLink;
            // this.context.newStamp = newStamp;
            // * 🔥启动概念推理：点火！ | 此时已经预设「当前信念」「当前信念链」「新时间戳」准备完毕
            RuleTables.reason(context);
            // * 🚩切换上下文中的「当前信念」「当前信念链」「新时间戳」 | 每次「概念推理」只更改「当前信念」与「当前信念链」
            final boolean hasNext = context.nextBelief() != null;
            if (!hasNext)
                // * 🚩没有更多词项链⇒结束
                break;
        }
        // * ✅归还「当前任务链/当前信念链」的工作已经在「吸收上下文」中被执行
        // * 🚩吸收并清空上下文
        context.absorbedByMemory(context.getMemory());
    }

    /* ---------- main loop ---------- */

    /**
     * 🆕✨预点火
     * * 📝属于「直接推理」和「概念推理」的过渡部分
     * * 📌仍有「参与构建『推理上下文』」的作用
     * * 🚩在此开始为「概念推理」建立上下文
     * * 🎯从「记忆区」拿出「概念」并从其中拿出「任务链」：若都有，则进入「概念推理」阶段
     *
     * @return 预点火结果 {@link PreFireResult}
     */
    private static DerivationContextReason preprocessConcept(
            final Memory self,
            final boolean noResult) {
        // * 🚩推理前判断「是否有必要」
        if (!noResult) // necessary?
            return null;

        // * 🚩从「记忆区」拿出一个「概念」准备推理 | 源自`processConcept`

        // * 🚩拿出一个概念，准备点火
        final Concept currentConcept = self.takeOutConcept();
        if (currentConcept == null) {
            return null;
        }
        self.getRecorder().append(" * Selected Concept: " + currentConcept.getTerm() + "\n");
        // current Concept remains in the bag all the time
        self.putBackConcept(currentConcept);
        // a working workCycle
        // * An atomic step in a concept, only called in {@link Memory#processConcept}
        // * 🚩预点火（实质上仍属于「直接推理」而非「概念推理」）

        // * 🚩从「概念」拿出一个「任务链」准备推理 | 源自`Concept.fire`
        final TaskLink currentTaskLink = currentConcept.__takeOutTaskLink();
        if (currentTaskLink == null) {
            return null;
        }
        // * 📝【2024-05-21 11:54:04】断言：直接推理不会涉及「词项链/信念链」
        // * ❓这里的「信念链」是否可空
        // * 📝此处应该是「重置信念链，以便后续拿取词项链做『概念推理』」
        self.getRecorder().append(" * Selected TaskLink: " + currentTaskLink + "\n");
        final Task currentTask = currentTaskLink.getTargetTask();
        // self.getRecorder().append(" * Selected Task: " + task + "\n");
        // for debugging
        if (currentTaskLink.getType() == TermLink.TRANSFORM) {
            // * 🚩创建「转换推理上下文」
            // * ⚠️此处「当前信念链」为空，可空情况不一致，可能需要一个专门的「推理上下文」类型
            final DerivationContextTransform context = new DerivationContextTransform(
                    self,
                    currentConcept,
                    currentTaskLink);
            TransformRules.transformTask(currentTaskLink, context);
            // to turn this into structural inference as below?
            // ? ↑【2024-05-17 23:13:45】似乎该注释意味着「应该放在『概念推理』而非『直接推理』中」
            // * 🚩独立吸收上下文
            self.absorbContext(context);
            return null;
        }

        // * 🚩从选取的「任务链」获取要（分别）参与推理的「词项链」
        final TermLink currentBeliefLink;
        final LinkedList<TermLink> toReasonLinks = chooseTermLinksToReason(
                self,
                currentConcept,
                currentTaskLink);
        if (toReasonLinks.isEmpty()) {
            return null;
        } else {
            // 先将首个元素作为「当前信念链」
            currentBeliefLink = toReasonLinks.poll();
        }

        // * 🚩在最后构造并返回
        final DerivationContextReason context = new DerivationContextReason(
                self,
                currentConcept,
                currentTask,
                currentTaskLink,
                currentBeliefLink,
                toReasonLinks);
        // * 🚩终于要轮到「点火」
        return context;
    }

    /**
     * 🆕围绕任务链，获取可推理的词项链列表
     *
     * @param currentTaskLink 当前任务链
     * @return 将要被拿去推理的词项链列表
     */
    private static LinkedList<TermLink> chooseTermLinksToReason(Memory self, Concept concept,
            TaskLink currentTaskLink) {
        final LinkedList<TermLink> toReasonLinks = new LinkedList<>();
        int termLinkCount = Parameters.MAX_REASONED_TERM_LINK;
        // while (self.noResult() && (termLinkCount > 0)) {
        while (termLinkCount > 0) {
            final TermLink termLink = concept.__takeOutTermLink(currentTaskLink, self.getTime());
            if (termLink == null)
                break;
            self.getRecorder().append(" * Selected TermLink: " + termLink + "\n");
            toReasonLinks.add(termLink);
            termLinkCount--;
        }
        return toReasonLinks;
    }
}
