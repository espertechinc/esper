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

public class PatternLevelAnnotationFlags {
    private boolean suppressSameEventMatches;
    private boolean discardPartialsOnMatch;

    public boolean isSuppressSameEventMatches() {
        return suppressSameEventMatches;
    }

    public void setSuppressSameEventMatches(boolean suppressSameEventMatches) {
        this.suppressSameEventMatches = suppressSameEventMatches;
    }

    public boolean isDiscardPartialsOnMatch() {
        return discardPartialsOnMatch;
    }

    public void setDiscardPartialsOnMatch(boolean discardPartialsOnMatch) {
        this.discardPartialsOnMatch = discardPartialsOnMatch;
    }
}
