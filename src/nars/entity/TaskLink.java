package nars.entity;

import nars.language.Term;
import nars.main_nogui.Parameters;

/**
 * Reference to a Task.
 * <p>
 * The reason to separate a Task and a TaskLink is that the same Task can be
 * linked from multiple Concepts, with different BudgetValue.
 */
public class TaskLink extends TLink<Task> {

    /**
     * Remember the TermLinks that has been used recently with this TaskLink
     */
    private final String recordedLinks[];
    /**
     * Remember the time when each TermLink is used with this TaskLink
     */
    private final long recordingTime[];
    /**
     * The number of TermLinks remembered
     */
    private int counter;

    /**
     * 🆕完全构造函数
     *
     * @param t
     * @param template
     * @param v
     */
    private TaskLink(final Task target, final BudgetValue budget, final short type, final short[] indices) {
        super(target, generateKey(target, type, indices), budget, type, indices);
        this.recordedLinks = new String[Parameters.TERM_LINK_RECORD_LENGTH];
        this.recordingTime = new long[Parameters.TERM_LINK_RECORD_LENGTH];
        this.counter = 0;
    }

    /**
     * Constructor
     * <p>
     * only called in Memory.continuedProcess
     * * 📝【2024-05-30 00:46:38】只在「链接概念到任务」中使用
     *
     * @param target   The target Task
     * @param template The TermLink template
     * @param budget   The budget
     */
    public TaskLink(final Task target, final TermLink template, final BudgetValue budget) {
        this(target, budget,
                template == null ? TermLink.SELF : template.getType(),
                template == null ? null : template.getIndices());

    }

    private static final String generateKey(final Task target, final short type, final short[] indices) {
        String key = generateKey(type, indices); // as defined in TermLink
        if (target != null)
            key += target.getContent();
        key += target.getKey();
        return key;
    }

    /**
     * To check whether a TaskLink should use a TermLink, return false if they
     * interacted recently
     * <p>
     * called in TermLinkBag only
     * * 📝在「概念推理」的「准备待推理词项链」的过程中用到
     * * 🔗ProcessReason.chooseTermLinksToReason
     * * TODO: 有待笔记注释
     *
     * @param termLink    The TermLink to be checked
     * @param currentTime The current time
     * @return Whether they are novel to each other
     */
    public boolean novel(final TermLink termLink, final long currentTime) {
        final Term bTerm = termLink.getTarget();
        if (bTerm.equals(target.getSentence().getContent())) {
            return false;
        }
        final String linkKey = termLink.getKey();
        int next;
        for (int i = 0; i < counter; i++) {
            next = i % Parameters.TERM_LINK_RECORD_LENGTH;
            if (linkKey.equals(recordedLinks[next])) {
                if (currentTime < recordingTime[next] + Parameters.TERM_LINK_RECORD_LENGTH) {
                    return false;
                } else {
                    recordingTime[next] = currentTime;
                    return true;
                }
            }
        }
        // * 📝此处`i`必定为`counter`
        next = counter % Parameters.TERM_LINK_RECORD_LENGTH;
        recordedLinks[next] = linkKey; // add knowledge reference to recordedLinks
        recordingTime[next] = currentTime;
        if (counter < Parameters.TERM_LINK_RECORD_LENGTH) { // keep a constant length
            counter++;
        }
        return true;
    }

    @Override
    public String toString() {
        return super.toString() + " " + getTarget().getSentence().getStamp();
    }
}
