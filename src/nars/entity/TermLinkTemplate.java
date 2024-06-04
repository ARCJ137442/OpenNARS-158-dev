package nars.entity;

import nars.language.Term;

// * 📝【2024-05-15 18:37:01】实际运行中的案例（复合词项の词项链模板）：
// * 🔬复现方法：仅输入"<(&&,A,B) ==> D>."
// * ⚠️其中的内容并不完整：只列出一些有代表性的示例
// * 📄【概念】"D"
// *   <~ "<(&&,A,B) ==> D>" i=[1] # 4=COMPOUND_STATEMENT " _@(T4-2) <(&&,A,B) ==> D>"
// * 📄【概念】"(&&,A,B)"
// *   ~> "A"                i=[0] # 2=COMPOUND           " @(T1-1)_ A"
// *   ~> "B"                i=[1] # 2=COMPOUND           " @(T1-2)_ B"
// *   <~ "<(&&,A,B) ==> D>" i=[0] # 4=COMPOUND_STATEMENT " _@(T4-1) <(&&,A,B) ==> D>"
// * 📄【概念】"<(&&,A,B) ==> D>"
// *   ~> "(&&,A,B)" i=[0]   # 4=COMPOUND_STATEMENT " @(T3-1)_ (&&,A,B)"
// *   ~> "A"        i=[0,0] # 6=COMPOUND_CONDITION " @(T5-1-1)_ A"
// *   ~> "B"        i=[0,1] # 6=COMPOUND_CONDITION " @(T5-1-2)_ B"
// *   ~> "D"        i=[1]   # 4=COMPOUND_STATEMENT " @(T3-2)_ D"
// *   ~T> null      i=null  # 0=SELF               " _@(T0) <(&&,A,B) ==> D>. %1.00;0.90%"

/**
 * 🆕从「词项链」独立出的「词项链模板」
 * * 🎯用于分离「作为模板的TermLink」与「实际在推理中使用的TermLink」
 * * 📌和「词项链」唯一的不同是：不用实现{@link Item}，仅作为`TLink<Term>`的一种实现
 */
public class TermLinkTemplate extends TLink<Term> {

    /**
     * Constructor for TermLink template
     * <p>
     * called in CompoundTerm.prepareComponentLinks only
     * * 🚩直接调用超类构造函数
     * * ⚠️此处的「目标」非彼「目标」，而是「模板」：针对「目标词项」构建「从元素到自身的词项链/任务链」
     * * 📌【2024-06-04 20:19:33】所以此处才会存在「虽然『目标』是『元素』，但『链接类型』是『链接到自身』」的情况
     *
     * @param target  Target Term
     * @param type    Link type
     * @param indices Component indices in compound, may be 1 to 4
     */
    public TermLinkTemplate(final Term target, final TLinkType type, final int[] indices) {
        super( // * 🚩直接传递到「完全构造方法」
                target,
                type,
                // * ✅现在不再需要传入null作为key了，因为TermLinkTemplate不需要key
                // template types all point to compound, though the target is component
                generateIndices(type, indices));
    }

    /**
     * 🆕获取这个「词项链模板」所【基于】的「目标」词项
     * * 📝后续
     */
    public final Term willFromSelfTo() {
        return getTarget();
    }

    /**
     * 🆕将构造方法中的「生成索引部分」独立出来
     * * ⚠️仅在「复合词项→元素」中使用
     * * 📄Concept@57 "<{tim} --> (/,livingIn,_,{graz})>"
     * * --[COMPOUND_STATEMENT]--> SetExt@20 "{tim}"
     *
     * @param type
     * @param indices
     * @return
     */
    private static final short[] generateIndices(
            final TLinkType type,
            final int[] indices) {
        // * 🚩假定此处是「COMPOUND」系列或「TRANSFORM」类型——链接到复合词项
        if (!(isToCompound(type) || type == TLinkType.TRANSFORM))
            throw new AssertionError("type " + type + " isn't from compound");
        final short[] index;
        // * 🚩原数组为「复合条件」⇒头部添加`0`
        if (type == TLinkType.COMPOUND_CONDITION) { // the first index is 0 by default
            index = new short[indices.length + 1];
            index[0] = 0;
            for (int i = 0; i < indices.length; i++) {
                index[i + 1] = (short) indices[i];
            }
        }
        // * 🚩否则：逐个转换并复制原索引数组
        else {
            index = new short[indices.length];
            for (int i = 0; i < index.length; i++) {
                index[i] = (short) indices[i];
            }
        }
        return index;
    }
}
