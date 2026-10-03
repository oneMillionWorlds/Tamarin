package com.onemillionworlds.tamarin.actions.controllerprofile;

import java.util.Map;
import java.util.Optional;

/**
 * Some interaction profiles are only available if an extension has been loaded. Suggesting bindings for such a
 * profile when the extension isn't loaded is an error, so Tamarin skips them.
 * <p>
 *     Primarily for internal use.
 * </p>
 */
public final class InteractionProfileExtensions{

    private static final Map<String, String> PROFILE_TO_REQUIRED_EXTENSION = Map.of(
            HandInteractionExt.PROFILE, HandInteractionExt.REQUIRED_EXTENSION
    );

    private InteractionProfileExtensions(){}

    /**
     * @param profile the interaction profile, e.g. {@link HandInteractionExt#PROFILE}
     * @return the extension that must be loaded for the profile to be usable, if any
     */
    public static Optional<String> requiredExtension(String profile){
        return Optional.ofNullable(PROFILE_TO_REQUIRED_EXTENSION.get(profile));
    }
}
