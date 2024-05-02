package nars.storage;

import nars.entity.Task;
import nars.main_nogui.Parameters;

/**
 * New tasks that contain new Term.
 */
public class NovelTaskBag extends Bag<Task> {

    /**
     * Constructor
     *
     * @param memory The reference of memory
     */
    public NovelTaskBag(Memory memory) {
        super(memory);
    }

    /**
     * Get the (constant) capacity of NovelTaskBag
     *
     * @return The capacity of NovelTaskBag
     */
    protected int capacity() {
        return Parameters.TASK_BUFFER_SIZE;
    }

    /**
     * Get the (constant) forget rate in NovelTaskBag
     *
     * @return The forget rate in NovelTaskBag
     */
    protected int forgetRate() {
        return Parameters.NEW_TASK_FORGETTING_CYCLE;
    }
}
