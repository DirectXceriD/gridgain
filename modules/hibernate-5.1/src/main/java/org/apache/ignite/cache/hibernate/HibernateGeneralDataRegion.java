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

package org.apache.ignite.cache.hibernate;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCheckedException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.GeneralDataRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.engine.spi.SessionImplementor;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of {@link GeneralDataRegion}. This interface defines common contract for {@link QueryResultsRegion}
 * and {@link TimestampsRegion}.
 */
public class HibernateGeneralDataRegion extends HibernateRegion implements GeneralDataRegion {
    /**
     * @param factory Region factory.
     * @param name Region name.
     * @param ignite Grid.
     * @param cache Region cache.
     */
    HibernateGeneralDataRegion(HibernateRegionFactory factory, String name,
        Ignite ignite, HibernateCacheProxy cache) {
        super(factory, name, ignite, cache);
    }

    /** {@inheritDoc} */
    @Nullable @Override public Object get(SessionImplementor ses, Object key) throws CacheException {
        try {
            return cache.get(key);
        }
        catch (IgniteCheckedException e) {
            throw new CacheException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public void put(SessionImplementor ses, Object key, Object val) throws CacheException {
        try {
            cache.put(key, val);
        }
        catch (IgniteCheckedException e) {
            throw new CacheException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public void evict(Object key) throws CacheException {
        HibernateAccessStrategyAdapter.evict(ignite, cache, key);
    }

    /** {@inheritDoc} */
    @Override public void evictAll() throws CacheException {
        try {
            HibernateAccessStrategyAdapter.evictAll(cache);
        }
        catch (IgniteCheckedException e) {
            throw HibernateRegionFactory.EXCEPTION_CONVERTER.convert(e);
        }
    }
}