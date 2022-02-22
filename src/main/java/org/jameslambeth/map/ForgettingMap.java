package org.jameslambeth.map;

public interface ForgettingMap<K, V> {

    boolean add(final K k, final V v);
    V find(final K k);
    int getMaxAssociations();
}
