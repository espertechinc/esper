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
package com.espertech.esper.common.internal.context.module;

import com.espertech.esper.common.client.type.EPTypeClass;

public interface StatementProvider {
    EPTypeClass EPTYPE = new EPTypeClass(StatementProvider.class);

    StatementInformationalsRuntime getInformationals();

    void initialize(EPStatementInitServices epInitServices);

    StatementAIFactoryProvider getStatementAIFactoryProvider();
}
