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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;

import java.util.Map;

public class AMQPSettingsSinkForge extends AMQPSettingsForgeBase {
    private Map<String, Object> collector;

    public AMQPSettingsSinkForge() {
    }

    CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        SAIFFInitializeBuilder builder = new SAIFFInitializeBuilder(AMQPSettingsSinkFactory.class, this.getClass(), "amqpSink", parent, symbols, classScope);
        super.make(builder);
        builder.map("collector", collector);
        return builder.build();
    }

    public Map<String, Object> getCollector() {
        return collector;
    }

    public void setCollector(Map<String, Object> collector) {
        this.collector = collector;
    }

    public String toString() {
        return super.toString() + "  AMQPSettingsSink{" +
            "objectToAmqpTransform=" + collector +
            '}';
    }
}
