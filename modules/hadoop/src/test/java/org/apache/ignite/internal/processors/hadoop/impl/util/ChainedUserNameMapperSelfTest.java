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

package org.apache.ignite.internal.processors.hadoop.impl.util;

import org.apache.ignite.IgniteException;
import org.apache.ignite.hadoop.util.BasicUserNameMapper;
import org.apache.ignite.hadoop.util.ChainedUserNameMapper;
import org.apache.ignite.hadoop.util.KerberosUserNameMapper;
import org.apache.ignite.hadoop.util.UserNameMapper;
import org.apache.ignite.internal.processors.igfs.IgfsUtils;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;

import java.util.Collections;
import java.util.concurrent.Callable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for chained user name mapper.
 */
@RunWith(JUnit4.class)
public class ChainedUserNameMapperSelfTest extends GridCommonAbstractTest {
    /** Test instance. */
    private static final String INSTANCE = "test_instance";

    /** Test realm. */
    private static final String REALM = "test_realm";

    /**
     * Test case when mappers are null.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testNullMappers() throws Exception {
        GridTestUtils.assertThrows(null, new Callable<Void>() {
            @Override public Void call() throws Exception {
                create((UserNameMapper[])null);

                return null;
            }
        }, IgniteException.class, null);
    }

    /**
     * Test case when one of mappers is null.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testNullMapperElement() throws Exception {
        GridTestUtils.assertThrows(null, new Callable<Void>() {
            @Override public Void call() throws Exception {
                create(new BasicUserNameMapper(), null);

                return null;
            }
        }, IgniteException.class, null);
    }

    /**
     * Test actual chaining logic.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testChaining() throws Exception {
        BasicUserNameMapper mapper1 = new BasicUserNameMapper();

        mapper1.setMappings(Collections.singletonMap("1", "101"));

        KerberosUserNameMapper mapper2 = new KerberosUserNameMapper();

        mapper2.setInstance(INSTANCE);
        mapper2.setRealm(REALM);

        ChainedUserNameMapper mapper = create(mapper1, mapper2);

        assertEquals("101" + "/" + INSTANCE + "@" + REALM, mapper.map("1"));
        assertEquals("2" + "/" + INSTANCE + "@" + REALM, mapper.map("2"));
        assertEquals(IgfsUtils.fixUserName(null) + "/" + INSTANCE + "@" + REALM, mapper.map(null));
    }

    /**
     * Create chained mapper.
     *
     * @param mappers Child mappers.
     * @return Chained mapper.
     */
    private ChainedUserNameMapper create(UserNameMapper... mappers) {
        ChainedUserNameMapper mapper = new ChainedUserNameMapper();

        mapper.setMappers(mappers);

        mapper.start();

        return mapper;
    }
}
