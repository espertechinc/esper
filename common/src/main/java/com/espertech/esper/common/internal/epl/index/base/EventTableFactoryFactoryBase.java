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
package com.espertech.esper.common.internal.epl.index.base;

public abstract class EventTableFactoryFactoryBase implements EventTableFactoryFactory {
    protected final int indexedStreamNum;
    protected final Integer subqueryNum;
    protected final boolean isFireAndForget;

    public EventTableFactoryFactoryBase(int indexedStreamNum, Integer subqueryNum, boolean isFireAndForget) {
        this.indexedStreamNum = indexedStreamNum;
        this.subqueryNum = subqueryNum;
        this.isFireAndForget = isFireAndForget;
    }
}
