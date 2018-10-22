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
package com.espertech.esper.common.internal.epl.enummethod.dot;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCodegenField;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.cast;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class ExprDotStaticMethodWrapArrayEvents implements ExprDotStaticMethodWrap {
    private EventBeanTypedEventFactory eventBeanTypedEventFactory;
    private BeanEventType type;

    public ExprDotStaticMethodWrapArrayEvents(EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventType type) {
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
        this.type = type;
    }

    public EPType getTypeInfo() {
        return EPTypeHelper.collectionOfEvents(type);
    }

    public Collection convertNonNull(Object result) {
        if (!result.getClass().isArray()) {
            return null;
        }
        return new WrappingCollection(eventBeanTypedEventFactory, type, result);
    }

    public CodegenExpression codegenConvertNonNull(CodegenExpression result, CodegenMethodScope codegenMethodScope, CodegenClassScope classScope) {
        CodegenExpressionField eventSvcMember = classScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpressionField typeMember = classScope.addFieldUnshared(true, BeanEventType.class, cast(BeanEventType.class, EventTypeUtility.resolveTypeCodegen(type, EPStatementInitServices.REF)));
        return newInstance(ExprDotStaticMethodWrapArrayEvents.WrappingCollection.class, eventSvcMember, typeMember, result);
    }

    public static class WrappingCollection implements Collection<EventBean> {

        private EventBeanTypedEventFactory eventBeanTypedEventFactory;
        private BeanEventType type;
        private Object array;

        public WrappingCollection(EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventType type, Object array) {
            this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
            this.type = type;
            this.array = array;
        }

        public int size() {
            return Array.getLength(array);
        }

        public boolean isEmpty() {
            return size() == 0;
        }

        public Iterator<EventBean> iterator() {
            return new WrappingIterator(eventBeanTypedEventFactory, type, array);
        }

        public boolean contains(Object o) {
            throw new UnsupportedOperationException("Partial implementation");
        }

        public Object[] toArray() {
            throw new UnsupportedOperationException("Partial implementation");
        }

        public <T> T[] toArray(T[] a) {
            throw new UnsupportedOperationException("Partial implementation");
        }

        public boolean add(EventBean eventBean) {
            throw new UnsupportedOperationException("Read-only implementation");
        }

        public boolean remove(Object o) {
            throw new UnsupportedOperationException("Read-only implementation");
        }

        public boolean containsAll(Collection<?> c) {
            throw new UnsupportedOperationException("Read-only implementation");
        }

        public boolean addAll(Collection<? extends EventBean> c) {
            throw new UnsupportedOperationException("Read-only implementation");
        }

        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException("Read-only implementation");
        }

        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException("Read-only implementation");
        }

        public void clear() {
            throw new UnsupportedOperationException("Read-only implementation");
        }
    }

    public static class WrappingIterator implements Iterator<EventBean> {
        private EventBeanTypedEventFactory eventBeanTypedEventFactory;
        private BeanEventType type;
        private Object array;
        private int count;

        public WrappingIterator(EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventType type, Object array) {
            this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
            this.type = type;
            this.array = array;
        }

        public boolean hasNext() {
            if (Array.getLength(array) > count) {
                return true;
            }
            return false;
        }

        public EventBean next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Object next = Array.get(array, count++);
            if (next == null) {
                return null;
            }
            return eventBeanTypedEventFactory.adapterForTypedBean(next, type);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
