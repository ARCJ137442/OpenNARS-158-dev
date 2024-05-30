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
     * Constructor for TermLink template
     * <p>
     * called in CompoundTerm.prepareComponentLinks only
     * * 🚩直接调用超类构造函数
     *
     * @param target  Target Term
     * @param type    Link type
     * @param indices Component indices in compound, may be 1 to 4
     */
    public TermLink(final Term target, final short type, final int[] indices) {
        super( // * 🚩直接传递到「完全构造方法」
                target,
                null, // * 🚩相当于调用Item的单Key构造函数
                // TODO: ↑这似乎是不好的可空性，需要调整（可能「链接模板」的实现需要商议）
                new BudgetValue(),
                type,
                // template types all point to compound, though the target is component
                generateIndices(type, indices));
    }

    /**
     * Constructor to make actual TermLink from a template
     * <p>
     * called in Concept.buildTermLinks only
     * * 🚩【2024-05-30 20:31:47】现在直接调用超类的「完全构造函数」
     *
     * @param target   Target Term
     * @param template TermLink template previously prepared
     * @param budget   Budget value of the link
     */
    public TermLink(final Term target, final TermLink template, final BudgetValue budget) {
        this(
                target,
                budget,
                generateTypeFromTemplate(target, template),
                template.getIndices());
    }

    /**
     * 🆕从「模板」中确定好「类型」与「索引」后，再进一步确定「键」
     */
    private TermLink(final Term target, final BudgetValue budget, final short type, final short[] indices) {
        super(
                target,
                /* target.getName() */
                generateKey(target, type, indices), budget,
                type,
                indices);
    }

    /**
     * 从「目标」、已生成的「类型」「索引」生成「键」
     *
     * @param target
     * @param type
     * @param indices
     * @return
     */
    private static String generateKey(final Term target, final short type, final short[] indices) {
        String key = TLink.generateKey(type, indices);
        if (target != null) {
            key += target;
        }
        return key;
    }

    /**
     * 🆕从「目标」与「模板」中产生链接类型
     *
     * @param <Target>
     * @param t
     * @param template
     * @return
     */
    protected static short generateTypeFromTemplate(final Term t, final TermLink template) {
        short type = template.getType();
        if (template.getTarget().equals(t)) {
            type--; // point to component
        }
        return type;
    }

    /**
     * 🆕将构造方法中的「生成索引部分」独立出来
     * * ⚠️仅在「复合词项→元素」中使用
     *
     * @param type
     * @param indices
     * @return
     */
    protected static final short[] generateIndices(
            final short type,
            final int[] indices) {
        if (type % 2 != 0)
            throw new AssertionError("type % 2 == " + type + " % 2 == " + (type % 2) + " != 0");
        final short[] index;
        if (type == TermLink.COMPOUND_CONDITION) { // the first index is 0 by default
            index = new short[indices.length + 1];
            index[0] = 0;
            for (int i = 0; i < indices.length; i++) {
                index[i + 1] = (short) indices[i];
            }
        } else {
            index = new short[indices.length];
            for (int i = 0; i < index.length; i++) {
                index[i] = (short) indices[i];
            }
        }
        return index;
    }
}
