package com.onemillionworlds.tamarin.actions.actionprofile;

import com.onemillionworlds.tamarin.actions.ActionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
class ActionManifestTest {

    @Test
    void validate_duplicateActionSetNames(){
        ActionManifest.ValidationException exception =
                assertThrows(ActionManifest.ValidationException.class, () -> {
                    ActionManifest.builder()
                            .withActionSet(ActionSet.builder()
                                    .name("test_name_one")
                                    .translatedName("testTranslatedName")
                                    .priority(0)
                                    .build())
                            .withActionSet(ActionSet.builder()
                                    .name("test_name_one")
                                    .translatedName("testTranslatedName Two")
                                    .priority(0)
                                    .build())
                            .build();
        });

        assertTrue(exception.getMessage().contains("test_name_one"));
        assertFalse(exception.getMessage().contains("testTranslatedName"));
    }
    @Test
    void validate_duplicateTranslatedActionSetNames(){
        ActionManifest.ValidationException exception =
                assertThrows(ActionManifest.ValidationException.class, () -> {
                    ActionManifest.builder()
                            .withActionSet(ActionSet.builder()
                                    .name("test_name_one")
                                    .translatedName("testTranslatedName")
                                    .priority(0)
                                    .build())
                            .withActionSet(ActionSet.builder()
                                    .name("test_name_two")
                                    .translatedName("testTranslatedName")
                                    .priority(0)
                                    .build())
                            .build();
                });

        assertFalse(exception.getMessage().contains("test_name"));
        assertTrue(exception.getMessage().contains("testTranslatedName"));
    }

    @Test
    void validate_bothDuplicateTranslatedAndActionSetNames(){
        ActionManifest.ValidationException exception =
                assertThrows(ActionManifest.ValidationException.class, () -> {
                    ActionManifest.builder()
                            .withActionSet(ActionSet.builder()
                                    .name("test_name")
                                    .translatedName("testTranslatedName")
                                    .priority(0)
                                    .build())
                            .withActionSet(ActionSet.builder()
                                    .name("test_name")
                                    .translatedName("testTranslatedName")
                                    .priority(0)
                                    .build())
                            .build();
                });

        assertTrue(exception.getMessage().contains("test_name"));
        assertTrue(exception.getMessage().contains("testTranslatedName"));
    }

    @Test
    void validate_ActionNames(){
        ActionHandle duplicateHandle = new ActionHandle("test_set", "test_action");

        ActionManifest.ValidationException exception =
                assertThrows(ActionManifest.ValidationException.class, () -> {
                    ActionManifest.builder()
                            .withActionSet(ActionSet.builder()
                                    .name("test_set")
                                    .translatedName("testTranslatedName")
                                    .priority(0)
                                    .withAction(Action.builder()
                                            .actionHandle(duplicateHandle)
                                            .actionType(ActionType.POSE)
                                            .withSuggestAllKnownHapticBindings()
                                            .translatedName("testTranslatedName"))
                                    .withAction(Action.builder()
                                            .actionHandle(duplicateHandle)
                                            .actionType(ActionType.POSE)
                                            .withSuggestAllKnownHapticBindings()
                                            .translatedName("testTranslatedName Two"))
                                    )
                            .build();
                });

        assertTrue(exception.getMessage().contains("test_action"));
    }
}