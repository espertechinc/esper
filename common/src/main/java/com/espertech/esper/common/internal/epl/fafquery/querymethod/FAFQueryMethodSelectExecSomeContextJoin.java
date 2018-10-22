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
package com.espertech.esper.common.internal.epl.fafquery.querymethod;

import com.espertech.esper.common.client.context.ContextPartitionSelector;
import com.espertech.esper.common.internal.context.mgr.ContextManagementService;

public class FAFQueryMethodSelectExecSomeContextJoin implements FAFQueryMethodSelectExec {
    public static final FAFQueryMethodSelectExec INSTANCE = new FAFQueryMethodSelectExecSomeContextJoin();

    private FAFQueryMethodSelectExecSomeContextJoin() {
    }

    public EPPreparedQueryResult execute(FAFQueryMethodSelect select, ContextPartitionSelector[] contextPartitionSelectors, FAFQueryMethodAssignerSetter assignerSetter, ContextManagementService contextManagementService) {
        throw new UnsupportedOperationException("Context with join is not supported");
    }
}
