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
package com.espertech.esper.common.internal.compile.stage1.spec;

import com.espertech.esper.common.internal.epl.pattern.core.EvalForgeNode;

/**
 * Pattern specification in unvalidated, unoptimized form.
 */
public class PatternStreamSpecRaw extends StreamSpecBase implements StreamSpecRaw {

    private final EvalForgeNode evalForgeNode;
    private final boolean suppressSameEventMatches;
    private final boolean discardPartialsOnMatch;

    public PatternStreamSpecRaw(EvalForgeNode evalForgeNode, ViewSpec[] viewSpecs, String optionalStreamName, StreamSpecOptions streamSpecOptions, boolean suppressSameEventMatches, boolean discardPartialsOnMatch) {
        super(optionalStreamName, viewSpecs, streamSpecOptions);
        this.evalForgeNode = evalForgeNode;
        this.suppressSameEventMatches = suppressSameEventMatches;
        this.discardPartialsOnMatch = discardPartialsOnMatch;
    }

    /**
     * Returns the pattern expression evaluation node for the top pattern operator.
     *
     * @return parent pattern expression node
     */
    public EvalForgeNode getEvalForgeNode() {
        return evalForgeNode;
    }

    public boolean isSuppressSameEventMatches() {
        return suppressSameEventMatches;
    }

    public boolean isDiscardPartialsOnMatch() {
        return discardPartialsOnMatch;
    }
}
