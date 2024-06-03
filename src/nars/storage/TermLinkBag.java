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
     * * 📌特殊的「根据任务链拿出词项链（信念链）」
     * * 🎯在「概念推理」的「准备待推理词项链」的过程中用到
     * * 🔗ProcessReason.chooseTermLinksToReason
     *
     * @param taskLink The selected TaskLink
     * @param time     The current time
     * @return The selected TermLink
     */
    public TermLink takeOutFromTaskLink(TaskLink taskLink, long time) {
        for (int i = 0; i < Parameters.MAX_MATCHED_TERM_LINK; i++) {
            // * 🚩尝试拿出词项链 | 📝此间存在资源竞争
            final TermLink termLink = this.takeOut();
            if (termLink == null)
                return null;
            // * 🚩任务链相对词项链「新近」⇒直接返回
            if (taskLink.novel(termLink, time))
                return termLink;
            // * 🚩当即放回
            putBack(termLink);
        }
        return null;
    }
}
