package nars.entity;

import java.util.TreeSet;

import nars.io.Symbols;
import nars.main.Parameters;

/**
 * 🆕证据（基）
 * * 🎯抽象描述「时间戳」的特征
 * * 📝核心：记载一系列「证据时间」，提供「证据是否重复」方法，以避免「重复推理」
 */
public interface Evidential {

    /**
     * 🆕提取出的「最大长度」常量
     */
    public static int maxEvidenceBaseLength = Parameters.MAXIMUM_STAMP_LENGTH;

    /**
     * serial numbers
     * * 📌由「序列号」组成的「证据基」
     * * 🎯用于「时间戳判重」，避免「重复推理」
     *
     * * ️📝可空性：非空
     * * 📝可变性：不变 | 仅构造时，无需可变
     * * 📝所有权：具所有权
     */
    long[] __evidentialBase();

    /**
     * creation time of the stamp
     * * 📌时间戳的「创建时间」，仅用作非逻辑性标识
     * * 🚩在「任务」中用作「合并预算值」的顺序依据
     *
     * * ️📝可空性：非空
     * * 📝可变性：不变 | 仅构造时，无需可变
     * * 📝所有权：具所有权
     */
    long __creationTime();

    /**
     * Get the creationTime of the truth-value
     *
     * @return The creation time
     */
    public default long getCreationTime() {
        return this.__creationTime();
    }

    /**
     * Return the baseLength of the evidentialBase
     * * 🚩返回「证据基」的实际长度
     *
     * @return Length of the Stamp
     */
    public default int evidenceLength() {
        return this.__evidentialBase().length;
    }

    /**
     * Get a number from the evidentialBase by index, called in this class only
     * * 🚩获取「证据基」某处的值
     *
     * @param i The index
     * @return The number at the index
     */
    public default long get(final int i) {
        return this.__evidentialBase()[i];
    }

    public default long[] getEvidentialBase() {
        return this.__evidentialBase();
    }

    /**
     * 🆕提取出的「合并证据基」方法
     * * ⚠️要求：第一个证据基不短于第二个证据基
     * * 🚩使用「拉链式合并」的方式解决："1-2-1-2-1-1-1……"
     * * 📌【2024-05-31 09:34:20】此处先派发长度，以兼容「自动计算长度」并避免「重复计算」（Java「构造前不能有代码」的缺陷）
     *
     * @param base1
     * @param base2
     * @param baseLength
     * @return
     */
    public static long[] mergedEvidentialBase(final long[] base1, final long[] base2) {
        // * 🚩计算新证据基长度：默认长度相加，一定长度后截断
        final int baseLength = Math.min( // * 📝一定程度上允许重复推理：在证据复杂时遗漏一定数据
                base1.length + base2.length,
                maxEvidenceBaseLength);
        // * 🚩计算长短证据基
        final long[] longer, shorter;
        if (base1.length > base2.length) {
            longer = base1;
            shorter = base2;
        } else {
            longer = base2;
            shorter = base1;
        }
        // * 🚩开始构造并填充数据：拉链式填充，1-2-1-2……
        int i1, i2, j;
        i1 = i2 = j = 0;
        final long[] evidentialBase = new long[baseLength];
        while (i2 < shorter.length && j < baseLength) {
            evidentialBase[j] = longer[i1];
            i1++;
            j++;
            evidentialBase[j] = shorter[i2];
            i2++;
            j++;
        }
        // * 🚩2的长度比1小，所以此后随1填充
        while (i1 < longer.length && j < baseLength) {
            evidentialBase[j] = longer[i1];
            i1++;
            j++;
        }
        // * 🚩返回构造好的新证据基
        return evidentialBase;
    }

    /**
     * 🆕独立出逻辑：时间戳是否不可合并
     * * 📝语义：是否不可修正 / 证据基是否重叠
     * * 🚩判断其证据基是否有相同证据（重叠）
     *
     * @param first  待合并的Stamp
     * @param second 待合并的Stamp
     * @return 是否可合并
     */
    public static boolean haveOverlap(final Evidential first, final Evidential second) {
        for (final long serial1 : first.getEvidentialBase()) {
            for (final long serial2 : second.getEvidentialBase()) {
                if (serial1 == serial2)
                    return true;
            }
        }
        return false;
    }

    /**
     * 🆕判断「证据重复」
     * * 🎯用于判断「重复推理」等
     * * 📄参照{@link Evidential#haveOverlap(Evidential, Evidential)}
     *
     * @param that
     * @return 证据基是否重复
     */
    public default boolean evidentialOverlap(final Evidential that) {
        return haveOverlap(this, that);
    }

    /**
     * Convert the evidentialBase into a set
     *
     * @return The TreeSet representation of the evidential base
     */
    private static TreeSet<Long> evidenceSet(Evidential self) {
        final TreeSet<Long> set = new TreeSet<>();
        for (final long serial : self.getEvidentialBase()) {
            set.add(serial);
        }
        return set;
    }

    /**
     * Check if two stamps contains the same content
     *
     * @param that The Stamp to be compared
     * @return Whether the two have contain the same elements
     */
    public default boolean evidentialEqual(final Evidential that) {
        final TreeSet<Long> set1 = evidenceSet(this);
        final TreeSet<Long> set2 = evidenceSet(that);
        return (set1.containsAll(set2) && set2.containsAll(set1));
    }

    /**
     * Get a String form of the Stamp for display
     * Format: {creationTime [: eventTime] : evidentialBase}
     * * 📝实质：作为「时间戳」转换为字符串，只提取其中一部分转换到字符串
     *
     * @return The Stamp as a String
     */
    public default String stampToString() {
        final StringBuilder buffer = new StringBuilder(" ")
                .append(Symbols.STAMP_OPENER)
                .append(this.getCreationTime())
                .append(" ")
                .append(Symbols.STAMP_STARTER)
                .append(" ");
        for (int i = 0; i < this.evidenceLength(); i++) {
            buffer.append(Long.toString(this.get(i)));
            if (i < this.evidenceLength() - 1)
                buffer.append(Symbols.STAMP_SEPARATOR);
            else
                buffer.append(Symbols.STAMP_CLOSER).append(" ");
        }
        return buffer.toString();
    }
}
