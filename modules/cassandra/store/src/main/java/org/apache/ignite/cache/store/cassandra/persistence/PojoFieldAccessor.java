/*
 *                   GridGain Community Edition Licensing
 *                   Copyright 2019 GridGain Systems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License") modified with Commons Clause
 * Restriction; you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * Commons Clause Restriction
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including without
 * limitation fees for hosting or consulting/ support services related to the Software), a product or
 * service whose value derives, entirely or substantially, from the functionality of the Software.
 * Any license notice or attribution required by the License must also include this Commons Clause
 * License Condition notice.
 *
 * For purposes of the clause above, the “Licensor” is Copyright 2019 GridGain Systems, Inc.,
 * the “License” is the Apache License, Version 2.0, and the Software is the GridGain Community
 * Edition software provided with this notice.
 */

package org.apache.ignite.cache.store.cassandra.persistence;

import org.apache.ignite.IgniteException;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Property accessor provides read/write access to POJO object properties defined through:
 *  1) Getter/setter methods
 *  2) Raw class members
 */
public class PojoFieldAccessor {
    /** Java Bean property descriptor */
    private PropertyDescriptor desc;

    /** Object field associated with property descriptor. Used just to get annotations which
     * applied not to property descriptor, but directly to object field associated with the property. */
    private Field descField;

    /** Object field */
    private Field field;

    /**
     * Constructs object instance from Java Bean property descriptor, providing access to getter/setter.
     *
     * @param desc Java Bean property descriptor.
     * @param field object field associated with property descriptor.
     */
    public PojoFieldAccessor(PropertyDescriptor desc, Field field) {
        if (desc.getReadMethod() == null) {
            throw new IllegalArgumentException("Field '" + desc.getName() +
                    "' of the class instance '" + desc.getPropertyType().getName() +
                    "' doesn't provide getter method");
        }

        desc.getReadMethod().setAccessible(true);

        if (desc.getWriteMethod() != null)
            desc.getWriteMethod().setAccessible(true);

        this.desc = desc;
        this.descField = field;
    }

    /**
     * Constructs object instance from Field, providing direct access to class member.
     *
     * @param field Field descriptor.
     */
    public PojoFieldAccessor(Field field) {
        field.setAccessible(true);
        this.field = field;
    }

    /**
     * Returns POJO field name.
     *
     * @return field name.
     */
    public String getName() {
        return desc != null ? desc.getName() : field.getName();
    }

    /**
     * Indicates if it's read-only field.
     *
     * @return true if field read-only, false if not.
     */
    public boolean isReadOnly() {
        return desc != null && desc.getWriteMethod() == null;
    }

    /**
     * Returns POJO field annotation.
     *
     * @return annotation.
     */
    public Annotation getAnnotation(Class clazz) {
        if (field != null)
            return field.getAnnotation(clazz);

        Annotation ann = desc.getReadMethod().getAnnotation(clazz);

        if (ann != null)
            return ann;

        ann = desc.getWriteMethod() == null ? null : desc.getWriteMethod().getAnnotation(clazz);

        if (ann != null)
            return ann;

        return descField == null ? null : descField.getAnnotation(clazz);
    }

    /**
     * Returns field value for the object instance.
     *
     * @param obj object instance.
     * @return field value.
     */
    public Object getValue(Object obj) {
        try {
            return desc != null ? desc.getReadMethod().invoke(obj) : field.get(obj);
        }
        catch (Throwable e) {
            throw new IgniteException("Failed to get value of the field '" + getName() + "' from the instance " +
                    " of '" + obj.getClass().toString() + "' class", e);
        }
    }

    /**
     * Assigns value for the object field.
     *
     * @param obj object instance.
     * @param val value to assign.
     */
    public void setValue(Object obj, Object val) {
        if (isReadOnly())
            throw new IgniteException("Can't assign value to read-only field '" + getName() + "' of the instance " +
                    " of '" + obj.getClass().toString() + "' class");

        try {
            if (desc != null)
                desc.getWriteMethod().invoke(obj, val);
            else
                field.set(obj, val);
        }
        catch (Throwable e) {
            throw new IgniteException("Failed to set value of the field '" + getName() + "' of the instance " +
                    " of '" + obj.getClass().toString() + "' class", e);
        }
    }

    /**
     * Returns field type.
     *
     * @return field type.
     */
    public Class getFieldType() {
        return desc != null ? desc.getPropertyType() : field.getType();
    }
}
