package com.onemillionworlds.tamarin.actions.actionprofile;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ActionSet{
    /**
     * This is the action sets programmatic name.
     */
    private final String name;

    /**
     * This is presented to the user as a description of the action set. This string should be presented in the system’s current active locale.
     */
    private final String translatedName;

    /**
     * Defines which action sets' actions are active on a given input source when actions on multiple active action sets are bound to the same input source.
     * Larger priority numbers take precedence over smaller priority numbers.
     */
    private final int priority;

    private final List<Action> actions;

    public ActionSet(String name, String translatedName, int priority, List<Action> actions){
        this.name = name;
        this.translatedName = translatedName;
        this.priority = priority;
        this.actions = actions;
    }

    public static ActionSetBuilder builder(){
        return new ActionSetBuilder();
    }

    @SuppressWarnings("unused")
    public static class ActionSetBuilder{
        private String name;
        private String translatedName;
        private int priority = 0;
        private final List<Action> actions = new ArrayList<>();

        /**
         * This is the action sets programmatic name.
         */
        public ActionSetBuilder name(String name){
            this.name = name;
            return this;
        }

        /**
         * This is presented to the user as a description of the action set. This string should be presented in the system’s current active locale.
         */
        public ActionSetBuilder translatedName(String translatedName){
            this.translatedName = translatedName;
            return this;
        }

        /**
         * Defines which action sets' actions are active on a given input source when actions on multiple active action sets are bound to the same input source.
         * Larger priority numbers take precedence over smaller priority numbers.
         */
        public ActionSetBuilder priority(int priority){
            this.priority = priority;
            return this;
        }

        /**
         * Adds an action to the action set. Can be called multiple times.
         */
        public ActionSetBuilder withAction(Action action){
            if(!action.getActionSetName().equals(name)){
                throw new IllegalArgumentException("ActionSet name must match the action's actionSetName, expected " + name + " and got " + action.getActionSetName() + " instead");
            }
            actions.add(action);
            return this;
        }

        public ActionSet build(){
            if(name == null){
                throw new IllegalArgumentException("name cannot be null");
            }
            if(translatedName == null){
                throw new IllegalArgumentException("translatedName cannot be null");
            }
            return new ActionSet(name, translatedName, priority, actions);
        }
    }
}
