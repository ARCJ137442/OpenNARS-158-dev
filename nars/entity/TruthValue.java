/*
 * TruthValue.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.entity;

import nars.io.Symbols;

/**
 * Frequency and confidence.
 */
public class TruthValue { // implements Cloneable {
    /** The charactor that marks the two ends of a truth value */
    private static final char DELIMITER = Symbols.TRUTH_VALUE_MARK;
    /** The charactor that separates the factors in a truth value */
    private static final char SEPARATOR = Symbols.VALUE_SEPARATOR;
    /** The frequency factor of the truth value */
    private ShortFloat frequency;
    /** The confidence factor of the truth value */
    private ShortFloat confidence;

    /**
     * Constructor with two ShortFloats
     * @param f The frequency value
     * @param c The confidence value
     */
    public TruthValue(float f, float c) {
        frequency = new ShortFloat(f);
        confidence = (c < 1) ? new ShortFloat(c) : new ShortFloat(0.9999f);
    }

    /**
     * Constructor with a TruthValue to clone
     * @param v The truth value to be cloned
     */
    public TruthValue(TruthValue v) {
        frequency = new ShortFloat(v.getFrequency());
        confidence = new ShortFloat(v.getConfidence());
    }

    /** 
     * Get the frequency value
     * @return The frequency value
     */
    public float getFrequency() {
        return frequency.getValue();
    }

    /** 
     * Get the confidence value
     * @return The confidence value
     */
    public float getConfidence() {
        return confidence.getValue();
    }

    /** 
     * Calculate the expectation value of the truth value
     * @return The expectation value
     */
    public float getExpectation() {
        return (float) (confidence.getValue() * (frequency.getValue() - 0.5) + 0.5);
    }

    /** 
     * Calculate the absolute difference of the expectation value and that of a given truth value
     * @param t The given value
     * @return The absolute difference
     */
    public float getExpDifAbs(TruthValue t) {
        return Math.abs(getExpectation() - t.getExpectation());
    }

    /**
     * Compare two truth values
     * @param that The other TruthValue
     * @return Whether the two are equivalent
     */
    @Override
    public boolean equals(Object that) {
        return ((that instanceof TruthValue) 
                && (getFrequency() == ((TruthValue) that).getFrequency()) 
                && (getConfidence() == ((TruthValue) that).getConfidence()));
    }

    /**
     * The hash code of a TruthValue
     * @return The hash code
     */
    @Override
    public int hashCode() {
        int hash = 5;
        return hash;
    }

    /**
     * The String representation of a TruthValue
     * @return The String
     */
    @Override
    public String toString() {
        return DELIMITER + frequency.toString() + SEPARATOR + confidence.toString() + DELIMITER;
    }

    /**
     * A simplified String representation of a TruthValue, where each factor is accruate to 1%
     * @return The String
     */
    public String toString2() {
        String s1 = DELIMITER + frequency.toString2() + SEPARATOR;
        String s2 = confidence.toString2();
        if (s2.equals("1.00")) {
            return s1 + "0.99" + DELIMITER;
        } else {
            return s1 + s2 + DELIMITER;
        }
    }
}
