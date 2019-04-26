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
package com.espertech.esper.common.internal.view.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenMakeable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

import java.util.Collections;
import java.util.List;

public interface ViewFactoryForge extends CodegenMakeable<SAIFFInitializeSymbol> {
    void setViewParameters(List<ExprNode> parameters, ViewForgeEnv viewForgeEnv, int streamNumber) throws ViewParameterException;

    void attach(EventType parentEventType, int streamNumber, ViewForgeEnv viewForgeEnv) throws ViewParameterException;

    default List<StmtClassForgeableFactory> initAdditionalForgeables(ViewForgeEnv viewForgeEnv) {
        return Collections.emptyList();
    }

    default List<ViewFactoryForge> getInnerForges() {
        return Collections.emptyList();
    }

    EventType getEventType();

    String getViewName();

    default void accept(ViewForgeVisitor visitor) {
        visitor.visit(this);
    }
}
