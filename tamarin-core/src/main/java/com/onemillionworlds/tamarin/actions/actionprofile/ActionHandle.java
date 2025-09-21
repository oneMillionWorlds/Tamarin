package com.onemillionworlds.tamarin.actions.actionprofile;

import java.util.regex.Pattern;

/**
 * This is used to identify the action when you want to programmatically interact with it e.g. getting an actions value. It is anticipated that
 * these may be held in a static final field, or something similar where they can be easily accessed
 * application wide.
 * <p>
 * @param actionSetName
 * @param actionName The action name. This should be things like "teleport", not things like "X Click". The idea is that they are
 *                   abstract concept your application would like to support, and they are bound to specific buttons based on the suggested
 *                   bindings (which may be changed by the user, or guessed at by the binding).
 */
public record ActionHandle(
        String actionSetName,
        String actionName
) {
    public static final Pattern VALID_ACTION_NAMES = Pattern.compile("^[a-z_]+$");

    public ActionHandle(String actionSetName, String actionName){
        this.actionSetName = actionSetName;
        this.actionName = actionName;


        if (!VALID_ACTION_NAMES.matcher(actionSetName).matches()){
            throw new IllegalArgumentException("Action set name must be lower case and only contain letters and underscores but was "+actionSetName);
        }

        if (!VALID_ACTION_NAMES.matcher(actionName).matches()){
            throw new IllegalArgumentException("Action name must be lower case and only contain letters and underscores but was "+actionName);
        }

        if (actionName.length() > 32){
            throw new IllegalArgumentException("Action name must be less than 32 characters but was "+actionName);
        }
        if (actionSetName.length() > 32){
            throw new IllegalArgumentException("Action set name must be less than 32 characters but was "+actionSetName);
        }
    }
}
