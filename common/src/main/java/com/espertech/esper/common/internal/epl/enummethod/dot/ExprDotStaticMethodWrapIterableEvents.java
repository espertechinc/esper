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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprDotStaticMethodWrapIterableEvents implements ExprDotStaticMethodWrap {
    private EventBeanTypedEventFactory eventBeanTypedEventFactory;
    private BeanEventType type;

    public ExprDotStaticMethodWrapIterableEvents(EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventType type) {
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
        this.type = type;
    }

    public EPType getTypeInfo() {
        return EPTypeHelper.collectionOfEvents(type);
    }

    public Collection convertNonNull(Object result) {
        // there is a need to read the iterator to the cache since if it's iterated twice, the iterator is already exhausted
        return new WrappingCollection(eventBeanTypedEventFactory, type, ((Iterable) result).iterator());
    }


    public CodegenExpression codegenConvertNonNull(CodegenExpression result, CodegenMethodScope codegenMethodScope, CodegenClassScope classScope) {
        CodegenExpressionField eventSvcMember = classScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpressionField typeMember = classScope.addFieldUnshared(true, BeanEventType.class, cast(BeanEventType.class, EventTypeUtility.resolveTypeCodegen(type, EPStatementInitServices.REF)));
        return newInstance(WrappingCollection.class, eventSvcMember, typeMember, exprDotMethod(result, "iterator"));
    }

    public static class WrappingCollection implements Collection<EventBean> {
        private EventBeanTypedEventFactory eventBeanTypedEventFactory;
        private BeanEventType type;
        private Iterator inner;
        private Object[] cache = null;

        public WrappingCollection(EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventType type, Iterator inner) {
            this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
            this.type = type;
            this.inner = inner;
        }

        public int size() {
            if (cache == null) {
                init();
            }
            return cache.length;
        }

        public boolean isEmpty() {
            if (cache == null) {
                init();
            }
            return cache.length == 0;
        }

        public Iterator<EventBean> iterator() {
            if (cache == null) {
                init();
            }
            return new ExprDotStaticMethodWrapArrayEvents.WrappingIterator(eventBeanTypedEventFactory, type, cache);
        }

        private void init() {
            Deque<Object> q = new ArrayDeque<Object>();
            for (; inner.hasNext(); ) {
                q.add(inner.next());
            }
            cache = q.toArray();
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
}
