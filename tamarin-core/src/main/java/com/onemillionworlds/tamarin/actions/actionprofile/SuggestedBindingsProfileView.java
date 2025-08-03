package com.onemillionworlds.tamarin.actions.actionprofile;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a view of the suggested bindings where they are all together for a particular profile (device).
 * This is not intended to be created directly (as its defined action with bindings per device) but to provide a view
 * on bindings from a profile (device) first approach.
 * <p>
 * Primarily for internal use.
 */
public class SuggestedBindingsProfileView{

    String profileName;

    Map<ActionData,String> actionToBindingMap = new HashMap<>();

    SuggestedBindingsProfileView(String profileName){
        this.profileName = profileName;
    }

    void addSuggestion(String actionSet, String action, String binding){
        actionToBindingMap.put(new ActionData(actionSet,action), binding);
    }

    public String getProfileName(){
        return profileName;
    }

    public Map<ActionData,String> getSetToActionToBindingMap(){
        return actionToBindingMap;
    }

    public static class ActionData{
        private final String actionSet;
        private final String actionName;

        public ActionData(String actionSet, String actionName){
            this.actionSet = actionSet;
            this.actionName = actionName;
        }

        public String getActionSet(){
            return actionSet;
        }

        public String getActionName(){
            return actionName;
        }
    }

}
