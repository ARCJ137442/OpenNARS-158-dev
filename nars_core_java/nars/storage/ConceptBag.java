/*
 * The MIT License
 *
 * Copyright 2019 The OpenNARS authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package nars.storage;

import nars.entity.Concept;
import nars.main_nogui.Parameters;

/**
 * Contains Concepts.
 */
public class ConceptBag extends Bag<Concept> {
    /**
     * Constructor
     *
     * @param memory The reference of memory
     */
    public ConceptBag(Memory memory) {
        super(memory);
    }

    /**
     *
     * Get the (constant) capacity of ConceptBag
     *
     * @return The capacity of ConceptBag
     */
    @Override
    protected int capacity() {
        return Parameters.CONCEPT_BAG_SIZE;
    }

    /**
     * Get the (adjustable) forget rate of ConceptBag
     *
     * @return The forget rate of ConceptBag
     */
    @Override
    protected int forgetRate() {
        return memory.getConceptForgettingRate().get();
    }
}