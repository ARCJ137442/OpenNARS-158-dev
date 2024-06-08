package nars.entity;

import nars.entity.TLink.TLinkage;
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
 * * ✅【2024-06-08 14:03:40】现在已经作为{@link TLinkage<Term>}的类型别名
 */
public class TermLinkTemplate extends TLinkage<Term> {
    public TermLinkTemplate(final Term target, final TLinkType type, final int[] indices) {
        // * 🚩实际上就是类型别名
        super(target, type, indices);
    }
}
