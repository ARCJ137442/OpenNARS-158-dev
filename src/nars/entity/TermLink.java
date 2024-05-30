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
 * A link between a compound term and a component term
 * <p>
 * A TermLink links the current Term to a target Term, which is
 * either a component of, or compound made from, the current term.
 * <p>
 * Neither of the two terms contain variable shared with other terms.
 * <p>
 * The index value(s) indicates the location of the component in the compound.
 * <p>
 * This class is mainly used in inference.RuleTable to dispatch premises to
 * inference rules
 */
public class TermLink extends TLink<Term> {

    /**
     * 🆕任务链模板构造
     * * 🚩直接调用超类构造函数
     *
     * @param target
     * @param type
     * @param indices
     */
    public TermLink(final Term target, final short type, final int[] indices) {
        super(target, type, indices);
    }

    /**
     * Constructor to make actual TermLink from a template
     * <p>
     * called in Concept.buildTermLinks only
     * * 🚩现在从「词项链」往下调用
     *
     * @param t        Target Term
     * @param template TermLink template previously prepared
     * @param v        Budget value of the link
     */
    public TermLink(Term t, TermLink template, BudgetValue v) {
        super(t, t.getName(), template, v);
        this.key = generateKey(this.type, this.index);
        if (target != null) {
            key += target;
        }
    }

    public static TermLink from(Term t, short p, int... indices) {
        return new TermLink(t, p, indices);
    }
}
