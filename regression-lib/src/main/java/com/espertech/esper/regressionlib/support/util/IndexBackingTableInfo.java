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
package com.espertech.esper.regressionlib.support.util;

import com.espertech.esper.common.client.annotation.HookType;
import com.espertech.esper.common.internal.epl.index.composite.PropertyCompositeEventTable;
import com.espertech.esper.common.internal.epl.index.hash.PropertyHashedEventTable;
import com.espertech.esper.common.internal.epl.index.hash.PropertyHashedEventTableUnique;
import com.espertech.esper.common.internal.epl.index.sorted.PropertySortedEventTable;
import com.espertech.esper.common.internal.epl.index.unindexed.UnindexedEventTable;

public interface IndexBackingTableInfo {
    public final static String INDEX_CALLBACK_HOOK = "@Hook(type=" + HookType.class.getName() + ".INTERNAL_QUERY_PLAN,hook='" + SupportQueryPlanIndexHook.resetGetClassName() + "')";
    public final static String BACKING_UNINDEXED = UnindexedEventTable.class.getSimpleName();

    public final static String BACKING_SINGLE_UNIQUE = PropertyHashedEventTableUnique.class.getSimpleName();
    public final static String BACKING_SINGLE_DUPS = PropertyHashedEventTable.class.getSimpleName();
    public final static String BACKING_MULTI_UNIQUE = PropertyHashedEventTableUnique.class.getSimpleName();
    public final static String BACKING_MULTI_DUPS = PropertyHashedEventTable.class.getSimpleName();
    public final static String BACKING_SORTED = PropertySortedEventTable.class.getSimpleName();
    public final static String BACKING_COMPOSITE = PropertyCompositeEventTable.class.getSimpleName();
}
