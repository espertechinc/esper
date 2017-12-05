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
package com.espertech.esper.epl.spec;

import com.espertech.esper.pattern.EvalFactoryNode;

/**
 * Pattern specification in unvalidated, unoptimized form.
 */
public class PatternStreamSpecRaw extends StreamSpecBase implements StreamSpecRaw {
    private static final long serialVersionUID = 6393401926404401433L;

    private final EvalFactoryNode evalFactoryNode;
    private final boolean suppressSameEventMatches;
    private final boolean discardPartialsOnMatch;

    public PatternStreamSpecRaw(EvalFactoryNode evalFactoryNode, ViewSpec[] viewSpecs, String optionalStreamName, StreamSpecOptions streamSpecOptions, boolean suppressSameEventMatches, boolean discardPartialsOnMatch) {
        super(optionalStreamName, viewSpecs, streamSpecOptions);
        this.evalFactoryNode = evalFactoryNode;
        this.suppressSameEventMatches = suppressSameEventMatches;
        this.discardPartialsOnMatch = discardPartialsOnMatch;
    }

    /**
     * Returns the pattern expression evaluation node for the top pattern operator.
     *
     * @return parent pattern expression node
     */
    public EvalFactoryNode getEvalFactoryNode() {
        return evalFactoryNode;
    }

    public boolean isSuppressSameEventMatches() {
        return suppressSameEventMatches;
    }

    public boolean isDiscardPartialsOnMatch() {
        return discardPartialsOnMatch;
    }
}
