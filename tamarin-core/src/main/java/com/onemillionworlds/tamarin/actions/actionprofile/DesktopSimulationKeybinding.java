package com.onemillionworlds.tamarin.actions.actionprofile;

import com.jme3.input.controls.KeyTrigger;

/**
 * Represents a keybinding for desktop simulation.
 * @param desktopDebugKeyTrigger The key trigger for the desktop debug key.
 * @param toggle Whether the keybinding is a toggle. If true, the action will be toggled on and off when the key is pressed.
 */
public record DesktopSimulationKeybinding(
    KeyTrigger desktopDebugKeyTrigger,
    boolean toggle
    ){
}