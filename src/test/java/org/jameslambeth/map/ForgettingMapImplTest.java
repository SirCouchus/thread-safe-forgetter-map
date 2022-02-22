package org.jameslambeth.map;

import org.jameslambeth.map.impl.ForgettingMapImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Forgetting Map Tests")
public class ForgettingMapImplTest {

    private static final int MAX_ASSOCIATIONS = 5;

    private static final ForgettingMap<String, String> FORGETTING_MAP_STR_STR =
            new ForgettingMapImpl<>(MAX_ASSOCIATIONS);

    @Nested
    @DisplayName("Maximum Association Tests")
    class MaximumAssociationTests {

        @Test
        void createForgettingMap_getMaxAssociations_returnCorrectMaxAssociations() {

            assertThat(FORGETTING_MAP_STR_STR.getMaxAssociations()).isEqualTo(MAX_ASSOCIATIONS);
        }

    }

    @Nested
    @DisplayName("Hash Code Tests")
    class HashCodeTests {

        @Test
        void createForgettingMap_sameEntrySetAndMaxAssociations_returnTrue() {
            ForgettingMap<Integer, Integer> firstForgettingMap = new ForgettingMapImpl<>(MAX_ASSOCIATIONS);
            ForgettingMap<Integer, Integer> secondForgettingMap = new ForgettingMapImpl<>(MAX_ASSOCIATIONS);

            firstForgettingMap.add(1, 2);
            secondForgettingMap.add(1, 2);

            assertThat(firstForgettingMap).isEqualTo(secondForgettingMap).hasSameHashCodeAs(secondForgettingMap);
        }

        @Test
        void createForgettingMap_sameEntrySetButDifferentMaxAssociations_returnFalse() {
            ForgettingMap<Integer, Integer> firstForgettingMap = new ForgettingMapImpl<>(MAX_ASSOCIATIONS);
            ForgettingMap<Integer, Integer> secondForgettingMap = new ForgettingMapImpl<>(MAX_ASSOCIATIONS + 1);

            firstForgettingMap.add(1, 2);
            secondForgettingMap.add(1, 2);

            assertThat(firstForgettingMap).isNotEqualTo(secondForgettingMap).doesNotHaveSameHashCodeAs(secondForgettingMap);
        }

        @Test
        void createForgettingMap_differentEntrySetButSameMaxAssociations_returnFalse() {
            ForgettingMap<Integer, Integer> firstForgettingMap = new ForgettingMapImpl<>(MAX_ASSOCIATIONS);
            ForgettingMap<Integer, Integer> secondForgettingMap = new ForgettingMapImpl<>(MAX_ASSOCIATIONS);

            firstForgettingMap.add(4, 1);
            secondForgettingMap.add(1, 2);

            assertThat(firstForgettingMap).isNotEqualTo(secondForgettingMap).doesNotHaveSameHashCodeAs(secondForgettingMap);
        }
    }

    @Nested
    @DisplayName("Test Add and Find")
    class AddTests {

        @Test
        void addNullKey_throwNPE() {

            NullPointerException npe = assertThrows(NullPointerException.class,
                    () -> FORGETTING_MAP_STR_STR.add(null, "null"));

            assertThat(npe.getMessage()).isEqualTo("Key or value must not be null.");
        }

        @Test
        void addNullValue_throwNPE() {

            NullPointerException npe = assertThrows(NullPointerException.class,
                    () -> FORGETTING_MAP_STR_STR.add("null", null));

            assertThat(npe.getMessage()).isEqualTo("Key or value must not be null.");
        }

        @Test
        void addAlreadyExistingKey_addReturnsFalse() {
            final String alreadyPresentKey = UUID.randomUUID().toString();

            ForgettingMap<String, String> forgettingMap = new ForgettingMapImpl<>(MAX_ASSOCIATIONS);

            forgettingMap.add(alreadyPresentKey, alreadyPresentKey);

            boolean expectedFalse = forgettingMap.add(alreadyPresentKey, alreadyPresentKey);

            assertThat(expectedFalse).isFalse();
        }

        @Test
        void addNewKey_sizeLessThanMaximumAssociations_returnTrue() {
            final String newKey = UUID.randomUUID().toString();

            ForgettingMap<String, String> forgettingMap = new ForgettingMapImpl<>(MAX_ASSOCIATIONS);

            boolean expectedTrue = forgettingMap.add(newKey, newKey);

            assertThat(expectedTrue).isTrue();
        }

        @Test
        void addNewKey_sizeGreaterThanMaximumAssociations_removeLeastUsedKey() {
            final String firstKey = UUID.randomUUID().toString();
            final String secondKey = UUID.randomUUID().toString();
            final String thirdKey = UUID.randomUUID().toString();

            ForgettingMap<String, String> forgettingMap = new ForgettingMapImpl<>(3);

            boolean firstKeyResult = forgettingMap.add(firstKey, firstKey);
            boolean secondKeyResult = forgettingMap.add(secondKey, secondKey);
            boolean thirdKeyResult = forgettingMap.add(thirdKey, thirdKey);

            // Increments the counter for the first and third keys
            forgettingMap.find(firstKey);
            forgettingMap.find(thirdKey);

            assertThat(firstKeyResult).isTrue();
            assertThat(secondKeyResult).isTrue();
            assertThat(thirdKeyResult).isTrue();

            final String fourthKey = UUID.randomUUID().toString();

            forgettingMap.add(fourthKey, fourthKey);

            final String retrievedValueForFirstKey = forgettingMap.find(firstKey);
            final String retrievedValueForSecondKey = forgettingMap.find(secondKey);
            final String retrievedValueForThirdKey = forgettingMap.find(thirdKey);
            final String retrievedValueForFourthKey = forgettingMap.find(fourthKey);

            assertThat(retrievedValueForFirstKey).isEqualTo(firstKey);
            assertThat(retrievedValueForSecondKey).isNull();
            assertThat(retrievedValueForThirdKey).isEqualTo(thirdKey);
            assertThat(retrievedValueForFourthKey).isEqualTo(fourthKey);

            final String fifthKey = UUID.randomUUID().toString();

            boolean fifthKeyResult = forgettingMap.add(fifthKey, fifthKey);

            assertThat(fifthKeyResult).isTrue();

            final String retrievedValueForFourthKeyAfterFifthInsertion = forgettingMap.find(fourthKey);
            final String retrievedValueForFifthKey = forgettingMap.find(fifthKey);

            assertThat(retrievedValueForFourthKeyAfterFifthInsertion).isNull();
            assertThat(retrievedValueForFifthKey).isEqualTo(fifthKey);
        }

        @Test
        void addNewKey_sizeGreaterThanMaximumAssociations_multipleMinimumValues_removeLast() {
            final String firstKey = UUID.randomUUID().toString();
            final String secondKey = UUID.randomUUID().toString();

            ForgettingMap<String, String> forgettingMap = new ForgettingMapImpl<>(2);

            boolean firstKeyResult = forgettingMap.add(firstKey, firstKey);
            boolean secondKeyResult = forgettingMap.add(secondKey, secondKey);

            assertThat(firstKeyResult).isTrue();
            assertThat(secondKeyResult).isTrue();

            forgettingMap.find(firstKey);
            forgettingMap.find(secondKey);

            final String thirdKey = UUID.randomUUID().toString();

            boolean thirdKeyResult = forgettingMap.add(thirdKey, thirdKey);

            assertThat(thirdKeyResult).isTrue();

            final String retrievedValueForFirstKey = forgettingMap.find(firstKey);
            final String retrievedValueForSecondKey = forgettingMap.find(secondKey);
            final String retrievedValueForThirdKey = forgettingMap.find(thirdKey);

            assertThat(retrievedValueForFirstKey).isEqualTo(firstKey);
            assertThat(retrievedValueForSecondKey).isNull();
            assertThat(retrievedValueForThirdKey).isEqualTo(thirdKey);
        }
    }
}
