package nars.entity;

import nars.io.Symbols;

/**
 * 🆕任务链与词项链共有的「T链接」
 */
public abstract class TLink<Target> implements Item {
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

    /**
     * 🆕Item令牌
     */
    private final Token token;

    @Override
    public String getKey() {
        return token.getKey();
    }

    @Override
    public BudgetValue getBudget() {
        return token.getBudget();
    }

    /**
     * The linked Target
     * * 📝【2024-05-30 19:39:14】final化：一切均在构造时确定，构造后不再改变
     *
     * * ️📝可空性：非空
     * * 📝可变性：不变 | 仅构造时，无需可变
     * * 📝所有权：具所有权，也可能是共享引用（见{@link TaskLink}）
     */
    protected final Target target;

    /**
     * The type of link, one of the above
     *
     * * ️📝可空性：非空
     * * 📝可变性：不变 | 仅构造时，无需可变
     * * 📝所有权：具所有权
     */
    protected final short type;

    /**
     * The index of the component in the component list of the compound,
     * may have up to 4 levels
     * * 📝「概念推理」中经常用到
     *
     * * ️📝可空性：非空
     * * 📝可变性：不变 | 仅构造时，无需可变
     * * 📝所有权：具所有权
     */
    protected final short[] index;

    /**
     * called from TaskLink
     * 📝完全构造方法
     *
     * @param s       The key of the TaskLink
     * @param v       The budget value of the TaskLink
     * @param type    Link type
     * @param indices Component indices in compound, may be 1 to 4
     */
    protected TLink(
            final Target target,
            final String key,
            final BudgetValue budget,
            final short type,
            final short[] indices) {
        this.token = new Token(key, budget);
        this.target = target;
        this.type = type;
        this.index = indices;
    }

    /**
     * Set the key of the link
     * * 📝原`setKey`就是「根据现有信息计算出key，并最终给自身key赋值」的功能
     * * 🚩【2024-05-30 19:06:30】现在不再有副作用，仅返回key让调用方自行决定
     * * 📌原`setKey()`要变成`this.key = generateKey(this.type, this.index)`
     */
    protected static final String generateKey(short type, short[] index) {
        final String at1, at2;
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
        return at1 + in + at2;
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
