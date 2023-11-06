package com.onemillionworlds.tamarin.actions;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * @param rawValue the raw value from the binding
 * @param handSide the left, right or N/A hand
 * @param fundamentalButton Things like joystick, A, B, X, Y, etc.
 * @param withinButtonAction Things like "touch", "click", "y" and "x" for the joystick, etc.
 */
public record PhysicalBindingInterpretation(
        String rawValue,
        Optional<HandSide> handSide,
        String fundamentalButton,
        String withinButtonAction
){
    private static final Pattern pattern = Pattern.compile(".*/([^/]+)/([^/]+)$");

    public static PhysicalBindingInterpretation interpretRawValue(String rawValue){
        Matcher matcher = pattern.matcher(rawValue);
        if(!matcher.matches()){
            throw new RuntimeException("Invalid physical binding interpretation: " + rawValue);
        }
        String fundamentalButton = matcher.group(1);
        String withinButtonAction = matcher.group(2);
        HandSide handSide = rawValue.contains("left") ? HandSide.LEFT : rawValue.contains("right") ? HandSide.RIGHT : null;
        return new PhysicalBindingInterpretation(rawValue, Optional.ofNullable(handSide), fundamentalButton, withinButtonAction);
    }
}
