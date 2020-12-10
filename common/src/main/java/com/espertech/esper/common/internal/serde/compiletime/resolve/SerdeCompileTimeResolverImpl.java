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
package com.espertech.esper.common.internal.serde.compiletime.resolve;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.hook.expr.EPLMethodInvocationContext;
import com.espertech.esper.common.client.serde.*;
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.event.core.BaseNestableEventType;
import com.espertech.esper.common.internal.serde.serdeset.builtin.DIOSkipSerde;
import com.espertech.esper.common.internal.serde.serdeset.multikey.DIOMultiKeyArraySerde;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner.getMKSerdeClassForComponentType;

public class SerdeCompileTimeResolverImpl implements SerdeCompileTimeResolver {

    private final boolean allowExtendedJVM;
    private final boolean allowSerializable;
    private final boolean allowExternalizable;
    private final boolean allowSerializationFallback;
    private final Collection<SerdeProvider> serdeProviders;

    public SerdeCompileTimeResolverImpl(List<SerdeProvider> serdeProviders, boolean allowExtendedJVM, boolean allowSerializable, boolean allowExternalizable, boolean allowSerializationFallback) {
        this.serdeProviders = new CopyOnWriteArrayList<>(serdeProviders);
        this.allowExtendedJVM = allowExtendedJVM;
        this.allowSerializable = allowSerializable;
        this.allowExternalizable = allowExternalizable;
        this.allowSerializationFallback = allowSerializationFallback;
    }

    public boolean isTargetHA() {
        return true;
    }

    public DataInputOutputSerdeForge serdeForFilter(EPType evaluationType, StatementRawInfo raw) {
        return serdeMayArray(evaluationType, new SerdeProviderAdditionalInfoFilter(raw));
    }

    public DataInputOutputSerdeForge serdeForKeyNonArray(EPType paramType, StatementRawInfo raw) {
        return serdeForClass(paramType, new SerdeProviderAdditionalInfoMultikey(raw));
    }

    public DataInputOutputSerdeForge[] serdeForMultiKey(EPType[] types, StatementRawInfo raw) {
        return serdeForClasses(types, new SerdeProviderAdditionalInfoMultikey(raw));
    }

    public DataInputOutputSerdeForge[] serdeForDataWindowSortCriteria(EPType[] sortCriteriaExpressions, StatementRawInfo raw) {
        return serdeForClasses(sortCriteriaExpressions, new SerdeProviderAdditionalInfoMultikey(raw));
    }

    public DataInputOutputSerdeForge serdeForDerivedViewAddProp(EPType evalType, StatementRawInfo raw) {
        return serdeForClass(evalType, new SerdeProviderAdditionalInfoDerivedViewProperty(raw));
    }

    public DataInputOutputSerdeForge serdeForIndexHashNonArray(EPTypeClass propType, StatementRawInfo raw) {
        return serdeForClass(propType, new SerdeProviderAdditionalInfoIndex(raw));
    }

    public DataInputOutputSerdeForge serdeForBeanEventType(StatementRawInfo raw, EPTypeClass underlyingType, String eventTypeName, EventType[] eventTypeSupertypes) {
        return serdeForClass(underlyingType, new SerdeProviderAdditionalInfoEventType(raw, eventTypeName, eventTypeSupertypes));
    }

    public DataInputOutputSerdeForge serdeForEventProperty(EPTypeClass typedProperty, String eventTypeName, String propertyName, StatementRawInfo raw) {
        return serdeForClass(typedProperty, new SerdeProviderAdditionalInfoEventProperty(raw, eventTypeName, propertyName));
    }

    public DataInputOutputSerdeForge serdeForAggregation(EPType type, StatementRawInfo raw) {
        return serdeForClass(type, new SerdeProviderAdditionalInfoAggregation(raw));
    }

    public DataInputOutputSerdeForge serdeForAggregationDistinct(EPType type, StatementRawInfo raw) {
        return serdeMayArray(type, new SerdeProviderAdditionalInfoAggregationDistinct(raw));
    }

    public DataInputOutputSerdeForge serdeForIndexBtree(EPTypeClass rangeType, StatementRawInfo raw) {
        return serdeForClass(rangeType, new SerdeProviderAdditionalInfoIndex(raw));
    }

    public DataInputOutputSerdeForge serdeForVariable(EPTypeClass type, String variableName, StatementRawInfo raw) {
        return serdeForClass(type, new SerdeProviderAdditionalInfoVariable(raw, variableName));
    }

    public DataInputOutputSerdeForge serdeForEventTypeExternalProvider(BaseNestableEventType eventType, StatementRawInfo raw) {
        if (serdeProviders.isEmpty()) {
            return null;
        }
        SerdeProviderEventTypeContext context = new SerdeProviderEventTypeContext(raw, eventType);
        for (SerdeProvider provider : serdeProviders) {
            try {
                SerdeProvision serde = provider.resolveSerdeForEventType(context);
                if (serde != null) {
                    return serde.toForge();
                }
            } catch (DataInputOutputSerdeException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                throw new DataInputOutputSerdeException("Unexpected exception invoking serde provider '" + provider.getClass().getName() + "' passing for event type '" + eventType.getName() + "': " + ex.getMessage(), ex);
            }
        }
        return null;
    }

    private DataInputOutputSerdeForge serdeMayArray(EPType type, SerdeProviderAdditionalInfo info) {
        if (type != EPTypeNull.INSTANCE && ((EPTypeClass) type).getType().isArray()) {
            EPTypeClass component = JavaClassHelper.getArrayComponentType((EPTypeClass) type);
            DIOMultiKeyArraySerde mkSerde = getMKSerdeClassForComponentType(component);
            return new DataInputOutputSerdeForgeSingletonMKArray(mkSerde.getClass(), mkSerde.componentType().getSimpleName());
        }
        return serdeForClass(type, info);
    }

    private DataInputOutputSerdeForge[] serdeForClasses(EPType[] sortCriteriaExpressions, SerdeProviderAdditionalInfo additionalInfo) {
        DataInputOutputSerdeForge[] forges = new DataInputOutputSerdeForge[sortCriteriaExpressions.length];
        for (int i = 0; i < sortCriteriaExpressions.length; i++) {
            forges[i] = serdeForClass(sortCriteriaExpressions[i], additionalInfo);
        }
        return forges;
    }

    private DataInputOutputSerdeForge serdeForClass(EPType type, SerdeProviderAdditionalInfo additionalInfo) {
        if (type == EPTypeNull.INSTANCE) {
            return DataInputOutputSerdeForgeSkip.INSTANCE;
        }
        EPTypeClass typeClass = (EPTypeClass) type;
        if (isJVMBasicBuiltin(typeClass.getType())) {
            DataInputOutputSerde serde = VMBasicBuiltinSerdeFactory.getSerde(typeClass.getType());
            if (serde == null) {
                throw new DataInputOutputSerdeException("Failed to find built-in serde for class " + typeClass.getType().getName());
            }
            return new DataInputOutputSerdeForgeSingletonBasicBuiltin(serde.getClass(), typeClass);
        }

        if (allowExtendedJVM) {
            DataInputOutputSerde serde = VMExtendedBuiltinSerdeFactory.getSerde(typeClass);
            if (serde != null) {
                return new DataInputOutputSerdeForgeSingletonExtendedBuiltin(serde.getClass(), typeClass);
            }
        }

        if (typeClass.getType() == EPLMethodInvocationContext.class) {
            return new DataInputOutputSerdeForgeSingleton(DIOSkipSerde.class);
        }

        SerdeProvision provision = SerdeCompileTimeResolverUtil.determineSerde(typeClass, serdeProviders, allowSerializable, allowExternalizable, allowSerializationFallback, additionalInfo);
        return provision.toForge();
    }

    private boolean isJVMBasicBuiltin(Class type) {
        return JavaClassHelper.isJavaBuiltinDataType(type) && type != BigInteger.class && type != BigDecimal.class;
    }
}
