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
package com.espertech.esper.common.internal.event.json.parser.delegates.endvalue;

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public class JsonEndValueRefs {
    private final static CodegenExpression STRINGVALUE = ref("stringValue");
    private final static CodegenExpression OBJECTVALUE = ref("objectValue");
    private final static CodegenExpression ISNUMBER = ref("isNumber");
    private final static CodegenExpression JSONFIELDNAME = ref("name");

    public final static JsonEndValueRefs INSTANCE = new JsonEndValueRefs(STRINGVALUE, ISNUMBER, OBJECTVALUE, JSONFIELDNAME);

    private final CodegenExpression valueString;
    private final CodegenExpression isNumber;
    private final CodegenExpression valueObject;
    private final CodegenExpression name;

    private JsonEndValueRefs(CodegenExpression valueString, CodegenExpression isNumber, CodegenExpression valueObject, CodegenExpression name) {
        this.valueString = valueString;
        this.isNumber = isNumber;
        this.valueObject = valueObject;
        this.name = name;
    }

    public CodegenExpression getValueString() {
        return valueString;
    }

    public CodegenExpression getIsNumber() {
        return isNumber;
    }

    public CodegenExpression getValueObject() {
        return valueObject;
    }

    public CodegenExpression getName() {
        return name;
    }
}
