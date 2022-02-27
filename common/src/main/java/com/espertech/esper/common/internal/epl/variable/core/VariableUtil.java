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
package com.espertech.esper.common.internal.epl.variable.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonVariable;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableTypeException;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCompileTime;
import com.espertech.esper.common.internal.event.eventtyperepo.EventTypeRepositoryImpl;
import com.espertech.esper.common.internal.serde.compiletime.eventtype.SerdeEventTypeUtility;
import com.espertech.esper.common.internal.settings.ClasspathExtensionClass;
import com.espertech.esper.common.internal.settings.ClasspathExtensionClassEmpty;
import com.espertech.esper.common.internal.settings.ClasspathImportEPTypeUtil;
import com.espertech.esper.common.internal.settings.ClasspathImportService;
import com.espertech.esper.common.internal.type.ClassDescriptor;
import com.espertech.esper.common.internal.util.ClassHelperPrint;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.common.internal.util.JavaClassHelper.isFragmentableType;

public class VariableUtil {
    private static final Logger log = LoggerFactory.getLogger(VariableUtil.class);

    public static String getAssigmentExMessage(String variableName, EPTypeClass variableType, EPTypeClass initValueClass) {
        return "Variable '" + variableName
                + "' of declared type " + ClassHelperPrint.getClassNameFullyQualPretty(variableType) +
                " cannot be assigned a value of type " + ClassHelperPrint.getClassNameFullyQualPretty(initValueClass);
    }

    public static void configureVariables(VariableRepositoryPreconfigured repo, Map<String, ConfigurationCommonVariable> variables, ClasspathImportService classpathImportService, EventBeanTypedEventFactory eventBeanTypedEventFactory, EventTypeRepositoryImpl eventTypeRepositoryPreconfigured, BeanEventTypeFactory beanEventTypeFactory) throws ConfigurationException {
        for (Map.Entry<String, ConfigurationCommonVariable> entry : variables.entrySet()) {
            String variableName = entry.getKey().trim();
            if (repo.getMetadata(variableName) != null) {
                continue;
            }

            VariableMetaData meta;
            try {
                ClassDescriptor variableType = ClassDescriptor.parseTypeText(entry.getValue().getType());
                VariableMetadataWithForgables result = getTypeInfo(variableName, null, NameAccessModifier.PRECONFIGURED, null, null, null, variableType, true, entry.getValue().isConstant(), entry.getValue().isConstant(), entry.getValue().getInitializationValue(), classpathImportService, ClasspathExtensionClassEmpty.INSTANCE, eventBeanTypedEventFactory, eventTypeRepositoryPreconfigured, beanEventTypeFactory, null, null);
                meta = result.getVariableMetaData();
            } catch (Throwable t) {
                throw new ConfigurationException("Error configuring variable '" + variableName + "': " + t.getMessage(), t);
            }

            repo.addVariable(variableName, meta);
        }
    }

    public static VariableMetadataWithForgables compileVariable(String variableName, String variableModuleName, NameAccessModifier variableVisibility, String optionalContextName, NameAccessModifier optionalContextVisibility, String optionalModuleName, ClassDescriptor variableType, boolean isConstant, boolean compileTimeConstant, Object initializationValue, StatementRawInfo raw, StatementCompileTimeServices services) throws ExprValidationException {
        try {
            return getTypeInfo(variableName, variableModuleName, variableVisibility, optionalContextName, optionalContextVisibility, optionalModuleName, variableType, false, isConstant, compileTimeConstant, initializationValue, services.getClasspathImportServiceCompileTime(), services.getClassProvidedClasspathExtension(), EventBeanTypedEventFactoryCompileTime.INSTANCE, services.getEventTypeRepositoryPreconfigured(), services.getBeanEventTypeFactoryPrivate(), raw, services);
        } catch (VariableTypeException t) {
            throw new ExprValidationException(t.getMessage(), t);
        } catch (Throwable t) {
            throw new ExprValidationException("Failed to compile variable '" + variableName + "': " + t.getMessage(), t);
        }
    }

    public static String checkVariableContextName(String optionalStatementContextName, VariableMetaData variableMetaData) {
        if (optionalStatementContextName == null) {
            if (variableMetaData.getOptionalContextName() != null) {
                return "Variable '" + variableMetaData.getVariableName() + "' defined for use with context '" + variableMetaData.getOptionalContextName() + "' can only be accessed within that context";
            }
        } else {
            if (variableMetaData.getOptionalContextName() != null &&
                    !variableMetaData.getOptionalContextName().equals(optionalStatementContextName)) {
                return "Variable '" + variableMetaData.getVariableName() + "' defined for use with context '" + variableMetaData.getOptionalContextName() + "' is not available for use with context '" + optionalStatementContextName + "'";
            }
        }
        return null;
    }

    private static VariableMetadataWithForgables getTypeInfo(String variableName,
                                                             String variableModuleName,
                                                             NameAccessModifier variableVisibility,
                                                             String optionalContextName,
                                                             NameAccessModifier optionalContextVisibility,
                                                             String optionalContextModule,
                                                             ClassDescriptor variableTypeWArray,
                                                             boolean preconfigured,
                                                             boolean constant,
                                                             boolean compileTimeConstant,
                                                             Object valueAsProvided,
                                                             ClasspathImportService classpathImportService,
                                                             ClasspathExtensionClass classpathExtension,
                                                             EventBeanTypedEventFactory eventBeanTypedEventFactory,
                                                             EventTypeRepositoryImpl eventTypeRepositoryPreconfigured,
                                                             BeanEventTypeFactory beanEventTypeFactory,
                                                             StatementRawInfo optionalRaw,
                                                             StatementCompileTimeServices optionalServices) throws VariableTypeException {

        EPTypeClass variableClass = null;
        List<StmtClassForgeableFactory> serdeForgeables = Collections.emptyList();
        ExprValidationException exTypeResolution = null;
        try {
            variableClass = ClasspathImportEPTypeUtil.resolveClassIdentifierToEPType(variableTypeWArray, true, classpathImportService, classpathExtension);
            if (variableClass == null) {
                throw new ExprValidationException("Failed to resolve type parameter '" + variableTypeWArray.toEPL() + "'");
            }
        } catch (ExprValidationException ex) {
            exTypeResolution = ex;
        }

        EventType variableEventType = null;
        if (variableClass == null) {
            variableEventType = eventTypeRepositoryPreconfigured.getTypeByName(variableTypeWArray.getClassIdentifier());

            if (variableEventType == null && optionalServices != null) {
                variableEventType = optionalServices.getEventTypeCompileTimeResolver().getTypeByName(variableTypeWArray.getClassIdentifier());
                serdeForgeables = SerdeEventTypeUtility.plan(variableEventType, optionalRaw, optionalServices.getSerdeEventTypeRegistry(), optionalServices.getSerdeResolver(), optionalServices.getStateMgmtSettingsProvider());
            }

            if (variableEventType != null) {
                variableClass = variableEventType.getUnderlyingEPType();
            }
        }

        if (variableClass == null) {
            throw new VariableTypeException("Cannot create variable '" + variableName + "', type '" +
                variableTypeWArray.getClassIdentifier() + "' is not a recognized type", exTypeResolution);
        }
        if (variableEventType != null && (variableTypeWArray.getArrayDimensions() > 0 || !variableTypeWArray.getTypeParameters().isEmpty())) {
            throw new VariableTypeException("Cannot create variable '" + variableName + "', type '" +
                variableTypeWArray.getClassIdentifier() + "' cannot be declared as an array type and cannot receive type parameters as it is an event type", exTypeResolution);
        }

        if ((variableEventType == null) && (!JavaClassHelper.isJavaBuiltinDataType(variableClass)) && (variableClass.getType() != Object.class) && !variableClass.getType().isArray() && !variableClass.getType().isEnum()
                && isFragmentableType(variableClass)) {
            if (variableTypeWArray.getArrayDimensions() > 0) {
                throw new VariableTypeException("Cannot create variable '" + variableName + "', type '" +
                    variableTypeWArray.getClassIdentifier() + "' cannot be declared as an array, only scalar types can be array");
            }
            variableEventType = beanEventTypeFactory.getCreateBeanType(variableClass, false);
        }

        Object coerced = getCoercedValue(valueAsProvided, variableEventType, variableName, variableClass, eventBeanTypedEventFactory);
        VariableMetaData variableMetaData = new VariableMetaData(variableName, variableModuleName, variableVisibility, optionalContextName, optionalContextVisibility, optionalContextModule, variableClass, variableEventType, preconfigured, constant, compileTimeConstant, coerced, true);
        return new VariableMetadataWithForgables(variableMetaData, serdeForgeables);
    }

    private static Object getCoercedValue(Object value, EventType eventType, String variableName, EPTypeClass variableType, EventBeanTypedEventFactory eventBeanTypedEventFactory) throws VariableTypeException {

        Object coercedValue = value;

        if (eventType != null) {
            if ((value != null) && (!JavaClassHelper.isSubclassOrImplementsInterface(value.getClass(), eventType.getUnderlyingType()))) {
                throw new VariableTypeException("Variable '" + variableName
                        + "' of declared event type '" + eventType.getName() + "' underlying type '" + eventType.getUnderlyingType().getName() +
                        "' cannot be assigned a value of type '" + value.getClass().getName() + "'");
            }
            if (eventBeanTypedEventFactory != EventBeanTypedEventFactoryCompileTime.INSTANCE) {
                coercedValue = eventBeanTypedEventFactory.adapterForTypedBean(value, eventType);
            }
        } else if (variableType.getType() == java.lang.Object.class) {
            // no validation
        } else {
            // allow string assignments to non-string variables
            if ((coercedValue != null) && (coercedValue instanceof String)) {
                try {
                    coercedValue = JavaClassHelper.parse(variableType.getType(), (String) coercedValue);
                } catch (Exception ex) {
                    throw new VariableTypeException("Variable '" + variableName
                            + "' of declared type " + ClassHelperPrint.getClassNameFullyQualPretty(variableType) +
                            " cannot be initialized by value '" + coercedValue + "': " + ex.toString());
                }
            }

            if ((coercedValue != null) && (!JavaClassHelper.isSubclassOrImplementsInterface(coercedValue.getClass(), variableType.getType()))) {
                // if the declared type is not numeric or the init value is not numeric, fail
                if ((!JavaClassHelper.isNumeric(variableType)) || (!(coercedValue instanceof Number))) {
                    throw getVariableTypeException(variableName, variableType.getType(), coercedValue.getClass());
                }
                if (!(JavaClassHelper.canCoerce(coercedValue.getClass(), variableType.getType()))) {
                    throw getVariableTypeException(variableName, variableType.getType(), coercedValue.getClass());
                }
                // coerce
                coercedValue = JavaClassHelper.coerceBoxed((Number) coercedValue, variableType.getType());
            }
        }

        return coercedValue;
    }

    private static VariableTypeException getVariableTypeException(String variableName, Class variableType, Class initValueClass) {
        return new VariableTypeException("Variable '" + variableName
                + "' of declared type " + ClassHelperPrint.getClassNameFullyQualPretty(variableType) +
                " cannot be initialized by a value of type " + ClassHelperPrint.getClassNameFullyQualPretty(initValueClass));
    }
}
