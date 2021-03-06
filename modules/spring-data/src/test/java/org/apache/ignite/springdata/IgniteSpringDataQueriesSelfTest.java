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

package org.apache.ignite.springdata;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import javax.cache.Cache;
import org.apache.ignite.springdata.misc.ApplicationConfiguration;
import org.apache.ignite.springdata.misc.PersonRepository;
import org.apache.ignite.springdata.misc.Person;
import org.apache.ignite.springdata.misc.PersonSecondRepository;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

/**
 *
 */
@RunWith(JUnit4.class)
public class IgniteSpringDataQueriesSelfTest extends GridCommonAbstractTest {
    /** Repository. */
    private static PersonRepository repo;

    /** Repository 2. */
    private static PersonSecondRepository repo2;

    /** Context. */
    private static AnnotationConfigApplicationContext ctx;

    /** Number of entries to store */
    private static int CACHE_SIZE = 1000;

    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        ctx = new AnnotationConfigApplicationContext();

        ctx.register(ApplicationConfiguration.class);

        ctx.refresh();

        repo = ctx.getBean(PersonRepository.class);
        repo2 = ctx.getBean(PersonSecondRepository.class);

        for (int i = 0; i < CACHE_SIZE; i++)
            repo.save(i, new Person("person" + Integer.toHexString(i),
                "lastName" + Integer.toHexString((i + 16) % 256)));
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        ctx.destroy();
    }

    /** */
    @Test
    public void testExplicitQuery() {
        List<Person> persons = repo.simpleQuery("person4a");

        assertFalse(persons.isEmpty());

        for (Person person : persons)
            assertEquals("person4a", person.getFirstName());
    }

    /** */
    @Test
    public void testEqualsPart() {
        List<Person> persons = repo.findByFirstName("person4e");

        assertFalse(persons.isEmpty());

        for (Person person : persons)
            assertEquals("person4e", person.getFirstName());
    }

    /** */
    @Test
    public void testContainingPart() {
        List<Person> persons = repo.findByFirstNameContaining("person4");

        assertFalse(persons.isEmpty());

        for (Person person : persons)
            assertTrue(person.getFirstName().startsWith("person4"));
    }

    /** */
    @Test
    public void testTopPart() {
        Iterable<Person> top = repo.findTopByFirstNameContaining("person4");

        Iterator<Person> iter = top.iterator();

        Person person = iter.next();

        assertFalse(iter.hasNext());

        assertTrue(person.getFirstName().startsWith("person4"));
    }

    /** */
    @Test
    public void testLikeAndLimit() {
        Iterable<Person> like = repo.findFirst10ByFirstNameLike("person");

        int cnt = 0;

        for (Person next : like) {
            assertTrue(next.getFirstName().contains("person"));

            cnt++;
        }

        assertEquals(10, cnt);
    }

    /** */
    @Test
    public void testCount() {
        int cnt = repo.countByFirstNameLike("person");

        assertEquals(1000, cnt);
    }

    /** */
    @Test
    public void testCount2() {
        int cnt = repo.countByFirstNameLike("person4");

        assertTrue(cnt < 1000);
    }

    /** */
    @Test
    public void testPageable() {
        PageRequest pageable = new PageRequest(1, 5, Sort.Direction.DESC, "firstName");

        HashSet<String> firstNames = new HashSet<>();

        List<Person> pageable1 = repo.findByFirstNameRegex("^[a-z]+$", pageable);

        assertEquals(5, pageable1.size());

        for (Person person : pageable1) {
            firstNames.add(person.getFirstName());
            assertTrue(person.getFirstName().matches("^[a-z]+$"));
        }

        List<Person> pageable2 = repo.findByFirstNameRegex("^[a-z]+$", pageable.next());

        assertEquals(5, pageable2.size());

        for (Person person : pageable2) {
            firstNames.add(person.getFirstName());
            assertTrue(person.getFirstName().matches("^[a-z]+$"));
        }

        assertEquals(10, firstNames.size());
    }

    /** */
    @Test
    public void testAndAndOr() {
        int cntAnd = repo.countByFirstNameLikeAndSecondNameLike("person1", "lastName1");

        int cntOr = repo.countByFirstNameStartingWithOrSecondNameStartingWith("person1", "lastName1");

        assertTrue(cntAnd <= cntOr);
    }

    /** */
    @Test
    public void testQueryWithSort() {
        List<Person> persons = repo.queryWithSort("^[a-z]+$", new Sort(Sort.Direction.DESC, "secondName"));

        Person previous = persons.get(0);

        for (Person person : persons) {
            assertTrue(person.getSecondName().compareTo(previous.getSecondName()) <= 0);

            assertTrue(person.getFirstName().matches("^[a-z]+$"));

            previous = person;
        }
    }

    /** */
    @Test
    public void testQueryWithPaging() {
        List<Person> persons = repo.queryWithPageable("^[a-z]+$", new PageRequest(1, 7, Sort.Direction.DESC, "secondName"));

        assertEquals(7, persons.size());

        Person previous = persons.get(0);

        for (Person person : persons) {
            assertTrue(person.getSecondName().compareTo(previous.getSecondName()) <= 0);

            assertTrue(person.getFirstName().matches("^[a-z]+$"));

            previous = person;
        }
    }

    /** */
    @Test
    public void testQueryFields() {
        List<String> persons = repo.selectField("^[a-z]+$", new PageRequest(1, 7, Sort.Direction.DESC, "secondName"));

        assertEquals(7, persons.size());
    }

    /** */
    @Test
    public void testFindCacheEntries() {
        List<Cache.Entry<Integer, Person>> cacheEntries = repo.findBySecondNameLike("stName1");

        assertFalse(cacheEntries.isEmpty());

        for (Cache.Entry<Integer, Person> entry : cacheEntries)
            assertTrue(entry.getValue().getSecondName().contains("stName1"));
    }

    /** */
    @Test
    public void testFindOneCacheEntry() {
        Cache.Entry<Integer, Person> cacheEntry = repo.findTopBySecondNameLike("tName18");

        assertNotNull(cacheEntry);

        assertTrue(cacheEntry.getValue().getSecondName().contains("tName18"));
    }

    /** */
    @Test
    public void testFindOneValue() {
        Person person = repo.findTopBySecondNameStartingWith("lastName18");

        assertNotNull(person);

        assertTrue(person.getSecondName().startsWith("lastName18"));
    }

    /** */
    @Test
    public void testSelectSeveralFields() {
        List<List> lists = repo.selectSeveralField("^[a-z]+$", new PageRequest(2, 6));

        assertEquals(6, lists.size());

        for (List list : lists) {
            assertEquals(2, list.size());

            assertTrue(list.get(0) instanceof Integer);
        }
    }

    /** */
    @Test
    public void testCountQuery() {
        int cnt = repo.countQuery(".*");

        assertEquals(256, cnt);
    }

    /** */
    @Test
    public void testSliceOfCacheEntries() {
        Slice<Cache.Entry<Integer, Person>> slice = repo2.findBySecondNameIsNot("lastName18", new PageRequest(3, 4));

        assertEquals(4, slice.getSize());

        for (Cache.Entry<Integer, Person> entry : slice)
            assertFalse("lastName18".equals(entry.getValue().getSecondName()));
    }

    /** */
    @Test
    public void testSliceOfLists() {
        Slice<List> lists = repo2.querySliceOfList("^[a-z]+$", new PageRequest(0, 3));

        assertEquals(3, lists.getSize());

        for (List list : lists) {
            assertEquals(2, list.size());

            assertTrue(list.get(0) instanceof Integer);
        }
    }

    /**
     * Tests the repository method with a custom query which takes no parameters.
     */
    @Test
    public void testCountAllPersons() {
        int cnt = repo.countAllPersons();

        assertEquals(CACHE_SIZE, cnt);
    }
}

