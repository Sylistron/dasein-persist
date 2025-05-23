import org.dasein.persist.*;
import org.dasein.persist.annotations.Schema;
import org.dasein.persist.jdbc.AutomatedSql.Operator;
import org.dasein.util.CachedItem;
import org.dasein.util.JiteratorFilter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;

public class MockPersistentCache<T extends CachedItem> extends PersistentCache<T> {
    private final Map<Object, T> store = new HashMap<>();

    public MockPersistentCache(Class<T> cls) {
        initBase(cls, null, "test", null, new Key("keyField"));
    }

    @Override
    public T create(Transaction xaction, Map<String, Object> state) throws PersistenceException {
        try {
            T item = getTarget().getDeclaredConstructor().newInstance();
            for (Map.Entry<String, Object> entry : state.entrySet()) {
                Field f = getField(getTarget(), entry.getKey());
                if (f != null) {
                    set(item, f, entry.getValue());
                }
            }
            Object keyVal = getValue(item, getPrimaryKeyField());
            store.put(keyVal, item);
            return item;
        } catch (Exception e) {
            throw new PersistenceException(e);
        }
    }

    private Field getField(Class<?> cls, String name) {
        Class<?> current = cls;
        while (!current.equals(Object.class)) {
            try {
                Field f = current.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignore) {
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private boolean matches(T item, SearchTerm[] terms) throws PersistenceException {
        if (terms == null) return true;
        for (SearchTerm term : terms) {
            Object val = getValue(item, term.getColumn());
            if (term.getOperator() == Operator.LIKE) {
                if (val == null || !val.toString().contains(String.valueOf(term.getValue()))) {
                    return false;
                }
            } else {
                if (val == null || !val.equals(term.getValue())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public @Nonnull Collection<T> find(@Nonnull SearchTerm[] terms, @Nullable JiteratorFilter<T> filter,
                                       @Nullable Boolean orderDesc, @Nullable String... orderFields) throws PersistenceException {
        List<T> results = new ArrayList<>();
        for (T item : store.values()) {
            if (matches(item, terms)) {
                results.add(item);
            }
        }
        if (orderFields != null && orderFields.length > 0) {
            final String field = orderFields[0];
            results.sort(Comparator.comparing(o -> (Comparable) getValue(o, field)));
            if (Boolean.TRUE.equals(orderDesc)) Collections.reverse(results);
        }
        return results;
    }

    @Override
    public T get(Object keyValue) throws PersistenceException {
        return store.get(keyValue);
    }

    @Override
    public T get(SearchTerm... terms) throws PersistenceException {
        for (T item : store.values()) {
            if (matches(item, terms)) return item;
        }
        return null;
    }

    @Override
    public String getSchema() {
        return "test";
    }

    @Override
    public Collection<T> list() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void remove(Transaction xaction, T item) {
        Object keyVal = getValue(item, getPrimaryKeyField());
        store.remove(keyVal);
    }

    @Override
    public void remove(Transaction xaction, SearchTerm... terms) throws PersistenceException {
        for (T item : new ArrayList<>(store.values())) {
            if (matches(item, terms)) {
                remove(xaction, item);
            }
        }
    }

    @Override
    public void update(Transaction xaction, T item, Map<String, Object> state) throws PersistenceException {
        for (Map.Entry<String, Object> entry : state.entrySet()) {
            Field f = getField(getTarget(), entry.getKey());
            if (f != null) {
                set(item, f, entry.getValue());
            }
        }
    }
}
