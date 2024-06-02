package nars.io;

/**
 * 🆕在输入输出/内部表征 方面，允许提供更长或更简略的字符串表示形式
 * * 📜默认：均使用{@link Object#toString}
 * * 🚩【2024-06-01 16:53:32】暂且弃用：在「语句」「任务」等处尚未完全理清调用关系
 */
public interface ToStringBriefAndLong {

    /**
     * Return a String representation of the Item
     * * 🚩强制要求实现（并覆盖）{@link Object#toString}
     * ! ❌无法覆盖{@link Object#toString}：A default method cannot override a method
     * from
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
    public String toStringBrief();
    // public default String toStringBrief() {
    // return this.toString();
    // }

    public String toStringLong();
    // public default String toStringLong() {
    // return this.toString();
    // }
}
