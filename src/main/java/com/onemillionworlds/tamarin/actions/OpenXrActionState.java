package com.onemillionworlds.tamarin.actions;

import com.onemillionworlds.tamarin.actions.actionprofile.ActionManifest;

import java.util.List;

/**
 * Deprecated. Use XrActionAppState instead.
 */
@Deprecated(since="2.6", forRemoval = true)
public class OpenXrActionState extends XrActionAppState{

    public static final String ID = XrActionBaseAppState.ID;

    /**
     * Deprecated. Use XrActionAppState instead.
     */
    @Deprecated(since="2.6", forRemoval = true)
    public OpenXrActionState(ActionManifest manifest, String startingActionSet){
        super(manifest, startingActionSet);
    }

    /**
     * Deprecated. Use XrActionAppState instead.
     */
    @Deprecated(since="2.6", forRemoval = true)
    public OpenXrActionState(ActionManifest manifest, List<String> startingActionSets){
        super(manifest, startingActionSets);
    }
}
