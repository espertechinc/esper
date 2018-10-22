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
package com.espertech.esper.common.internal.epl.index.advanced.index.service;

import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.lookup.AdvancedIndexConfigContextPartition;

public interface AdvancedIndexFactoryProvider {
    EventAdvancedIndexProvisionCompileTime validateEventIndex(String indexName, String indexTypeName, ExprNode[] columns, ExprNode[] parameters)
            throws ExprValidationException;

    AdvancedIndexConfigContextPartition validateConfigureFilterIndex(String indexName, String indexTypeName, ExprNode[] parameters, ExprValidationContext validationContext)
            throws ExprValidationException;
}
