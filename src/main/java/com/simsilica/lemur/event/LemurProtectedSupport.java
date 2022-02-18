package com.simsilica.lemur.event;

import com.jme3.input.event.MouseButtonEvent;

import java.util.List;

/**
 * This is just to provide a way in to get protected methods
 */
public class LemurProtectedSupport{

    public static PickEventSession getSession(BasePickState pickState){
        return pickState.getSession();
    }

    public static List<PickEventSession.RootEntry> getPickRoots(PickEventSession session){
        return session.getRootList();
    }


}
