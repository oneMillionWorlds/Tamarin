package com.onemillionworlds.tamarin.lemursupport;

/**
 * Checks whether Lemur is on the classpath. Lemur is optional, so this class must never reference Lemur types; it is
 * what other classes use to decide whether it is safe to load the Lemur-dependent parts of Tamarin.
 */
public class LemurAvailability{

    private static boolean lemurCheckedAvailable = false;
    private static boolean lemurIsAvailable = false;

    public static boolean isLemurAvailable(){
        if(!lemurCheckedAvailable){
            try{
                Class.forName("com.simsilica.lemur.Button");
                lemurIsAvailable = true;
            } catch(Throwable ex){
                lemurIsAvailable = false;
            }
            lemurCheckedAvailable = true;
        }
        return lemurIsAvailable;
    }

    /**
     * Throws an exception if Lemur isn't available, for use by methods that can't work without it.
     */
    public static void assertLemurAvailable(){
        if(!isLemurAvailable()){
            throw new RuntimeException("Lemur not available on class path. Lemur required for methods named _lemurSupport or classes with Lemur in name");
        }
    }
}
