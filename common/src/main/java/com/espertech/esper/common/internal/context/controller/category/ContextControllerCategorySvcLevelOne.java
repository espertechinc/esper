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
package com.espertech.esper.common.internal.context.controller.category;

import com.espertech.esper.common.internal.collection.IntSeqKey;

public class ContextControllerCategorySvcLevelOne implements ContextControllerCategorySvc {
    private final static Object[] EMPTY_PARENT_PARTITION_KEYS = new Object[0];

    private int[] subpathOrCPId;

    public void mgmtCreate(IntSeqKey controllerPath, Object[] parentPartitionKeys, int[] subpathOrCPId) {
        this.subpathOrCPId = subpathOrCPId;
    }

    public int[] mgmtGetSubpathOrCPIds(IntSeqKey controllerPath) {
        return subpathOrCPId;
    }

    public int[] mgmtDelete(IntSeqKey controllerPath) {
        int[] tmp = subpathOrCPId;
        subpathOrCPId = null;
        return tmp;
    }

    public void destroy() {
        subpathOrCPId = null;
    }
}
