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

public class PatternAttributionKeyStream implements PatternAttributionKey {
    private final int streamNum;

    public PatternAttributionKeyStream(int streamNum) {
        this.streamNum = streamNum;
    }

    public int getStreamNum() {
        return streamNum;
    }

    public <T> T accept(PatternAttributionKeyVisitor<T> visitor, short factoryNodeId) {
        return visitor.visit(this, factoryNodeId);
    }
}
