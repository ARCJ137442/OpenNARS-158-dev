package nars.inference;

import static nars.io.Symbols.*;

import nars.control.DerivationContextReason;
import nars.entity.Judgement;
import nars.entity.Stamp;
import nars.entity.Task;
import nars.language.Term;
import nars.language.VariableProcess;

/**
 * 🆕重新创建「匹配规则」
 * * 🎯用于在「概念推理」中【匹配】内容相近的语句
 * * 📌现在诸多规则已迁移到「三段论规则」中
 */
public abstract class MatchingRules {

    /**
     * The task and belief have the same content
     * <p>
     * called in RuleTables.reason
     * TODO: 【2024-06-08 09:18:23】预计将所有「本地规则」均迁移到「直接推理」中
     *
     * @param task    The task
     * @param belief  The belief
     * @param context Reference to the derivation context
     */
    public static void matchTaskAndBelief(DerivationContextReason context) {
        // * 📝【2024-05-18 14:35:35】自调用者溯源：此处的`task`一定是`context.currentTask`
        final Task currentTask = context.getCurrentTask();
        // * 📝【2024-05-18 14:35:35】自调用者溯源：此处的`belief`一定是`context.currentBelief`
        final Judgement belief = context.getCurrentBelief();

        // * 🚩按照标点分派
        switch (currentTask.getPunctuation()) {
            // * 🚩判断⇒尝试修正
            case JUDGMENT_MARK:
                // * 🚩判断「当前任务」是否能与「当前信念」做修正
                if (currentTask.asJudgement().revisable(belief))
                    revision(currentTask.asJudgement(), belief, context);
                return;
            // * 🚩问题⇒尝试回答「特殊疑问」（此处用「变量替换」解决查询变量）
            case QUESTION_MARK:
                // * 🚩查看是否可以替换「查询变量」，具体替换从「特殊疑问」转变为「一般疑问」
                // * 📄Task :: SentenceV1@49 "<{?1} --> murder>? {105 : 6} "
                // * & Belief: SentenceV1@39 "<{tom} --> murder>. %1.0000;0.7290% {147 : 3;4;2}"
                // * ⇒ Unified SentenceV1@23 "<{tom} --> murder>? {105 : 6} "
                final boolean hasUnified = VariableProcess.hasUnificationQ(
                        currentTask.getContent().clone(),
                        belief.getContent().clone());
                // * ⚠️只针对「特殊疑问」：传入的只有「带变量问题」，因为「一般疑问」通过直接推理就完成了
                if (hasUnified)
                    // * 🚩此时「当前任务」「当前信念」仍然没变
                    LocalRules.trySolution(belief, currentTask, context);
                return;
            // * 🚩其它
            default:
                System.err.println("未知的语句类型：" + currentTask);
                return;
        }
    }

    /**
     * 🆕基于「概念推理」的「修正」规则
     * * 📝和「直接推理」的唯一区别：有「当前信念」（会作为「父信念」使用 ）
     * * 💭【2024-06-09 01:35:41】需要合并逻辑
     */
    private static void revision(Judgement newBelief, Judgement oldBelief, DerivationContextReason context) {
        // * 🚩计算真值/预算值
        final Truth truth = TruthFunctions.revision(newBelief, oldBelief);
        final Budget budget = BudgetInference.reviseMatching(newBelief, oldBelief, truth, context);
        final Term content = newBelief.getContent();
        // * 🚩创建并导入结果：双前提 | 📝仅在此处用到「当前信念」作为「导出信念」
        // * 🚩【2024-06-06 08:52:56】现场构建「新时间戳」
        final Stamp newStamp = Stamp.uncheckedMerge(
                newBelief, oldBelief,
                context.getTime(),
                context.getMaxEvidenceBaseLength());
        context.doublePremiseTask(
                context.getCurrentTask(),
                content,
                truth, budget,
                newStamp);
    }
}
