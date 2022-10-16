package com.onemillionworlds.tamarin.vrhands.functions.handmenu;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.scene.Spatial;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An item that goes in a ring around the hand
 * @param <T> the type of objects that this menu contains
 */
@AllArgsConstructor
public class MenuItem<T>{
    /**
     * A function to create the geometry for the menu item. It should be zero centred (the zero point is what is put in a
     * ring round the hand)
     */
    @Getter
    Spatial optionGeometry;
}
