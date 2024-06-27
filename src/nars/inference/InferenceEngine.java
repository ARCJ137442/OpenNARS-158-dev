package nars.inference;

import nars.control.DerivationContextDirect;
import nars.control.DerivationContextReason;
import nars.control.DerivationContextTransform;

/**
 * 🆕作为一个整体的「推理引擎」
 * * 📌只处理「推理上下文」，修改其中传入的对象
 * * 📌只处理会「推陈出新」的「转换推理」与「概念推理」
 */
public abstract class InferenceEngine {
    /**
     * 直接推理 入口
     *
     * @param context
     */
    public abstract void directProcess(DerivationContextDirect context);

    /**
     * 转换推理 入口
     *
     * @param context
     */
    public abstract void transform(DerivationContextTransform context);

    /**
     * 概念推理 入口
     *
     * @param context
     */
    public abstract void reason(DerivationContextReason context);
}
