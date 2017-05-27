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
package com.espertech.esper.supportregression.multithread;

import com.espertech.esper.dispatch.Dispatchable;

/**
 * DF3   &lt;--&gt;   DF2  &lt;--&gt;  DF1
 * <p>
 * DF1 completes: set DF2.earlier = null, notify DF2
 */
public class DispatchFuture implements Dispatchable {
    private UpdateDispatchViewModel view;
    private DispatchFuture earlier;
    private DispatchFuture later;
    private transient boolean isCompleted;

    public DispatchFuture(UpdateDispatchViewModel view, DispatchFuture earlier) {
        this.view = view;
        this.earlier = earlier;
    }

    public DispatchFuture() {
        isCompleted = true;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setLater(DispatchFuture later) {
        this.later = later;
    }

    public void execute() {
        while (!earlier.isCompleted) {
            synchronized (this) {
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }

        view.execute();
        isCompleted = true;

        if (later != null) {
            synchronized (later) {
                later.notify();
            }
        }
        earlier = null;
        later = null;
    }
}
