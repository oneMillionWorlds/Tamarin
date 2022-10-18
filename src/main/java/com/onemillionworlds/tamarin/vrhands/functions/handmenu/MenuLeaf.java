package com.onemillionworlds.tamarin.vrhands.functions.handmenu;

import com.jme3.scene.Spatial;
import lombok.Getter;

import java.util.function.Supplier;

/**
 * A terminal leaf-node that actually contains items
 * @param <T> the type of objects that this menu contains
 */
public class MenuLeaf<T> extends MenuItem<T>{
    @Getter
    private final T leafItem;

    public MenuLeaf(Spatial optionGeometry, T leafItem){
        super(optionGeometry);
        this.leafItem = leafItem;
    }

    public MenuLeaf(Supplier<Spatial> optionGeometry, T leafItem){
        super(optionGeometry);
        this.leafItem = leafItem;
    }

    @Override
    public String toString(){
        return "MenuLeaf{" + leafItem +'}';
    }
}
