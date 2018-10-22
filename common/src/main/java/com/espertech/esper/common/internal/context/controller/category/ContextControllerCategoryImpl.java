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

import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;

public class ContextControllerCategoryImpl extends ContextControllerCategory {

    public ContextControllerCategoryImpl(ContextManagerRealization realization, ContextControllerCategoryFactory factory) {
        super(realization, factory);
        if (factory.getFactoryEnv().isRoot()) {
            categorySvc = new ContextControllerCategorySvcLevelOne();
        } else {
            categorySvc = new ContextControllerCategorySvcLevelAny();
        }
    }
}
