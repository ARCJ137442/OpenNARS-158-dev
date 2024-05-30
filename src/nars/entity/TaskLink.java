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
    private TaskLink(Task target, String key, BudgetValue budget, short type, short[] indices) {
        super(target, key, budget, type, indices);
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
     * @param v        The budget
     */
    public TaskLink(Task target, TermLink template, BudgetValue v) {
        this(target, "", v,
                template == null ? TermLink.SELF : template.getType(),
                template == null ? null : template.getIndices());

        this.key = generateKey(this.type, this.index); // as defined in TermLink
        if (this.target != null)
            this.key += this.target.getContent();
        this.key += target.getKey();
    }

    /**
     * To check whether a TaskLink should use a TermLink, return false if they
     * interacted recently
     * <p>
     * called in TermLinkBag only
     *
     * @param termLink    The TermLink to be checked
     * @param currentTime The current time
     * @return Whether they are novel to each other
     */
    public boolean novel(TermLink termLink, long currentTime) {
        Term bTerm = termLink.getTarget();
        if (bTerm.equals(target.getSentence().getContent())) {
            return false;
        }
        String linkKey = termLink.getKey();
        int next, i;
        for (i = 0; i < counter; i++) {
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
        next = i % Parameters.TERM_LINK_RECORD_LENGTH;
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
