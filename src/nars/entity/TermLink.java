package nars.entity;

import nars.io.Symbols;
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
public class TermLink extends Item {
    /** At C, point to C; TaskLink only */
    public static final short SELF = 0;
    /** At (&&, A, C), point to C */
    public static final short COMPONENT = 1;
    /** At C, point to (&&, A, C) */
    public static final short COMPOUND = 2;
    /** At <C --> A>, point to C */
    public static final short COMPONENT_STATEMENT = 3;
    /** At C, point to <C --> A> */
    public static final short COMPOUND_STATEMENT = 4;
    /** At <(&&, C, B) ==> A>, point to C */
    public static final short COMPONENT_CONDITION = 5;
    /** At C, point to <(&&, C, B) ==> A> */
    public static final short COMPOUND_CONDITION = 6;
    /** At C, point to <(*, C, B) --> A>; TaskLink only */
    public static final short TRANSFORM = 8;
    /** The linked Term */
    private final Term target;
    /** The type of link, one of the above */
    protected final short type;
    /**
     * The index of the component in the component list of the compound,
     * may have up to 4 levels
     * * 📝「复合条件」+「NAL-4 转换」
     */
    protected final short[] index;

    /**
     * Constructor for TermLink template
     * <p>
     * called in CompoundTerm.prepareComponentLinks only
     *
     * @param target  Target Term
     * @param type    Link type
     * @param indices Component indices in compound, may be 1 to 4
     */
    public TermLink(final Term target, final short type, final int[] indices) {
        super(null);
        this.target = target;
        this.type = type;
        assert (type % 2 == 0); // template types all point to compound, though the target is component
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
    }

    public static TermLink from(Term t, short p, int... indices) {
        return new TermLink(t, p, indices);
    }

    /**
     * called from TaskLink
     *
     * @param s       The key of the TaskLink
     * @param v       The budget value of the TaskLink
     * @param type    Link type
     * @param indices Component indices in compound, may be 1 to 4
     */
    protected TermLink(String s, BudgetValue v, short type, short[] indices) {
        super(s, v);
        this.target = null;
        this.type = type;
        this.index = indices;
    }

    /**
     * Constructor to make actual TermLink from a template
     * <p>
     * called in Concept.buildTermLinks only
     *
     * @param t        Target Term
     * @param template TermLink template previously prepared
     * @param v        Budget value of the link
     */
    public TermLink(Term t, TermLink template, BudgetValue v) {
        super(t.getName(), v);
        target = t;
        short type = template.getType();
        if (template.getTarget().equals(t)) {
            type--; // point to component
        }
        this.type = type;
        index = template.getIndices();
        setKey();
    }

    /**
     * Set the key of the link
     */
    protected final void setKey() {
        String at1, at2;
        if ((type % 2) == 1) { // to component
            at1 = Symbols.TO_COMPONENT_1;
            at2 = Symbols.TO_COMPONENT_2;
        } else { // to compound
            at1 = Symbols.TO_COMPOUND_1;
            at2 = Symbols.TO_COMPOUND_2;
        }
        String in = "T" + type;
        if (index != null) {
            for (int i = 0; i < index.length; i++) {
                in += "-" + (index[i] + 1);
            }
        }
        key = at1 + in + at2;
        if (target != null) {
            key += target;
        }
    }

    /**
     * Get the target of the link
     *
     * @return The Term pointed by the link
     */
    public Term getTarget() {
        return target;
    }

    /**
     * Get the link type
     *
     * @return Type of the link
     */
    public short getType() {
        return type;
    }

    /**
     * Get all the indices
     *
     * @return The index array
     */
    public short[] getIndices() {
        return index;
    }

    /**
     * Get one index by level
     *
     * @param i The index level
     * @return The index value
     */
    public short getIndex(int i) {
        if ((index != null) && (i < index.length)) {
            return index[i];
        } else {
            return -1;
        }
    }
}
