package com.onemillionworlds.tamarin.vrhands.functions.handmenu;

import com.jme3.app.SimpleApplication;
import com.jme3.app.VRAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.TamarinUtilities;
import com.onemillionworlds.tamarin.compatibility.ActionBasedOpenVrState;
import com.onemillionworlds.tamarin.compatibility.DigitalActionState;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.functions.BoundHandFunction;
import lombok.Setter;

import java.util.List;
import java.util.function.Consumer;

/**
 * A hand menu function has a menu of geometries that are opened from an action and then the hand is
 * moved through them to select them.
 *
 * The menu can be a tree, where passing the hand through 1 item opens up a new set of branches
 *
 * Optionally a grab action may be required to select the items (either just the final leaf items or all items
 */
public class HandMenuFunction<T> implements BoundHandFunction{

    List<MenuItem<T>> menuItems;

    Consumer<T> selectionConsumer;

    String digitalActionToOpenMenu;

    private BoundHand boundHand;
    private ActionBasedOpenVrState actionBasedOpenVrState;
    private VRAppState vrAppState;
    Node menuNode = new Node("MenuNode");
    private boolean menuOpen = false;

    /**
     * For the first ring of items determines where they should be placed. Default behaviour is to use the top
     * semicircle, starting on the left, evenly spaced
     */
    @Setter
    InnerRingPositioner innerRingItemPositioner = (index, items) -> -FastMath.HALF_PI + (float)(index * Math.PI/(items-1));

    float firstRingRadius = 0.15f;

    /**
     * @param menuItems the tree of menu items
     * @param selectionConsumer when an item is selected it is given to this consumer
     * @param digitalActionToOpenMenu The digital action (button press) that opens the menu
     */
    public HandMenuFunction(List<MenuItem<T>> menuItems, Consumer<T> selectionConsumer, String digitalActionToOpenMenu){
        this.menuItems = menuItems;
        this.selectionConsumer = selectionConsumer;
        this.digitalActionToOpenMenu = digitalActionToOpenMenu;
    }


    @Override
    public void onBind(BoundHand boundHand, AppStateManager stateManager){
        this.boundHand= boundHand;
        this.actionBasedOpenVrState = stateManager.getState(ActionBasedOpenVrState.ID, ActionBasedOpenVrState.class);
        this.vrAppState = stateManager.getState(VRAppState.class);

        Node rootNode = ((SimpleApplication)stateManager.getApplication()).getRootNode();
        rootNode.attachChild(menuNode);
        menuNode.setCullHint(Spatial.CullHint.Always);
        buildMenu();
    }

    @Override
    public void onUnbind(BoundHand boundHand, AppStateManager stateManager){
        menuNode.removeFromParent();
    }

    @Override
    public void update(float timeSlice, BoundHand boundHand, AppStateManager stateManager){
        DigitalActionState toggleMenuAction = actionBasedOpenVrState.getDigitalActionState(digitalActionToOpenMenu, boundHand.getHandSide().restrictToInputString);
        boolean toggleMenu = toggleMenuAction.changed && toggleMenuAction.state;
        if (toggleMenu){
            if(menuOpen){
                closeMenu();
            } else{
                openMenu();
            }
        }
    }

    public void closeMenu(){
        menuNode.setCullHint(Spatial.CullHint.Always);
        menuOpen = false;
    }

    public void openMenu(){
        menuNode.setCullHint(Spatial.CullHint.Inherit);
        menuNode.setLocalTranslation(boundHand.getPalmNode().getWorldTranslation());
        menuNode.lookAt(TamarinUtilities.getVrCameraPosition(this.vrAppState), Vector3f.UNIT_Y);
        menuOpen = true;
    }

    /**
     * Builds the entire menu, the menu is traversed by hiding and unhiding parts of it
     */
    private void buildMenu(){

        for(int menuItemIndex = 0; menuItemIndex<menuItems.size();menuItemIndex++){
            float angle = innerRingItemPositioner.determineAngleForItem(menuItemIndex,menuItems.size());

            Vector3f position = new Vector3f(firstRingRadius* FastMath.sin(angle), firstRingRadius*FastMath.cos(angle), 0);

            Node menuItemNode = new Node();
            menuItemNode.setLocalTranslation(position);
            MenuItem<T> menuItem = menuItems.get(menuItemIndex);
            menuItemNode.attachChild(menuItem.getOptionGeometry());

            menuNode.attachChild(menuItemNode);
        }
    }

    @FunctionalInterface
    public interface InnerRingPositioner{

        /**
         * Given the index of the item (and how many items there are) return what angle the
         * item should be at about the hand
         */
        float determineAngleForItem(int itemIndex, int totalNumberOfItems);
    }
}
