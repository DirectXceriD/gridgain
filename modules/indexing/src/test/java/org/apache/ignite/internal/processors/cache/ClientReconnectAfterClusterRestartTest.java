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

package org.apache.ignite.internal.processors.cache;

import javax.cache.CacheException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteClientDisconnectedException;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.binary.BinaryObjectBuilder;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.QueryIndex;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.Event;
import org.apache.ignite.events.EventType;
import org.apache.ignite.internal.binary.BinaryMarshaller;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 */
@RunWith(JUnit4.class)
public class ClientReconnectAfterClusterRestartTest extends GridCommonAbstractTest {
    /** Server id. */
    private static final int SERVER_ID = 0;

    /** Client id. */
    private static final int CLIENT_ID = 1;

    /** Cache params. */
    private static final String CACHE_PARAMS = "PPRB_PARAMS";

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        cfg.setMarshaller(new BinaryMarshaller());
        cfg.setIncludeEventTypes(EventType.EVTS_CACHE);

        if (getTestIgniteInstanceName(CLIENT_ID).equals(igniteInstanceName)) {
            cfg.setClientMode(true);

            CacheConfiguration ccfg = getCacheConfiguration();

            cfg.setCacheConfiguration(ccfg);
        }

        return cfg;
    }

    /**
     * @return CacheConfiguration Cache configuration.
     */
    @NotNull private CacheConfiguration getCacheConfiguration() {
        CacheConfiguration ccfg = defaultCacheConfiguration();

        ccfg.setName(CACHE_PARAMS);
        ccfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        ccfg.setCacheMode(CacheMode.PARTITIONED);

        List<QueryEntity> queryEntities = new ArrayList<>();

        QueryEntity entity = new QueryEntity();

        entity.setValueType("Params");
        entity.setKeyType("java.lang.Long");

        LinkedHashMap<String, String> fields = new LinkedHashMap<>();

        fields.put("ID", "java.lang.Long");
        fields.put("PARTITIONID", "java.lang.Long");
        fields.put("CLIENTID", "java.lang.Long");
        fields.put("PARAMETRCODE", "java.lang.Long");
        fields.put("PARAMETRVALUE", "java.lang.Object");
        fields.put("PARENTID", "java.lang.Long");

        entity.setFields(fields);

        List<QueryIndex> indexes = new ArrayList<>();

        indexes.add(new QueryIndex("CLIENTID"));
        indexes.add(new QueryIndex("ID"));
        indexes.add(new QueryIndex("PARENTID"));

        entity.setIndexes(indexes);

        queryEntities.add(entity);

        ccfg.setQueryEntities(queryEntities);
        return ccfg;
    }

    /** */
    @Test
    public void testReconnectClient() throws Exception {
        try {
            startGrid(SERVER_ID);

            Ignite client = startGrid(CLIENT_ID);

            checkTopology(2);

            IgniteCache<Long, BinaryObject> cache = client.getOrCreateCache(CACHE_PARAMS).withKeepBinary();

            client.events().localListen(new IgnitePredicate<Event>() {

                @Override public boolean apply(Event event) {
                    switch (event.type()) {
                        case EventType.EVT_CLIENT_NODE_DISCONNECTED:
                            info("Client disconnected");

                            break;
                        case EventType.EVT_CLIENT_NODE_RECONNECTED:
                            info("Client reconnected");
                    }

                    return true;
                }
            }, EventType.EVT_CLIENT_NODE_DISCONNECTED, EventType.EVT_CLIENT_NODE_RECONNECTED);

            IgniteDataStreamer<Long, BinaryObject> streamer = client.dataStreamer(CACHE_PARAMS);

            streamer.allowOverwrite(true);
            streamer.keepBinary(true);
            streamer.perNodeBufferSize(10000);
            streamer.perNodeParallelOperations(100);

            BinaryObjectBuilder builder = client.binary().builder("PARAMS");

            builder.setField("ID", 1L);
            builder.setField("PARTITIONID", 1L);
            builder.setField("CLIENTID", 1L);
            builder.setField("PARAMETRCODE", 1L);
            builder.setField("PARAMETRVALUE", "Test value");
            builder.setField("PARENTID", 1L);
            BinaryObject obj = builder.build();

            streamer.addData(1L, obj);
            streamer.flush();

            stopAllServers(false);

            Thread.sleep(2_000);

            startGrid(SERVER_ID);

            try {
                assertNull(cache.get(1L));
            } catch (CacheException ce) {
                IgniteClientDisconnectedException icde = (IgniteClientDisconnectedException)ce.getCause();

                icde.reconnectFuture().get();

                assertNull(cache.get(1L));
            }

            info("Pre-insert");

            builder = client.binary().builder("PARAMS");
            builder.setField("ID", 2L);
            builder.setField("PARTITIONID", 1L);
            builder.setField("CLIENTID", 1L);
            builder.setField("PARAMETRCODE", 1L);
            builder.setField("PARAMETRVALUE", "Test value");
            builder.setField("PARENTID", 1L);
            obj = builder.build();

            //streamer.addData(2L, obj);
            cache.put(2L, obj);

            builder = client.binary().builder("PARAMS");
            builder.setField("ID", 3L);
            builder.setField("PARTITIONID", 1L);
            builder.setField("CLIENTID", 1L);
            builder.setField("PARAMETRCODE", 1L);
            builder.setField("PARAMETRVALUE", "Test value");
            builder.setField("PARENTID", 1L);
            obj = builder.build();

            //streamer.addData(3L, obj);
            cache.put(3L, obj);

            builder = client.binary().builder("PARAMS");
            builder.setField("ID", 4L);
            builder.setField("PARTITIONID", 1L);
            builder.setField("CLIENTID", 1L);
            builder.setField("PARAMETRCODE", 1L);
            builder.setField("PARAMETRVALUE", "Test value");
            builder.setField("PARENTID", 1L);
            obj = builder.build();

            cache.put(4L, obj);

            info("Post-insert");

            obj = cache.get(4L);

            assertNotNull(obj);

            info("End");
        }
        finally {
            stopAllGrids();
        }
    }
}
