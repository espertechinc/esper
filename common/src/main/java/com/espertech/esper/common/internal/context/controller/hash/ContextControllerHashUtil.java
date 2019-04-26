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
package com.espertech.esper.common.internal.context.controller.hash;

import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage1.spec.ContextSpecHash;
import com.espertech.esper.common.internal.compile.stage1.spec.ContextSpecHashItem;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFactory;
import com.espertech.esper.common.internal.context.mgr.ContextManagerRealization;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupableForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.core.EventPropertyValueGetterForge;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForgeSingleton;
import com.espertech.esper.common.internal.serde.serdeset.builtin.DIONullableIntegerSerde;
import com.espertech.esper.common.internal.settings.ClasspathImportSingleRowDesc;

public class ContextControllerHashUtil {
    public static void validateContextDesc(String contextName, ContextSpecHash hashedSpec, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) throws ExprValidationException {

        if (hashedSpec.getItems().isEmpty()) {
            throw new ExprValidationException("Empty list of hash items");
        }

        for (ContextSpecHashItem item : hashedSpec.getItems()) {
            if (item.getFunction().getParameters().isEmpty()) {
                throw new ExprValidationException("For context '" + contextName + "' expected one or more parameters to the hash function, but found no parameter list");
            }

            // determine type of hash to use
            String hashFuncName = item.getFunction().getName();
            HashFunctionEnum hashFunction = HashFunctionEnum.determine(contextName, hashFuncName);
            Pair<Class, ClasspathImportSingleRowDesc> hashSingleRowFunction = null;
            if (hashFunction == null) {
                try {
                    hashSingleRowFunction = services.getClasspathImportServiceCompileTime().resolveSingleRow(hashFuncName);
                } catch (Exception e) {
                    // expected
                }

                if (hashSingleRowFunction == null) {
                    throw new ExprValidationException("For context '" + contextName + "' expected a hash function that is any of {" + HashFunctionEnum.getStringList() +
                            "} or a plug-in single-row function or script but received '" + hashFuncName + "'");
                }
            }

            // get first parameter
            ExprNode paramExpr = item.getFunction().getParameters().get(0);
            Class paramType = paramExpr.getForge().getEvaluationType();
            EventPropertyValueGetterForge getter;

            if (hashFunction == HashFunctionEnum.CONSISTENT_HASH_CRC32) {
                if (item.getFunction().getParameters().size() > 1 || paramType != String.class) {
                    getter = new ContextControllerHashedGetterCRC32SerializedForge(item.getFunction().getParameters(), hashedSpec.getGranularity());
                } else {
                    getter = new ContextControllerHashedGetterCRC32SingleForge(paramExpr, hashedSpec.getGranularity());
                }
            } else if (hashFunction == HashFunctionEnum.HASH_CODE) {
                if (item.getFunction().getParameters().size() > 1) {
                    getter = new ContextControllerHashedGetterHashMultiple(item.getFunction().getParameters(), hashedSpec.getGranularity());
                } else {
                    getter = new ContextControllerHashedGetterHashSingleForge(paramExpr, hashedSpec.getGranularity());
                }
            } else if (hashSingleRowFunction != null) {
                getter = new ContextControllerHashedGetterSingleRowForge(hashSingleRowFunction, item.getFunction().getParameters(),
                        hashedSpec.getGranularity(), item.getFilterSpecCompiled().getFilterForEventType(), statementRawInfo, services);
            } else {
                throw new IllegalArgumentException("Unrecognized hash code function '" + hashFuncName + "'");
            }

            // create and register expression
            String expression = item.getFunction().getName() + "(" + ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(paramExpr) + ")";
            DataInputOutputSerdeForge valueSerde = new DataInputOutputSerdeForgeSingleton(DIONullableIntegerSerde.class);
            ExprFilterSpecLookupableForge lookupable = new ExprFilterSpecLookupableForge(expression, getter, Integer.class, true, valueSerde);
            item.setLookupable(lookupable);
        }
    }

    public static ContextControllerHashSvc makeService(ContextControllerHashFactory factory, ContextManagerRealization realization) {
        ContextControllerFactory[] factories = realization.getContextManager().getContextDefinition().getControllerFactories();
        boolean preallocate = factory.getHashSpec().isPreallocate();
        if (factories.length == 1) {
            return new ContextControllerHashSvcLevelOne(preallocate);
        }
        return new ContextControllerHashSvcLevelAny(preallocate);
    }
}
