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
package com.espertech.esper.common.internal.event.bean.manufacturer;

import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;

import java.lang.reflect.Constructor;

public class InstanceManufacturerFactoryFactory {
    public static InstanceManufacturerFactory getManufacturer(Class targetClass, ClasspathImportServiceCompileTime classpathImportService, ExprNode[] childNodes)
            throws ExprValidationException {
        ExprForge[] forgesUnmodified = ExprNodeUtilityQuery.getForges(childNodes);
        Object[] returnTypes = new Object[forgesUnmodified.length];
        for (int i = 0; i < forgesUnmodified.length; i++) {
            returnTypes[i] = forgesUnmodified[i].getEvaluationType();
        }

        Pair<Constructor, ExprForge[]> ctor = InstanceManufacturerUtil.getManufacturer(targetClass, classpathImportService, forgesUnmodified, returnTypes);
        return new InstanceManufacturerFactoryFastCtor(targetClass, ctor.getFirst(), ctor.getSecond());
    }
}
