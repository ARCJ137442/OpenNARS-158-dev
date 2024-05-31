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
     * evidentialBase baseLength
     * * 📌证据基的长度，构造时计算并锁定
     *
     * * ️📝可空性：非空
     * * 📝可变性：不变 | 仅构造时，无需可变
     * * 📝所有权：具所有权
     */
    private final int baseLength;
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
     * Generate a new stamp, with a new serial number, for a new Task
     *
     * @param time Creation time of the stamp
     */
    public Stamp(final long currentSerial, final long time) {
        baseLength = 1;
        evidentialBase = new long[baseLength];
        evidentialBase[0] = currentSerial;
        creationTime = time;
    }

    /**
     * Generate a new stamp identical with a given one
     *
     * @param old The stamp to be cloned
     */
    private Stamp(Stamp old) {
        baseLength = old.length();
        evidentialBase = old.getBase();
        creationTime = old.getCreationTime();
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
    public Stamp(Stamp old, long time) {
        baseLength = old.length();
        evidentialBase = old.getBase();
        creationTime = time;
    }

    /**
     * Generate a new stamp for derived sentence by merging the two from parents
     * the first one is no shorter than the second
     *
     * @param first  The first Stamp
     * @param second The second Stamp
     */
    private Stamp(Stamp first, Stamp second, long time) {
        int i1, i2, j;
        i1 = i2 = j = 0;
        baseLength = Math.min(first.length() + second.length(), Parameters.MAXIMUM_STAMP_LENGTH);
        evidentialBase = new long[baseLength];
        while (i2 < second.length() && j < baseLength) {
            evidentialBase[j] = first.get(i1);
            i1++;
            j++;
            evidentialBase[j] = second.get(i2);
            i2++;
            j++;
        }
        while (i1 < first.length() && j < baseLength) {
            evidentialBase[j] = first.get(i1);
            i1++;
            j++;
        }
        creationTime = time;
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
    public static Stamp make(Stamp first, Stamp second, long time) {
        return haveOverlap(first, second)
            ? null
            : uncheckedMerge(first, second, time);
    }

    /**
     * 🆕独立出逻辑：时间戳是否不可合并
     * * 📝语义：是否不可修正 / 证据基是否重叠
     * * 🚩判断其证据基是否有相同证据
     *
     * @param first  待合并的Stamp
     * @param second 待合并的Stamp
     * @return 是否可合并
     */
    public static boolean haveOverlap(Stamp first, Stamp second) {
        for (int i = 0; i < first.length(); i++) {
            for (int j = 0; j < second.length(); j++) {
                if (first.get(i) == second.get(j)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 🆕独立出逻辑：不经检查的合并
     * * 🚩会按照顺序合并「时间戳」的证据基
     */
    public static Stamp uncheckedMerge(Stamp first, Stamp second, long time) {
        return first.length() > second.length()
            ? new Stamp(first, second, time)
            : new Stamp(second, first, time);
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
     *
     * @return Length of the Stamp
     */
    public int length() {
        return baseLength;
    }

    /**
     * Get a number from the evidentialBase by index, called in this class only
     *
     * @param i The index
     * @return The number at the index
     */
    public long get(int i) {
        return evidentialBase[i];
    }

    /**
     * Get the evidentialBase, called in this class only
     *
     * @return The evidentialBase of numbers
     */
    private long[] getBase() {
        return evidentialBase;
    }

    /**
     * Convert the evidentialBase into a set
     *
     * @return The TreeSet representation of the evidential base
     */
    private TreeSet<Long> toSet() {
        TreeSet<Long> set = new TreeSet<>();
        for (int i = 0; i < baseLength; i++) {
            set.add(evidentialBase[i]);
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
    public boolean equals(Object that) {
        if (!(that instanceof Stamp)) {
            return false;
        }
        final TreeSet<Long> set1 = toSet();
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
        return creationTime;
    }

    /**
     * Get a String form of the Stamp for display
     * Format: {creationTime [: eventTime] : evidentialBase}
     *
     * @return The Stamp as a String
     */
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(" " + Symbols.STAMP_OPENER + creationTime);
        buffer.append(" ").append(Symbols.STAMP_STARTER).append(" ");
        for (int i = 0; i < baseLength; i++) {
            buffer.append(Long.toString(evidentialBase[i]));
            if (i < (baseLength - 1)) {
                buffer.append(Symbols.STAMP_SEPARATOR);
            } else {
                buffer.append(Symbols.STAMP_CLOSER).append(" ");
            }
        }
        return buffer.toString();
    }
}
