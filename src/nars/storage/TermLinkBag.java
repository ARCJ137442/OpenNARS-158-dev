package nars.storage;

import nars.entity.*;
import nars.main_nogui.Parameters;

/**
 * Contains TermLinks to relevant (compound or component) Terms.
 */
public class TermLinkBag extends Bag<TermLink> {

    /**
     * Constructor
     *
     * @param memory The reference of memory
     */
    public TermLinkBag(Memory memory) {
        super(memory);
    }

    /**
     * Get the (constant) capacity of TermLinkBag
     *
     * @return The capacity of TermLinkBag
     */
    protected int capacity() {
        return Parameters.TERM_LINK_BAG_SIZE;
    }

    /**
     * Get the (adjustable) forget rate of TermLinkBag
     *
     * @return The forget rate of TermLinkBag
     */
    protected int forgetRate() {
        return memory.getBeliefForgettingRate().get();
    }

    /**
     * Replace default to prevent repeated inference, by checking TaskLink
     *
     * @param taskLink The selected TaskLink
     * @param time     The current time
     * @return The selected TermLink
     */
    public TermLink takeOut(TaskLink taskLink, long time) {
        for (int i = 0; i < Parameters.MAX_MATCHED_TERM_LINK; i++) {
            final TermLink termLink = takeOut();
            if (termLink == null) {
                return null;
            }
            if (taskLink.novel(termLink, time)) {
                return termLink;
            }
            putBack(termLink);
        }
        return null;
    }
}
