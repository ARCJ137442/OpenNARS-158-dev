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