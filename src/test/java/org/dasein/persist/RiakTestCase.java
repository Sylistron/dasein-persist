package org.dasein.persist;

import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.dasein.persist.annotations.IndexType;
import org.dasein.persist.jdbc.AutomatedSql.Operator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

public class RiakTestCase extends TestCase {
    private PersistentCache<PersistentObject> cache;
    private PersistentCache<SchemaChange> changeCache;
    private Logger logger;
    private TreeSet<PersistentObject> testMatches;

    @Before
    @Override
    public void setUp() throws Exception {
        cache = Mockito.spy(new MockPersistentCache<>(PersistentObject.class));
        changeCache = new MockPersistentCache<>(SchemaChange.class);
        logger = Logger.getLogger("org.dasein.persist.test." + getName());
        testMatches = new TreeSet<>();

        long id = 1L;
        if (!getName().equals("testCreate")) {
            Map<String,Object> state = new HashMap<>();
            String name = "Get Name";
            String description = "Get Description";
            state.put("keyField", id);
            state.put("name", name);
            state.put("description", description);
            state.put("indexType", IndexType.FOREIGN);
            state.put("currency", Currency.getInstance("USD"));
            state.put("amount", 24.56);
            state.put("indexA", "a");
            state.put("indexB", "unknown");
            state.put("indexC", "unknown");
            cache.create(null, state);

            Map<String,Object> state2 = new HashMap<>();
            state2.put("keyField", id);
            state2.put("name", name);
            state2.put("funDescription", description);
            changeCache.create(null, state2);
        }
        if (getName().equals("testFindMulti") || getName().equals("testFindMultiIncomplete")) {
            Map<String,Object> state = new HashMap<>();
            state.put("keyField", ++id);
            state.put("name", "Multi Test");
            state.put("description", "A multi test object");
            state.put("indexType", IndexType.SECONDARY);
            state.put("currency", Currency.getInstance("USD"));
            state.put("amount", 78.90);
            state.put("indexA", "a");
            state.put("indexB", "b");
            if (getName().equals("testFindMultiIncomplete")) {
                state.put("indexC", "c");
            } else {
                state.put("indexC", "unknown");
            }
            testMatches.add(cache.create(null, state));
        }
        if (getName().equals("testFindLike") || getName().equals("testSort")) {
            String[][] data = new String[][] {
                    {"Second Test", "A second test object"},
                    {"Third Test", "A third Test object"},
                    {"Fourth Test", "Test number four"}
            };
            for (String[] d : data) {
                Map<String,Object> state = new HashMap<>();
                state.put("keyField", ++id);
                state.put("name", d[0]);
                state.put("description", d[1]);
                state.put("indexType", IndexType.FOREIGN);
                state.put("currency", Currency.getInstance("USD"));
                state.put("amount", 78.90);
                PersistentObject item = cache.create(null, state);
                testMatches.add(item);
            }
        }
    }

    @After
    @Override
    public void tearDown() {
        for (PersistentObject item : cache.list()) {
            cache.remove(null, item);
        }
    }

    @Test
    public void testCount() throws PersistenceException {
        assertEquals(1, cache.count());
    }

    @Test
    public void testCountEmpty() throws PersistenceException {
        assertEquals(0, cache.count(new SearchTerm("name", "No such name")));
    }

    @Test
    public void testCountMatch() throws PersistenceException {
        assertEquals(1, cache.count(new SearchTerm("name", "Get Name")));
    }

    @Test
    public void testCreate() throws PersistenceException {
        Map<String,Object> state = new HashMap<>();
        state.put("keyField", 99L);
        state.put("name", "Create - 99");
        state.put("description", "Creating persistent object 99");
        state.put("indexType", IndexType.FOREIGN);
        state.put("currency", Currency.getInstance("GBP"));
        state.put("amount", 56.78);
        cache.create(null, state);
        assertNotNull(cache.get(99L));
        Mockito.verify(cache).create(Mockito.isNull(), Mockito.anyMap());
    }

    @Test
    public void testGet() throws PersistenceException {
        PersistentObject item = cache.get(1L);
        assertNotNull(item);
        assertEquals("Get Name", item.getName());
        Mockito.verify(cache).get(1L);
    }

    @Test
    public void testFindEmpty() throws PersistenceException {
        int count = 0;
        for (PersistentObject item : cache.find(new SearchTerm("name", "No such value"))) {
            count++;
        }
        assertEquals(0, count);
    }

    @Test
    public void testFindNamed() throws PersistenceException {
        int count = 0;
        for (PersistentObject item : cache.find(new SearchTerm("name", "Get Name"))) {
            count++;
        }
        assertEquals(1, count);
    }

    @Test
    public void testFindMulti() throws PersistenceException {
        SearchTerm[] terms = { new SearchTerm("indexA", "a"), new SearchTerm("indexB", "b") };
        int count = 0;
        for (PersistentObject item : cache.find(terms)) {
            assertEquals("a", item.getIndexA());
            assertEquals("b", item.getIndexB());
            count++;
        }
        assertTrue(count > 0);
    }

    @Test
    public void testFindMultiIncomplete() throws PersistenceException {
        SearchTerm[] terms = { new SearchTerm("indexA", "a"), new SearchTerm("indexB", "b"), new SearchTerm("indexC", "c") };
        int count = 0;
        for (PersistentObject item : cache.find(terms)) {
            assertEquals("c", item.getIndexC());
            count++;
        }
        assertTrue(count > 0);
    }

    @Test
    public void testFindLike() throws PersistenceException {
        int count = 0;
        for (PersistentObject item : cache.find(new SearchTerm("description", Operator.LIKE, "test"))) {
            logger.debug("Item: " + item);
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void testList() throws PersistenceException {
        int count = 0;
        for (PersistentObject item : cache.list()) {
            logger.debug("Item: " + item);
            count++;
        }
        assertEquals(1, count);
    }

    @Test
    public void testRemove() throws PersistenceException {
        PersistentObject item = cache.get(1L);
        cache.remove(null, item);
        assertNull(cache.get(1L));
        Mockito.verify(cache).remove(Mockito.isNull(), Mockito.eq(item));
    }

    @Test
    public void testSort() throws PersistenceException {
        TreeSet<PersistentObject> matches = new TreeSet<>();
        for (PersistentObject item : cache.find(new SearchTerm[]{ new SearchTerm("description", Operator.LIKE, "test") }, null, false, "name")) {
            matches.add(item);
        }
        assertEquals(matches.size(), testMatches.size());
    }

    @Test
    public void testUpdate() throws PersistenceException {
        PersistentObject item = cache.get(1L);
        Map<String,Object> state = new HashMap<>();
        state.put("name", "New Name");
        state.put("description", "New Description");
        cache.update(null, item, state);
        assertEquals("New Name", item.getName());
        Mockito.verify(cache).update(Mockito.isNull(), Mockito.eq(item), Mockito.anyMap());
    }

    @Test
    public void testConversion() throws PersistenceException {
        SchemaChange sc = changeCache.get(1L);
        assertNotNull(sc);
        assertEquals("Get Description", sc.getFunDescription());
    }
}
