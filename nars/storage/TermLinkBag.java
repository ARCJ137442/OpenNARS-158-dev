/*
 * TermLinkBag.java
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
package nars.storage;

import nars.entity.*;
import nars.gui.MainWindow;
import nars.main.Parameters;

/**
 * Contains TermLinks to relevant (compound or component) Terms.
 */
public class TermLinkBag extends Bag<TermLink> {
    /**
     * Get the (constant) capacity of TermLinkBag
     * @return The capacity of TermLinkBag
     */
    protected int capacity() {
        return Parameters.TERM_LINK_BAG_SIZE;
    }

    /**
     * Get the (adjustable) forget rate of TermLinkBag
     * @return The forget rate of TermLinkBag
     */
    protected int forgetRate() {
        return MainWindow.forgetBW.value();
    }

    /**
     * Replace defualt to prevent repeated inference, by checking TaskLink
     * @param taskLink The selected TaskLink
     * @return The selected TermLink
     */
    public TermLink takeOut(TaskLink taskLink) {
        for (int i = 0; i < Parameters.MAX_MATCHED_TERM_LINK; i++) {
            TermLink termLink = takeOut();
            if (termLink == null) {
                return null;
            }
            if (taskLink.novel(termLink)) {
                return termLink;
            }
            putBack(termLink);
        }
        return null;
    }
}

