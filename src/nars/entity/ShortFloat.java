package nars.entity;

/**
 * A float value in [0, 1], with 4 digits accuracy.
 * * 📝短浮点，以整数绝对精确地存储十进制四位浮点
 */
public class ShortFloat implements Cloneable {

    /**
     * To save space, the values are stored as short integers
     * (-32768 to 32767, only 0 to 10000 used),
     * but used as float
     *
     * * ️📝可空性：非空
     * * 📝可变性：可变 | 需要在「预算值」中被修改
     * * 📝所有权：具所有权
     */
    private short value;

    /**
     * Constructor
     * * 🚩复制构造函数
     *
     * @param v The initial value
     */
    private ShortFloat(final short v) {
        this.value = v;
    }

    /**
     * Constructor
     * * 🚩从浮点数构造
     *
     * @param v The initial value in float
     */
    public ShortFloat(final float v) {
        this.setValue(v);
    }

    /**
     * To access the value as float
     * * 🚩获取值，通过乘法转换为单精度浮点数
     *
     * @return The current value in float
     */
    public float getValue() {
        return value * 0.0001f;
    }

    // /**
    // * To access the value as short
    // * * 🚩直接获得其内部存储的「短整数」
    // * * 🚩【2024-06-03 10:37:00】目前直接内联，不再需要此方法
    // *
    // * @return The current value in short
    // */
    // private short getShortValue() {
    // return value;
    // }

    /**
     * Set new value, rounded, with validity checking
     * * 🚩取整方式：+0.5，然后向下取整 → 四舍五入
     *
     * @param v The new value
     */
    public final void setValue(float v) {
        // * 🚩在范围内⇒转换为短整数并赋值
        if (0 <= v && v <= 1)
            value = (short) (v * 10000.0 + 0.5);
        // * 🚩在范围外⇒报错
        else
            throw new Error("Invalid value: " + v);
    }

    /**
     * Compare two ShortFloat values
     *
     * @param that The other value to be compared
     * @return Whether the two have the same value
     */
    @Override
    public boolean equals(Object that) {
        return ((that instanceof ShortFloat) && (value == ((ShortFloat) that).value));
    }

    /**
     * The hash code of the ShortFloat
     *
     * @return The hash code
     */
    @Override
    public int hashCode() {
        return this.value + 17;
    }

    /**
     * To create an identical copy of the ShortFloat
     *
     * @return A cloned ShortFloat
     */
    @Override
    public ShortFloat clone() {
        return new ShortFloat(value);
    }

    /**
     * Convert the value into a String
     * * 🚩四位浮点数
     *
     * @return The String representation, with 4 digits accuracy
     */
    @Override
    public String toString() {
        // * 🚩大于1 ⇒ 补足「1」
        if (value >= 10000)
            return "1.0000";
        // * 🚩否则 ⇒ 补足0的四位浮点
        String s = String.valueOf(value);
        while (s.length() < 4) {
            s = "0" + s;
        }
        return "0." + s;
    }

    /**
     * Round the value into a short String
     *
     * @return The String representation, with 2 digits accuracy
     */
    public String toStringBrief() {
        // * 🚩手动四舍五入
        value += 50;
        final String s = toString();
        value -= 50;
        // * 🚩`0.xx`削减
        return s.length() > 4 ? s.substring(0, 4) : s;
    }
}
