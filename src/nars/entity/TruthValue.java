package nars.entity;

import nars.io.Symbols;

/**
 * Frequency and confidence.
 * * 📌真值类型：频率 & 信度
 * * 📝此类型接近一种「值类型」：所有值只读、写入时复制/重新构造
 */
public class TruthValue implements Cloneable { // implements Cloneable {

    /**
     * The character that marks the two ends of a truth value
     */
    private static final char DELIMITER = Symbols.TRUTH_VALUE_MARK;
    /**
     * The character that separates the factors in a truth value
     */
    private static final char SEPARATOR = Symbols.VALUE_SEPARATOR;
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
     * * 📌完全参数构造函数
     * * 🚩限制其中的「信度」在[0,1)之间
     *
     * @param f The frequency value
     * @param c The confidence value
     *
     */
    public TruthValue(float f, float c, boolean analytic) {
        // * 🚩逐一赋值
        this.frequency = new ShortFloat(f);
        this.confidence = new ShortFloat(c);
        this.isAnalytic = analytic;
    }

    /**
     * Constructor with a TruthValue to clone
     * * 📌复制构造函数
     *
     * @param v The truth value to be cloned
     */
    public TruthValue(final TruthValue v) {
        this.frequency = v.frequency.clone();
        this.confidence = v.confidence.clone();
        this.isAnalytic = v.isAnalytic;
    }

    /**
     * Get the frequency value
     *
     * @return The frequency value
     */
    public float getFrequency() {
        return frequency.getValue();
    }

    /**
     * Get the confidence value
     *
     * @return The confidence value
     */
    public float getConfidence() {
        return confidence.getValue();
    }

    /**
     * Get the isAnalytic flag
     *
     * @return The isAnalytic value
     */
    public boolean getAnalytic() {
        return isAnalytic;
    }

    /**
     * Calculate the expectation value of the truth value
     * * 📝从0.5开始逐渐逼近其「频率」，信度越大，越接近真实的「频率」
     *
     * @return The expectation value
     */
    public float getExpectation() {
        return (float) (confidence.getValue() * (frequency.getValue() - 0.5) + 0.5);
    }

    /**
     * Calculate the absolute difference of the expectation value and that of a
     * given truth value
     * * ️📝期望绝对差
     *
     * @param t The given value
     * @return The absolute difference
     */
    public float getExpDifAbs(TruthValue t) {
        return Math.abs(getExpectation() - t.getExpectation());
    }

    /**
     * Check if the truth value is negative
     *
     * @return True if the frequency is less than 1/2
     */
    public boolean isNegative() {
        return getFrequency() < 0.5;
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

    /**
     * The String representation of a TruthValue
     *
     * @return The String
     */
    @Override
    public String toString() {
        // * 🚩格式化字符串"%【频率】;【信度】%"，没有`Brief`
        return DELIMITER + frequency.toString() + SEPARATOR + confidence.toString() + DELIMITER;
    }

    /**
     * A simplified String representation of a TruthValue, where each factor is
     * accurate to 1%
     * * 📝保留两位小数
     *
     * @return The String
     */
    public String toStringBrief() {
        // * 🚩格式化字符串"%【频率】;"
        final String s1 = DELIMITER + frequency.toStringBrief() + SEPARATOR;
        // * 🚩准备「信度」字符串：1⇒0.99；其它⇒不变
        final String s2 = confidence.toStringBrief();
        // * 🚩格式化字符串"%【频率】;【信度】%"
        return s1 + s2 + DELIMITER;
    }
}
