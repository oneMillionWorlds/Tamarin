package com.onemillionworlds.tamarin.actions.controllerprofile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Some interaction profiles are only available if an extension has been loaded, and some extensions add extra inputs
 * to an existing profile. Suggesting bindings for such a profile (or input) when the extension isn't loaded is an
 * error, so Tamarin skips them.
 * <p>
 *     Primarily for internal use.
 * </p>
 */
public final class InteractionProfileExtensions{

    private static final Map<String, String> PROFILE_TO_REQUIRED_EXTENSION = Map.of(
            HandInteractionExt.PROFILE, HandInteractionExt.REQUIRED_EXTENSION
    );

    /**
     * Inputs (component paths) that an extension adds to a profile that otherwise doesn't need it.
     */
    private static final Map<String, String> COMPONENT_TO_REQUIRED_EXTENSION = Map.of(
            MetaHandMicrogestures.ComponentPaths.SWIPE_LEFT, MetaHandMicrogestures.REQUIRED_EXTENSION,
            MetaHandMicrogestures.ComponentPaths.SWIPE_RIGHT, MetaHandMicrogestures.REQUIRED_EXTENSION,
            MetaHandMicrogestures.ComponentPaths.SWIPE_FORWARD, MetaHandMicrogestures.REQUIRED_EXTENSION,
            MetaHandMicrogestures.ComponentPaths.SWIPE_BACKWARD, MetaHandMicrogestures.REQUIRED_EXTENSION,
            MetaHandMicrogestures.ComponentPaths.TAP_THUMB, MetaHandMicrogestures.REQUIRED_EXTENSION
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
     * @param profile the interaction profile, e.g. {@link HandInteractionExt#PROFILE}
     * @param binding the full binding path, e.g. /user/hand/left/input/pinch_ext/value
     * @return every extension that must be loaded for the binding to be suggested (the profile's and the input's)
     */
    public static List<String> requiredExtensions(String profile, String binding){
        List<String> extensions = new ArrayList<>(2);
        requiredExtension(profile).ifPresent(extensions::add);
        for(Map.Entry<String, String> componentExtension : COMPONENT_TO_REQUIRED_EXTENSION.entrySet()){
            if (binding.endsWith(componentExtension.getKey())){
                extensions.add(componentExtension.getValue());
            }
        }
        return extensions;
    }
}
