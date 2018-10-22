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
package com.espertech.esper.common.internal.compile.stage3;

import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;

public interface ModuleAccessModifierService {
    NameAccessModifier getAccessModifierEventType(StatementRawInfo raw, String eventTypeName);

    NameAccessModifier getAccessModifierContext(StatementBaseInfo base, String contextName);

    NameAccessModifier getAccessModifierVariable(StatementBaseInfo base, String variableName);

    NameAccessModifier getAccessModifierExpression(StatementBaseInfo base, String expressionName);

    NameAccessModifier getAccessModifierTable(StatementBaseInfo base, String tableName);

    NameAccessModifier getAccessModifierNamedWindow(StatementBaseInfo base, String namedWindowName);

    NameAccessModifier getAccessModifierScript(StatementBaseInfo base, String scriptName, int numParameters);

    EventTypeBusModifier getBusModifierEventType(StatementRawInfo raw, String eventTypeName);
}
