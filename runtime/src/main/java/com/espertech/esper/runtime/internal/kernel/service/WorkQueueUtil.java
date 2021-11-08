/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.runtime.internal.kernel.service;

import java.util.ArrayList;
import java.util.Collections;

public class WorkQueueUtil {
    public static int insert(WorkQueueItemPrecedenced item, ArrayList<WorkQueueItemPrecedenced> queue) {
        int insertionIndex = Collections.binarySearch(queue, item);
        if (insertionIndex < 0) {
            insertionIndex = -(insertionIndex + 1);
        } else {
            insertionIndex++;
        }

        // bump insertion index to get to last same-precedence item
        while (insertionIndex < queue.size()) {
            WorkQueueItemPrecedenced atInsert = queue.get(insertionIndex);
            if (atInsert.getPrecedence() == item.getPrecedence()) {
                insertionIndex++;
            } else {
                break;
            }
        }

        if (insertionIndex >= queue.size()) {
            queue.add(item);
        } else {
            queue.add(insertionIndex, item);
        }
        return insertionIndex;
    }
}
