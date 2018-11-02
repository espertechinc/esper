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
package com.espertech.esper.common.internal.context.cpidsvc;

import java.util.Collection;

public interface ContextPartitionIdService {
    int allocateId(Object[] partitionKeys);

    Collection<Integer> getIds();

    Object[] getPartitionKeys(int id);

    void removeId(int id);

    void destroy();

    void clear();

    void clearCaches();

    long getCount();
}
