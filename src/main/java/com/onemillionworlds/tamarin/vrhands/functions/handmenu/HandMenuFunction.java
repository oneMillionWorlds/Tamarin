package com.onemillionworlds.tamarin.vrhands.functions.handmenu;

import com.jme3.app.SimpleApplication;
import com.jme3.app.VRAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bounding.BoundingSphere;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.TamarinUtilities;
import com.onemillionworlds.tamarin.compatibility.ActionBasedOpenVrState;
import com.onemillionworlds.tamarin.compatibility.DigitalActionState;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.functions.BoundHandFunction;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A hand menu function has a menu of geometries that are opened from an action and then the hand is
 * moved through them to select them.
 *
 * The menu can be a tree, where passing the hand through 1 item opens up a new set of branches
 *
 * The menu remains open for as long as the digital action remains true. When the action is released where the hand is
 * controls what is selected
 */
public class HandMenuFunction<T> implements BoundHandFunction{

    private static final String MENU_BRANCH_PATH = "MENU_BRANCH_PATH";

    private final List<MenuItem<T>> menuItems;

    private final Consumer<T> selectionConsumer;

    String digitalActionToOpenMenu;

    private BoundHand boundHand;
    private ActionBasedOpenVrState actionBasedOpenVrState;
    private VRAppState vrAppState;
    Node menuNode = new Node("MenuNode");
    private boolean menuOpen = false;

    /**
     * This records a set of menu item paths (The great grandparent -> grandparent -> parent etc) and the ring
     * centre node that should be shown if that path is active
     */
    private final Map<ArrayList<MenuBranch<T>>, Node> subRingCentreNodes = new HashMap<>();


    /**
     * For the first ring of items determines where they should be placed. Default behaviour is to use the top
     * 3 quarters, starting on the left, evenly spaced.
     *
     * Zero radians is straight up, negative is left
     */
    @Setter
    private InnerRingPositioner innerRingItemPositioner = (index, items) -> -0.75f * FastMath.PI + (float)(index * 1.5f*Math.PI/(items-1));

    /**
     * For the subcategory rings (which can have children of children of children etc) this function determines the angle
     * at which the child members should be.
     *
     * Zero radians is straight up, negative is left
     */
    @Setter
    private ChildRingPositioner childRingPositioner = this::defaultChildRingPositioner;

    /**
     * Where the centre of the first ring is
     */
    @Setter
    @Getter
    private float firstRingRadius = 0.15f;

    /**
     * Distance between ring 1, ring 2, ring 3 etc
     */
    @Setter
    @Getter
    private float interRingDistance = 0.15f;



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
        DigitalActionState menuButtonPressed = actionBasedOpenVrState.getDigitalActionState(digitalActionToOpenMenu, boundHand.getHandSide().restrictToInputString);
        if (menuButtonPressed.state && !this.menuOpen){
            openMenu();
        }

        if (!menuButtonPressed.state && this.menuOpen){
            closeMenuAndSelect();
        }

        if (this.menuOpen){
            //scan the palm area and finger tip area for menu items
            pickForMenuInteraction().ifPresent(this::updateRingVisibilityForSelectedPath);
        }

    }

    public void closeMenuAndSelect(){
        menuNode.setCullHint(Spatial.CullHint.Always);
        menuOpen = false;
    }

    public void openMenu(){
        menuNode.setCullHint(Spatial.CullHint.Inherit);
        menuNode.setLocalTranslation(boundHand.getPalmNode().getWorldTranslation());
        menuNode.lookAt(TamarinUtilities.getVrCameraPosition(this.vrAppState), Vector3f.UNIT_Y);
        subRingCentreNodes.values().forEach(n -> n.setCullHint(Spatial.CullHint.Always));

        menuOpen = true;
    }

    /**
     * Looks through spatials near the hands and if it finds one that implies a menu path change returns it
     * @return A menu path that the spatial corresponds to
     */
    private Optional<ArrayList<MenuBranch<T>>> pickForMenuInteraction(){
        CollisionResults results = new CollisionResults();

        Vector3f palmCentre = boundHand.getPalmNode().getWorldTranslation();
        BoundingSphere sphere = new BoundingSphere(0.02f, palmCentre);
        menuNode.collideWith(sphere, results);

        for(int index=0;index<results.size();index++){
            CollisionResult collision = results.getCollision(index);
            Spatial spatial = collision.getGeometry();
            while(spatial!=null && spatial.getUserData(MENU_BRANCH_PATH)==null){
                spatial = spatial.getParent();
            }
            if (spatial!=null){
                return Optional.of(spatial.getUserData(MENU_BRANCH_PATH));
            }
        }
        return Optional.empty();
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

            if (menuItem instanceof MenuBranch){
                MenuBranch<T> menuBranch = (MenuBranch<T>)menuItem;
                ArrayList<MenuBranch<T>> ringPathControlled = new ArrayList<>(List.of(menuBranch));
                configureMenuBranchForTouch(menuItemNode, ringPathControlled);
                buildSubItem(1, angle, menuBranch.getSubItems(), ringPathControlled);
            }else{
                MenuLeaf<T> menuLeaf = (MenuLeaf<T>)menuItem;
                //handle grab
            }
        }
    }

    private void buildSubItem(int ringIndex, float angleOfParent, List<MenuItem<T>> menuItems, ArrayList<MenuBranch<T>> parents){
        Node ringCentreNode = new Node("Ring " + ringIndex + " at angle " + angleOfParent);
        ringCentreNode.setCullHint(Spatial.CullHint.Always);
        menuNode.attachChild(ringCentreNode);
        subRingCentreNodes.put(parents, ringCentreNode);
        for(int menuItemIndex = 0; menuItemIndex<menuItems.size();menuItemIndex++){
            float angle = childRingPositioner.determineAngleForItem(menuItemIndex, menuItems.size(), angleOfParent, ringIndex);

            Vector3f position = new Vector3f((firstRingRadius + ringIndex*interRingDistance)* FastMath.sin(angle), (firstRingRadius + ringIndex*interRingDistance)*FastMath.cos(angle), 0);

            Node menuItemNode = new Node();
            menuItemNode.setLocalTranslation(position);
            MenuItem<T> menuItem = menuItems.get(menuItemIndex);
            menuItemNode.attachChild(menuItem.getOptionGeometry());

            ringCentreNode.attachChild(menuItemNode);

            if (menuItem instanceof MenuBranch){
                MenuBranch<T> menuBranch = (MenuBranch<T>)menuItem;
                ArrayList<MenuBranch<T>> childParents = new ArrayList<>(parents);
                childParents.add(menuBranch);
                buildSubItem(ringIndex+1, angle, menuBranch.getSubItems(), childParents);
            }else{
                MenuLeaf<T> menuLeaf = (MenuLeaf<T>)menuItem;
                //handle grab
            }
        }
    }

    private void updateRingVisibilityForSelectedPath(List<MenuBranch<T>> path){
        for(Map.Entry<ArrayList<MenuBranch<T>>, Node> ring : subRingCentreNodes.entrySet()){
            if(path.containsAll(ring.getKey())){
                ring.getValue().setCullHint(Spatial.CullHint.Inherit);
            }else{
                ring.getValue().setCullHint(Spatial.CullHint.Always);
            }
        }
    }

    private void configureMenuBranchForTouch(Spatial menuBranchGeometry, List<MenuBranch<T>> pathToOpen){
        menuBranchGeometry.setUserData(MENU_BRANCH_PATH, pathToOpen);
    }

    private float defaultChildRingPositioner(int itemIndex, int totalNumberOfSubItems, float angleOfParent, int ringDepth){
        float ringRadius = firstRingRadius + (ringDepth)*interRingDistance;
        float circumferencePerItem = 0.15f;
        float anglePerItem = circumferencePerItem/ringRadius;

        return angleOfParent + (itemIndex- (totalNumberOfSubItems-1)/2f) *anglePerItem;

    }

    @FunctionalInterface
    public interface InnerRingPositioner{

        /**
         * Given the index of the item (and how many items there are) return what angle the
         * item should be at about the hand
         */
        float determineAngleForItem(int itemIndex, int totalNumberOfItems);
    }

    @FunctionalInterface
    public interface ChildRingPositioner{

        /**
         * Given the index of the item (and how many items there are) return what angle the
         * item should be at about the hand
         * @param ringDepth what ring this is, the first child ring is 1, child of child is 2 etc
         */
        float determineAngleForItem(int itemIndex, int totalNumberOfSubItems, float angleOfParent, int ringDepth);
    }
}
