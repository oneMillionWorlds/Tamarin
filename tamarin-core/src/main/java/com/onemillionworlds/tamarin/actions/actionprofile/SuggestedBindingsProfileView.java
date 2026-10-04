package com.onemillionworlds.tamarin.actions.actionprofile;

import com.onemillionworlds.tamarin.actions.controllerprofile.InteractionProfileExtensions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * This is a view of the suggested bindings where they are all together for a particular profile (device).
 * This is not intended to be created directly (as its defined action with bindings per device) but to provide a view
 * on bindings from a profile (device) first approach.
 * <p>
 * Primarily for internal use.
 */
public class SuggestedBindingsProfileView{

    private static final Logger LOGGER = Logger.getLogger(SuggestedBindingsProfileView.class.getName());

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

    /**
     * Gets the suggested bindings that can actually be suggested to the runtime. Bindings are left out if they need
     * an extension that isn't loaded (either because the whole profile needs it, or just that component does, see
     * {@link InteractionProfileExtensions}). Suggesting such a binding is an OpenXR error that would fail all the
     * bindings for the profile. Anything left out is logged.
     *
     * @param isExtensionLoaded tests if an extension (by name) is loaded
     * @return the action to binding pairs that can be suggested. May be empty
     */
    public List<Map.Entry<ActionData,String>> getBindingsWithLoadedExtensions(Predicate<String> isExtensionLoaded){
        Optional<String> profileExtension = InteractionProfileExtensions.requiredExtension(profileName);
        if (profileExtension.isPresent() && !isExtensionLoaded.test(profileExtension.get())){
            LOGGER.info("Skipping suggested bindings for " + profileName + " as it requires the " + profileExtension.get() + " extension, which isn't loaded");
            return List.of();
        }

        List<Map.Entry<ActionData,String>> bindings = new ArrayList<>();
        for(Map.Entry<ActionData,String> actionAndBinding : actionToBindingMap.entrySet()){
            Optional<String> missingExtension = InteractionProfileExtensions.requiredExtensionsForBinding(profileName, actionAndBinding.getValue()).stream()
                    .filter(extension -> !isExtensionLoaded.test(extension))
                    .findFirst();
            if (missingExtension.isPresent()){
                LOGGER.info("Skipping suggested binding " + actionAndBinding.getValue() + " for " + profileName + " as it requires the " + missingExtension.get() + " extension, which isn't loaded");
            }else{
                bindings.add(actionAndBinding);
            }
        }
        return bindings;
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
