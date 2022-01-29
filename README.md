# Tamarin
A VR utilities library for JMonkeyEngine. 

## Typical imports

To successfully use Tamarin you should typically have at least the following dependencies

    implementation "org.jmonkeyengine:jme3-core:$jmonkeyengine_version"
    implementation "org.jmonkeyengine:jme3-lwjgl3:$jmonkeyengine_version"
    implementation "org.jmonkeyengine:jme3-vr:$jmonkeyengine_version"

Optionally you can also have

    implementation "com.simsilica:lemur:1.14.0"

## Logging

This library uses simple java.util.Logger for maximum compatibility. Bind that to your preferred logging library.

## Best practices

Grabbing and menu picking both use geometry picking. With simple scenes performance may be fine picking
against the root node. However with complex scenes this may become slow, creating a node that contains 
all the grabbables (or all the UI interactables) may give better performance

## Coding standard

Standard Java coding conventions (try to match existing style). 

British English; "Centre", "Colour" etc

## Upgrade

This library forward ports JMonkeyEngine 3.6 functionality into JMonkeyEngine 3.5. Expect breaking changes on upgrading
to JMonkeyEngine 3.6 (Although as this tracks the intended functionality in 3.6, so shouldn't be too painful)