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
package com.espertech.esper.epl.rettype;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprEnumerationForge;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.util.JavaClassHelper;

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
 * Use {@link EPTypeHelper#collectionOfEvents(com.espertech.esper.client.EventType)}
 * to indicate that the expression returns a collection of events.
 * </li>
 * <li>
 * Use {@link EPTypeHelper#singleEvent(com.espertech.esper.client.EventType)}
 * to indicate that the expression returns a single event.
 * </li>
 * <li>
 * Use {@link EPTypeHelper#collectionOfSingleValue(Class)}
 * to indicate that the expression returns a collection of single values.
 * A single value can be any object including null.
 * </li>
 * <li>
 * Use {@link EPTypeHelper#array(Class)}
 * to indicate that the expression returns an array of single values.
 * A single value can be any object including null.
 * </li>
 * <li>
 * Use {@link EPTypeHelper#singleValue(Class)}
 * to indicate that the expression returns a single value.
 * A single value can be any object including null.
 * Such expression results cannot be used as input to enumeration methods, for example.
 * </li>
 * </ol>
 */
public class EPTypeHelper {

    public static EventType getEventTypeSingleValued(EPType type) {
        if (type instanceof EventEPType) {
            return ((EventEPType) type).getType();
        }
        return null;
    }

    public static EventType getEventTypeMultiValued(EPType type) {
        if (type instanceof EventMultiValuedEPType) {
            return ((EventMultiValuedEPType) type).getComponent();
        }
        return null;
    }

    public static Class getClassMultiValued(EPType type) {
        if (type instanceof ClassMultiValuedEPType) {
            return ((ClassMultiValuedEPType) type).getComponent();
        }
        return null;
    }

    public static Class getClassSingleValued(EPType type) {
        if (type instanceof ClassEPType) {
            return ((ClassEPType) type).getType();
        }
        return null;
    }

    public static boolean isCarryEvent(EPType epType) {
        return epType instanceof EventMultiValuedEPType || epType instanceof EventEPType;
    }

    public static EventType getEventType(EPType epType) {
        if (epType instanceof EventMultiValuedEPType) {
            return ((EventMultiValuedEPType) epType).getComponent();
        }
        if (epType instanceof EventEPType) {
            return ((EventEPType) epType).getType();
        }
        return null;
    }

    /**
     * Indicate that the expression return type is an array of a given component type.
     *
     * @param arrayComponentType array component type
     * @return array of single value expression result type
     */
    public static EPType array(Class arrayComponentType) {
        if (arrayComponentType == null) {
            throw new IllegalArgumentException("Invalid null array component type");
        }
        return new ClassMultiValuedEPType(JavaClassHelper.getArrayType(arrayComponentType), arrayComponentType);
    }

    /**
     * Indicate that the expression return type is a single (non-enumerable) value of the given type.
     * The expression can still return an array or collection or events however
     * since the engine would not know the type of such objects and may not use runtime reflection
     * it may not allow certain operations on expression results.
     *
     * @param singleValueType type of single value returned, or null to indicate that the expression always returns null
     * @return single-value expression result type
     */
    public static EPType singleValue(Class singleValueType) {
        // null value allowed
        if (singleValueType != null && singleValueType.isArray()) {
            return new ClassMultiValuedEPType(singleValueType, singleValueType.getComponentType());
        }
        return new ClassEPType(singleValueType);
    }

    public static EPType nullValue() {
        return NullEPType.INSTANCE;
    }

    /**
     * Indicate that the expression return type is a collection of a given component type.
     *
     * @param collectionComponentType collection component type
     * @return collection of single value expression result type
     */
    public static EPType collectionOfSingleValue(Class collectionComponentType) {
        if (collectionComponentType == null) {
            throw new IllegalArgumentException("Invalid null collection component type");
        }
        return new ClassMultiValuedEPType(Collection.class, collectionComponentType);
    }

    /**
     * Indicate that the expression return type is a collection of a given type of events.
     *
     * @param eventTypeOfCollectionEvents the event type of the events that are part of the collection
     * @return collection of events expression result type
     */
    public static EPType collectionOfEvents(EventType eventTypeOfCollectionEvents) {
        if (eventTypeOfCollectionEvents == null) {
            throw new IllegalArgumentException("Invalid null event type");
        }
        return new EventMultiValuedEPType(Collection.class, eventTypeOfCollectionEvents);
    }

    /**
     * Indicate that the expression return type is single event of a given event type.
     *
     * @param eventTypeOfSingleEvent the event type of the event returned
     * @return single-event expression result type
     */
    public static EPType singleEvent(EventType eventTypeOfSingleEvent) {
        if (eventTypeOfSingleEvent == null) {
            throw new IllegalArgumentException("Invalid null event type");
        }
        return new EventEPType(eventTypeOfSingleEvent);
    }

    /**
     * Interrogate the provided method and determine whether it returns
     * single-value, array of single-value or collection of single-value and
     * their component type.
     *
     * @param method the class methods
     * @return expression return type
     */
    public static EPType fromMethod(Method method) {
        Class returnType = method.getReturnType();
        if (JavaClassHelper.isImplementsInterface(returnType, Collection.class)) {
            Class componentType = JavaClassHelper.getGenericReturnType(method, true);
            return EPTypeHelper.collectionOfSingleValue(componentType);
        }
        if (method.getReturnType().isArray()) {
            Class componentType = method.getReturnType().getComponentType();
            return EPTypeHelper.array(componentType);
        }
        return EPTypeHelper.singleValue(JavaClassHelper.getBoxedType(method.getReturnType()));
    }

    /**
     * Returns a nice text detailing the expression result type.
     *
     * @param epType type
     * @return descriptive text
     */
    public static String toTypeDescriptive(EPType epType) {
        if (epType instanceof EventEPType) {
            EventEPType type = (EventEPType) epType;
            return "event type '" + type.getType().getName() + "'";
        } else if (epType instanceof EventMultiValuedEPType) {
            EventMultiValuedEPType type = (EventMultiValuedEPType) epType;
            if (type.getContainer() == EventType[].class) {
                return "array of events of type '" + type.getComponent().getName() + "'";
            } else {
                return "collection of events of type '" + type.getComponent().getName() + "'";
            }
        } else if (epType instanceof ClassMultiValuedEPType) {
            ClassMultiValuedEPType type = (ClassMultiValuedEPType) epType;
            if (type.getContainer().isArray()) {
                return "array of " + type.getComponent().getSimpleName();
            } else {
                return "collection of " + type.getComponent().getSimpleName();
            }
        } else if (epType instanceof ClassEPType) {
            ClassEPType type = (ClassEPType) epType;
            return "class " + JavaClassHelper.getClassNameFullyQualPretty(type.getType());
        } else if (epType instanceof NullEPType) {
            return "null type";
        } else {
            throw new IllegalArgumentException("Unrecognized type " + epType);
        }
    }

    public static Class getNormalizedClass(EPType theType) {
        if (theType instanceof EventMultiValuedEPType) {
            EventMultiValuedEPType type = (EventMultiValuedEPType) theType;
            return JavaClassHelper.getArrayType(type.getComponent().getUnderlyingType());
        } else if (theType instanceof EventEPType) {
            EventEPType type = (EventEPType) theType;
            return type.getType().getUnderlyingType();
        } else if (theType instanceof ClassMultiValuedEPType) {
            ClassMultiValuedEPType type = (ClassMultiValuedEPType) theType;
            return type.getContainer();
        } else if (theType instanceof ClassEPType) {
            ClassEPType type = (ClassEPType) theType;
            return type.getType();
        } else if (theType instanceof NullEPType) {
            return null;
        }
        throw new IllegalArgumentException("Unrecognized type " + theType);
    }

    public static Class getCodegenReturnType(EPType theType) {
        if (theType instanceof EventMultiValuedEPType) {
            return Collection.class;
        } else if (theType instanceof ClassMultiValuedEPType) {
            return ((ClassMultiValuedEPType) theType).getContainer();
        } else if (theType instanceof EventEPType) {
            return EventBean.class;
        } else if (theType instanceof ClassEPType) {
            ClassEPType type = (ClassEPType) theType;
            return type.getType();
        } else if (theType instanceof NullEPType) {
            return null;
        }
        throw new IllegalArgumentException("Unrecognized type " + theType);
    }

    public static EPType optionalFromEnumerationExpr(int statementId, EventAdapterService eventAdapterService, ExprNode exprNode)
            throws ExprValidationException {
        if (!(exprNode instanceof ExprEnumerationForge)) {
            return null;
        }
        ExprEnumerationForge enumInfo = (ExprEnumerationForge) exprNode;
        if (enumInfo.getComponentTypeCollection() != null) {
            return EPTypeHelper.collectionOfSingleValue(enumInfo.getComponentTypeCollection());
        }
        EventType eventTypeSingle = enumInfo.getEventTypeSingle(eventAdapterService, statementId);
        if (eventTypeSingle != null) {
            return EPTypeHelper.singleEvent(eventTypeSingle);
        }
        EventType eventTypeColl = enumInfo.getEventTypeCollection(eventAdapterService, statementId);
        if (eventTypeColl != null) {
            return EPTypeHelper.collectionOfEvents(eventTypeColl);
        }
        return null;
    }

    public static EventType optionalIsEventTypeColl(EPType type) {
        if (type != null && type instanceof EventMultiValuedEPType) {
            return ((EventMultiValuedEPType) type).getComponent();
        }
        return null;
    }

    public static Class optionalIsComponentTypeColl(EPType type) {
        if (type != null && type instanceof ClassMultiValuedEPType) {
            return ((ClassMultiValuedEPType) type).getComponent();
        }
        return null;
    }

    public static EventType optionalIsEventTypeSingle(EPType type) {
        if (type != null && type instanceof EventEPType) {
            return ((EventEPType) type).getType();
        }
        return null;
    }
}
