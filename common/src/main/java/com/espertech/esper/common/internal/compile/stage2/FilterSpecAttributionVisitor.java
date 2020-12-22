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
package com.espertech.esper.common.internal.compile.stage2;

public interface FilterSpecAttributionVisitor<T> {
    T accept(FilterSpecAttributionSubquery attribution);
    T accept(FilterSpecAttributionStreamPattern attribution);
    T accept(FilterSpecAttributionContextController attribution);
    T accept(FilterSpecAttributionContextCondition attribution);
    T accept(FilterSpecAttributionContextConditionPattern attribution);
    T accept(FilterSpecAttributionNamedWindow attribution);
    T accept(FilterSpecAttributionStream attribution);
    T accept(FilterSpecAttributionDataflow attribution);
}
