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
package com.espertech.esper.regressionlib.support.extend.pattern;

import com.espertech.esper.common.internal.epl.pattern.observer.EventObserver;
import com.espertech.esper.common.internal.epl.pattern.observer.EventObserverVisitor;
import com.espertech.esper.common.internal.epl.pattern.observer.ObserverEventEvaluator;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;

import java.io.File;

public class MyFileExistsObserver implements EventObserver {
    private final MatchedEventMap beginState;
    private final ObserverEventEvaluator observerEventEvaluator;
    private final String filename;

    public MyFileExistsObserver(MatchedEventMap beginState, ObserverEventEvaluator observerEventEvaluator, String filename) {
        this.beginState = beginState;
        this.observerEventEvaluator = observerEventEvaluator;
        this.filename = filename;
    }

    public MatchedEventMap getBeginState() {
        return beginState;
    }

    public void startObserve() {
        File file = new File(filename);
        if (file.exists()) {
            observerEventEvaluator.observerEvaluateTrue(beginState, true);
        } else {
            observerEventEvaluator.observerEvaluateFalse(true);
        }
    }

    public void stopObserve() {
        // this is called when the subexpression quits or the pattern is stopped
        // no action required
    }

    public void accept(EventObserverVisitor visitor) {
    }
}
