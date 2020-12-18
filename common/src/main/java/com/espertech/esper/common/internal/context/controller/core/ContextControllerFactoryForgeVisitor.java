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
package com.espertech.esper.common.internal.context.controller.core;

import com.espertech.esper.common.internal.context.controller.category.ContextControllerCategoryFactoryForge;
import com.espertech.esper.common.internal.context.controller.hash.ContextControllerHashFactoryForge;
import com.espertech.esper.common.internal.context.controller.initterm.ContextControllerInitTermFactoryForge;
import com.espertech.esper.common.internal.context.controller.keyed.ContextControllerKeyedFactoryForge;

public interface ContextControllerFactoryForgeVisitor<T>  {
    T visit(ContextControllerCategoryFactoryForge forge);
    T visit(ContextControllerInitTermFactoryForge forge);
    T visit(ContextControllerHashFactoryForge forge);
    T visit(ContextControllerKeyedFactoryForge forge);
}
