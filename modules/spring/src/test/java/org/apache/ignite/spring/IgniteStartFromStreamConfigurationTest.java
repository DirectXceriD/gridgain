/*
 *                    GridGain Community Edition Licensing
 *                    Copyright 2019 GridGain Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License") modified with Commons Clause
 *  Restriction; you may not use this file except in compliance with the License. You may obtain a
 *  copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the
 *  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the specific language governing permissions
 *  and limitations under the License.
 *
 *  Commons Clause Restriction
 *
 *  The Software is provided to you by the Licensor under the License, as defined below, subject to
 *  the following condition.
 *
 *  Without limiting other conditions in the License, the grant of rights under the License will not
 *  include, and the License does not grant to you, the right to Sell the Software.
 *  For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 *  under the License to provide to third parties, for a fee or other consideration (including without
 *  limitation fees for hosting or consulting/ support services related to the Software), a product or
 *  service whose value derives, entirely or substantially, from the functionality of the Software.
 *  Any license notice or attribution required by the License must also include this Commons Clause
 *  License Condition notice.
 *
 *  For purposes of the clause above, the “Licensor” is Copyright 2019 GridGain Systems, Inc.,
 *  the “License” is the Apache License, Version 2.0, and the Software is the GridGain Community
 *  Edition software provided with this notice.
 */

package org.apache.ignite.spring;

import java.io.FileInputStream;
import java.net.URL;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Checks starts from Stream.
 */
@RunWith(JUnit4.class)
public class IgniteStartFromStreamConfigurationTest extends GridCommonAbstractTest {
    /**
     * Tests starts from stream.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testStartFromStream() throws Exception {
        String cfg = "examples/config/example-cache.xml";

        URL cfgLocation = U.resolveIgniteUrl(cfg);

        try (Ignite grid = Ignition.start(new FileInputStream(cfgLocation.getFile()))) {
            grid.cache(DEFAULT_CACHE_NAME).put("1", "1");

            assert grid.cache(DEFAULT_CACHE_NAME).get("1").equals("1");

            IgniteConfiguration icfg = Ignition.loadSpringBean(new FileInputStream(cfgLocation.getFile()), "ignite.cfg");

            assert icfg.getCacheConfiguration()[0].getAtomicityMode() == CacheAtomicityMode.ATOMIC;
        }
    }

}
