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
package com.espertech.esper.common.internal.bytecodemodel.base;

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.name.CodegenFieldName;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.field;

public class CodegenPackageScope {
    private final String packageName;
    private final String fieldsClassNameOptional;
    private final boolean instrumented;
    private final CodegenMethod initMethod = CodegenMethod.makeParentNode(void.class, CodegenPackageScope.class, new CodegenClassScope(true, this, null)).addParam(EPStatementInitServices.class, EPStatementInitServices.REF.getRef()).setStatic(true);

    private int currentMemberNumber;
    private int currentSubstitutionParamNumber;

    // Well-named fields
    private final LinkedHashMap<CodegenFieldName, CodegenField> fieldsNamed = new LinkedHashMap<>();

    // Unshared fields
    private final LinkedHashMap<CodegenField, CodegenExpression> fieldsUnshared = new LinkedHashMap<>();

    // Shared fields
    private final LinkedHashMap<CodegenFieldSharable, CodegenField> fieldsShared = new LinkedHashMap<>();

    // Substitution parameters
    private List<CodegenSubstitutionParamEntry> substitutionParamsByNumber = new ArrayList<>();
    private LinkedHashMap<String, CodegenSubstitutionParamEntry> substitutionParamsByName = new LinkedHashMap<>();

    public CodegenPackageScope(String packageName, String fieldsClassNameOptional, boolean instrumented) {
        this.packageName = packageName;
        this.fieldsClassNameOptional = fieldsClassNameOptional;
        this.instrumented = instrumented;
    }

    public <T> CodegenExpressionField addFieldUnshared(boolean isFinal, Class<? extends T> clazz, CodegenExpression initCtorScoped) {
        if (fieldsClassNameOptional == null) {
            throw new IllegalStateException("No fields class name");
        }
        return field(addFieldUnsharedInternal(isFinal, clazz, initCtorScoped));
    }

    public CodegenExpressionField addOrGetFieldSharable(CodegenFieldSharable sharable) {
        CodegenField member = fieldsShared.get(sharable);
        if (member != null) {
            return field(member);
        }
        member = addFieldUnsharedInternal(true, sharable.type(), sharable.initCtorScoped());
        fieldsShared.put(sharable, member);
        return field(member);
    }

    public CodegenExpressionField addOrGetFieldWellKnown(CodegenFieldName fieldName, Class type) {
        CodegenField existing = fieldsNamed.get(fieldName);
        if (existing != null) {
            if (existing.getType() != type) {
                throw new IllegalStateException("Field '" + fieldName + "' already registered with a different type, registered with " + existing.getType().getSimpleName() + " but provided " + type.getSimpleName());
            }
            return field(existing);
        }
        CodegenField field = new CodegenField(fieldsClassNameOptional, fieldName.getName(), type, null, false);
        fieldsNamed.put(fieldName, field);
        return field(field);
    }

    public String getPackageName() {
        return packageName;
    }

    public CodegenMethod getInitMethod() {
        return initMethod;
    }

    public LinkedHashMap<CodegenFieldName, CodegenField> getFieldsNamed() {
        return fieldsNamed;
    }

    public LinkedHashMap<CodegenField, CodegenExpression> getFieldsUnshared() {
        return fieldsUnshared;
    }

    public String getFieldsClassNameOptional() {
        return fieldsClassNameOptional;
    }

    public List<CodegenSubstitutionParamEntry> getSubstitutionParamsByNumber() {
        return substitutionParamsByNumber;
    }

    public LinkedHashMap<String, CodegenSubstitutionParamEntry> getSubstitutionParamsByName() {
        return substitutionParamsByName;
    }

    private <T> CodegenField addFieldUnsharedInternal(boolean isFinal, Class<? extends T> clazz, CodegenExpression initCtorScoped) {
        int memberNumber = currentMemberNumber++;
        String name = CodegenPackageScopeNames.anyField(memberNumber);
        CodegenField member = new CodegenField(fieldsClassNameOptional, name, clazz, null, isFinal);
        fieldsUnshared.put(member, initCtorScoped);
        return member;
    }

    public CodegenField addSubstitutionParameter(String name, Class type) {
        boolean mixed = false;
        if (name == null) {
            if (!substitutionParamsByName.isEmpty()) {
                mixed = true;
            }
        } else if (!substitutionParamsByNumber.isEmpty()) {
            mixed = true;
        }
        if (mixed) {
            throw new IllegalArgumentException("Mixing named and unnamed substitution parameters is not allowed");
        }

        if (name != null) {
            CodegenSubstitutionParamEntry entry = substitutionParamsByName.get(name);
            if (entry != null && !JavaClassHelper.isSubclassOrImplementsInterface(type, entry.getType())) {
                throw new IllegalArgumentException("Substitution parameter '" + name + "' of type '" + entry.getType() + "' cannot be assigned type '" + type + "'");
            }
        }

        CodegenField member;
        if (name == null) {
            int assigned = ++currentSubstitutionParamNumber;
            String fieldName = CodegenPackageScopeNames.anySubstitutionParam(assigned);
            member = new CodegenField(fieldsClassNameOptional, fieldName, type, null, false);
            substitutionParamsByNumber.add(new CodegenSubstitutionParamEntry(member, name, type));
        } else {
            CodegenSubstitutionParamEntry existing = substitutionParamsByName.get(name);
            if (existing == null) {
                int assigned = ++currentSubstitutionParamNumber;
                String fieldName = CodegenPackageScopeNames.anySubstitutionParam(assigned);
                member = new CodegenField(fieldsClassNameOptional, fieldName, type, null, false);
                substitutionParamsByName.put(name, new CodegenSubstitutionParamEntry(member, name, type));
            } else {
                member = existing.getField();
            }
        }

        return member;
    }

    public boolean isInstrumented() {
        return instrumented;
    }
}
