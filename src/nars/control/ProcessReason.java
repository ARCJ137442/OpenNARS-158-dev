package nars.control;

import java.util.LinkedList;

import nars.entity.Concept;
import nars.entity.Sentence;
import nars.entity.Stamp;
import nars.entity.Task;
import nars.entity.TaskLink;
import nars.entity.TermLink;
import nars.inference.DerivationContextReason;
import nars.inference.RuleTables;
import nars.main_nogui.Parameters;
import nars.storage.Memory;

public abstract class ProcessReason {

    /**
     * 🆕「概念推理」控制机制的入口函数
     */
    public static void processReason(final Memory self, final boolean noResult) {
        // * 🚩从「直接推理」到「概念推理」过渡 阶段 * //
        // * 🚩选择概念、选择任务链、选择词项链（中间亦有推理）
        // TODO: 是否要合并这俩返回值？比如，将`toReasonLinks`内置到「概念推理上下文」作为字段
        final DerivationContextReason.IBuilder contextBuilder = ProcessReason.preprocessConcept(self, noResult);
        if (contextBuilder == null)
            return;

        // * 🚩构建「概念推理上下文」
        final DerivationContextReason context = contextBuilder.build();

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
            final TermLink termLink = context.getCurrentBeliefLink();
            // * 🚩每次「概念推理」只更改「当前信念」与「当前信念链」
            final TermLink newBeliefLink = termLink;
            final Sentence newBelief;
            final Stamp newStamp;
            final Concept beliefConcept = context.getMemory().termToConcept(termLink.getTarget());
            if (beliefConcept != null) {
                newBelief = beliefConcept.getBelief(context.getCurrentTask()); // ! may be null
                if (newBelief != null) {
                    newStamp = Stamp.uncheckedMerge( // ! 此前已在`getBelief`处检查
                            context.getCurrentTask().getSentence().getStamp(),
                            // * 📌此处的「时间戳」一定是「当前信念」的时间戳
                            // * 📄理由：最后返回的信念与「成功时比对的信念」一致（只隔着`clone`）
                            newBelief.getStamp(),
                            context.getTime());
                } else {
                    newStamp = null;
                }
            } else {
                newBelief = null;
                newStamp = null;
            }
            // * 🚩实际上就是「当前信念」「当前信念链」更改后的「新上下文」
            // this.context.currentBelief = newBelief;
            // this.context.currentBeliefLink = newBeliefLink;
            // this.context.newStamp = newStamp;
            context.switchToNewBelief(newBeliefLink, newBelief, newStamp);
            // * 🔥启动概念推理：点火！
            RuleTables.reason(context);
            // * ♻️回收词项链
            context.getCurrentConcept().__putTermLinkBack(termLink);
            // * 🚩尝试从「待推理词项链列表」中拿取（并替换）词项链
            if (context.getTermLinksToReason().isEmpty())
                break;
            else
                context.setCurrentBeliefLink(context.getTermLinksToReason().poll());
        }
        context.getCurrentConcept().__putTaskLinkBack(context.getCurrentTaskLink());
        // * 🚩吸收并清空上下文
        context.getMemory().absorbContext(context);
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
    private static DerivationContextReason.IBuilder preprocessConcept(
            final Memory self,
            final boolean noResult) {
        final DerivationContextReason.Builder context = new DerivationContextReason.Builder(self);
        // * 🚩推理前判断「是否有必要」
        if (!noResult) // necessary?
            return null;

        // * 🚩强制清空旧上下文 | 防止「概念推理后直接推理导致变量遗留」的情况
        context.clear();

        // * 🚩从「记忆区」拿出一个「概念」准备推理 | 源自`processConcept`

        // * 🚩拿出一个概念，准备点火
        context.setCurrentConcept(self.takeOutConcept());
        if (context.getCurrentConcept() == null) {
            return null;
        }
        // * ✅【2024-05-20 08:52:34】↓不再需要：自始至终都是「当前概念」所对应的词项
        // context.setCurrentTerm(context.getCurrentConcept().getTerm());
        self.getRecorder().append(" * Selected Concept: " + context.getCurrentTerm() + "\n");
        // current Concept remains in the bag all the time
        self.putBackConcept(context.getCurrentConcept());
        // a working workCycle
        // * An atomic step in a concept, only called in {@link Memory#processConcept}
        // * 🚩预点火（实质上仍属于「直接推理」而非「概念推理」）

        // * 🚩从「概念」拿出一个「任务链」准备推理 | 源自`Concept.fire`
        final TaskLink currentTaskLink = context.getCurrentConcept().__takeOutTaskLink();
        if (currentTaskLink == null) {
            return null;
        }
        context.setCurrentTaskLink(currentTaskLink);
        // * 📝【2024-05-21 11:54:04】断言：直接推理不会涉及「词项链/信念链」
        // * ❓这里的「信念链」是否可空
        // * 📝此处应该是「重置信念链，以便后续拿取词项链做『概念推理』」
        self.getRecorder().append(" * Selected TaskLink: " + currentTaskLink + "\n");
        final Task task = currentTaskLink.getTargetTask();
        context.setCurrentTask(task); // one of the two places where this variable is set
        // self.getRecorder().append(" * Selected Task: " + task + "\n");
        // for debugging
        if (currentTaskLink.getType() == TermLink.TRANSFORM) {
            RuleTables.transformTask(currentTaskLink, context);
            // to turn this into structural inference as below?
            // ? ↑【2024-05-17 23:13:45】似乎该注释意味着「应该放在『概念推理』而非『直接推理』中」
            // ! 🚩放回并结束 | 虽然导致代码重复，但以此让`switch`不再必要
            context.getCurrentConcept().__putTaskLinkBack(currentTaskLink);
            return null;
        }

        // * 🚩从选取的「任务链」获取要（分别）参与推理的「词项链」
        final LinkedList<TermLink> toReasonLinks = chooseTermLinksToReason(
                self,
                context.getCurrentConcept(),
                currentTaskLink);
        if (toReasonLinks.isEmpty()) {
            return null;
        } else {
            // 先将首个元素作为「当前信念链」
            final TermLink currentBeliefLink = toReasonLinks.poll();
            context.setCurrentBeliefLink(currentBeliefLink);
        }
        // 再将其余元素添加进「待推理词项链表」
        for (final TermLink termLink : toReasonLinks) {
            context.getTermLinksToReason().add(termLink);
        }
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
