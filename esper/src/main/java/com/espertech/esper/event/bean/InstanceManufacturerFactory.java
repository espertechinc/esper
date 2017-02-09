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
package com.espertech.esper.event.bean;

import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import net.sf.cglib.reflect.FastConstructor;

public class InstanceManufacturerFactory {
    public static InstanceManufacturer getManufacturer(Class targetClass, EngineImportService engineImportService, ExprNode[] childNodes)
            throws ExprValidationException {
        ExprEvaluator[] evalsUnmodified = ExprNodeUtility.getEvaluators(childNodes);
        Object[] returnTypes = new Object[evalsUnmodified.length];
        for (int i = 0; i < evalsUnmodified.length; i++) {
            returnTypes[i] = evalsUnmodified[i].getType();
        }

        Pair<FastConstructor, ExprEvaluator[]> ctor = InstanceManufacturerUtil.getManufacturer(targetClass, engineImportService, evalsUnmodified, returnTypes);
        return new InstanceManufacturerFastCtor(targetClass, ctor.getFirst(), ctor.getSecond());
    }
}
