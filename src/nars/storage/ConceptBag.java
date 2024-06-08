package nars.storage;

import java.util.concurrent.atomic.AtomicInteger;

import nars.entity.Concept;
import nars.main.Parameters;

/**
 * Contains Concepts.
 */
public class ConceptBag extends Bag<Concept> {
    /**
     * Constructor
     */
    public ConceptBag(AtomicInteger forgetRate) {
        super(forgetRate, Parameters.CONCEPT_BAG_SIZE);
    }
}