package com.onemillionworlds.tamarin.lemursupport;

import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.event.MouseEventControl;

import static com.onemillionworlds.tamarin.vrhands.BoundHand.NO_PICK;

public class LemurSupport{

    /**
     * Given a set of collision results, simulates a click on them
     * @param results the result of a pick that returns things that could be a lemur ui
     */
    public static void clickThroughCollisionResults(CollisionResults results){
        for( int i=0;i<results.size();i++ ){
            CollisionResult collision = results.getCollision(i);
            boolean skip = Boolean.TRUE.equals(collision.getGeometry().getUserData(NO_PICK));

            if (!skip){
                Spatial processedSpatial = collision.getGeometry();

                while(processedSpatial!=null){
                    if (processedSpatial instanceof Button){
                        ((Button)processedSpatial).click();
                        return;
                    }
                    MouseEventControl mec = processedSpatial.getControl(MouseEventControl.class);
                    if ( mec!=null ){
                        mec.mouseButtonEvent(new MouseButtonEvent(0, true, 0, 0), processedSpatial, processedSpatial);
                        return;
                    }

                    processedSpatial = processedSpatial.getParent();
                }
                return;
            }

        }
    }

}
