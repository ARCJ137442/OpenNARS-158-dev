package nars.inference;

import nars.main_nogui.Parameters;

/**
 * Common functions on real numbers, mostly in [0,1].
 * * 📌【2024-06-07 13:15:14】暂时还不能封闭：「预算值」和「概念」需要用到
 */
class UtilityFunctions {

    /**
     * 🆕扩展逻辑非
     *
     * @param value
     * @return
     */
    static final float not(final float value) {
        return 1 - value;
    }

    /**
     * A function where the output is conjunctively determined by the inputs
     *
     * @param arr The inputs, each in [0, 1]
     * @return The output that is no larger than each input
     */
    static final float and(final float... arr) {
        float product = 1;
        for (final float f : arr) {
            product *= f;
        }
        return product;
    }

    /**
     * A function where the output is disjunctively determined by the inputs
     *
     * @param arr The inputs, each in [0, 1]
     * @return The output that is no smaller than each input
     */
    static final float or(final float... arr) {
        float product = 1;
        for (final float f : arr) {
            product *= not(f);
        }
        return not(product);
    }

    /**
     * A function where the output is the arithmetic average the inputs
     *
     * @param arr The inputs, each in [0, 1]
     * @return The arithmetic average the inputs
     */
    static final float aveAri(final float... arr) {
        float sum = 0;
        for (final float f : arr) {
            sum += f;
        }
        return sum / arr.length;
    }

    /** 🆕特别优化 */
    static final float aveAri(final float f) {
        return f;
    }

    /** 🆕特别优化 */
    static final float aveAri(final float f1, final float f2) {
        return (f1 + f2) / 2;
    }

    /**
     * A function where the output is the geometric average the inputs
     *
     * @param arr The inputs, each in [0, 1]
     * @return The geometric average the inputs
     */
    static final float aveGeo(final float... arr) {
        float product = 1;
        for (final float f : arr) {
            product *= f;
        }
        return (float) Math.pow(product, 1.00 / arr.length);
    }

    /**
     * A function to convert weight to confidence
     *
     * @param w Weight of evidence, a non-negative real number
     * @return The corresponding confidence, in [0, 1)
     */
    static final float w2c(final float w) {
        return w / (w + Parameters.HORIZON);
    }

    /**
     * A function to convert confidence to weight
     *
     * @param c confidence, in [0, 1)
     * @return The corresponding weight of evidence, a non-negative real number
     */
    static final float c2w(float c) {
        return Parameters.HORIZON * c / (1 - c);
    }
}
