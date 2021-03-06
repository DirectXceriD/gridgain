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

#ifndef _MSC_VER
#   define BOOST_TEST_DYN_LINK
#endif

#include <boost/test/unit_test.hpp>

#include "ignite/common/utils.h"

#include "ignite/ignite.h"
#include "ignite/ignition.h"
#include "ignite/test_utils.h"

using namespace ignite;
using namespace boost::unit_test;

/*
 * Test setup fixture.
 */
struct CacheStoreTestSuiteFixture
{
    /* Nodes started during the test. */
    Ignite node1;

    /*
     * Constructor.
     */
    CacheStoreTestSuiteFixture() :
#ifdef IGNITE_TESTS_32
        node1(ignite_test::StartNode("cache-store-32.xml", "node1"))
#else
        node1(ignite_test::StartNode("cache-store.xml", "node1"))
#endif

    {
        // No-op.
    }

    /*
     * Destructor.
     */
    ~CacheStoreTestSuiteFixture()
    {
        GetCache().RemoveAll();

        Ignition::StopAll(true);
    }

    /**
     * Cache accessor.
     */
    cache::Cache<int64_t, std::string> GetCache()
    {
        return node1.GetOrCreateCache<int64_t, std::string>("cache1");
    }
};

void FillStore(cache::Cache<int64_t, std::string>& cache, int64_t n)
{
    for (int64_t i = 0; i < n; ++i)
        cache.Put(i, common::LexicalCast<std::string>(i));

    cache.Clear();
}

BOOST_FIXTURE_TEST_SUITE(CacheStoreTestSuite, CacheStoreTestSuiteFixture)

BOOST_AUTO_TEST_CASE(LoadCacheSingleNodeNoPredicate)
{
    const int64_t entriesNum = 100;

    cache::Cache<int64_t, std::string> cache = GetCache();

    BOOST_CHECK(cache.IsEmpty());

    FillStore(cache, entriesNum);

    BOOST_CHECK(cache.IsEmpty());

    cache.LoadCache();

    BOOST_CHECK(!cache.IsEmpty());

    BOOST_CHECK_EQUAL(cache.Size(cache::CachePeekMode::PRIMARY), entriesNum);

    std::string val42 = cache.Get(42);

    BOOST_CHECK_EQUAL(val42, "42");
}

BOOST_AUTO_TEST_CASE(LoadCacheSeveralNodesNoPredicate)
{
    BOOST_CHECKPOINT("Starting additional node");
#ifdef IGNITE_TESTS_32
    Ignite node2 = ignite_test::StartNode("cache-store-32.xml", "node2");
#else
    Ignite node2 = ignite_test::StartNode("cache-store.xml", "node2");
#endif

    const int64_t entriesNum = 100;

    cache::Cache<int64_t, std::string> cache = GetCache();

    BOOST_CHECK(cache.IsEmpty());

    FillStore(cache, entriesNum);

    BOOST_CHECK(cache.IsEmpty());

    cache.LoadCache();

    BOOST_CHECK(!cache.IsEmpty());

    BOOST_CHECK_EQUAL(cache.Size(cache::CachePeekMode::PRIMARY), entriesNum);

    std::string val42 = cache.Get(42);

    BOOST_CHECK_EQUAL(val42, "42");
}

BOOST_AUTO_TEST_CASE(LocalLoadCacheSingleNodeNoPredicate)
{
    const int64_t entriesNum = 100;

    cache::Cache<int64_t, std::string> cache = GetCache();

    BOOST_CHECK(cache.IsEmpty());

    FillStore(cache, entriesNum);

    BOOST_CHECK(cache.IsEmpty());

    cache.LocalLoadCache();

    BOOST_CHECK(!cache.IsEmpty());

    BOOST_CHECK_EQUAL(cache.Size(cache::CachePeekMode::PRIMARY), entriesNum);

    std::string val42 = cache.Get(42);

    BOOST_CHECK_EQUAL(val42, "42");
}

BOOST_AUTO_TEST_SUITE_END()