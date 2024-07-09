package nars.inference;

import nars.control.DerivationContextDirect;
import nars.control.DerivationContextReason;
import nars.control.DerivationContextTransform;

/**
 * 🆕推理引擎 初代实现
 * * 🚩【2024-06-07 23:20:47】目前直接调用规则表，封存内部推理规则的复杂度
 */
public class InferenceEngineV1 implements InferenceEngine {
    public void directProcess(DerivationContextDirect context) {
        LocalInference.process(context);
    }

    public void transform(DerivationContextTransform context) {
        TransformRules.transformTask(context);
    }

    public void match(DerivationContextReason context) {
        MatchingRules.matchTaskAndBelief(context);
    }

    public void reason(DerivationContextReason context) {
        final String oldTContent = context.getCurrentTask().getContent().toString();
        final String oldBContent = context.hasCurrentBelief() ? context.getCurrentBelief().getContent().toString()
                : null;
        RuleTables.reason(context);
        final String newTContent = context.getCurrentTask().getContent().toString();
        final String newBContent = context.hasCurrentBelief() ? context.getCurrentBelief().getContent().toString()
                : null;
        if (!oldTContent.equals(newTContent))
            throw new AssertionError("概念推理改变了当前任务的内容！" + oldTContent + "->" + newTContent);
        if (oldBContent != null && !oldBContent.equals(newBContent))
            throw new AssertionError("概念推理改变了当前信念的内容！" + oldBContent + "->" + newBContent);
    }
}