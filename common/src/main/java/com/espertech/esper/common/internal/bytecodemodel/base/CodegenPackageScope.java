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

import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerByteCode;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClass;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClassType;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionStaticMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.statement.CodegenStatement;
import com.espertech.esper.common.internal.bytecodemodel.model.statement.CodegenStatementExpression;
import com.espertech.esper.common.internal.bytecodemodel.name.CodegenFieldName;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.field;

public class CodegenPackageScope {
    private final String packageName;
    private final String fieldsClassNameOptional;
    private final boolean instrumented;
    private final ConfigurationCompilerByteCode config;
    private final CodegenMethod initMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), CodegenPackageScope.class, new CodegenClassScope(true, this, null)).addParam(EPStatementInitServices.EPTYPE, EPStatementInitServices.REF.getRef()).setStatic(true);

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

    public CodegenPackageScope(String packageName, String fieldsClassNameOptional, boolean instrumented, ConfigurationCompilerByteCode config) {
        this.packageName = packageName;
        this.fieldsClassNameOptional = fieldsClassNameOptional;
        this.instrumented = instrumented;
        this.config = config;
    }

    public boolean hasAnyFields() {
        return !getFieldsNamed().isEmpty() ||
                !getFieldsUnshared().isEmpty() ||
                !getSubstitutionParamsByNumber().isEmpty() ||
                !getSubstitutionParamsByName().isEmpty();
    }

    public CodegenExpressionField addFieldUnshared(boolean isFinal, EPTypeClass clazz, CodegenExpression initCtorScoped) {
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

    public CodegenExpressionField addOrGetFieldWellKnown(CodegenFieldName fieldName, EPTypeClass type) {
        CodegenField existing = fieldsNamed.get(fieldName);
        if (existing != null) {
            if (!existing.getType().equals(type)) {
                throw new IllegalStateException("Field '" + fieldName + "' already registered with a different type, registered with " + existing.getType().toFullName() + " but provided " + type.toFullName());
            }
            return field(existing);
        }
        CodegenField field = new CodegenField(fieldsClassNameOptional, fieldName.getName(), type, false);
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

    public boolean hasAssignableStatementFields() {
        return !fieldsNamed.isEmpty();
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

    private CodegenField addFieldUnsharedInternal(boolean isFinal, EPTypeClass type, CodegenExpression initCtorScoped) {
        int memberNumber = currentMemberNumber++;
        String name = CodegenPackageScopeNames.anyField(memberNumber);
        CodegenField member = new CodegenField(fieldsClassNameOptional, name, type, isFinal);
        fieldsUnshared.put(member, initCtorScoped);
        return member;
    }

    public CodegenField addSubstitutionParameter(String name, EPTypeClass type) {
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
            member = new CodegenField(fieldsClassNameOptional, fieldName, type, false);
            substitutionParamsByNumber.add(new CodegenSubstitutionParamEntry(member, name, type));
        } else {
            CodegenSubstitutionParamEntry existing = substitutionParamsByName.get(name);
            if (existing == null) {
                int assigned = ++currentSubstitutionParamNumber;
                String fieldName = CodegenPackageScopeNames.anySubstitutionParam(assigned);
                member = new CodegenField(fieldsClassNameOptional, fieldName, type, false);
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

    public boolean isHasSubstitution() {
        return !substitutionParamsByNumber.isEmpty() || !substitutionParamsByName.isEmpty();
    }

    public ConfigurationCompilerByteCode getConfig() {
        return config;
    }

    public void rewriteStatementFieldUse(List<CodegenClass> classes) {
        if (getFieldsClassNameOptional() != null && !hasAnyFields()) {
            rewriteProviderNoFieldInit(classes, getFieldsClassNameOptional());
        }
    }

    private static void rewriteProviderNoFieldInit(List<CodegenClass> classes, String fieldClassName) {
        // Rewrite the constructor of providers to remove calls to field initialization, for when there is no fields-class.
        // Field initialization cannot be predicted as forging adds fields.
        // The forge order puts the forging of the fields-class last so that fields can be added during forging.
        // Since the fields-class is forged last the provider classes cannot predict whether fields are required or not.
        for (CodegenClass clazz : classes) {
            if (clazz.getClassType() == CodegenClassType.FAFQUERYMETHODPROVIDER || clazz.getClassType() == CodegenClassType.STATEMENTAIFACTORYPROVIDER) {
                Iterator<CodegenStatement> it = clazz.getOptionalCtor().getBlock().getStatements().iterator();
                while (it.hasNext()) {
                    CodegenStatement statement = it.next();
                    if (statement instanceof CodegenStatementExpression) {
                        CodegenStatementExpression expression = (CodegenStatementExpression) statement;
                        if (expression.getExpression() instanceof CodegenExpressionStaticMethod) {
                            CodegenExpressionStaticMethod staticMethod = (CodegenExpressionStaticMethod) expression.getExpression();
                            if (staticMethod.getTargetClassName() != null && staticMethod.getTargetClassName().equals(fieldClassName)) {
                                it.remove();
                            }
                        }
                    }
                }
            }
        }
    }
}
