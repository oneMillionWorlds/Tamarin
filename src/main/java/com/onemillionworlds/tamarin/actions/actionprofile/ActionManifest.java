package com.onemillionworlds.tamarin.actions.actionprofile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * An action manifest includes the actions (abstracted versions of button presses, haptics, hand poses etc.) that the game
 * will use as well as suggested bindings to actual buttons etc for known controllers. It is fine if you don't supply
 * suggested bindings for all controller types (as the runtime will try to guess) but the more you can provide the better
 */
public class ActionManifest{

    List<ActionSet> actionSets;

    public ActionManifest(List<ActionSet> actionSets){
        this.actionSets = actionSets;
    }

    public List<ActionSet> getActionSets(){
        return actionSets;
    }

    /**
     * This is primarily for internal use. Gives a profile first view of the suggested bindings
     */
    public Collection<SuggestedBindingsProfileView> getSuggestedBindingsGroupedByProfile(){

        Map<String, SuggestedBindingsProfileView> profileSuggestedBindings = new HashMap<>();

        for(ActionSet actionSet : actionSets){
            for(Action action : actionSet.getActions()){
                for(SuggestedBinding suggestedBinding : action.getSuggestedBindings()){
                    profileSuggestedBindings.computeIfAbsent(suggestedBinding.profile, SuggestedBindingsProfileView::new)
                            .addSuggestion(actionSet.getName(), action.getActionName(), suggestedBinding.binding);
                }
            }
        }
        return profileSuggestedBindings.values();
    }

    public static ActionProfileBuilder builder(){
        return new ActionProfileBuilder();
    }

    public static class ActionProfileBuilder{
        private final List<ActionSet> actionSets = new ArrayList<>();

        public ActionProfileBuilder withActionSet(ActionSet actionSet){
            actionSets.add(actionSet);
            return this;
        }

        public ActionManifest build(){
            return new ActionManifest(actionSets);
        }
    }
}
