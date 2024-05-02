package nars.inference;

import nars.main_nogui.Parameters;

/**
 * Common functions on real numbers, mostly in [0,1].
 */
public class UtilityFunctions {

    /**
     * A function where the output is conjunctively determined by the inputs
     *
     * @param arr The inputs, each in [0, 1]
     * @return The output that is no larger than each input
     */
    public static float and(float... arr) {
        float product = 1;
        for (float f : arr) {
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
    public static float or(float... arr) {
        float product = 1;
        for (float f : arr) {
            product *= (1 - f);
        }
        return 1 - product;
    }

    /**
     * A function where the output is the arithmetic average the inputs
     *
     * @param arr The inputs, each in [0, 1]
     * @return The arithmetic average the inputs
     */
    public static float aveAri(float... arr) {
        float sum = 0;
        for (float f : arr) {
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
    public static float aveGeo(float... arr) {
        float product = 1;
        for (float f : arr) {
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
    public static float w2c(float w) {
        return w / (w + Parameters.HORIZON);
    }

    /**
     * A function to convert confidence to weight
     *
     * @param c confidence, in [0, 1)
     * @return The corresponding weight of evidence, a non-negative real number
     */
    public static float c2w(float c) {
        return Parameters.HORIZON * c / (1 - c);
    }
}
