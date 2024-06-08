package nars.inference;

import nars.entity.ShortFloat;
import nars.io.Symbols;

/**
 * Frequency and __confidence().
 * * 📌真值类型：频率 & 信度
 * * 📝此类型接近一种「值类型」：所有值只读、写入时复制/重新构造
 */
public interface Truth extends Cloneable {

    /**
     * The frequency factor of the truth value
     *
     * * ️📝可空性：非空
     * * 📝可变性：不变 | 仅构造时，无需可变
     * * 📝所有权：具所有权
     */
    ShortFloat __frequency();

    /**
     * The confidence factor of the truth value
     *
     * * ️📝可空性：非空
     * * 📝可变性：不变 | 仅构造时，无需可变
     * * 📝所有权：具所有权
     */
    ShortFloat __confidence();

    /**
     * Whether the truth value is derived from a definition
     *
     * * ️📝可空性：非空
     * * 📝可变性：不变 | 仅构造时，无需可变
     * * 📝所有权：具所有权
     */
    boolean __isAnalytic();

    /**
     * Get the frequency value
     *
     * @return The frequency value
     */
    public default float getFrequency() {
        return __frequency().getValue();
    }

    /**
     * Get the confidence value
     *
     * @return The confidence value
     */
    public default float getConfidence() {
        return __confidence().getValue();
    }

    /**
     * Get the isAnalytic flag
     *
     * @return The isAnalytic value
     */
    public default boolean getAnalytic() {
        return __isAnalytic();
    }

    /**
     * Calculate the expectation value of the truth value
     * * 📝从0.5开始逐渐逼近其「频率」，信度越大，越接近真实的「频率」
     *
     * @return The expectation value
     */
    public default float getExpectation() {
        return (float) (__confidence().getValue() * (__frequency().getValue() - 0.5) + 0.5);
    }

    /**
     * Calculate the absolute difference of the expectation value and that of a
     * given truth value
     * * ️📝期望绝对差
     *
     * @param t The given value
     * @return The absolute difference
     */
    public default float getExpDifAbs(Truth t) {
        return Math.abs(getExpectation() - t.getExpectation());
    }

    /**
     * Check if the truth value is negative
     *
     * @return True if the frequency is less than 1/2
     */
    public default boolean isNegative() {
        return getFrequency() < 0.5;
    }

    /**
     * The character that marks the two ends of a truth value
     */
    static final char DELIMITER = Symbols.TRUTH_VALUE_MARK;
    /**
     * The character that separates the factors in a truth value
     */
    static final char SEPARATOR = Symbols.VALUE_SEPARATOR;

    /**
     * The String representation of a TruthValue
     *
     * @return The String
     */
    public default String truthToString() {
        // * 🚩格式化字符串"%【频率】;【信度】%"，没有`Brief`
        return DELIMITER + __frequency().toString() + SEPARATOR + __confidence().toString() + DELIMITER;
    }

    /**
     * A simplified String representation of a TruthValue, where each factor is
     * accurate to 1%
     * * 📝保留两位小数
     *
     * @return The String
     */
    public default String truthToStringBrief() {
        // * 🚩格式化字符串"%【频率】;"
        final String s1 = DELIMITER + __frequency().toStringBrief() + SEPARATOR;
        // * 🚩准备「信度」字符串：1⇒0.99；其它⇒不变
        final String s2 = __confidence().toStringBrief();
        // * 🚩格式化字符串"%【频率】;【信度】%"
        return s1 + s2 + DELIMITER;
    }
}
