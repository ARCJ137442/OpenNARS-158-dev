package nars.entity;

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
public class Stamp implements Cloneable, Evidential {

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

    @Override
    public long[] __evidentialBase() {
        return evidentialBase;
    }

    @Override
    public long __creationTime() {
        return creationTime;
    }

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
    private Stamp(final Evidential old) {
        this(old, old.getCreationTime());
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
    public Stamp(final Evidential old, final long time) {
        this(old.getEvidentialBase(), time);
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
    public static Stamp uncheckedMerge(
            final Evidential parent1, final Evidential parent2,
            final long time,
            final int maxEvidenceBaseLength) {
        return new Stamp(
                // * 🚩合并的证据基，拥有新的长度和「父母证据基」各自的成员
                Evidential.mergedEvidentialBase(
                        parent1.getEvidentialBase(), parent2.getEvidentialBase(),
                        maxEvidenceBaseLength),
                // * 🚩新的「创建时间」
                time);
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
    public static Stamp merge(
            final Evidential first, final Evidential second,
            final long time,
            final int maxEvidenceBaseLength) {
        // * 🚩有重合证据⇒返回空；无重合证据⇒合并证据
        return Evidential.haveOverlap(first, second)
                ? null
                : uncheckedMerge(first, second, time, maxEvidenceBaseLength);
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
     * The hash code of Stamp
     *
     * @return The hash code
     */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Get a String form of the Stamp for display
     * Format: {creationTime [: eventTime] : evidentialBase}
     *
     * @return The Stamp as a String
     */
    @Override
    public String toString() {
        // * 🚩直接转发到默认方法
        return this.stampToString();
    }
}
