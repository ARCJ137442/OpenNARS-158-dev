package nars.entity;

import nars.language.Term;
import nars.main_nogui.Parameters;

/**
 * Reference to a Task.
 * <p>
 * The reason to separate a Task and a TaskLink is that the same Task can be
 * linked from multiple Concepts, with different BudgetValue.
 */
public class TaskLink extends TermLink {

    /**
     * The Task linked. The "target" field in TermLink is not used here.
     */
    private Task targetTask;
    /**
     * Remember the TermLinks that has been used recently with this TaskLink
     */
    private String recordedLinks[];
    /**
     * Remember the time when each TermLink is used with this TaskLink
     */
    private long recordingTime[];
    /**
     * The number of TermLinks remembered
     */
    int counter;

    /**
     * Constructor
     * <p>
     * only called in Memory.continuedProcess
     *
     * @param t        The target Task
     * @param template The TermLink template
     * @param v        The budget
     */
    public TaskLink(Task t, TermLink template, BudgetValue v) {
        super("", v);
        targetTask = t;
        if (template == null) {
            type = TermLink.SELF;
            index = null;
        } else {
            type = template.getType();
            index = template.getIndices();
        }
        recordedLinks = new String[Parameters.TERM_LINK_RECORD_LENGTH];
        recordingTime = new long[Parameters.TERM_LINK_RECORD_LENGTH];
        counter = 0;
        setKey(); // as defined in TermLink
        key += t.getKey();
    }

    /**
     * Get the target Task
     *
     * @return The linked Task
     */
    public Task getTargetTask() {
        return targetTask;
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
        if (bTerm.equals(targetTask.getSentence().getContent())) {
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
        return super.toString() + " " + getTargetTask().getSentence().getStamp();
    }
}
