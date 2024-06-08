package nars.control;

/**
 * The type of report.
 * * 🚩【2024-04-19 12:44:36】增加了多种输出方式
 */
public enum ReportType {
    IN,
    OUT,
    ANSWER,
    EXE;

    /**
     * 将报告类型转换为字符串
     * * 📝Java在枚举的开头用一个语句定义所有枚举项
     *
     * @param type 报告类型
     * @return 字符串（仅名称）
     */
    @Override
    public String toString() {
        switch (this) {
            case IN:
                return "IN";
            case OUT:
                return "OUT";
            case ANSWER:
                return "ANSWER";
            case EXE:
                return "EXE";
            default: // * 穷举后不会发生
                return "OTHER";
        }
    }
}