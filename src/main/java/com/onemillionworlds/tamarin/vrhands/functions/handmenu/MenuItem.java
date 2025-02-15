package com.onemillionworlds.tamarin.vrhands.functions.handmenu;

import com.jme3.scene.Spatial;

import java.util.function.Supplier;

/**
 * An item that goes in a ring around the hand
 *
 * @param <T> the type of objects that this menu contains
 */
public class MenuItem<T>{

    Spatial optionGeometry;

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

    /**
     * The geometry for the menu item. It should be zero centred (the zero point is what is put in a
     * ring round the hand)
     */
    public Spatial getOptionGeometry(){
        return this.optionGeometry;
    }

    /**
     * A function to create the geometry for the menu item. It should be zero centred (the zero point is what is put in a
     * ring round the hand).
     * <p>
     *     Each time the menu is opened this geometry is refreshed (so should be used sparingly
     * </p>
     */
    public Supplier<Spatial> getDynamicOptionGeometry(){
        return this.dynamicOptionGeometry;
    }
}
