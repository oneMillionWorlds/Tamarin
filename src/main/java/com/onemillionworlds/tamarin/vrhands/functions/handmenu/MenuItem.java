package com.onemillionworlds.tamarin.vrhands.functions.handmenu;

import com.jme3.scene.Spatial;
import lombok.Getter;

import java.util.function.Supplier;

/**
 * An item that goes in a ring around the hand
 * @param <T> the type of objects that this menu contains
 */
public class MenuItem<T>{
    /**
     * The geometry for the menu item. It should be zero centred (the zero point is what is put in a
     * ring round the hand)
     */
    @Getter
    Spatial optionGeometry;

    /**
     * A function to create the geometry for the menu item. It should be zero centred (the zero point is what is put in a
     * ring round the hand).
     *
     * Each time the menu is opened this geometry is refreshed (so should be used sparingly
     */
    @Getter
    Supplier<Spatial> dynamicOptionGeometry;

    public MenuItem(Spatial optionGeometry){
        this.optionGeometry = optionGeometry;
    }

    public MenuItem(Supplier<Spatial> dynamicOptionGeometry){
        this.dynamicOptionGeometry = dynamicOptionGeometry;
    }

    public boolean isDynamicIcon(){
        return dynamicOptionGeometry != null;
    }
}
