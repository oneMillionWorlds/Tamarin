package com.onemillionworlds.tamarin.actions.actionprofile;

import com.onemillionworlds.tamarin.actions.ActionType;
import com.onemillionworlds.tamarin.actions.controllerprofile.HandInteractionExt;
import com.onemillionworlds.tamarin.actions.controllerprofile.MetaHandMicrogestures;
import com.onemillionworlds.tamarin.actions.controllerprofile.OculusTouchController;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Test
    void allBindingsAreSuggestedWhenTheirExtensionsAreLoaded(){
        Collection<SuggestedBindingsProfileView> views = manifestWithHandBindings().getSuggestedBindingsGroupedByProfile(extension -> true);

        assertEquals(Set.of(
                HandInteractionExt.pathBuilder().leftHand().pinchValue(),
                MetaHandMicrogestures.pathBuilder().leftHand().tapThumb()), bindingsFor(views, HandInteractionExt.PROFILE));
        assertEquals(Set.of(OculusTouchController.pathBuilder().leftHand().xClick()), bindingsFor(views, OculusTouchController.PROFILE));
    }

    @Test
    void microgestureBindingsAreOmittedWithoutTheirExtensionButTheRestOfTheProfileIsKept(){
        Collection<SuggestedBindingsProfileView> views = manifestWithHandBindings().getSuggestedBindingsGroupedByProfile(extension -> !extension.equals(MetaHandMicrogestures.REQUIRED_EXTENSION));

        assertEquals(Set.of(HandInteractionExt.pathBuilder().leftHand().pinchValue()), bindingsFor(views, HandInteractionExt.PROFILE));
    }

    @Test
    void profileIsOmittedWithoutItsExtension(){
        Collection<SuggestedBindingsProfileView> views = manifestWithHandBindings().getSuggestedBindingsGroupedByProfile(extension -> false);

        assertEquals(Set.of(OculusTouchController.PROFILE), views.stream().map(SuggestedBindingsProfileView::getProfileName).collect(Collectors.toSet()));
    }

    private static ActionManifest manifestWithHandBindings(){
        return ActionManifest.builder()
                .withActionSet(ActionSet.builder()
                        .name("test_set")
                        .translatedName("Test set")
                        .priority(0)
                        .withAction(Action.builder()
                                .actionHandle(new ActionHandle("test_set", "select"))
                                .translatedName("Select")
                                .actionType(ActionType.BOOLEAN)
                                .withSuggestedBinding(OculusTouchController.PROFILE, OculusTouchController.pathBuilder().leftHand().xClick())
                                .withSuggestedBinding(HandInteractionExt.PROFILE, HandInteractionExt.pathBuilder().leftHand().pinchValue()))
                        .withAction(Action.builder()
                                .actionHandle(new ActionHandle("test_set", "tap"))
                                .translatedName("Tap")
                                .actionType(ActionType.BOOLEAN)
                                .withSuggestedBinding(MetaHandMicrogestures.PROFILE, MetaHandMicrogestures.pathBuilder().leftHand().tapThumb())))
                .build();
    }

    private static Set<String> bindingsFor(Collection<SuggestedBindingsProfileView> views, String profile){
        return views.stream()
                .filter(view -> view.getProfileName().equals(profile))
                .flatMap(view -> view.getSetToActionToBindingMap().values().stream())
                .collect(Collectors.toSet());
    }
}
