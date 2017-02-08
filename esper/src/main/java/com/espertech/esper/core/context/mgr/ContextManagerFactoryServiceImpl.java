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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.ContextDetail;
import com.espertech.esper.epl.spec.ContextDetailNested;

public class ContextManagerFactoryServiceImpl implements ContextManagerFactoryService {
    public ContextManager make(ContextDetail contextDetail, ContextControllerFactoryServiceContext factoryServiceContext) throws ExprValidationException {
        if (contextDetail instanceof ContextDetailNested) {
            return new ContextManagerNested(factoryServiceContext);
        }
        return new ContextManagerImpl(factoryServiceContext);
    }

    public boolean isSupportsExtract() {
        return true;
    }
}
