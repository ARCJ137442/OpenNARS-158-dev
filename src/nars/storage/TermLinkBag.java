package nars.storage;

import java.util.concurrent.atomic.AtomicInteger;

import nars.entity.*;
import nars.main.Parameters;

/**
 * Contains TermLinks to relevant (compound or component) Terms.
 */
public class TermLinkBag extends Bag<TermLink> {

    /**
     * Constructor
     */
    public TermLinkBag(AtomicInteger forgetRate) {
        super(forgetRate, Parameters.TERM_LINK_BAG_SIZE);
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
