package nars.storage;

import java.util.concurrent.atomic.AtomicInteger;

import nars.entity.TaskLink;
import nars.main.Parameters;

/**
 * TaskLinkBag contains links to tasks.
 */
public class TaskLinkBag extends Bag<TaskLink> {

    /**
     * Constructor
     *
     * @param memory The reference of memory
     */
    public TaskLinkBag(AtomicInteger forgetRate) {
        super(forgetRate, Parameters.TASK_LINK_BAG_SIZE);
    }
}
