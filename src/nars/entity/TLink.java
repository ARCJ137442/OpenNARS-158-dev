package nars.entity;

import nars.io.Symbols;

/**
 * 🆕任务链与词项链共有的「T链接」
 * * 🚩【2024-06-01 20:56:49】现在不再实现{@link Item}接口，交由后续「词项链」「任务链」「词项链模板」自由组合
 */
public abstract class TLink<Target> {
    // TODO: 使用枚举而非不稳定的短整数
    /** Targeted to C, from C; TaskLink only */
    public static final short SELF = 0;
    /** Targeted to (&&, A, C), from C */
    public static final short COMPONENT = 1;
    /** Targeted to C, from (&&, A, C) */
    public static final short COMPOUND = 2;
    /** Targeted to <C --> A>, from C */
    public static final short COMPONENT_STATEMENT = 3;
    /** Targeted to C, from <C --> A> */
    public static final short COMPOUND_STATEMENT = 4;
    /** Targeted to <(&&, C, B) ==> A>, from C */
    public static final short COMPONENT_CONDITION = 5;
    /** Targeted to C, from <(&&, C, B) ==> A> */
    public static final short COMPOUND_CONDITION = 6;
    /** Targeted to C, from <(*, C, B) --> A>; TaskLink only */
    public static final short TRANSFORM = 8;

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
            final short type,
            final short[] indices) {
        // * 🚩动态检查可空性
        if (target == null)
            throw new IllegalArgumentException("target cannot be null");
        if (indices == null)
            throw new IllegalArgumentException("indices cannot be null");
        // * 🚩对位赋值
        this.target = target;
        this.type = type;
        this.index = indices;
    }

    /**
     * 🆕判断一个「T链接类型」是否为「从元素链接到复合词项」
     *
     * @param type
     * @return
     */
    public static boolean isFromComponent(short type) {
        return type % 2 == 1;
    }

    public boolean isFromComponent() {
        return isFromComponent(this.type);
    }

    /**
     * 🆕判断一个「T链接类型」是否为「从元素链接到复合词项」
     *
     * @param type
     * @return
     */
    public static boolean isFromCompound(short type) {
        return type > 0 && type % 2 == 0;
    }

    public boolean isFromCompound() {
        return isFromCompound(this.type);
    }

    /**
     * 🆕从「整体→元素」变成「元素→整体」
     */
    public static short changeLinkIntoFromComponent(final short type) {
        return (short) (type - 1);
    }

    /**
     * Set the key of the link
     * * 📝原`setKey`就是「根据现有信息计算出key，并最终给自身key赋值」的功能
     * * 🚩【2024-05-30 19:06:30】现在不再有副作用，仅返回key让调用方自行决定
     * * 📌原`setKey()`要变成`this.key = generateKey(this.type, this.index)`
     */
    protected static final String generateKey(short type, short[] index) {
        // * 🚩先添加左右括弧，分
        final String at1, at2;
        if (isFromComponent(type)) { // to component
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
