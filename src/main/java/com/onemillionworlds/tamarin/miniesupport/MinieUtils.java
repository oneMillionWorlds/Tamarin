package com.onemillionworlds.tamarin.miniesupport;

public class MinieUtils{
    private static boolean minieIsAvailableChecked = false;
    private static boolean minieIsAvailable = false;

    public static boolean isMinieAvailable(){
        if(!minieIsAvailableChecked){
            try{
                Class.forName("com.jme3.bullet.BulletAppState");
                minieIsAvailable = true;
            } catch(Throwable ex){
                minieIsAvailable = false;
            }
            minieIsAvailableChecked = true;
        }
        return minieIsAvailable;
    }

    public static void ensureMinieIsAvailable(){
        if(!isMinieAvailable()){
            throw new RuntimeException("Minie phyics is not available on the class path. This functionality requires optional dependency minie");
        }
    }

}
