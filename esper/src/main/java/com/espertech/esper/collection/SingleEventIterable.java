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
package com.espertech.esper.collection;

import com.espertech.esper.client.EventBean;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

public class SingleEventIterable implements Iterable<EventBean> {
    private final AtomicReference<EventBean> ref;

    public SingleEventIterable(AtomicReference<EventBean> ref) {
        this.ref = ref;
    }

    public Iterator<EventBean> iterator() {
        return new SingleEventIterator(ref.get());
    }
}
