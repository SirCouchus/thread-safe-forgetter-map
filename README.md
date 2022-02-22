# Thread-safe forgetting map

## James Lambeth

** Built and tested on Java 11 **

## Overview
Objective  
The objective of this task is design, implement and test a thread-safe &#39;forgetting map&#39;.  
A &#39;forgetting&#39; map should hold associations between a ‘key’ and some ‘content’. It should implement at least two methods:

1. add (add an association)
2. find (find content using the specified key).

It should hold as many associations as it can, but no more than x associations at any time, with x being a parameter passed to the
constructor. The association that is least used is removed from the map when a new entry requires a space and the map is at capacity,
where “least used” relates to the number of times each association has been retrieved by the “find” method.  A suitable tie breaker should
be used in the case where there are multiple least-used entries.

## Add
Adding a new association to the forgetting map will first check
whether adding the new association will exceed the configured maximum
association count.

In the event of the maximum association count being exceeded, the forgetting map
will remove the entry from the map which has the fewest number of retrievals.

For example, if the maximum association count is 2, and we have two associations present:

Entry 1: `Entry<"james1", "lam1">`

Entry 2: `Entry<"james2", "lam2">`

and Entry 1 has been retrieved once, whereas Entry 2 has never been retrieved,
then when we try to add a new Entry (thus triggering the maximum associations to be exceeded),
Entry 2 will be removed as it has the fewest number of retrievals.

In the event of there being multiple Entries all with equally "few" retrievals, the Entry that is removed
is the last Entry in the LinkedHashMap that is used to record the retrieval count.

## Find
When finding an association, the key's retrieval count is incremented when successfully retrieved.
The count is recorded in a LinkedHashMap which keeps track of each key and its associated count.

## Tests
The forgetting map has a series of [unit tests](src/test/java/org/jameslambeth/map/ForgettingMapImplTest.java) which
cover the following scenarios:

* Maximum Association tests
  * Verifies that the value passed to the constructor is correctly set
* Hash code tests
  * Verifies that the implementation's hash code is behaving correctly
* Add/Find tests
  * Verifies that adding an entry responds correctly when encountering errors (NPEs, already existing keys)
  * Verifies adding a new entry is added correctly
  * Verifies that the exceeding of the configured maximum will result in the least-retrieved item being removed
  * Verifies that the exceeding of the configured maximum will result in the tie-breaker being executed in the case of equal least-retrieved items