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
package com.espertech.esper.common.internal.rettype;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.*;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEnumerationForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.util.ClassHelperGenericType;
import com.espertech.esper.common.internal.util.ClassHelperPrint;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Carries return type information related to the return values returned by expressions.
 * <p>
 * Use factory methods to initialize return type information according to the return values
 * that your expression is going to provide.
 * </p>
 * <ol>
 * <li>
 * Use {@link EPChainableTypeHelper#collectionOfEvents(com.espertech.esper.common.client.EventType)}
 * to indicate that the expression returns a collection of events.
 * </li>
 * <li>
 * Use {@link EPChainableTypeHelper#singleEvent(com.espertech.esper.common.client.EventType)}
 * to indicate that the expression returns a single event.
 * </li>
 * <li>
 * Use {@link EPChainableTypeHelper#collectionOfSingleValue(EPTypeClass)}
 * to indicate that the expression returns a collection of single values.
 * A single value can be any object including null.
 * </li>
 * <li>
 * Use {@link EPChainableTypeHelper#array(EPTypeClass)}
 * to indicate that the expression returns an array of single values.
 * A single value can be any object including null.
 * </li>
 * <li>
 * Use {@link EPChainableTypeHelper#singleValue(EPType)}
 * to indicate that the expression returns a single value.
 * A single value can be any object including null.
 * Such expression results cannot be used as input to enumeration methods, for example.
 * </li>
 * </ol>
 */
public class EPChainableTypeHelper {

    public static EventType getEventTypeMultiValued(EPChainableType type) {
        if (type instanceof EPChainableTypeEventMulti) {
            return ((EPChainableTypeEventMulti) type).getComponent();
        }
        return null;
    }

    public static EPTypeClass getCollectionOrArrayComponentTypeOrNull(EPChainableType type) {
        if (type instanceof EPChainableTypeClass) {
            EPChainableTypeClass classInfo = (EPChainableTypeClass) type;
            if (classInfo.getType().getType().isArray()) {
                return JavaClassHelper.getArrayComponentType(classInfo.getType());
            }
            if (JavaClassHelper.isSubclassOrImplementsInterface(classInfo.getType(), Collection.class)) {
                return JavaClassHelper.getSingleParameterTypeOrObject(classInfo.getType());
            }
        }
        return null;
    }

    public static boolean isCarryEvent(EPChainableType epType) {
        return epType instanceof EPChainableTypeEventMulti || epType instanceof EPChainableTypeEventSingle;
    }

    public static EventType getEventType(EPChainableType epType) {
        if (epType instanceof EPChainableTypeEventMulti) {
            return ((EPChainableTypeEventMulti) epType).getComponent();
        }
        if (epType instanceof EPChainableTypeEventSingle) {
            return ((EPChainableTypeEventSingle) epType).getType();
        }
        return null;
    }

    /**
     * Indicate that the expression return type is an array of a given component type.
     *
     * @param arrayComponentType array component type
     * @return array of single value expression result type
     */
    public static EPChainableType array(EPTypeClass arrayComponentType) {
        if (arrayComponentType == null) {
            throw new IllegalArgumentException("Invalid null array component type");
        }
        EPTypeClass array = JavaClassHelper.getArrayType(arrayComponentType);
        return new EPChainableTypeClass(array);
    }

    public static EPChainableTypeClass singleValueNonNull(EPType typeClass) {
        if (typeClass == null || typeClass == EPTypeNull.INSTANCE) {
            throw new IllegalArgumentException("Null-type not supported as a return type");
        }
        return new EPChainableTypeClass((EPTypeClass) typeClass);
    }

    public static EPChainableType singleValue(EPType singleValueType) {
        if (singleValueType == EPTypeNull.INSTANCE) {
            return EPChainableTypeNull.INSTANCE;
        }
        return new EPChainableTypeClass((EPTypeClass) singleValueType);
    }

    public static EPChainableType singleValue(Class clazz) {
        if (clazz == null) {
            return EPChainableTypeNull.INSTANCE;
        }
        return new EPChainableTypeClass(ClassHelperGenericType.getClassEPType(clazz));
    }

    public static EPChainableType nullValue() {
        return EPChainableTypeNull.INSTANCE;
    }

    /**
     * Indicate that the expression return type is a collection of a given component type.
     *
     * @param collectionComponentType collection component type
     * @return collection of single value expression result type
     */
    public static EPChainableType collectionOfSingleValue(EPTypeClass collectionComponentType) {
        if (collectionComponentType == null) {
            throw new IllegalArgumentException("Invalid null collection component type");
        }
        EPTypeClassParameterized typeClass = EPTypeClassParameterized.from(Collection.class, JavaClassHelper.getBoxedType(collectionComponentType));
        return new EPChainableTypeClass(typeClass);
    }

    /**
     * Indicate that the expression return type is a collection of a given type of events.
     *
     * @param eventTypeOfCollectionEvents the event type of the events that are part of the collection
     * @return collection of events expression result type
     */
    public static EPChainableType collectionOfEvents(EventType eventTypeOfCollectionEvents) {
        if (eventTypeOfCollectionEvents == null) {
            throw new IllegalArgumentException("Invalid null event type");
        }
        return new EPChainableTypeEventMulti(Collection.class, eventTypeOfCollectionEvents);
    }

    /**
     * Indicate that the expression return type is an array of events of given type.
     *
     * @param eventTypeOfArrayEvents the event type of the events that are part of the array
     * @return array of events expression result type
     */
    public static EPChainableType arrayOfEvents(EventType eventTypeOfArrayEvents) {
        if (eventTypeOfArrayEvents == null) {
            throw new IllegalArgumentException("Invalid null event type");
        }
        return new EPChainableTypeEventMulti(EventBean[].class, eventTypeOfArrayEvents);
    }

    /**
     * Indicate that the expression return type is single event of a given event type.
     *
     * @param eventTypeOfSingleEvent the event type of the event returned
     * @return single-event expression result type
     */
    public static EPChainableType singleEvent(EventType eventTypeOfSingleEvent) {
        if (eventTypeOfSingleEvent == null) {
            throw new IllegalArgumentException("Invalid null event type");
        }
        return new EPChainableTypeEventSingle(eventTypeOfSingleEvent);
    }

    /**
     * Interrogate the provided method and determine whether it returns
     * single-value, array of single-value or collection of single-value and
     * their component type.
     *
     * @param method the class methods
     * @param methodTargetType method target class if available
     * @return expression return type
     */
    public static EPChainableType fromMethod(Method method, EPTypeClass methodTargetType) {
        EPTypeClass type = JavaClassHelper.getBoxedType(ClassHelperGenericType.getMethodReturnEPType(method, methodTargetType));
        return new EPChainableTypeClass(type);
    }

    /**
     * Returns a nice text detailing the expression result type.
     *
     * @param epType type
     * @return descriptive text
     */
    public static String toTypeDescriptive(EPChainableType epType) {
        if (epType instanceof EPChainableTypeEventSingle) {
            EPChainableTypeEventSingle type = (EPChainableTypeEventSingle) epType;
            return "event type '" + type.getType().getName() + "'";
        } else if (epType instanceof EPChainableTypeEventMulti) {
            EPChainableTypeEventMulti type = (EPChainableTypeEventMulti) epType;
            if (type.getContainer() == EventType[].class) {
                return "array of events of type '" + type.getComponent().getName() + "'";
            } else {
                return "collection of events of type '" + type.getComponent().getName() + "'";
            }
        } else if (epType instanceof EPChainableTypeClass) {
            EPChainableTypeClass type = (EPChainableTypeClass) epType;
            return ClassHelperPrint.getClassNameFullyQualPretty(type.getType());
        } else if (epType instanceof EPChainableTypeNull) {
            return "null type";
        } else {
            throw new IllegalArgumentException("Unrecognized type " + epType);
        }
    }

    public static EPType getNormalizedEPType(EPChainableType theType) {
        if (theType instanceof EPChainableTypeEventMulti) {
            EPChainableTypeEventMulti type = (EPChainableTypeEventMulti) theType;
            EPTypeClass underlyingType = type.getComponent().getUnderlyingEPType();
            return JavaClassHelper.getArrayType(underlyingType);
        } else if (theType instanceof EPChainableTypeEventSingle) {
            EPChainableTypeEventSingle type = (EPChainableTypeEventSingle) theType;
            return EPTypePremade.getOrCreate(type.getType().getUnderlyingType());
        } else if (theType instanceof EPChainableTypeClass) {
            EPChainableTypeClass type = (EPChainableTypeClass) theType;
            return type.getType();
        } else if (theType instanceof EPChainableTypeNull) {
            return EPTypeNull.INSTANCE;
        }
        throw new IllegalArgumentException("Unrecognized type " + theType);
    }

    public static EPTypeClass getCodegenReturnType(EPChainableType theType) {
        if (theType instanceof EPChainableTypeEventMulti) {
            EPChainableTypeEventMulti multi = (EPChainableTypeEventMulti) theType;
            return EPTypeClassParameterized.from(multi.getContainer(), EventBean.EPTYPE);
        } else if (theType instanceof EPChainableTypeEventSingle) {
            return EventBean.EPTYPE;
        } else if (theType instanceof EPChainableTypeClass) {
            EPChainableTypeClass type = (EPChainableTypeClass) theType;
            return type.getType();
        } else if (theType instanceof EPChainableTypeNull) {
            return EPTypePremade.OBJECT.getEPType();
        }
        throw new IllegalArgumentException("Unrecognized type " + theType);
    }

    public static EPChainableType optionalFromEnumerationExpr(StatementRawInfo raw, StatementCompileTimeServices services, ExprNode exprNode)
            throws ExprValidationException {
        if (!(exprNode instanceof ExprEnumerationForge)) {
            return null;
        }
        ExprEnumerationForge enumInfo = (ExprEnumerationForge) exprNode;
        if (enumInfo.getComponentTypeCollection() != null) {
            return EPChainableTypeHelper.collectionOfSingleValue(enumInfo.getComponentTypeCollection());
        }
        EventType eventTypeSingle = enumInfo.getEventTypeSingle(raw, services);
        if (eventTypeSingle != null) {
            return EPChainableTypeHelper.singleEvent(eventTypeSingle);
        }
        EventType eventTypeColl = enumInfo.getEventTypeCollection(raw, services);
        if (eventTypeColl != null) {
            return EPChainableTypeHelper.collectionOfEvents(eventTypeColl);
        }
        return null;
    }

    public static EventType optionalIsEventTypeColl(EPChainableType type) {
        if (type instanceof EPChainableTypeEventMulti) {
            return ((EPChainableTypeEventMulti) type).getComponent();
        }
        return null;
    }

    public static EventType optionalIsEventTypeSingle(EPChainableType type) {
        if (type instanceof EPChainableTypeEventSingle) {
            return ((EPChainableTypeEventSingle) type).getType();
        }
        return null;
    }
}
