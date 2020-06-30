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
package com.espertech.esper.common.internal.event.json.parser.forge;

import com.espertech.esper.common.client.annotation.JsonSchemaField;
import com.espertech.esper.common.client.json.util.JsonFieldAdapterString;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.json.compiletime.JsonApplicationClassDelegateDesc;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateJsonGenericArray;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateJsonGenericObject;
import com.espertech.esper.common.internal.event.json.parser.delegates.array.*;
import com.espertech.esper.common.internal.event.json.parser.delegates.array2dim.*;
import com.espertech.esper.common.internal.event.json.parser.delegates.endvalue.*;
import com.espertech.esper.common.internal.event.json.write.*;
import com.espertech.esper.common.internal.settings.ClasspathExtensionClassEmpty;
import com.espertech.esper.common.internal.settings.ClasspathImportException;
import com.espertech.esper.common.internal.util.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.common.client.type.EPTypePremade.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;

public class JsonForgeFactoryBuiltinClassTyped {
    private final static Map<EPTypeClass, JsonEndValueForge> END_VALUE_FORGES = new HashMap<>();
    private final static Map<EPTypeClass, EPTypeClass> START_ARRAY_FORGES = new HashMap<>();
    private final static Map<EPTypeClass, EPTypeClass> START_COLLECTION_FORGES = new HashMap<>();
    private final static Map<EPTypeClass, JsonWriteForge> WRITE_FORGES = new HashMap<>();
    private final static Map<EPTypeClass, JsonWriteForge> WRITE_ARRAY_FORGES = new HashMap<>();
    private final static Map<EPTypeClass, JsonWriteForge> WRITE_COLLECTION_FORGES = new HashMap<>();

    static {
        END_VALUE_FORGES.put(STRING.getEPType(), JsonEndValueForgeString.INSTANCE);
        END_VALUE_FORGES.put(CHARBOXED.getEPType(), JsonEndValueForgeCharacter.INSTANCE);
        END_VALUE_FORGES.put(BOOLEANBOXED.getEPType(), JsonEndValueForgeBoolean.INSTANCE);
        END_VALUE_FORGES.put(BYTEBOXED.getEPType(), JsonEndValueForgeByte.INSTANCE);
        END_VALUE_FORGES.put(SHORTBOXED.getEPType(), JsonEndValueForgeShort.INSTANCE);
        END_VALUE_FORGES.put(INTEGERBOXED.getEPType(), JsonEndValueForgeInteger.INSTANCE);
        END_VALUE_FORGES.put(LONGBOXED.getEPType(), JsonEndValueForgeLong.INSTANCE);
        END_VALUE_FORGES.put(DOUBLEBOXED.getEPType(), JsonEndValueForgeDouble.INSTANCE);
        END_VALUE_FORGES.put(FLOATBOXED.getEPType(), JsonEndValueForgeFloat.INSTANCE);
        END_VALUE_FORGES.put(BIGDECIMAL.getEPType(), JsonEndValueForgeBigDecimal.INSTANCE);
        END_VALUE_FORGES.put(BIGINTEGER.getEPType(), JsonEndValueForgeBigInteger.INSTANCE);

        END_VALUE_FORGES.put(UUID.getEPType(), JsonEndValueForgeUUID.INSTANCE);
        END_VALUE_FORGES.put(OFFSETDATETIME.getEPType(), JsonEndValueForgeOffsetDateTime.INSTANCE);
        END_VALUE_FORGES.put(LOCALDATE.getEPType(), JsonEndValueForgeLocalDate.INSTANCE);
        END_VALUE_FORGES.put(LOCALDATETIME.getEPType(), JsonEndValueForgeLocalDateTime.INSTANCE);
        END_VALUE_FORGES.put(ZONEDDATETIME.getEPType(), JsonEndValueForgeZonedDateTime.INSTANCE);
        END_VALUE_FORGES.put(NETURL.getEPType(), JsonEndValueForgeURL.INSTANCE);
        END_VALUE_FORGES.put(NETURI.getEPType(), JsonEndValueForgeURI.INSTANCE);

        WRITE_FORGES.put(STRING.getEPType(), JsonWriteForgeString.INSTANCE);
        WRITE_FORGES.put(CHARBOXED.getEPType(), JsonWriteForgeStringWithToString.INSTANCE);
        WRITE_FORGES.put(BOOLEANBOXED.getEPType(), JsonWriteForgeBoolean.INSTANCE);
        WRITE_FORGES.put(BYTEBOXED.getEPType(), JsonWriteForgeNumberWithToString.INSTANCE);
        WRITE_FORGES.put(SHORTBOXED.getEPType(), JsonWriteForgeNumberWithToString.INSTANCE);
        WRITE_FORGES.put(INTEGERBOXED.getEPType(), JsonWriteForgeNumberWithToString.INSTANCE);
        WRITE_FORGES.put(LONGBOXED.getEPType(), JsonWriteForgeNumberWithToString.INSTANCE);
        WRITE_FORGES.put(DOUBLEBOXED.getEPType(), JsonWriteForgeNumberWithToString.INSTANCE);
        WRITE_FORGES.put(FLOATBOXED.getEPType(), JsonWriteForgeNumberWithToString.INSTANCE);
        WRITE_FORGES.put(BIGDECIMAL.getEPType(), JsonWriteForgeNumberWithToString.INSTANCE);
        WRITE_FORGES.put(BIGINTEGER.getEPType(), JsonWriteForgeNumberWithToString.INSTANCE);
        for (EPTypeClass clazz : new EPTypeClass[]{UUID.getEPType(), OFFSETDATETIME.getEPType(), LOCALDATE.getEPType(), LOCALDATETIME.getEPType(), ZONEDDATETIME.getEPType(),
            NETURL.getEPType(), NETURI.getEPType()}) {
            WRITE_FORGES.put(clazz, JsonWriteForgeStringWithToString.INSTANCE);
        }

        START_ARRAY_FORGES.put(STRINGARRAY.getEPType(), JsonDelegateArrayString.EPTYPE);
        START_ARRAY_FORGES.put(CHARBOXEDARRAY.getEPType(), JsonDelegateArrayCharacter.EPTYPE);
        START_ARRAY_FORGES.put(BOOLEANBOXEDARRAY.getEPType(), JsonDelegateArrayBoolean.EPTYPE);
        START_ARRAY_FORGES.put(BYTEBOXEDARRAY.getEPType(), JsonDelegateArrayByte.EPTYPE);
        START_ARRAY_FORGES.put(SHORTBOXEDARRAY.getEPType(), JsonDelegateArrayShort.EPTYPE);
        START_ARRAY_FORGES.put(INTEGERBOXEDARRAY.getEPType(), JsonDelegateArrayInteger.EPTYPE);
        START_ARRAY_FORGES.put(LONGBOXEDARRAY.getEPType(), JsonDelegateArrayLong.EPTYPE);
        START_ARRAY_FORGES.put(DOUBLEBOXEDARRAY.getEPType(), JsonDelegateArrayDouble.EPTYPE);
        START_ARRAY_FORGES.put(FLOATBOXEDARRAY.getEPType(), JsonDelegateArrayFloat.EPTYPE);
        START_ARRAY_FORGES.put(CHARPRIMITIVEARRAY.getEPType(), JsonDelegateArrayCharacterPrimitive.EPTYPE);
        START_ARRAY_FORGES.put(BOOLEANPRIMITIVEARRAY.getEPType(), JsonDelegateArrayBooleanPrimitive.EPTYPE);
        START_ARRAY_FORGES.put(BYTEPRIMITIVEARRAY.getEPType(), JsonDelegateArrayBytePrimitive.EPTYPE);
        START_ARRAY_FORGES.put(SHORTPRIMITIVEARRAY.getEPType(), JsonDelegateArrayShortPrimitive.EPTYPE);
        START_ARRAY_FORGES.put(INTEGERPRIMITIVEARRAY.getEPType(), JsonDelegateArrayIntegerPrimitive.EPTYPE);
        START_ARRAY_FORGES.put(LONGPRIMITIVEARRAY.getEPType(), JsonDelegateArrayLongPrimitive.EPTYPE);
        START_ARRAY_FORGES.put(DOUBLEPRIMITIVEARRAY.getEPType(), JsonDelegateArrayDoublePrimitive.EPTYPE);
        START_ARRAY_FORGES.put(FLOATPRIMITIVEARRAY.getEPType(), JsonDelegateArrayFloatPrimitive.EPTYPE);
        START_ARRAY_FORGES.put(BIGDECIMALARRAY.getEPType(), JsonDelegateArrayBigDecimal.EPTYPE);
        START_ARRAY_FORGES.put(BIGINTEGERARRAY.getEPType(), JsonDelegateArrayBigInteger.EPTYPE);
        START_ARRAY_FORGES.put(UUIDARRAY.getEPType(), JsonDelegateArrayUUID.EPTYPE);
        START_ARRAY_FORGES.put(OFFSETDATETIMEARRAY.getEPType(), JsonDelegateArrayOffsetDateTime.EPTYPE);
        START_ARRAY_FORGES.put(LOCALDATEARRAY.getEPType(), JsonDelegateArrayLocalDate.EPTYPE);
        START_ARRAY_FORGES.put(LOCALDATETIMEARRAY.getEPType(), JsonDelegateArrayLocalDateTime.EPTYPE);
        START_ARRAY_FORGES.put(ZONEDDATETIMEARRAY.getEPType(), JsonDelegateArrayZonedDateTime.EPTYPE);
        START_ARRAY_FORGES.put(NETURLARRAY.getEPType(), JsonDelegateArrayURL.EPTYPE);
        START_ARRAY_FORGES.put(NETURIARRAY.getEPType(), JsonDelegateArrayURI.EPTYPE);

        START_ARRAY_FORGES.put(STRINGARRAYARRAY.getEPType(), JsonDelegateArray2DimString.EPTYPE);
        START_ARRAY_FORGES.put(CHARBOXEDARRAYARRAY.getEPType(), JsonDelegateArray2DimCharacter.EPTYPE);
        START_ARRAY_FORGES.put(BOOLEANBOXEDARRAYARRAY.getEPType(), JsonDelegateArray2DimBoolean.EPTYPE);
        START_ARRAY_FORGES.put(BYTEBOXEDARRAYARRAY.getEPType(), JsonDelegateArray2DimByte.EPTYPE);
        START_ARRAY_FORGES.put(SHORTBOXEDARRAYARRAY.getEPType(), JsonDelegateArray2DimShort.EPTYPE);
        START_ARRAY_FORGES.put(INTEGERBOXEDARRAYARRAY.getEPType(), JsonDelegateArray2DimInteger.EPTYPE);
        START_ARRAY_FORGES.put(LONGBOXEDARRAYARRAY.getEPType(), JsonDelegateArray2DimLong.EPTYPE);
        START_ARRAY_FORGES.put(DOUBLEBOXEDARRAYARRAY.getEPType(), JsonDelegateArray2DimDouble.EPTYPE);
        START_ARRAY_FORGES.put(FLOATBOXEDARRAYARRAY.getEPType(), JsonDelegateArray2DimFloat.EPTYPE);
        START_ARRAY_FORGES.put(CHARPRIMITIVEARRAYARRAY.getEPType(), JsonDelegateArray2DimCharacterPrimitive.EPTYPE);
        START_ARRAY_FORGES.put(BOOLEANPRIMITIVEARRAYARRAY.getEPType(), JsonDelegateArray2DimBooleanPrimitive.EPTYPE);
        START_ARRAY_FORGES.put(BYTEPRIMITIVEARRAYARRAY.getEPType(), JsonDelegateArray2DimBytePrimitive.EPTYPE);
        START_ARRAY_FORGES.put(SHORTPRIMITIVEARRAYARRAY.getEPType(), JsonDelegateArray2DimShortPrimitive.EPTYPE);
        START_ARRAY_FORGES.put(INTEGERPRIMITIVEARRAYARRAY.getEPType(), JsonDelegateArray2DimIntegerPrimitive.EPTYPE);
        START_ARRAY_FORGES.put(LONGPRIMITIVEARRAYARRAY.getEPType(), JsonDelegateArray2DimLongPrimitive.EPTYPE);
        START_ARRAY_FORGES.put(DOUBLEPRIMITIVEARRAYARRAY.getEPType(), JsonDelegateArray2DimDoublePrimitive.EPTYPE);
        START_ARRAY_FORGES.put(FLOATPRIMITIVEARRAYARRAY.getEPType(), JsonDelegateArray2DimFloatPrimitive.EPTYPE);
        START_ARRAY_FORGES.put(BIGDECIMALARRAYARRAY.getEPType(), JsonDelegateArray2DimBigDecimal.EPTYPE);
        START_ARRAY_FORGES.put(BIGINTEGERARRAYARRAY.getEPType(), JsonDelegateArray2DimBigInteger.EPTYPE);
        START_ARRAY_FORGES.put(UUIDARRAYARRAY.getEPType(), JsonDelegateArray2DimUUID.EPTYPE);
        START_ARRAY_FORGES.put(OFFSETDATETIMEARRAYARRAY.getEPType(), JsonDelegateArray2DimOffsetDateTime.EPTYPE);
        START_ARRAY_FORGES.put(LOCALDATEARRAYARRAY.getEPType(), JsonDelegateArray2DimLocalDate.EPTYPE);
        START_ARRAY_FORGES.put(LOCALDATETIMEARRAYARRAY.getEPType(), JsonDelegateArray2DimLocalDateTime.EPTYPE);
        START_ARRAY_FORGES.put(ZONEDDATETIMEARRAYARRAY.getEPType(), JsonDelegateArray2DimZonedDateTime.EPTYPE);
        START_ARRAY_FORGES.put(NETURLARRAYARRAY.getEPType(), JsonDelegateArray2DimURL.EPTYPE);
        START_ARRAY_FORGES.put(NETURIARRAYARRAY.getEPType(), JsonDelegateArray2DimURI.EPTYPE);

        WRITE_ARRAY_FORGES.put(STRINGARRAY.getEPType(), new JsonWriteForgeByMethod("writeArrayString"));
        WRITE_ARRAY_FORGES.put(CHARBOXEDARRAY.getEPType(), new JsonWriteForgeByMethod("writeArrayCharacter"));
        WRITE_ARRAY_FORGES.put(BOOLEANBOXEDARRAY.getEPType(), new JsonWriteForgeByMethod("writeArrayBoolean"));
        WRITE_ARRAY_FORGES.put(BYTEBOXEDARRAY.getEPType(), new JsonWriteForgeByMethod("writeArrayByte"));
        WRITE_ARRAY_FORGES.put(SHORTBOXEDARRAY.getEPType(), new JsonWriteForgeByMethod("writeArrayShort"));
        WRITE_ARRAY_FORGES.put(INTEGERBOXEDARRAY.getEPType(), new JsonWriteForgeByMethod("writeArrayInteger"));
        WRITE_ARRAY_FORGES.put(LONGBOXEDARRAY.getEPType(), new JsonWriteForgeByMethod("writeArrayLong"));
        WRITE_ARRAY_FORGES.put(DOUBLEBOXEDARRAY.getEPType(), new JsonWriteForgeByMethod("writeArrayDouble"));
        WRITE_ARRAY_FORGES.put(FLOATBOXEDARRAY.getEPType(), new JsonWriteForgeByMethod("writeArrayFloat"));
        WRITE_ARRAY_FORGES.put(CHARPRIMITIVEARRAY.getEPType(), new JsonWriteForgeByMethod("writeArrayCharPrimitive"));
        WRITE_ARRAY_FORGES.put(BOOLEANPRIMITIVEARRAY.getEPType(), new JsonWriteForgeByMethod("writeArrayBooleanPrimitive"));
        WRITE_ARRAY_FORGES.put(BYTEPRIMITIVEARRAY.getEPType(), new JsonWriteForgeByMethod("writeArrayBytePrimitive"));
        WRITE_ARRAY_FORGES.put(SHORTPRIMITIVEARRAY.getEPType(), new JsonWriteForgeByMethod("writeArrayShortPrimitive"));
        WRITE_ARRAY_FORGES.put(INTEGERPRIMITIVEARRAY.getEPType(), new JsonWriteForgeByMethod("writeArrayIntPrimitive"));
        WRITE_ARRAY_FORGES.put(LONGPRIMITIVEARRAY.getEPType(), new JsonWriteForgeByMethod("writeArrayLongPrimitive"));
        WRITE_ARRAY_FORGES.put(DOUBLEPRIMITIVEARRAY.getEPType(), new JsonWriteForgeByMethod("writeArrayDoublePrimitive"));
        WRITE_ARRAY_FORGES.put(FLOATPRIMITIVEARRAY.getEPType(), new JsonWriteForgeByMethod("writeArrayFloatPrimitive"));
        WRITE_ARRAY_FORGES.put(BIGDECIMALARRAY.getEPType(), new JsonWriteForgeByMethod("writeArrayBigDecimal"));
        WRITE_ARRAY_FORGES.put(BIGINTEGERARRAY.getEPType(), new JsonWriteForgeByMethod("writeArrayBigInteger"));
        for (EPTypeClass clazz : new EPTypeClass[]{UUIDARRAY.getEPType(), OFFSETDATETIMEARRAY.getEPType(), LOCALDATEARRAY.getEPType(), LOCALDATETIMEARRAY.getEPType(), ZONEDDATETIMEARRAY.getEPType(),
            NETURLARRAY.getEPType(), NETURIARRAY.getEPType()}) {
            WRITE_ARRAY_FORGES.put(clazz, new JsonWriteForgeByMethod("writeArrayObjectToString"));
        }

        WRITE_ARRAY_FORGES.put(STRINGARRAYARRAY.getEPType(), new JsonWriteForgeByMethod("writeArray2DimString"));
        WRITE_ARRAY_FORGES.put(CHARBOXEDARRAYARRAY.getEPType(), new JsonWriteForgeByMethod("writeArray2DimCharacter"));
        WRITE_ARRAY_FORGES.put(BOOLEANBOXEDARRAYARRAY.getEPType(), new JsonWriteForgeByMethod("writeArray2DimBoolean"));
        WRITE_ARRAY_FORGES.put(BYTEBOXEDARRAYARRAY.getEPType(), new JsonWriteForgeByMethod("writeArray2DimByte"));
        WRITE_ARRAY_FORGES.put(SHORTBOXEDARRAYARRAY.getEPType(), new JsonWriteForgeByMethod("writeArray2DimShort"));
        WRITE_ARRAY_FORGES.put(INTEGERBOXEDARRAYARRAY.getEPType(), new JsonWriteForgeByMethod("writeArray2DimInteger"));
        WRITE_ARRAY_FORGES.put(LONGBOXEDARRAYARRAY.getEPType(), new JsonWriteForgeByMethod("writeArray2DimLong"));
        WRITE_ARRAY_FORGES.put(DOUBLEBOXEDARRAYARRAY.getEPType(), new JsonWriteForgeByMethod("writeArray2DimDouble"));
        WRITE_ARRAY_FORGES.put(FLOATBOXEDARRAYARRAY.getEPType(), new JsonWriteForgeByMethod("writeArray2DimFloat"));
        WRITE_ARRAY_FORGES.put(CHARPRIMITIVEARRAYARRAY.getEPType(), new JsonWriteForgeByMethod("writeArray2DimCharPrimitive"));
        WRITE_ARRAY_FORGES.put(BOOLEANPRIMITIVEARRAYARRAY.getEPType(), new JsonWriteForgeByMethod("writeArray2DimBooleanPrimitive"));
        WRITE_ARRAY_FORGES.put(BYTEPRIMITIVEARRAYARRAY.getEPType(), new JsonWriteForgeByMethod("writeArray2DimBytePrimitive"));
        WRITE_ARRAY_FORGES.put(SHORTPRIMITIVEARRAYARRAY.getEPType(), new JsonWriteForgeByMethod("writeArray2DimShortPrimitive"));
        WRITE_ARRAY_FORGES.put(INTEGERPRIMITIVEARRAYARRAY.getEPType(), new JsonWriteForgeByMethod("writeArray2DimIntPrimitive"));
        WRITE_ARRAY_FORGES.put(LONGPRIMITIVEARRAYARRAY.getEPType(), new JsonWriteForgeByMethod("writeArray2DimLongPrimitive"));
        WRITE_ARRAY_FORGES.put(DOUBLEPRIMITIVEARRAYARRAY.getEPType(), new JsonWriteForgeByMethod("writeArray2DimDoublePrimitive"));
        WRITE_ARRAY_FORGES.put(FLOATPRIMITIVEARRAYARRAY.getEPType(), new JsonWriteForgeByMethod("writeArray2DimFloatPrimitive"));
        WRITE_ARRAY_FORGES.put(BIGDECIMALARRAYARRAY.getEPType(), new JsonWriteForgeByMethod("writeArray2DimBigDecimal"));
        WRITE_ARRAY_FORGES.put(BIGINTEGERARRAYARRAY.getEPType(), new JsonWriteForgeByMethod("writeArray2DimBigInteger"));
        for (EPTypeClass clazz : new EPTypeClass[]{UUIDARRAYARRAY.getEPType(), OFFSETDATETIMEARRAYARRAY.getEPType(), LOCALDATEARRAYARRAY.getEPType(), LOCALDATETIMEARRAYARRAY.getEPType(), ZONEDDATETIMEARRAYARRAY.getEPType(),
            NETURLARRAYARRAY.getEPType(), NETURIARRAYARRAY.getEPType()}) {
            WRITE_ARRAY_FORGES.put(clazz, new JsonWriteForgeByMethod("writeArray2DimObjectToString"));
        }

        START_COLLECTION_FORGES.put(STRING.getEPType(), JsonDelegateCollectionString.EPTYPE);
        START_COLLECTION_FORGES.put(CHARBOXED.getEPType(), JsonDelegateCollectionCharacter.EPTYPE);
        START_COLLECTION_FORGES.put(BOOLEANBOXED.getEPType(), JsonDelegateCollectionBoolean.EPTYPE);
        START_COLLECTION_FORGES.put(BYTEBOXED.getEPType(), JsonDelegateCollectionByte.EPTYPE);
        START_COLLECTION_FORGES.put(SHORTBOXED.getEPType(), JsonDelegateCollectionShort.EPTYPE);
        START_COLLECTION_FORGES.put(INTEGERBOXED.getEPType(), JsonDelegateCollectionInteger.EPTYPE);
        START_COLLECTION_FORGES.put(LONGBOXED.getEPType(), JsonDelegateCollectionLong.EPTYPE);
        START_COLLECTION_FORGES.put(DOUBLEBOXED.getEPType(), JsonDelegateCollectionDouble.EPTYPE);
        START_COLLECTION_FORGES.put(FLOATBOXED.getEPType(), JsonDelegateCollectionFloat.EPTYPE);
        START_COLLECTION_FORGES.put(BIGDECIMAL.getEPType(), JsonDelegateCollectionBigDecimal.EPTYPE);
        START_COLLECTION_FORGES.put(BIGINTEGER.getEPType(), JsonDelegateCollectionBigInteger.EPTYPE);
        START_COLLECTION_FORGES.put(UUID.getEPType(), JsonDelegateCollectionUUID.EPTYPE);
        START_COLLECTION_FORGES.put(OFFSETDATETIME.getEPType(), JsonDelegateCollectionOffsetDateTime.EPTYPE);
        START_COLLECTION_FORGES.put(LOCALDATE.getEPType(), JsonDelegateCollectionLocalDate.EPTYPE);
        START_COLLECTION_FORGES.put(LOCALDATETIME.getEPType(), JsonDelegateCollectionLocalDateTime.EPTYPE);
        START_COLLECTION_FORGES.put(ZONEDDATETIME.getEPType(), JsonDelegateCollectionZonedDateTime.EPTYPE);
        START_COLLECTION_FORGES.put(NETURL.getEPType(), JsonDelegateCollectionURL.EPTYPE);
        START_COLLECTION_FORGES.put(NETURI.getEPType(), JsonDelegateCollectionURI.EPTYPE);

        WRITE_COLLECTION_FORGES.put(STRING.getEPType(), new JsonWriteForgeByMethod("writeCollectionString"));
        WRITE_COLLECTION_FORGES.put(CHARBOXED.getEPType(), new JsonWriteForgeByMethod("writeCollectionWToString"));
        WRITE_COLLECTION_FORGES.put(BOOLEANBOXED.getEPType(), new JsonWriteForgeByMethod("writeCollectionBoolean"));
        WRITE_COLLECTION_FORGES.put(BYTEBOXED.getEPType(), new JsonWriteForgeByMethod("writeCollectionNumber"));
        WRITE_COLLECTION_FORGES.put(SHORTBOXED.getEPType(), new JsonWriteForgeByMethod("writeCollectionNumber"));
        WRITE_COLLECTION_FORGES.put(INTEGERBOXED.getEPType(), new JsonWriteForgeByMethod("writeCollectionNumber"));
        WRITE_COLLECTION_FORGES.put(LONGBOXED.getEPType(), new JsonWriteForgeByMethod("writeCollectionNumber"));
        WRITE_COLLECTION_FORGES.put(DOUBLEBOXED.getEPType(), new JsonWriteForgeByMethod("writeCollectionNumber"));
        WRITE_COLLECTION_FORGES.put(FLOATBOXED.getEPType(), new JsonWriteForgeByMethod("writeCollectionNumber"));
        WRITE_COLLECTION_FORGES.put(BIGDECIMAL.getEPType(), new JsonWriteForgeByMethod("writeCollectionNumber"));
        WRITE_COLLECTION_FORGES.put(BIGINTEGER.getEPType(), new JsonWriteForgeByMethod("writeCollectionNumber"));
        for (EPTypeClass clazz : new EPTypeClass[]{UUID.getEPType(), OFFSETDATETIME.getEPType(), LOCALDATE.getEPType(), LOCALDATETIME.getEPType(), ZONEDDATETIME.getEPType(),
            NETURL.getEPType(), NETURI.getEPType()}) {
            WRITE_COLLECTION_FORGES.put(clazz, new JsonWriteForgeByMethod("writeCollectionWToString"));
        }
    }

    public static JsonForgeDesc forge(EPTypeClass classType, String fieldName, Field optionalField, Map<EPTypeClass, JsonApplicationClassDelegateDesc> deepClasses, Annotation[] annotations, StatementCompileTimeServices services) throws ExprValidationException {
        classType = JavaClassHelper.getBoxedType(classType);
        JsonDelegateForge startObject = null;
        JsonDelegateForge startArray = null;
        JsonEndValueForge end = END_VALUE_FORGES.get(classType);
        JsonWriteForge write = WRITE_FORGES.get(classType);

        JsonSchemaField fieldAnnotation = findFieldAnnotation(fieldName, annotations);
        EPTypeClass type = classType;

        if (fieldAnnotation != null) {
            EPTypeClass clazz;
            try {
                Class resolved = services.getClasspathImportServiceCompileTime().resolveClass(fieldAnnotation.adapter(), true, ClasspathExtensionClassEmpty.INSTANCE);
                clazz = ClassHelperGenericType.getClassEPType(resolved);
            } catch (ClasspathImportException e) {
                throw new ExprValidationException("Failed to resolve Json schema field adapter class: " + e.getMessage(), e);
            }
            if (!JavaClassHelper.isImplementsInterface(clazz, JsonFieldAdapterString.class)) {
                throw new ExprValidationException("Json schema field adapter class does not implement interface '" + JsonFieldAdapterString.class.getSimpleName());
            }
            if (!ConstructorHelper.hasDefaultConstructor(clazz.getType())) {
                throw new ExprValidationException("Json schema field adapter class '" + clazz + "' does not have a default constructor");
            }
            Method writeMethod;
            try {
                writeMethod = MethodResolver.resolveMethod(clazz.getType(), "parse", new EPTypeClass[]{STRING.getEPType()}, true, new boolean[1], new boolean[1]);
            } catch (MethodResolverNoSuchMethodException e) {
                throw new ExprValidationException("Failed to resolve write method of Json schema field adapter class: " + e.getMessage(), e);
            }
            if (!JavaClassHelper.isSubclassOrImplementsInterface(type, writeMethod.getReturnType())) {
                throw new ExprValidationException("Json schema field adapter class '" + clazz + "' mismatches the return type of the parse method, expected '" + type + "' but found '" + writeMethod.getReturnType().getSimpleName() + "'");
            }
            end = new JsonEndValueForgeProvidedStringAdapter(clazz);
            write = new JsonWriteForgeProvidedStringAdapter(clazz);
        } else if (type.getType() == Object.class) {
            startObject = new JsonDelegateForgeByClass(JsonDelegateJsonGenericObject.EPTYPE);
            startArray = new JsonDelegateForgeByClass(JsonDelegateJsonGenericArray.EPTYPE);
            end = JsonEndValueForgeJsonValue.INSTANCE;
            write = new JsonWriteForgeByMethod("writeJsonValue");
        } else if (type.getType() == Object[].class) {
            startArray = new JsonDelegateForgeByClass(JsonDelegateJsonGenericArray.EPTYPE);
            end = new JsonEndValueForgeCast(EPTypePremade.OBJECTARRAY.getEPType());
            write = new JsonWriteForgeByMethod("writeJsonArray");
        } else if (type.getType() == Map.class) {
            startObject = new JsonDelegateForgeByClass(JsonDelegateJsonGenericObject.EPTYPE);
            end = new JsonEndValueForgeCast(EPTypePremade.MAP.getEPType());
            write = new JsonWriteForgeByMethod("writeJsonMap");
        } else if (type.getType().isEnum()) {
            end = new JsonEndValueForgeEnum(classType);
            write = JsonWriteForgeStringWithToString.INSTANCE;
        } else if (type.getType().isArray()) {
            if (type.getType().getComponentType().isEnum()) {
                startArray = new JsonDelegateForgeByClass(JsonDelegateArrayEnum.EPTYPE, constant(JavaClassHelper.getArrayComponentType(type)));
                write = new JsonWriteForgeByMethod("writeEnumArray");
            } else if (type.getType().getComponentType().isArray() && type.getType().getComponentType().getComponentType().isEnum()) {
                startArray = new JsonDelegateForgeByClass(JsonDelegateArray2DimEnum.EPTYPE, constant(JavaClassHelper.getArrayComponentType(JavaClassHelper.getArrayComponentType(type))));
                write = new JsonWriteForgeByMethod("writeEnumArray2Dim");
            } else {
                EPTypeClass componentTypeInnermost = JavaClassHelper.getArrayComponentTypeInnermost(type);
                JsonApplicationClassDelegateDesc classNames = deepClasses.get(componentTypeInnermost);
                if (classNames != null && JavaClassHelper.getArrayDimensions(componentTypeInnermost.getType()) <= 2) {
                    if (type.getType().getComponentType().isArray()) {
                        startArray = new JsonDelegateForgeWithDelegateFactoryArray2Dim(classNames.getDelegateFactoryClassName(), JavaClassHelper.getArrayComponentType(type));
                        write = new JsonWriteForgeAppClass(classNames.getDelegateFactoryClassName(), "writeArray2DimAppClass");
                    } else {
                        startArray = new JsonDelegateForgeWithDelegateFactoryArray(classNames.getDelegateFactoryClassName(), componentTypeInnermost);
                        write = new JsonWriteForgeAppClass(classNames.getDelegateFactoryClassName(), "writeArrayAppClass");
                    }
                } else {
                    EPTypeClass startArrayDelegateClass = START_ARRAY_FORGES.get(type);
                    if (startArrayDelegateClass == null) {
                        throw getUnsupported(type.getType(), fieldName);
                    }
                    startArray = new JsonDelegateForgeByClass(startArrayDelegateClass);
                    write = WRITE_ARRAY_FORGES.get(classType);
                }
            }
            end = new JsonEndValueForgeCast(classType);
        } else if (type.getType() == List.class) {
            if (optionalField != null) {
                EPTypeClass fieldType = ClassHelperGenericType.getFieldEPType(optionalField);
                EPTypeClass genericTypeClass = fieldType instanceof EPTypeClassParameterized ? ((EPTypeClassParameterized) fieldType).getParameters()[0] : null; 
                if (genericTypeClass == null) {
                    return null;
                }
                end = new JsonEndValueForgeCast(EPTypePremade.LIST.getEPType()); // we are casting to list
                JsonApplicationClassDelegateDesc classNames = deepClasses.get(genericTypeClass);
                if (classNames != null) {
                    startArray = new JsonDelegateForgeWithDelegateFactoryCollection(classNames.getDelegateFactoryClassName());
                    write = new JsonWriteForgeAppClass(classNames.getDelegateFactoryClassName(), "writeCollectionAppClass");
                } else {
                    if (genericTypeClass.getType().isEnum()) {
                        startArray = new JsonDelegateForgeByClass(JsonDelegateCollectionEnum.EPTYPE, constant(genericTypeClass));
                        write = new JsonWriteForgeByMethod("writeEnumCollection");
                    } else {
                        EPTypeClass startArrayDelegateClass = START_COLLECTION_FORGES.get(genericTypeClass);
                        if (startArrayDelegateClass == null) {
                            throw getUnsupported(genericTypeClass.getType(), fieldName);
                        }
                        startArray = new JsonDelegateForgeByClass(startArrayDelegateClass);
                        write = WRITE_COLLECTION_FORGES.get(genericTypeClass);
                    }
                }
            }
        }

        if (end == null) {
            JsonApplicationClassDelegateDesc delegateDesc = deepClasses.get(type);
            if (delegateDesc == null) {
                throw getUnsupported(type.getType(), fieldName);
            }
            end = new JsonEndValueForgeCast(classType);
            write = new JsonWriteForgeDelegate(delegateDesc.getDelegateFactoryClassName());
            if (optionalField != null && optionalField.getDeclaringClass() == optionalField.getType()) {
                EPTypeClass beanClass = ClassHelperGenericType.getFieldEPType(optionalField);
                startObject = new JsonDelegateForgeWithDelegateFactorySelf(delegateDesc.getDelegateClassName(), beanClass);
            } else {
                startObject = new JsonDelegateForgeWithDelegateFactory(delegateDesc.getDelegateFactoryClassName());
            }
        }
        if (write == null) {
            throw getUnsupported(type.getType(), fieldName);
        }

        return new JsonForgeDesc(fieldName, startObject, startArray, end, write);
    }

    private static JsonSchemaField findFieldAnnotation(String fieldName, Annotation[] annotations) {
        if (annotations == null || annotations.length == 0) {
            return null;
        }
        for (Annotation annotation : annotations) {
            if (!(annotation instanceof JsonSchemaField)) {
                continue;
            }
            JsonSchemaField field = (JsonSchemaField) annotation;
            if (field.name().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    private static UnsupportedOperationException getUnsupported(Class type, String fieldName) {
        return new UnsupportedOperationException("Unsupported type '" + type.getName() + "' for property '" + fieldName + "' (use @JsonSchemaField to declare additional information)");
    }
}
