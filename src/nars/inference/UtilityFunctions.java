package nars.inference;

import nars.main_nogui.Parameters;

/**
 * Common functions on real numbers, mostly in [0,1].
 */
public class UtilityFunctions {

    /**
     * 🆕扩展逻辑非
     *
     * @param value
     * @return
     */
    public static final float not(final float value) {
        return 1 - value;
    }

    /**
     * A function where the output is conjunctively determined by the inputs
     *
     * @param arr The inputs, each in [0, 1]
     * @return The output that is no larger than each input
     */
    public static final float and(final float... arr) {
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
    public static final float or(final float... arr) {
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
    public static final float aveAri(final float... arr) {
        float sum = 0;
        for (final float f : arr) {
            sum += f;
        }
        return sum / arr.length;
    }

    /**
     * A function where the output is the geometric average the inputs
     *
     * @param arr The inputs, each in [0, 1]
     * @return The geometric average the inputs
     */
    public static final float aveGeo(final float... arr) {
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
    public static final float w2c(final float w) {
        return w / (w + Parameters.HORIZON);
    }

    /**
     * A function to convert confidence to weight
     *
     * @param c confidence, in [0, 1)
     * @return The corresponding weight of evidence, a non-negative real number
     */
    public static final float c2w(float c) {
        return Parameters.HORIZON * c / (1 - c);
    }
}
