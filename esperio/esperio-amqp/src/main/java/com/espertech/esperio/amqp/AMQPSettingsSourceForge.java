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
package com.espertech.esperio.amqp;

import com.espertech.esper.common.client.dataflow.util.DataFlowParameterValidation;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpForgeInitializeContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import java.util.Map;

public class AMQPSettingsSourceForge extends AMQPSettingsForgeBase {
    private Map<String, Object> collector;
    private ExprNode prefetchCount;
    private ExprNode consumeAutoAck;

    public AMQPSettingsSourceForge() {
    }

    public void validate(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
        super.validate(context);
        prefetchCount = DataFlowParameterValidation.validate("prefetchCount", prefetchCount, int.class, context);
        consumeAutoAck = DataFlowParameterValidation.validate("consumeAutoAck", consumeAutoAck, boolean.class, context);
    }

    CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        SAIFFInitializeBuilder builder = new SAIFFInitializeBuilder(AMQPSettingsSourceFactory.class, this.getClass(), "amqpSource", parent, symbols, classScope);
        super.make(builder);
        builder.map("collector", collector)
            .exprnode("prefetchCount", prefetchCount)
            .exprnode("consumeAutoAck", consumeAutoAck);
        return builder.build();
    }

    public Map getCollector() {
        return collector;
    }

    public void setCollector(Map<String, Object> collector) {
        this.collector = collector;
    }

    public ExprNode getPrefetchCount() {
        return prefetchCount;
    }

    public void setPrefetchCount(ExprNode prefetchCount) {
        this.prefetchCount = prefetchCount;
    }

    public ExprNode getConsumeAutoAck() {
        return consumeAutoAck;
    }

    public void setConsumeAutoAck(ExprNode consumeAutoAck) {
        this.consumeAutoAck = consumeAutoAck;
    }
}
