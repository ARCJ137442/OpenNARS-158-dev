package nars.entity;

import java.util.*;

import nars.io.Symbols;
import nars.main_nogui.Parameters;

/**
 * Each Sentence has a time stamp, consisting the following components:
 * (1) The creation time of the sentence,
 * (2) A evidentialBase of serial numbers of sentence, from which the sentence
 * is derived.
 * Each input sentence gets a unique serial number, though the creation time may
 * be not unique.
 * The derived sentences inherits serial numbers from its parents, cut at the
 * baseLength limit.
 *
 * * 📝该类型基本【仅由「构造后完全不可变的轻量类型字段」组成】，故可随意复制与存储
 */
public class Stamp implements Cloneable {

    /**
     * serial numbers
     * * 📌由「序列号」组成的「证据基」
     * * 🎯用于「时间戳判重」，避免「重复推理」
     *
     * * ️📝可空性：非空
     * * 📝可变性：不变 | 仅构造时，无需可变
     * * 📝所有权：具所有权
     */
    private final long[] evidentialBase;

    /**
     * creation time of the stamp
     * * 📌时间戳的「创建时间」，仅用作非逻辑性标识
     * * 🚩在「任务」中用作「合并预算值」的顺序依据
     *
     * * ️📝可空性：非空
     * * 📝可变性：不变 | 仅构造时，无需可变
     * * 📝所有权：具所有权
     */
    private final long creationTime;

    /**
     * 🆕完全参数的构造函数
     *
     * @param evidentialBase
     * @param creationTime
     */
    private Stamp(final long[] evidentialBase, final long creationTime) {
        this.evidentialBase = evidentialBase;
        this.creationTime = creationTime;
    }

    /**
     * Generate a new stamp, with a new serial number, for a new Task
     *
     * @param time Creation time of the stamp
     */
    public Stamp(final long currentSerial, final long time) {
        this(new long[] { currentSerial }, time);
    }

    /**
     * Generate a new stamp identical with a given one
     *
     * @param old The stamp to be cloned
     */
    private Stamp(final Stamp old) {
        this(old.evidentialBase, old.creationTime);
    }

    /**
     * Generate a new stamp from an existing one, with the same evidentialBase but
     * different creation time
     * <p>
     * For single-premise rules
     *
     * @param old  The stamp of the single premise
     * @param time The current time
     */
    public Stamp(final Stamp old, final long time) {
        this(old.evidentialBase, time);
    }

    /**
     * Generate a new stamp for derived sentence by merging the two from parents
     * // the first one is no shorter than the second
     * * 📌逻辑：不经检查的合并
     * * 🚩会按照顺序合并「时间戳」的证据基
     * * 🚩【2024-05-31 09:41:36】现在只在「具体合并证据基」时判断长度，此处无需再判断长度
     *
     * @param parent1 The first Stamp
     * @param parent2 The second Stamp
     */
    public static Stamp uncheckedMerge(final Stamp parent1, final Stamp parent2, final long time) {
        return new Stamp(
                // * 🚩合并的证据基，拥有新的长度和「父母证据基」各自的成员
                mergedEvidentialBase(parent1.evidentialBase, parent2.evidentialBase),
                // * 🚩新的「创建时间」
                time);
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
    private static final long[] mergedEvidentialBase(final long[] base1, final long[] base2) {
        // * 🚩计算新证据基长度：默认长度相加，一定长度后截断
        final int baseLength = Math.min( // * 📝一定程度上允许重复推理：在证据复杂时遗漏一定数据
                base1.length + base2.length,
                Parameters.MAXIMUM_STAMP_LENGTH);
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
     * Try to merge two Stamps, return null if have overlap
     * <p>
     * By default, the event time of the first stamp is used in the result
     *
     * @param first  The first Stamp
     * @param second The second Stamp
     * @param time   The new creation time
     * @return The merged Stamp, or null
     */
    public static Stamp merge(final Stamp first, final Stamp second, final long time) {
        // * 🚩有重合证据⇒返回空；无重合证据⇒合并证据
        return haveOverlap(first, second)
                ? null
                : uncheckedMerge(first, second, time);
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
    public static boolean haveOverlap(final Stamp first, final Stamp second) {
        for (final long serial1 : first.evidentialBase) {
            for (final long serial2 : second.evidentialBase) {
                if (serial1 == serial2)
                    return true;
            }
        }
        return false;
    }

    /**
     * Clone a stamp
     *
     * @return The cloned stamp
     */
    @Override
    public Stamp clone() {
        return new Stamp(this);
    }

    /**
     * Return the baseLength of the evidentialBase
     * * 🚩返回「证据基」的实际长度
     *
     * @return Length of the Stamp
     */
    public int length() {
        return this.evidentialBase.length;
    }

    /**
     * Get a number from the evidentialBase by index, called in this class only
     *
     * @param i The index
     * @return The number at the index
     */
    public long get(final int i) {
        return this.evidentialBase[i];
    }

    /**
     * Convert the evidentialBase into a set
     *
     * @return The TreeSet representation of the evidential base
     */
    private TreeSet<Long> toSet() {
        final TreeSet<Long> set = new TreeSet<>();
        for (final long serial : evidentialBase) {
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
    @Override
    public boolean equals(final Object that) {
        if (!(that instanceof Stamp)) {
            return false;
        }
        final TreeSet<Long> set1 = this.toSet();
        final TreeSet<Long> set2 = ((Stamp) that).toSet();
        return (set1.containsAll(set2) && set2.containsAll(set1));
    }

    /**
     * The hash code of Stamp
     *
     * @return The hash code
     */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Get the creationTime of the truth-value
     *
     * @return The creation time
     */
    public long getCreationTime() {
        return this.creationTime;
    }

    /**
     * Get a String form of the Stamp for display
     * Format: {creationTime [: eventTime] : evidentialBase}
     *
     * @return The Stamp as a String
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(" ")
                .append(Symbols.STAMP_OPENER)
                .append(this.creationTime)
                .append(" ")
                .append(Symbols.STAMP_STARTER)
                .append(" ");
        for (int i = 0; i < this.length(); i++) {
            buffer.append(Long.toString(this.evidentialBase[i]));
            if (i < this.length() - 1)
                buffer.append(Symbols.STAMP_SEPARATOR);
            else
                buffer.append(Symbols.STAMP_CLOSER).append(" ");
        }
        return buffer.toString();
    }
}
