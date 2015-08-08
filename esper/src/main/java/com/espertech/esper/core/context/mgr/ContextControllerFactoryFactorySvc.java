/*
 * *************************************************************************************
 *  Copyright (C) 2008 EsperTech, Inc. All rights reserved.                            *
 *  http://esper.codehaus.org                                                          *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.core.context.mgr;

import com.espertech.esper.epl.spec.ContextDetail;
import com.espertech.esper.filter.FilterSpecCompiled;

import java.util.List;

public interface ContextControllerFactoryFactorySvc {
    public ContextControllerFactory make(ContextControllerFactoryContext factoryContext, ContextDetail detail, List<FilterSpecCompiled> optFiltersNested, ContextStateCache contextStateCache);
}
