package nars.inference;

import nars.control.DerivationContextDirect;
import nars.control.DerivationContextReason;
import nars.control.DerivationContextTransform;

/**
 * 🆕推理引擎 初代实现
 * * 🚩【2024-06-07 23:20:47】目前直接调用规则表，封存内部推理规则的复杂度
 */
public class InferenceEngineV1 extends InferenceEngine {
    public void directProcess(DerivationContextDirect context) {
        LocalInference.process(context);
    }

    public void reason(DerivationContextReason context) {
        RuleTables.reason(context);
    }

    public void transform(DerivationContextTransform context) {
        TransformRules.transformTask(context);
    }
}