package com.onemillionworlds.tamarin.actions.actionprofile;

import com.onemillionworlds.tamarin.actions.controllerprofile.InteractionProfileExtensions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An action manifest includes the actions (abstracted versions of button presses, haptics, hand poses etc.) that the game
 * will use as well as suggested bindings to actual buttons etc for known controllers. It is fine if you don't supply
 * suggested bindings for all controller types (as the runtime will try to guess) but the more you can provide the better
 */
public class ActionManifest{

    private static final Logger LOGGER = Logger.getLogger(ActionManifest.class.getName());

    List<ActionSet> actionSets;

    public ActionManifest(List<ActionSet> actionSets){
        this.actionSets = actionSets;
    }

    /**
     * This is primarily for internal use. Gives a profile first view of the suggested bindings (all of them, regardless
     * of any extensions they require).
     */
    public Collection<SuggestedBindingsProfileView> getSuggestedBindingsGroupedByProfile(){
        return getSuggestedBindingsGroupedByProfile(extension -> true);
    }

    /**
     * This is primarily for internal use. Gives a profile first view of the suggested bindings, leaving out any that
     * need an extension that isn't loaded (see {@link InteractionProfileExtensions}), because suggesting them to the
     * runtime would be an error. Profiles left with no bindings are not included.
     *
     * @param isExtensionLoaded returns true if the given extension is loaded
     */
    public Collection<SuggestedBindingsProfileView> getSuggestedBindingsGroupedByProfile(Predicate<String> isExtensionLoaded){

        Map<String, SuggestedBindingsProfileView> profileSuggestedBindings = new HashMap<>();
        Set<String> loggedMissingExtensions = new HashSet<>();

        for(ActionSet actionSet : actionSets){
            for(Action action : actionSet.getActions()){
                for(SuggestedBinding suggestedBinding : action.getSuggestedBindings()){
                    Optional<String> missingExtension = InteractionProfileExtensions.requiredExtensions(suggestedBinding.profile, suggestedBinding.binding).stream()
                            .filter(extension -> !isExtensionLoaded.test(extension))
                            .findFirst();
                    if (missingExtension.isPresent()){
                        if (loggedMissingExtensions.add(missingExtension.get())){
                            LOGGER.info("Skipping suggested bindings that require the " + missingExtension.get() + " extension (e.g. " + suggestedBinding.binding + " for " + suggestedBinding.profile + "), as it isn't loaded");
                        }
                        continue;
                    }
                    profileSuggestedBindings.computeIfAbsent(suggestedBinding.profile, SuggestedBindingsProfileView::new)
                            .addSuggestion(actionSet.getName(), action.getActionName(), suggestedBinding.binding);
                }
            }
        }
        return profileSuggestedBindings.values();
    }

    public void validate(){
        Collection<ValidationProblem> duplicateActionSets =
                findDuplicates(actionSets.stream().map(ActionSet::getName))
                        .map(name -> "Duplicate action set name: " + name)
                        .map(ValidationProblem::new)
                        .collect(Collectors.toList());


        Collection<ValidationProblem> duplicateActionTranslatedName =
                findDuplicates(actionSets.stream().map(ActionSet::getTranslatedName))
                        .map(name -> "Duplicate action set translated name: " + name)
                        .map(ValidationProblem::new)
                        .collect(Collectors.toList());

        Collection<ValidationProblem> withinSetProblems =
                actionSets.stream().flatMap(as -> as.validate().stream()).collect(Collectors.toList());

        Collection<ValidationProblem> overallProblems = new ArrayList<>(0);
        overallProblems.addAll(duplicateActionSets);
        overallProblems.addAll(duplicateActionTranslatedName);
        overallProblems.addAll(withinSetProblems);

        if (!overallProblems.isEmpty()){
            throw new ValidationException(overallProblems);
        }
    }

    public List<ActionSet> getActionSets(){
        return actionSets;
    }

    @SuppressWarnings("unused")
    public static ActionProfileBuilder builder(){
        return new ActionProfileBuilder();
    }

    public static class ActionProfileBuilder{
        private final List<ActionSet> actionSets = new ArrayList<>();

        @SuppressWarnings("unused")
        public ActionProfileBuilder withActionSet(ActionSet actionSet){
            actionSets.add(actionSet);
            return this;
        }
        @SuppressWarnings("unused")
        public ActionProfileBuilder withActionSet(ActionSet.ActionSetBuilder actionSet){
            return withActionSet(actionSet.build());
        }

        @SuppressWarnings("unused")
        public ActionManifest build(){
            ActionManifest manifest = new ActionManifest(actionSets);
            manifest.validate();
            return manifest;
        }
    }

    public static class ValidationException extends RuntimeException{
        Collection<ValidationProblem> problems;
        public ValidationException(Collection<ValidationProblem> problems){
            super(problems.stream().map(ValidationProblem::issue).collect(Collectors.joining(", ")));
            this.problems = problems;
        }

        public Collection<ValidationProblem> getProblems(){
            return problems;
        }
    }

    public static final class ValidationProblem{
        private final String issue;

        public ValidationProblem(String issue){
            this.issue = issue;
        }

        public String issue(){
            return issue;
        }

        @Override
        public boolean equals(Object o){
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ValidationProblem that = (ValidationProblem) o;
            return issue != null ? issue.equals(that.issue) : that.issue == null;
        }

        @Override
        public int hashCode(){
            return issue != null ? issue.hashCode() : 0;
        }

        @Override
        public String toString(){
            return "ValidationProblem{" +
                    "issue='" + issue + '\'' +
                    '}';
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public static Stream<String> findDuplicates(Stream<String> stream) {
        return stream
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey);

    }
}
