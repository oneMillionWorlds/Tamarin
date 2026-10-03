# Tamarin

A VR library for JMonkeyEngine (JME) built on OpenXR. It is published to Maven Central as `com.onemillionworlds:tamarin`
(desktop), `tamarin-android` and `tamarin-core`. User docs are on the GitHub wiki, and example usage is in the separate
TamarinTestBed repo (https://github.com/oneMillionWorlds/TamarinTestBed). This repo has no runnable app.

## Build and test

```
./gradlew :tamarin-core:test          # fast; nearly all unit tests live here
./gradlew build                       # all modules, what CI runs (JDK 17 on ubuntu)
./gradlew publishToMavenLocal         # to try changes in TamarinTestBed (add mavenLocal() there)
```

- The Android module needs an Android SDK (`local.properties` -> `sdk.dir`, not committed).
- Signing only happens when `signing.keyId` is configured, so local publishing works without GPG.
- The version is in `gradle.properties`. The release GitHub action bumps it, so don't change it unless asked.
  Release and publishing steps are in `README.md`.
- Dependency versions are in `gradle/libs.versions.toml`. `lwjgl3` must match the LWJGL version JME ships with.

## Modules

| Module | Package root | Purpose |
|---|---|---|
| `tamarin-core` | `com.onemillionworlds.tamarin` | Platform-independent API and most features: hands, grabbing, actions model, Lemur/Minie support, desktop simulation |
| `tamarin-desktop` | `com.onemillionworlds.tamarin` (same packages) | OpenXR implementation using **LWJGL** (`org.lwjgl.openxr.*`) and an OpenGL binding |
| `tamarin-android` | `tamarin.android` | OpenXR implementation using the project's own JNI bindings (`com.onemillionworlds.tamarin.openxrbindings`, artifact `openxr-bindings-native`) with EGL |

### The parallel class structure

The key thing to understand. Each XR concept has an abstract base in core, then **three** implementations:

| Abstract (core) | Desktop VR | Android VR | No-VR desktop simulation (core) |
|---|---|---|---|
| `openxr.XrBaseAppState` -> `openxr.XrVrAppState` (shared VR logic) | `openxr.XrAppState` | `XrAndroidAppState` | `openxr.DesktopSimulatingXrAppState` |
| `actions.XrActionBaseAppState` | `actions.XrActionAppState` | `XrActionAndroidAppState` | `actions.DesktopSimulatingXrActionAppState` |
| `openxr.PassthroughControl` etc. (interfaces) | `openxr.OpenXrSessionManager` | `OpenXrAndroidSessionManager` | - |

- The desktop and Android session managers and action states are largely **hand-maintained copies** that differ mainly
  in the binding API (LWJGL structs/`XR10` vs the `openxrbindings` equivalents). A change to OpenXR logic in one almost
  always has to be mirrored in the other. See the `cross-platform-xr-feature` skill.
- Application code should only depend on the `*Base*` classes, retrieved by `XrBaseAppState.ID` /
  `XrActionBaseAppState.ID`. This is what lets one app run in VR or in desktop simulation. Base class Javadoc says
  "calls that don't make sense in one mode will be ignored (not exception!)", so the simulation states should no-op or
  return a sensible default. They should not throw.
- Anything that can be done without OpenXR types goes in core (`XrVrAppState` or a plain helper class) so it is written
  once and can be unit tested.

### Core packages (tamarin-core)

- `openxr`: app states, `XrSettings` (requested extensions, passthrough, blend mode), `XrSessionObservables` (session
  state, focus, user presence, gameplay interrupts).
- `actions`: action manifest model (`actionprofile.ActionManifest/ActionSet/Action/ActionHandle`), state types
  (`state.*`), controller profile constants (`controllerprofile.*`), `InteractionProfileTracker`, `SyntheticDPad`.
- `vrhands`: `VRHandsAppState`, `BoundHand` (the main per-hand API), pluggable `functions.BoundHandFunction`s (grab,
  press, Lemur click, ring menu, climbing), `grabbing.*` controls with move restrictions and snap-to points,
  `touching.*` (mechanical buttons and toggles), skeleton synthesis for runtimes without hand skeletons.
- `observable`: the change-notification pattern used for app-facing state (see below).
- `lemursupport`, `miniesupport`: optional integrations. `com.simsilica.lemur.event.LemurProtectedSupport` deliberately
  sits in Lemur's package to reach package-private API.
- `viewports`, `vinette`, `audio`, `debug`, `debugwindow`, `deferredattachment`, `math`.

## Conventions

- **Java 11 source/target.** No records, switch expressions, pattern-matching `instanceof`, or text blocks. Earlier
  commits deliberately removed these for Android compatibility.
- **British English** in identifiers, Javadoc and log messages (`initialise`, `colour`, `centre`, `normaliser`). Don't
  "fix" existing British spellings. Existing misspellings that are part of the public API (e.g. the `vinette` package)
  should stay too.
- Brace style has no space before `{`: `public void foo(){`, `if (x){`, `class Foo{`. Match the surrounding file.
- Logging is `java.util.logging` only (`private static final Logger LOGGER = Logger.getLogger(X.class.getName());`).
  Android code may also use `android.util.Log`. For errors that would repeat every frame, use `SingleOccurrenceLog`.
- **JME, Lemur and Minie are `compileOnly`.** Users bring their own versions. Lemur and Minie are optional at runtime,
  so classes that touch them must only be loaded on paths the user opted into (see `BoundHand#assertLemurAvailable`).
  Don't add Lemur or Minie imports to classes that every app loads.
- This is a public library. Avoid breaking public API. Prefer adding overloads or deprecating. Javadoc on public methods
  is expected and is the primary documentation. Many public methods carry explanatory Javadoc, so keep it accurate when
  behaviour changes.
- App-facing state uses `observable.*`. Expose `subscribeToX()` returning an `ObservableValueSubscription` /
  `ObservableEventSubscription` / `ObservableDataEventSubscription`. Callers poll it each frame
  (`checkHasChanged()`, `pollEvents()`). Don't add listener or callback interfaces for this kind of state. Logic like
  this belongs in a small core class with no OpenXR dependency that has its own unit test (e.g.
  `InteractionProfileTracker`, `XrSessionObservables`).
- Optional OpenXR extensions are requested in `XrSettings` (the constructor's default list, or via
  `addRequiredXrExtension`). Code must check `xrAppState.checkExtensionLoaded(...)` before relying on one, because
  runtimes may silently lack it.
- Tests: JUnit 5 plus Mockito, in `tamarin-core/src/test`, in the same package as the code under test. Test helpers are
  in `testhelpers` (`Vector3fAsserts`, `Line3fAsserts`). Test method names describe the behaviour
  (`controllerIsOnlyLostAfterTheGracePeriod`, `validate_duplicateActionSetNames`).

## Verifying changes

Unit tests cannot exercise OpenXR. For desktop/Android XR code, the minimum bar is that `./gradlew build` compiles.
Behaviour needs testing on a real headset via TamarinTestBed, which only the user can do. Say clearly when a change has
only been compile-checked.
