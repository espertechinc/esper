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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class JsonEndValueForgeCharacter implements JsonEndValueForge {
    public final static JsonEndValueForgeCharacter INSTANCE = new JsonEndValueForgeCharacter();

    private JsonEndValueForgeCharacter() {
    }

    public CodegenExpression captureValue(JsonEndValueRefs refs, CodegenMethod method, CodegenClassScope classScope) {
        return staticMethod(JsonEndValueForgeCharacter.class, "jsonToCharacter", refs.getValueString());
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param value value
     * @return char
     */
    public static Character jsonToCharacter(String value) {
        return value == null ? null : value.charAt(0);
    }

    public static Object jsonToCharacterNonNull(String stringValue) {
        return stringValue.charAt(0);
    }
}
