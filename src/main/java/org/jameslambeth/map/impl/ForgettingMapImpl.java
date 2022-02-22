package org.jameslambeth.map.impl;

import org.jameslambeth.map.ForgettingMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Thread-safe Forgetting Map which extends ConcurrentHashMap
 *
 * @param <K> The map key
 * @param <V> The map value
 */
public final class ForgettingMapImpl<K, V> extends ConcurrentHashMap<K, V> implements ForgettingMap<K, V> {

    private static final Logger log = LoggerFactory.getLogger(ForgettingMapImpl.class);

    /*
     * Keep count of the associations and the position of the 'last added' in case of tie-breaker;
     * AtomicInteger allows us to `getAndIncrement()` when finding
     */
    private final LinkedHashMap<K, AtomicInteger> associationCount = new LinkedHashMap<>();

    private final int maxAssociations;

    /**
     * Constructs a Forgetting Map with a given maximum of key-value associations
     *
     * @param maxAssociations The maximum number of associations
     */
    public ForgettingMapImpl(final int maxAssociations) {
        this.maxAssociations = maxAssociations;
    }

    /**
     * Add a new association to the forgetting map.
     * <p>
     * If the size of the forgetting map will exceed the maximum associations,
     * The least-retrieved association is removed from the map. If there are multiple
     * associations that tie for the least-retrieved, then the last item in the LinkedList
     * will be removed.
     *
     * @param k The key of the association
     * @param v The value of the association
     * @return True if the forgetting map was updated
     */
    @Override
    public synchronized boolean add(final K k, final V v) {
        isValidKeyValue(k, v);
        if (this.containsKey(k)) {
            log.debug("Association already exists");
            return false;
        }

        if (this.size() < maxAssociations) {
            addToMapAndCounter(k, v);
            return true;
        }

        var leastUsedAssociations = getLeastUsedAssociations();

        // Should never really be true
        if (leastUsedAssociations.isEmpty()) {
            log.error("No association count found.");
            return false;
        }

        if (leastUsedAssociations.size() > 1) {
            doTieBreakerAndUpdateMap(k, v, leastUsedAssociations);
            return true;
        }

        // Only one occurrence of min value so remove that
        removeLeastUsedAndUpdateMap(k, v, leastUsedAssociations);

        return true;
    }

    /**
     * Finds a value in the forgetting map by its associated key
     * <p>
     * Each time an item is retrieved from the forgetting map, the count for that
     * particular association is incremented. When the maximum associations have been
     * exceeded, the association with the fewest retrievals will be removed.
     *
     * @param k The key associated with the desired value
     * @return The associated value
     */
    @Override
    public synchronized V find(final K k) {
        if (!this.containsKey(k)) {
            return null;
        }
        incrementAssociationCount(k);
        return this.get(k);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        var that = (ForgettingMapImpl<?, ?>) o;
        return maxAssociations == that.maxAssociations;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.entrySet(), maxAssociations);
    }

    @Override
    public int getMaxAssociations() {
        return this.maxAssociations;
    }

    /**
     * Validates null key/value entries. If the given key or value is null,
     * a NullPointerException is thrown
     *
     * @param k The key to validate
     * @param v The value to validate
     */
    private void isValidKeyValue(K k, V v) {
        if (k == null || v == null) {
            log.error("Key or value must not be null.");
            throw new NullPointerException("Key or value must not be null.");
        }
    }

    /**
     * Find the minimum retrieval count of each association then return all associations with that
     * minimum count.
     *
     * @return LinkedList of every Entry matching the minimum retrieval count.
     */
    private LinkedList<Entry<K, AtomicInteger>> getLeastUsedAssociations() {
        // Get the minimum retrieval associations count
        int minimumAmountOfAssociations = Collections.min(associationCount.entrySet(),
                        Comparator.comparingInt(assoc -> assoc.getValue().get()))
                .getValue()
                .get();

        // Return a list of all keys matching minimum
        return associationCount.entrySet()
                .stream()
                .filter(assoc -> assoc.getValue().get() == minimumAmountOfAssociations)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * If there are multiple associations in the forgetting map with a minimum retrieval count,
     * the tie-breaker will remove the last item in the map through the Linked List's removeLast() method
     * before adding the new association.
     *
     * @param k                     The key of the new association
     * @param v                     The value of the new association
     * @param leastUsedAssociations The list of Entries with an equal retrieval count
     */
    private void doTieBreakerAndUpdateMap(K k, V v,
                                          LinkedList<Entry<K, AtomicInteger>> leastUsedAssociations) {
        log.info("Multiple least used entries. Using tie-breaker.");
        var removedAssociation = leastUsedAssociations.removeLast();
        log.debug("Removed {} from forgetting map.", removedAssociation);
        removeFromCountAndForgettingMap(removedAssociation.getKey());
        addToMapAndCounter(k, v);
    }

    /**
     * Removes the least-retrieved association from the forgetting before adding the new
     * association.
     *
     * @param k                     The key of the new association
     * @param v                     The value of the new association
     * @param leastUsedAssociations The list of Entries with an equal retrieval count
     */
    private void removeLeastUsedAndUpdateMap(K k, V v,
                                             LinkedList<Entry<K, AtomicInteger>> leastUsedAssociations) {
        K leastUsedAssociation = leastUsedAssociations.get(0).getKey();
        removeFromCountAndForgettingMap(leastUsedAssociation);
        addToMapAndCounter(k, v);
    }

    /**
     * Removes an association from the association count and the forgetting map.
     *
     * @param leastUsedAssociation The least-retrieved association to remove
     */
    private void removeFromCountAndForgettingMap(K leastUsedAssociation) {
        remove(leastUsedAssociation);
        associationCount.remove(leastUsedAssociation);
    }

    /**
     * Adds a new association to the forgetting map and the association counter.
     *
     * @param k The key to add
     * @param v The value to add
     */
    private void addToMapAndCounter(K k, V v) {
        this.put(k, v);
        associationCount.put(k, new AtomicInteger(0));
    }

    /**
     * Increments the association count for a given key.
     *
     * @param k The key to increment
     */
    private void incrementAssociationCount(K k) {
        associationCount.get(k).getAndIncrement();
    }

}
