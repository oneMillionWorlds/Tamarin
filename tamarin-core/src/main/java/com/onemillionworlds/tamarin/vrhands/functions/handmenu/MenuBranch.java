package com.onemillionworlds.tamarin.vrhands.functions.handmenu;

import com.jme3.scene.Spatial;

import java.util.List;
import java.util.function.Supplier;

/**
 * A Menu branch has subitems (which may themselves be either leaves or branches)
 *
 * @param <T> the type of objects that this menu contains
 */
public class MenuBranch<T> extends MenuItem<T>{
    List<MenuItem<T>> subItems;

    public MenuBranch(Spatial optionGeometry, List<MenuItem<T>> subItems){
        super(optionGeometry);
        this.subItems = subItems;
    }

    public MenuBranch(Supplier<Spatial> optionGeometry, List<MenuItem<T>> subItems){
        super(optionGeometry);
        this.subItems = subItems;
    }

    public List<MenuItem<T>> getSubItems(){
        return this.subItems;
    }
}
