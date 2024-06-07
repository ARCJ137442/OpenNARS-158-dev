package nars.storage;

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
    public TaskLinkBag(Memory memory) {
        super(memory);
    }

    /**
     * Get the (constant) capacity of TaskLinkBag
     *
     * @return The capacity of TaskLinkBag
     */
    protected int capacity() {
        return Parameters.TASK_LINK_BAG_SIZE;
    }

    /**
     * Get the (adjustable) forget rate of TaskLinkBag
     *
     * @return The forget rate of TaskLinkBag
     */
    protected int forgetRate() {
        return memory.getTaskForgettingRate().get();
    }
}
