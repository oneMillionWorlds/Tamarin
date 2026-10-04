package com.onemillionworlds.tamarin.actions.controllerprofile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Some interaction profiles are only available if an extension has been loaded. Suggesting bindings for such a
 * profile when the extension isn't loaded is an error, so Tamarin skips them.
 * <p>
 *     Some extensions (e.g. XR_META_hand_tracking_microgestures) add extra components to a profile that already exists.
 *     Suggesting a binding to one of those components without the extension is also an error (and fails all the
 *     bindings for that profile) so those individual bindings are skipped as well.
 * </p>
 * <p>
 *     Primarily for internal use.
 * </p>
 */
public final class InteractionProfileExtensions{

    private static final Map<String, String> PROFILE_TO_REQUIRED_EXTENSION = Map.of(
            HandInteractionExt.PROFILE, HandInteractionExt.REQUIRED_EXTENSION
    );

    /**
     * Profile -> (component path -> extension). Component paths exclude the top level user path (e.g. /user/hand/left).
     */
    private static final Map<String, Map<String, String>> COMPONENT_TO_REQUIRED_EXTENSION = Map.of(
            MetaHandMicrogestures.PROFILE, Map.of(
                    MetaHandMicrogestures.ComponentPaths.SWIPE_LEFT, MetaHandMicrogestures.REQUIRED_EXTENSION,
                    MetaHandMicrogestures.ComponentPaths.SWIPE_RIGHT, MetaHandMicrogestures.REQUIRED_EXTENSION,
                    MetaHandMicrogestures.ComponentPaths.SWIPE_FORWARD, MetaHandMicrogestures.REQUIRED_EXTENSION,
                    MetaHandMicrogestures.ComponentPaths.SWIPE_BACKWARD, MetaHandMicrogestures.REQUIRED_EXTENSION,
                    MetaHandMicrogestures.ComponentPaths.TAP_THUMB, MetaHandMicrogestures.REQUIRED_EXTENSION
            )
    );

    private InteractionProfileExtensions(){}

    /**
     * @param profile the interaction profile, e.g. {@link HandInteractionExt#PROFILE}
     * @return the extension that must be loaded for the profile to be usable, if any
     */
    public static Optional<String> requiredExtension(String profile){
        return Optional.ofNullable(PROFILE_TO_REQUIRED_EXTENSION.get(profile));
    }

    /**
     * Gets all the extensions that must be loaded for a particular binding to be suggested. This includes the
     * extension required by the profile itself (see {@link #requiredExtension(String)}) and any extension required
     * by the specific component.
     *
     * @param profile the interaction profile, e.g. {@link HandInteractionExt#PROFILE}
     * @param binding the full binding path, e.g. "/user/hand/left/input/tap_thumb_meta/click"
     * @return the extensions that must be loaded, empty if none are required
     */
    public static List<String> requiredExtensionsForBinding(String profile, String binding){
        List<String> requiredExtensions = new ArrayList<>(2);
        requiredExtension(profile).ifPresent(requiredExtensions::add);

        Map<String, String> componentExtensions = COMPONENT_TO_REQUIRED_EXTENSION.get(profile);
        if (componentExtensions != null){
            for(Map.Entry<String, String> componentAndExtension : componentExtensions.entrySet()){
                if (binding.endsWith(componentAndExtension.getKey()) && !requiredExtensions.contains(componentAndExtension.getValue())){
                    requiredExtensions.add(componentAndExtension.getValue());
                }
            }
        }
        return requiredExtensions;
    }
}
