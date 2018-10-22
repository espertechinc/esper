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
package com.espertech.esper.common.internal.statement.multimatch;

public class MultiMatchHandlerFactoryImpl implements MultiMatchHandlerFactory {

    private final boolean isSubselectPreeval;

    public MultiMatchHandlerFactoryImpl(boolean isSubselectPreeval) {
        this.isSubselectPreeval = isSubselectPreeval;
    }

    public MultiMatchHandler make(boolean hasSubselect, boolean needDedup) {
        if (!hasSubselect) {
            if (!needDedup) {
                return MultiMatchHandlerNoSubqueryNoDedup.INSTANCE;
            }
            return MultiMatchHandlerNoSubqueryWDedup.INSTANCE;
        }
        if (!needDedup) {
            if (isSubselectPreeval) {
                return MultiMatchHandlerSubqueryPreevalNoDedup.INSTANCE;
            }
            return MultiMatchHandlerSubqueryPostevalNoDedup.INSTANCE;
        }
        return new MultiMatchHandlerSubqueryWDedup(isSubselectPreeval);
    }
}
