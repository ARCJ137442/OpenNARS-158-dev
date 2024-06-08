package nars.storage;

import java.util.concurrent.atomic.AtomicInteger;

import nars.entity.Task;
import nars.main.Parameters;

/**
 * New tasks that contain new Term.
 */
public class NovelTaskBag extends Bag<Task> {

    /**
     * Constructor
     */
    public NovelTaskBag(AtomicInteger forgetRate) {
        super(forgetRate, Parameters.TASK_BUFFER_SIZE);
    }
}
