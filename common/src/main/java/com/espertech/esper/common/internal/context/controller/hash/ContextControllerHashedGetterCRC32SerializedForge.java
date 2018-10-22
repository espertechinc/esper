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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.event.core.EventPropertyValueGetterForge;
import com.espertech.esper.common.internal.util.Serializer;
import com.espertech.esper.common.internal.util.SerializerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.zip.CRC32;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ContextControllerHashedGetterCRC32SerializedForge implements EventPropertyValueGetterForge {
    private static final Logger log = LoggerFactory.getLogger(ContextControllerHashedGetterCRC32SerializedForge.class);

    private final ExprNode[] nodes;
    private final int granularity;

    public ContextControllerHashedGetterCRC32SerializedForge(List<ExprNode> nodes, int granularity) {
        this.nodes = nodes.toArray(new ExprNode[nodes.size()]);
        this.granularity = granularity;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param objectMayArray value
     * @param granularity    granularity
     * @param serializers    serializers
     * @return hash
     */
    public static int serializeAndCRC32Hash(Object objectMayArray, int granularity, Serializer[] serializers) {

        byte[] bytes;
        try {
            if (objectMayArray instanceof Object[]) {
                bytes = SerializerFactory.serialize(serializers, (Object[]) objectMayArray);
            } else {
                bytes = SerializerFactory.serialize(serializers[0], objectMayArray);
            }
        } catch (IOException e) {
            log.error("Exception serializing parameters for computing consistent hash: " + e.getMessage(), e);
            bytes = new byte[0];
        }

        CRC32 crc = new CRC32();
        crc.update(bytes);
        long value = crc.getValue() % granularity;

        int result = (int) value;
        if (result >= 0) {
            return result;
        }
        return -result;
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenExpressionField serializers = classScope.addFieldUnshared(true, Serializer[].class, staticMethod(SerializerFactory.class, "getSerializers", constant(ExprNodeUtilityQuery.getExprResultTypes(nodes))));

        CodegenMethod method = parent.makeChild(Object.class, this.getClass(), classScope).addParam(EventBean.class, "eventBean");
        method.getBlock()
                .declareVar(EventBean[].class, "events", newArrayWithInit(EventBean.class, ref("eventBean")));

        // method to return object-array from expressions
        ExprForgeCodegenSymbol exprSymbol = new ExprForgeCodegenSymbol(true, null);
        CodegenMethod exprMethod = method.makeChildWithScope(Object.class, CodegenLegoMethodExpression.class, exprSymbol, classScope).addParam(ExprForgeCodegenNames.PARAMS);
        CodegenExpression[] expressions = new CodegenExpression[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            expressions[i] = nodes[i].getForge().evaluateCodegen(nodes[i].getForge().getEvaluationType(), exprMethod, exprSymbol, classScope);
        }
        exprSymbol.derivedSymbolsCodegen(method, exprMethod.getBlock(), classScope);

        if (nodes.length == 1) {
            exprMethod.getBlock().methodReturn(expressions[0]);
        } else {
            exprMethod.getBlock().declareVar(Object[].class, "values", newArrayByLength(Object.class, constant(nodes.length)));
            for (int i = 0; i < nodes.length; i++) {
                CodegenExpression result = expressions[i];
                exprMethod.getBlock().assignArrayElement("values", constant(i), result);
            }
            exprMethod.getBlock().methodReturn(ref("values"));
        }

        method.getBlock()
                .declareVar(Object.class, "values", localMethod(exprMethod, ref("events"), constantTrue(), constantNull()))
                .methodReturn(staticMethod(ContextControllerHashedGetterCRC32SerializedForge.class, "serializeAndCRC32Hash",
                        ref("values"), constant(granularity), serializers));

        return localMethod(method, beanExpression);
    }
}
