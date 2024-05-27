package nars.entity;

import nars.io.Symbols;

/**
 * 🆕任务链与词项链共有的「T链接」
 */
public abstract class TLink<Target> extends Item {
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
    /** The linked Target */
    protected final Target target;
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
    public TLink(final Target target, final short type, final int[] indices) {
        super(null);
        this.target = target;
        this.type = type;
        if (type % 2 != 0)
            throw new AssertionError("type % 2 == " + type + " % 2 == " + (type % 2) + " != 0");
        // template types all point to compound, though the target is component
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

    /**
     * called from TaskLink
     *
     * @param s       The key of the TaskLink
     * @param v       The budget value of the TaskLink
     * @param type    Link type
     * @param indices Component indices in compound, may be 1 to 4
     */
    protected TLink(Target target, String s, BudgetValue v, short type, short[] indices) {
        super(s, v);
        this.target = target;
        this.type = type;
        this.index = indices;
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
    protected TLink(Target t, String name, TermLink template, BudgetValue v) {
        super(name, v);
        target = t;
        short type = template.getType();
        if (template.getTarget().equals(t)) {
            type--; // point to component
        }
        this.type = type;
        index = template.getIndices();
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
    }

    /**
     * Get the target of the link
     *
     * @return The Term/Task pointed by the link
     */
    public final Target getTarget() {
        return target;
    }

    /**
     * Get the link type
     *
     * @return Type of the link
     */
    public final short getType() {
        return type;
    }

    /**
     * Get all the indices
     *
     * @return The index array
     */
    public final short[] getIndices() {
        return index;
    }

    /**
     * Get one index by level
     *
     * @param i The index level
     * @return The index value
     */
    public final short getIndex(int i) {
        if (index != null && i < index.length)
            return index[i];
        else
            return -1;
    }
}
