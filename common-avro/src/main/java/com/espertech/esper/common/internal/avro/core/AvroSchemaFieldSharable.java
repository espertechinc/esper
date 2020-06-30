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
package com.espertech.esper.common.internal.avro.core;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenFieldSharable;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import org.apache.avro.Schema;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AvroSchemaFieldSharable implements CodegenFieldSharable {
    private final Schema schema;

    public AvroSchemaFieldSharable(Schema schema) {
        this.schema = schema;
    }

    public EPTypeClass type() {
        return AvroConstant.EPTYPE_SCHEMA;
    }

    public CodegenExpression initCtorScoped() {
        return exprDotMethod(newInstance(AvroConstant.EPTYPE_SCHEMAPARSER), "parse", constant(schema.toString()));
    }
}
