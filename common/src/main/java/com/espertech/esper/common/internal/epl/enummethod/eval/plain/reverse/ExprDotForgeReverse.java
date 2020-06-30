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
package com.espertech.esper.common.internal.epl.enummethod.eval.plain.reverse;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.enummethod.dot.EnumMethodEnum;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParam;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotForgeEnumMethodBase;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForge;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeDesc;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeDescFactory;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeLambdaDesc;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.methodbase.DotMethodFP;
import com.espertech.esper.common.internal.rettype.EPChainableType;
import com.espertech.esper.common.internal.rettype.EPChainableTypeHelper;

import java.util.List;

public class ExprDotForgeReverse extends ExprDotForgeEnumMethodBase {

    public EnumForgeDescFactory getForgeFactory(DotMethodFP footprint, List<ExprNode> parameters, EnumMethodEnum enumMethod, String enumMethodUsedName, EventType inputEventType, EPTypeClass collectionComponentType, ExprValidationContext validationContext) {
        EPChainableType type;
        if (inputEventType != null) {
            type = EPChainableTypeHelper.collectionOfEvents(inputEventType);
        } else {
            type = EPChainableTypeHelper.collectionOfSingleValue(collectionComponentType);
        }
        return new EnumForgeDescFactoryReverse(type);
    }

    private static class EnumForgeDescFactoryReverse implements EnumForgeDescFactory {
        private final EPChainableType type;

        public EnumForgeDescFactoryReverse(EPChainableType type) {
            this.type = type;
        }

        public EnumForgeLambdaDesc getLambdaStreamTypesForParameter(int parameterNum) {
            throw new IllegalStateException("No lambda expected");
        }

        public EnumForgeDesc makeEnumForgeDesc(List<ExprDotEvalParam> bodiesAndParameters, int streamCountIncoming, StatementCompileTimeServices services) {
            EnumForge forge = new EnumReverseForge(streamCountIncoming);
            return new EnumForgeDesc(type, forge);
        }
    }
}
