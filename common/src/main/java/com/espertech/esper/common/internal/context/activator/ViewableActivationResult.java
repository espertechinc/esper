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
package com.espertech.esper.common.internal.context.activator;

import com.espertech.esper.common.internal.context.util.AgentInstanceMgmtCallback;
import com.espertech.esper.common.internal.epl.pattern.core.EvalRootMatchRemover;
import com.espertech.esper.common.internal.epl.pattern.core.EvalRootState;
import com.espertech.esper.common.internal.view.core.Viewable;

public class ViewableActivationResult {
    private final Viewable viewable;
    private final AgentInstanceMgmtCallback stopCallback;
    private final EvalRootMatchRemover optEvalRootMatchRemover;
    private final boolean suppressSameEventMatches;
    private final boolean discardPartialsOnMatch;
    private final EvalRootState optionalPatternRoot;
    private final ViewableActivationResultExtension viewableActivationResultExtension;
    private final Runnable optPostContextMergeRunnable;

    public ViewableActivationResult(Viewable viewable, AgentInstanceMgmtCallback stopCallback, EvalRootMatchRemover optEvalRootMatchRemover, boolean suppressSameEventMatches, boolean discardPartialsOnMatch, EvalRootState optionalPatternRoot, ViewableActivationResultExtension viewableActivationResultExtension, Runnable optPostContextMergeRunnable) {
        this.viewable = viewable;
        this.stopCallback = stopCallback;
        this.optEvalRootMatchRemover = optEvalRootMatchRemover;
        this.suppressSameEventMatches = suppressSameEventMatches;
        this.discardPartialsOnMatch = discardPartialsOnMatch;
        this.optionalPatternRoot = optionalPatternRoot;
        this.viewableActivationResultExtension = viewableActivationResultExtension;
        this.optPostContextMergeRunnable = optPostContextMergeRunnable;
    }

    public Viewable getViewable() {
        return viewable;
    }

    public AgentInstanceMgmtCallback getStopCallback() {
        return stopCallback;
    }

    public ViewableActivationResultExtension getViewableActivationResultExtension() {
        return viewableActivationResultExtension;
    }

    public EvalRootMatchRemover getOptEvalRootMatchRemover() {
        return optEvalRootMatchRemover;
    }

    public boolean isSuppressSameEventMatches() {
        return suppressSameEventMatches;
    }

    public boolean isDiscardPartialsOnMatch() {
        return discardPartialsOnMatch;
    }

    public EvalRootState getOptionalPatternRoot() {
        return optionalPatternRoot;
    }

    public Runnable getOptPostContextMergeRunnable() {
        return optPostContextMergeRunnable;
    }
}
