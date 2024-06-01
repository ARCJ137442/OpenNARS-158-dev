package util;

/**
 * 🆕在输入输出/内部表征 方面，允许提供更长或更简略的字符串表示形式
 * * 📜默认：均使用{@link Object#toString}
 */
public interface ToStringBriefAndLong {

    /**
     * Return a String representation of the Item
     * * 🚩强制要求实现（并覆盖）{@link Object#toString}
     * ! ❌无法覆盖{@link Object#toString}：A default method cannot override a method from
     * java.lang.Object Java(67109915)
     *
     * @return The String representation of the full content
     */
    public String toString();

    /**
     * Return a String representation of the instance after simplification
     *
     * @return A simplified String representation of the content
     */
    public default String toStringBrief() {
        return this.toString();
    }

    public default String toStringLong() {
        return this.toString();
    }
}
