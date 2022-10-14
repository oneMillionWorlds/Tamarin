package com.onemillionworlds.tamarin.lemursupport;

import com.jme3.app.Application;
import com.jme3.app.VRAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.KeyboardEvent;
import com.onemillionworlds.tamarin.lemursupport.keyboardstyles.buttons.KeyboardButton;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.ListBox;
import com.simsilica.lemur.Selector;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.event.MouseListener;
import com.simsilica.lemur.list.SelectionModel;

import java.util.List;

/**
 * This is a free floating pop up bit of a selector (aka dropdown) as that doesm't seem to work on its own in 3D
 * so this is added instead
 */
public class SelectorPopUp <T> extends BaseAppState{

    public float scale;

    private final Node rootNodeDelegate = new Node("SelectorPopUpRootNodeDelegate");
    private final Node popupNode = new Node("SelectorPopUp");

    private final Vector3f ownerPosition;
    private final Quaternion ownerRotation;

    ListBox<T> listBox;
    private VersionedReference<List<T>> options;
    VersionedReference<Integer> listBoxSelection;

    Selector<T> owner;

    public SelectorPopUp(Node nodeToAttachTo, Selector<T> selector){
        nodeToAttachTo.attachChild(rootNodeDelegate);
        owner = selector;
        //the world transform includes scale, but we are dealing with that ourselves later so that's fine
        rootNodeDelegate.setLocalTransform(nodeToAttachTo.getWorldTransform().invert());

        this.ownerPosition= selector.getWorldTranslation();
        this.ownerRotation = selector.getWorldRotation();
        this.scale = selector.getWorldScale().x;
        options = selector.getModel().createReference();
    }

    @Override
    protected void initialize(Application app){
        rootNodeDelegate.attachChild(popupNode);
        popupNode.setLocalTranslation(ownerPosition.add(0,0,0.02f));
        popupNode.setLocalRotation(this.ownerRotation);

        refreshList();
    }

    private void refreshList(){
        popupNode.detachAllChildren();

        listBox = new ListBox<>(owner.getModel(), owner.getValueRenderer(), owner.getElementId().child("list"), owner.getStyle());
        listBoxSelection = listBox.getSelectionModel().createSelectionReference();
        int listDisplayedItems = owner.getMaximumVisibleItems() == 0 ? owner.getModel().size() : Math.min(owner.getModel().size() , owner.getMaximumVisibleItems());
        listBox.setVisibleItems(listDisplayedItems);
        Container container = new Container();
        container.addChild(listBox);
        container.setLocalScale(scale);
        popupNode.attachChild(container);
    }


    @Override
    public void stateDetached(AppStateManager stateManager){
        super.stateDetached(stateManager);
        rootNodeDelegate.removeFromParent();
    }

    @Override
    protected void cleanup(Application app){

    }

    @Override
    public void update(float tpf){
        super.update(tpf);
        if (options.update()){
            refreshList();
        }
        if (listBoxSelection.update()){
            owner.setSelectedItem(listBox.getSelectedItem());
            getStateManager().detach(this);
        }
    }

    @Override protected void onEnable(){}

    @Override protected void onDisable(){}
}
