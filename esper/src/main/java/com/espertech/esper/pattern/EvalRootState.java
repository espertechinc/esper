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
package com.espertech.esper.pattern;

import com.espertech.esper.filterspec.MatchedEventMap;

/**
 * Interface for a root state node accepting a callback to use to indicate pattern results.
 */
public interface EvalRootState extends PatternStopCallback, EvalRootMatchRemover {
    /**
     * Accept callback to indicate pattern results.
     *
     * @param callback is a pattern result call
     */
    public void setCallback(PatternMatchCallback callback);

    public void startRecoverable(boolean startRecoverable, MatchedEventMap beginState);

    public void accept(EvalStateNodeVisitor visitor);
}
