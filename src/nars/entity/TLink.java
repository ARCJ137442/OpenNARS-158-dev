package nars.entity;

import nars.io.Symbols;

/**
 * 🆕任务链与词项链共有的「T链接」
 * * 🚩【2024-06-01 20:56:49】现在不再实现{@link Item}接口，交由后续「词项链」「任务链」「词项链模板」自由组合
 */
public interface TLink<Target> {
    /**
     * 基于枚举的「链接类型」
     * * 📌【2024-06-04 19:35:12】拨乱反正：此处的「类型名」均为「从自身向目标」视角下「目标相对自身」的类型
     * * 📄目标是自身的元素⇒COMPONENT「元素」链接
     */
    public static enum TLinkType {
        /** From C, targeted to "SELF" C; TaskLink only */
        SELF, // = 0
        /** From (&&, A, C), targeted to "COMPONENT" C */
        COMPONENT, // = 1
        /** From C, targeted to "COMPOUND" (&&, A, C) */
        COMPOUND, // = 2
        /** From <C --> A>, targeted to "COMPONENT_STATEMENT" C */
        COMPONENT_STATEMENT, // = 3
        /** From C, targeted to "COMPOUND_STATEMENT" <C --> A> */
        COMPOUND_STATEMENT, // = 4
        /** From <(&&, C, B) ==> A>, targeted to "COMPONENT_CONDITION" C */
        COMPONENT_CONDITION, // = 5
        /** From C, targeted to "COMPOUND_CONDITION" <(&&, C, B) ==> A> */
        COMPOUND_CONDITION, // = 6
        /** From C, targeted to "TRANSFORM" <(*, C, B) --> A>; TaskLink only */
        TRANSFORM; // = 8

        /**
         * 🆕获取「链接类型」的「排序」，即原OpenNARS中的编号
         *
         * @return 01234568
         */
        public short toOrder() {
            switch (this) {
                case SELF:
                    return 0;
                case COMPONENT:
                    return 1;
                case COMPOUND:
                    return 2;
                case COMPONENT_STATEMENT:
                    return 3;
                case COMPOUND_STATEMENT:
                    return 4;
                case COMPONENT_CONDITION:
                    return 5;
                case COMPOUND_CONDITION:
                    return 6;
                case TRANSFORM:
                    return 8;
                default:
                    throw new IllegalArgumentException("Wrong enum variant @ TLinkType");
            }
        }

        /**
         * 🆕从「元素→整体」变成「整体→元素」
         * * 🚩「自元素到整体」⇒「自整体到元素」
         * * 📌【2024-06-04 19:51:48】目前只在「元素→整体」⇒「整体→元素」的过程中调用
         * * 🚩其它⇒报错
         */
        public TLinkType tryPointToComponent() {
            switch (this) {
                // case COMPONENT:
                // return TLinkType.COMPOUND;
                // case COMPONENT_STATEMENT:
                // return TLinkType.COMPOUND_STATEMENT;
                // case COMPONENT_CONDITION:
                // return TLinkType.COMPOUND_CONDITION;
                // * 🚩「自整体」⇒「自元素」
                case COMPOUND:
                    return TLinkType.COMPONENT;
                case COMPOUND_STATEMENT:
                    return TLinkType.COMPONENT_STATEMENT;
                case COMPOUND_CONDITION:
                    return TLinkType.COMPONENT_CONDITION;
                // * 🚩其它⇒报错
                default:
                    throw new IllegalArgumentException("Unexpected type: " + this + " not to compound");
            }
        }
    }

    /**
     * 🆕一个基本的默认实现
     */
    public static class TLinkage<Target> implements TLink<Target> {

        // struct TLinkage<Target>

        /**
         * The linked Target
         * * 📝【2024-05-30 19:39:14】final化：一切均在构造时确定，构造后不再改变
         *
         * * ️📝可空性：非空
         * * 📝可变性：不变 | 仅构造时，无需可变
         * * 📝所有权：具所有权，也可能是共享引用（见{@link TaskLink}）
         */
        private final Target target;

        /**
         * The type of link, one of the above
         *
         * * ️📝可空性：非空
         * * 📝可变性：不变 | 仅构造时，无需可变
         * * 📝所有权：具所有权
         */
        private final TLinkType type;

        /**
         * The index of the component in the component list of the compound,
         * may have up to 4 levels
         * * 📝「概念推理」中经常用到
         *
         * * ️📝可空性：非空
         * * 📝可变性：不变 | 仅构造时，无需可变
         * * 📝所有权：具所有权
         */
        private final short[] index;

        // impl<Target> TLinkage<Target>

        /**
         * called from TaskLink
         * 📝完全构造方法
         *
         * @param s       The key of the TaskLink
         * @param v       The budget value of the TaskLink
         * @param type    Link type
         * @param indices Component indices in compound, may be 1 to 4
         */
        protected TLinkage(
                final Target target,
                final TLinkType type,
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
         * Constructor for TermLink template
         * <p>
         * called in CompoundTerm.prepareComponentLinks only
         * * 🚩直接调用超类构造函数
         * * ⚠️此处的「目标」非彼「目标」，而是「模板」：针对「目标词项」构建「从元素到自身的词项链/任务链」
         * * 📌【2024-06-04 20:19:33】所以此处才会存在「虽然『目标』是『元素』，但『链接类型』是『链接到自身』」的情况
         * * 🎯用于「词项链模板」的类型别名
         *
         * @param target  Target Term
         * @param type    Link type
         * @param indices Component indices in compound, may be 1 to 4
         */
        protected TLinkage(final Target target, final TLinkType type, final int[] indices) {
            this( // * 🚩直接传递到「完全构造方法」
                    target,
                    type,
                    // * ✅现在不再需要传入null作为key了，因为TermLinkTemplate不需要key
                    // template types all point to compound, though the target is component
                    generateIndices(type, indices));
        }

        /**
         * 🆕「目标」的别名
         */
        public final Target willFromSelfTo() {
            return this.getTarget();
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
            if (!(TLink.isToCompound(type) || type == TLinkType.TRANSFORM))
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

        // impl<Target> TLinkage<Target>

        @Override
        public final Target getTarget() {
            return target;
        }

        @Override
        public final TLinkType getType() {
            return type;
        }

        @Override
        public final short[] getIndices() {
            return index;
        }
    }

    /**
     * Get the target of the link
     *
     * @return The Term/Task pointed by the link
     */
    public Target getTarget();

    /**
     * Get the link type
     *
     * @return Type of the link
     */
    public TLinkType getType();

    /**
     * Get all the indices
     *
     * @return The index array
     */
    public short[] getIndices();

    /**
     * 🆕判断一个「T链接类型」是否为「从元素链接到复合词项」
     *
     * @param type
     * @return
     */
    public static boolean isToComponent(TLinkType type) {
        switch (type) {
            // from COMPONENT
            case COMPONENT: // 1
            case COMPONENT_STATEMENT: // 3
            case COMPONENT_CONDITION: // 5
                return true;
            // #other
            default:
                return false;
        }
    }

    public default boolean isToComponent() {
        return isToComponent(this.getType());
    }

    /**
     * 🆕判断一个「T链接类型」是否为「从元素链接到复合词项」
     *
     * @param type
     * @return
     */
    public static boolean isToCompound(TLinkType type) {
        switch (type) {
            // from COMPONENT
            case COMPOUND: // 2
            case COMPOUND_STATEMENT: // 4
            case COMPOUND_CONDITION: // 6
                return true;
            // #other | 🚩【2024-06-04 18:25:26】目前不包括TRANSFORM
            default:
                return false;
        }
    }

    public default boolean isToCompound() {
        return isToCompound(this.getType());
    }

    /**
     * Set the key of the link
     * * 📝原`setKey`就是「根据现有信息计算出key，并最终给自身key赋值」的功能
     * * 🚩【2024-05-30 19:06:30】现在不再有副作用，仅返回key让调用方自行决定
     * * 📌原`setKey()`要变成`this.key = generateKey(this.type, this.index)`
     */
    static String generateKey(final TLinkType type, final short[] index) {
        // * 🚩先添加左右括弧，分「向元素」和「向整体」表示
        // * 📌格式：自身 - 目标 | "_"即「元素」
        // * 📝 向元素: 整体 "@(【索引】)_" 元素
        // * 📝 向整体: 元素 "_@(【索引】)" 整体
        final String at1, at2;
        if (isToComponent(type)) { // to component
            at1 = Symbols.TO_COMPONENT_1;
            at2 = Symbols.TO_COMPONENT_2;
        } else { // to compound
            at1 = Symbols.TO_COMPOUND_1;
            at2 = Symbols.TO_COMPOUND_2;
        }
        // * 🚩再生成内部索引
        String in = "T" + type.toOrder();
        if (index != null) {
            for (int i = 0; i < index.length; i++) {
                in += "-" + (index[i] + 1);
            }
        }
        return at1 + in + at2;
    }

    /**
     * Get one index by level
     *
     * @param i The index level
     * @return The index value
     */
    public default short getIndex(int i) {
        // * 🚩索引之内⇒正常返回，索引之外⇒返回-1（未找到）
        return i < getIndices().length ? getIndices()[i] : -1;
    }
}
