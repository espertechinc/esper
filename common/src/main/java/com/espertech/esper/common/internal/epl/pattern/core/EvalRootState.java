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
package com.espertech.esper.common.internal.epl.pattern.core;

import com.espertech.esper.common.internal.filterspec.MatchedEventMap;
import com.espertech.esper.common.internal.util.StopCallback;

/**
 * Interface for a root state node accepting a callback to use to indicate pattern results.
 */
public interface EvalRootState extends StopCallback, EvalRootMatchRemover {

    EvalRootState[] EMPTY_ARRAY = new EvalRootState[0];

    /**
     * Accept callback to indicate pattern results.
     *
     * @param callback is a pattern result call
     */
    public void setCallback(PatternMatchCallback callback);

    // public void accept(EvalStateNodeVisitor visitor);

    public void startRecoverable(boolean startRecoverable, MatchedEventMap beginState);

    public void accept(EvalStateNodeVisitor visitor);
}
