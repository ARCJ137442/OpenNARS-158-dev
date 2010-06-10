/*
 * TaskLink.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.entity;

import java.util.*;

import nars.language.Term;
import nars.main.*;

/**
 * Reference to a Task.
 * <p>
 * The reason to separate a Task and a TaskLink is that the same Task can be linked from 
 * multiple Concepts, with different BudgetValue.
 */
public class TaskLink extends TermLink {

    /** The Task linked. The "target" field in TermLink is not used here. */
    private Task targetTask;
    /** Remember the TermLinks that has been used recently with this TaskLink */
    private String recordedLinks[];
    private long recordingTime[];
    int counter, first;

    /**
     * Constructor
     * <p>
     * only called in Memory.continuedProcess
     * @param t The target Task
     * @param template The TermLink template
     * @param v The budget
     */
    public TaskLink(Task t, TermLink template, BudgetValue v) {
        super(v);
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
        first = 0;
        setKey();   // as defined in TermLink
        key += t.getKey();
    }

    /**
     * Get the target Task
     * @return The linked Task
     */
    public Task getTargetTask() {
        return targetTask;
    }

    /**
     * Get the TermLink recordedLinks
     * @return The list of TermLinks recently used
     */
    String[] getRecordedLinks() {
        return recordedLinks;
    }

    long[] getRecordingTime() {
        return recordingTime;
    }

    int getCounter() {
        return counter;
    }

    int getFirst() {
        return first;
    }

    /**
     * To check whether a TaskLink should use a TermLink, return false if they 
     * interacted recently
     * <p>
     * called in TermLinkBag only
     * @param termLink The TermLink to be checked
     * @return Whether they are novel to each other
     */
    public boolean novel(TermLink termLink) {
        Term bTerm = termLink.getTarget();
        if (bTerm.equals(targetTask.getSentence().getContent())) {
            return false;
        }
        String linkKey = termLink.getKey();
        int next = 0;
        int i;
        for (i = 0; i < counter; i++) {
            next = (i + first) % Parameters.TERM_LINK_RECORD_LENGTH;
            if (linkKey.equals(recordedLinks[next])) {
                if (Center.getTime() < recordingTime[next] + Parameters.TERM_LINK_RECORD_LENGTH) {
                    return false;
                } else {
                    recordingTime[next] = Center.getTime();
                    return true;
                }
            }
        }
        next = (i + first) % Parameters.TERM_LINK_RECORD_LENGTH;
        recordedLinks[next] = linkKey;       // add knowledge reference to recordedLinks
        recordingTime[next] = Center.getTime();
        if (counter < Parameters.TERM_LINK_RECORD_LENGTH) { // keep a constant length
            counter++;
        }
        return true;
    }
}

