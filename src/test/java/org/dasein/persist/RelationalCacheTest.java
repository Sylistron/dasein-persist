package org.dasein.persist;

import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class RelationalCacheTest extends TestCase {
    private PersistentCache<PersistentObject> cache;

    @Before
    @Override
    public void setUp() throws Exception {
        cache = Mockito.spy(new MockPersistentCache<>(PersistentObject.class));
        Map<String,Object> state = new HashMap<>();
        state.put("keyField", 1L);
        state.put("name", "Get Name");
        state.put("description", "Get Description");
        cache.create(null, state);
    }

    @After
    @Override
    public void tearDown() throws PersistenceException {
        for(PersistentObject obj : cache.list()) {
            cache.remove(null, obj);
        }
    }

    @Test
    public void testGet() throws PersistenceException {
        PersistentObject obj = cache.get(1L);
        assertNotNull(obj);
        assertEquals("Get Name", obj.getName());
        Mockito.verify(cache).get(1L);
    }

    @Test
    public void testCreate() throws PersistenceException {
        Map<String,Object> state = new HashMap<>();
        state.put("keyField", 2L);
        state.put("name", "New Object");
        state.put("description", "Created");
        cache.create(null, state);
        assertEquals(2, cache.count());
        Mockito.verify(cache).create(Mockito.isNull(), Mockito.anyMap());
    }

    @Test
    public void testUpdate() throws PersistenceException {
        PersistentObject obj = cache.get(1L);
        Map<String,Object> state = new HashMap<>();
        state.put("name", "Updated");
        cache.update(null, obj, state);
        assertEquals("Updated", obj.getName());
        Mockito.verify(cache).update(Mockito.isNull(), Mockito.eq(obj), Mockito.anyMap());
    }

    @Test
    public void testRemove() throws PersistenceException {
        PersistentObject obj = cache.get(1L);
        cache.remove(null, obj);
        assertEquals(0, cache.count());
        Mockito.verify(cache).remove(Mockito.isNull(), Mockito.eq(obj));
    }
}
