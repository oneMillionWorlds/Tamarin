# ANGLE / EGL / OpenXR Binding Research

Findings from investigating how to bind jMonkeyEngine (jME) 3.10's rendering context
to OpenXR after jME's desktop backend change. This documents *why* the obvious paths
fail and what the realistic options are, so we don't have to rediscover it.

- **jME version:** `3.10.0-beta1`
- **LWJGL version:** `3.4.1` (catalog) — must match the version jME ships
- **Context:** Tamarin is a VR library that binds an OpenGL context to an OpenXR session.
- **Date of investigation:** 2026-06

---

## TL;DR

- jME 3.10 **did not remove desktop OpenGL** — it changed the **default renderer** to
  `ANGLE_GLES3`. The desktop-GL renderers (`LWJGL_OPENGL33`/`41`/`42`/`43`/`44`/`45`)
  still exist and are selectable via `AppSettings.setRenderer(...)`.
- Under the new default, jME brings up the LWJGL **`opengles`** module + an EGL/ANGLE
  (Direct3D 11-backed) context. **No desktop OpenXR runtime can interop with that via
  any *OpenGL* graphics binding.**
- **Road A (chosen for now):** require the desktop-GL renderer for desktop VR. The
  existing `XrGraphicsBindingOpenGLWin32KHR` / `…Xlib…` binding then works unchanged.
  Tamarin can only *detect and fail fast* (it can't change the renderer itself).
- **Road B (future, large):** support ANGLE-GLES properly by binding at the **D3D11**
  layer (`XR_KHR_D3D11_enable`) and bridging ANGLE's D3D11 textures back into GL via
  `EGL_ANGLE_d3d_texture_client_buffer`.

---

## Background: what jME changed

The jME commit *"Replace desktop backend with SDL3 GL and ANGLE-GLES"* made two
*independent* changes:

1. **Windowing: GLFW → SDL3.** GLFW natives were removed. This is why Tamarin commit
   `4ae55d8` replaced GLFW handle-extraction with direct WGL/GLX/EGL queries against the
   *current* context — the right move for SDL3, and orthogonal to the renderer problem.

2. **Default renderer: desktop GL → ANGLE-GLES.** This is the change that breaks VR.

jME now offers a **dual rendering backend** (confirmed on jMonkeyEngine.org and in the
`AppSettings` source):

- **Direct desktop OpenGL** (3.2 – 4.5) via the LWJGL `opengl` module.
- **ANGLE GLES 3.0** via the LWJGL `opengles` + `egl` + `angle` modules, translating
  GLES → Vulkan / Metal / **Direct3D 11** depending on platform (D3D11 on Windows).

### The smoking gun in `AppSettings`

From jME `jme3-core/.../system/AppSettings.java` (master):

```java
// renderer constants (desktop GL ladder still present)
LWJGL_OPENGL33 = "LWJGL-OpenGL33";
LWJGL_OPENGL41 = "LWJGL-OpenGL41";
LWJGL_OPENGL42 = "LWJGL-OpenGL42";
LWJGL_OPENGL43 = "LWJGL-OpenGL43";
LWJGL_OPENGL44 = "LWJGL-OpenGL44";
LWJGL_OPENGL45 = "LWJGL-OpenGL45";
ANGLE_GLES3    = "ANGLE_GLES3";

// the change that bit us:
defaults.put("Renderer", ANGLE_GLES3);   // default is now ANGLE, not desktop GL
```

So an app that never calls `setRenderer(...)` silently gets ANGLE-GLES. That is what
"jME has moved to ANGLE, so doing nothing isn't an option" actually means: not that GL
is gone, but that the *default* flipped.

---

## The two failures we hit (and why)

### Attempt 1 — force the cross-platform EGL/MNDX binding

We forced `XrGraphicsBindingEGLMNDX` whenever `eglGetCurrentDisplay()` returned non-null
(`XrUtils.createGraphicsBindingOpenGL`, the `if (true || useEGL)` override).

**Result:** `XR_ERROR_GRAPHICS_DEVICE_INVALID` from `xrCreateSession`.

**Why:**

1. `XR_MNDX_egl_enable` is a **Monado** vendor extension (`MNDX` = Monado experimental).
   SteamVR, Oculus/Meta, Windows Mixed Reality and Varjo do **not** implement it. So the
   extension is never actually enabled on the instance (`makeExtensionsCheck` only adds
   extensions the runtime enumerates; `useEglGraphicsBinding` comes back `false`). Passing
   a binding struct whose extension was never enabled → the runtime rejects it.
2. Even if it *were* enabled: ANGLE on Windows is GLES→D3D11 **inside our process**. Every
   GL texture is backed by a D3D11 texture owned by **ANGLE's private D3D11 device**. The
   OpenXR runtime has its own device and no way to import ANGLE's internal textures →
   `GRAPHICS_DEVICE_INVALID` is precisely "the device behind this binding isn't one I can
   interop with."

> Note: `eglGetCurrentDisplay()` returning non-null only tells you *jME/ANGLE* is on EGL.
> It says nothing about what the *runtime* supports. Don't use it to decide the binding.

### Attempt 2 — re-enable the Windows desktop-GL binding

Re-enabling the `XrGraphicsBindingOpenGLWin32KHR` path threw, on touching
`org.lwjgl.opengl.WGL`:

```
java.lang.ExceptionInInitializerError
    at org.lwjgl.opengl.WGL.<clinit>(WGL.java:24)
    ...
Caused by: java.lang.IllegalStateException: setFunctionMissingAddresses has been called already
    at org.lwjgl.system.ThreadLocalUtil.setFunctionMissingAddresses(ThreadLocalUtil.java:205)
    at org.lwjgl.opengl.GL.create(GL.java:230)
    ...
```

**Why:** LWJGL enforces a **process-global mutual exclusion** between its `opengl`
(desktop GL) and `opengles` (GLES/ANGLE) modules. Both `GL.create()` and `GLES.create()`
call `ThreadLocalUtil.setFunctionMissingAddresses(...)`, and the **second** one to run
throws. The stack shows `WGL.<clinit>` → `GL.create()` throwing *because something already
called it* — that something is jME initialising **GLES** (ANGLE). So the error is proof
that the active jME context is GLES/ANGLE and that **there is no desktop-GL context in the
process at all**.

Consequences:

- `WGL.wglGetCurrentContext()` / `wglGetCurrentDC()` would return `NULL` under ANGLE even
  if you bypassed LWJGL via raw FFI (ANGLE makes an *EGL* context current, not a WGL one).
- So the Win32-GL binding is doubly dead under ANGLE: class-load clash *and* no WGL context.

**The user's intuition ("mixing GL and GLES") was correct** — refined: it's not individual
calls, it's that LWJGL won't let both GL-family modules initialise in one JVM, and ANGLE
got there first.

### The unifying insight

Both failures are the same root cause: **under the new default jME renders through ANGLE
(GLES → D3D11), and no Windows OpenXR runtime can interop with that through an *OpenGL*
binding.** Fix the renderer and both problems vanish.

When jME uses a **desktop-GL** renderer instead, jME itself initialises
`org.lwjgl.opengl.GL`; Tamarin later touching `WGL` finds the class already initialised, so
`GL.create()` does not run a second time and the `setFunctionMissingAddresses` clash never
occurs. A real WGL `HGLRC` exists, and the existing binding works.

---

## Road A — require the desktop-GL renderer (CHOSEN, implemented on the test bed)

**Stance:** Desktop OpenXR runtimes interop with desktop GL, D3D11/12, or Vulkan — never
with an in-process ANGLE GLES device. ANGLE on desktop exists for portability/testing and
for macOS/mobile; for Windows/Linux VR there's no upside. So *Tamarin VR on desktop
requires `LWJGL_OPENGL33`+*.

**User-side fix (before `app.start()`):**

```java
settings.setRenderer(AppSettings.LWJGL_OPENGL45); // or 33+, before the context is created
```

**Tamarin-side work:**

- Add a **fail-fast guard**: detect when the active renderer is `ANGLE_GLES3` (e.g. via
  `settings.getRenderer()`, available in `XrAppState.initialize` at `XrAppState.java:34`)
  and throw a clear, actionable exception instead of the cryptic `GRAPHICS_DEVICE_INVALID`
  / `ExceptionInInitializerError`. Suggested message points the user at
  `setRenderer(AppSettings.LWJGL_OPENGL33)` and explains ANGLE renders GLES over D3D and
  can't be bound to a desktop OpenXR OpenGL session.
- Revert the forced-EGL override in `XrUtils.createGraphicsBindingOpenGL`
  (`if (true || useEGL)` → `if (useEGL)`), so the existing helpful "EGL not supported by
  this runtime" exception is produced rather than a cryptic device error.
- Optionally provide a convenience helper, e.g. `XrSettings.applyVrRenderer(AppSettings)`,
  that sets a desktop-GL renderer, to be called before `app.start()`.

**Hard constraint:** Tamarin **cannot fix the renderer itself.** By the time
`XrAppState.initialize` runs, the context and renderer already exist, so `setRenderer` is
too late. Tamarin can only *validate and fail fast*; the real fix lives in user code +
docs (or the optional pre-start helper).

**Pros:** Restores VR with essentially the current binding code. Tiny change. Robust.
**Cons:** Desktop VR users must opt out of the new jME default — a documentation burden.

---

## Road B — support ANGLE-GLES via D3D11 interop (FUTURE, large)

Only worth doing if there's a hard requirement to run VR under a single GLES renderer
across all platforms. ANGLE on Windows is GLES-over-**D3D11**, so bind at the D3D11 layer,
not the GL layer:

1. Use **`XR_KHR_D3D11_enable`** instead of `XR_KHR_opengl_enable` for the session binding.
   (LWJGL ships `org.lwjgl.openxr.KHRD3D11Enable`:
   `XrGraphicsBindingD3D11KHR`, `XrSwapchainImageD3D11KHR`, `xrGetD3D11GraphicsRequirementsKHR`.)
2. Pull ANGLE's own `ID3D11Device` out of EGL via the device-query extensions:
   - `eglQueryDisplayAttribEXT(dpy, EGL_DEVICE_EXT, &device)`
   - `eglQueryDeviceAttribEXT(device, EGL_D3D11_DEVICE_ANGLE, &d3dDevice)`
   (`EGL_EXT_device_query` / `EGL_ANGLE_device_d3d`; bindings in LWJGL `lwjgl-egl`).
3. Verify ANGLE's device sits on the adapter LUID that `xrGetD3D11GraphicsRequirementsKHR`
   demands, then create the session with `XrGraphicsBindingD3D11KHR{ device = d3dDevice }`.
4. Create swapchains as D3D11 textures (`XrSwapchainImageD3D11KHR` → `ID3D11Texture2D*`).
5. For each swapchain texture, wrap it back into an ANGLE GL texture via
   **`EGL_ANGLE_d3d_texture_client_buffer`** —
   `eglCreatePbufferFromClientBuffer(dpy, EGL_D3D_TEXTURE_ANGLE, d3dTexture, config, attribs)` —
   so jME can render into it as a normal FBO colour target.
6. Render jME's scene into those GL textures (which *are* the D3D11 swapchain images),
   release the swapchain image, submit the frame.

**Nice property:** you never link a D3D11 native lib or call D3D methods yourself — you only
pass opaque pointers (ANGLE's device, the swapchain textures) between OpenXR and ANGLE
through EGL. Tamarin already depends on `lwjgl-openxr` and `lwjgl-egl`.

**Open items to verify before committing to Road B:**
- Exact LWJGL `lwjgl-egl` coverage of `EGL_D3D11_DEVICE_ANGLE`, `EGL_D3D_TEXTURE_ANGLE`,
  `eglCreatePbufferFromClientBuffer`, and the device-query entry points in 3.4.1.
- Texture format negotiation between OpenXR's allowed swapchain formats and what ANGLE will
  accept for the client-buffer wrap.
- Whether jME's render pipeline can be pointed at an externally-provided FBO/texture cleanly.

**Pros:** VR works even under jME's ANGLE default; single renderer across platforms.
**Cons:** Substantial D3D/EGL interop engineering, more moving parts, more to maintain.

---

## Quick reference: which binding goes with which jME renderer

| jME renderer            | LWJGL module | Context type        | OpenXR graphics binding                      | Status            |
|-------------------------|--------------|---------------------|----------------------------------------------|-------------------|
| `LWJGL_OPENGL33`–`45`   | `opengl`     | desktop GL (WGL/GLX)| `XrGraphicsBindingOpenGLWin32KHR` / `…Xlib…` | **Works (Road A)**|
| `ANGLE_GLES3` (default) | `opengles`   | EGL/ANGLE → D3D11   | `XR_KHR_D3D11_enable` + EGL interop          | Needs Road B      |
| `ANGLE_GLES3` + Monado  | `opengles`   | EGL/ANGLE           | `XrGraphicsBindingEGLMNDX`                    | Monado-only, rare |

---

## Relevant code touchpoints

- `tamarin-desktop/.../openxr/XrUtils.java` — `createGraphicsBindingOpenGL(...)`:
  builds the graphics binding struct; contains the forced-EGL override to revert and the
  Win32/Xlib/EGL branches.
- `tamarin-desktop/.../openxr/OpenXrSessionManager.java` — `initializeAndBindOpenGL()`
  (~line 357) calls the binding helper then `xrCreateSession`; `makeExtensionsCheck()`
  (~line 938) only enables runtime-advertised extensions; `useEglGraphicsBinding` reflects
  whether `XR_MNDX_egl_enable` was actually enabled.
- `tamarin-desktop/.../openxr/XrAppState.java` — `initialize(...)` (~line 34) reads the
  already-built `AppSettings`; this is the natural place for the Road-A fail-fast guard
  (and proof that the renderer can't be changed from here).
- `tamarin-core/.../openxr/XrSettings.java` — requests `XR_MNDX_egl_enable` etc.; candidate
  home for an `applyVrRenderer(AppSettings)` helper.

## Sources

- jME `AppSettings.java` (renderer constants + `ANGLE_GLES3` default):
  https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-core/src/main/java/com/jme3/system/AppSettings.java
- jMonkeyEngine (dual GLES-via-ANGLE / direct-OpenGL backend overview): https://jmonkeyengine.org/
