package com.onemillionworlds.tamarin.vrhands.functions.handmenu;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.scene.Spatial;
import lombok.Getter;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A Menu branch has subitems (which may themselves be either leaves or branches)
 * @param <T> the type of objects that this menu contains
 */
public class MenuBranch<T> extends MenuItem<T>{
    @Getter
    List<MenuItem<T>> subItems;

    public MenuBranch(Spatial optionGeometry, List<MenuItem<T>> subItems){
        super(optionGeometry);
        this.subItems = subItems;
    }
}
