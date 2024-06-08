package nars.entity;

import nars.inference.Truth;

/**
 * Frequency and confidence.
 * * 📌真值类型：频率 & 信度
 * * 📝此类型接近一种「值类型」：所有值只读、写入时复制/重新构造
 */
public class TruthValue implements Truth {
    /**
     * The frequency factor of the truth value
     *
     * * ️📝可空性：非空
     * * 📝可变性：不变 | 仅构造时，无需可变
     * * 📝所有权：具所有权
     */
    private final ShortFloat frequency;
    /**
     * The confidence factor of the truth value
     *
     * * ️📝可空性：非空
     * * 📝可变性：不变 | 仅构造时，无需可变
     * * 📝所有权：具所有权
     */
    private final ShortFloat confidence;
    /**
     * Whether the truth value is derived from a definition
     *
     * * ️📝可空性：非空
     * * 📝可变性：不变 | 仅构造时，无需可变
     * * 📝所有权：具所有权
     */
    private final boolean isAnalytic;

    @Override
    public ShortFloat __frequency() {
        return this.frequency;
    }

    @Override
    public ShortFloat __confidence() {
        return this.confidence;
    }

    @Override
    public boolean __isAnalytic() {
        return this.isAnalytic;
    }

    /** 🆕完全参数构造函数 */
    private TruthValue(ShortFloat f, ShortFloat c, boolean analytic) {
        this.frequency = f;
        this.confidence = c;
        this.isAnalytic = analytic;
    }

    /**
     * Constructor with two ShortFloats
     * * 🚩默认是「非分析性的」
     *
     * @param f The frequency value
     * @param c The confidence value
     */
    public TruthValue(float f, float c) {
        this(f, c, false);
    }

    /**
     * Constructor with two ShortFloats
     * * 🚩限制其中的「信度」在[0,1)之间
     *
     * @param f The frequency value
     * @param c The confidence value
     *
     */
    public TruthValue(float f, float c, boolean analytic) {
        // * 🚩构造 & 重定向
        this(new ShortFloat(f), new ShortFloat(c), analytic);
    }

    /**
     * Constructor with a TruthValue to clone
     * * 📌复制构造函数
     *
     * @param v The truth value to be cloned
     */
    protected TruthValue(final Truth v) {
        this(v.__frequency().clone(), v.__confidence().clone(), v.__isAnalytic());
    }

    /** 🎯兼容null的构造函数 */
    public static TruthValue from(Truth v) {
        if (v instanceof Sentence)
            // ! 📌【2024-06-07 16:13:34】有可能源自「语句」然后「语句非空但无真值」
            return TruthValue.from(((Sentence) v).__truth());
        return v == null ? null : new TruthValue(v);
    }

    /**
     * Compare two truth values
     *
     * @param that The other TruthValue
     * @return Whether the two are equivalent
     */
    @Override
    public boolean equals(Object that) {
        return ((that instanceof TruthValue)
                // * 🚩【2024-06-03 08:41:50】弃用浮点判等，转为短浮点判等
                && (frequency.equals(((TruthValue) that).frequency))
                && (confidence.equals(((TruthValue) that).confidence)));
    }

    /**
     * The hash code of a TruthValue
     *
     * @return The hash code
     */
    @Override
    public int hashCode() {
        return (int) (getExpectation() * 37);
    }

    @Override
    public TruthValue clone() {
        return new TruthValue(this);
    }

    @Override
    public String toString() {
        return this.truthToString();
    }

    public String toStringBrief() {
        return this.truthToStringBrief();
    }
}
