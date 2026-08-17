<link rel="stylesheet" href="assets/css/style.css">
<style>:root { --accent: #3d5a80; }</style>

# Software Design and Engineering — 3D OpenGL Scene

[Home](index.md) | [Software Engineering](software-engineering.md) | [Algorithms](algorithms.md) | [Databases](databases.md) | [Code Review](code-review.md)

---

## Artifact Description

The artifact is a 3D scene rendering application built in C++ using the OpenGL graphics API and GLSL shading language, originally created for CS 330, Computational Graphics and Visualization. It renders a desk scene with a laptop, monitor, coffee mug, pens, and a stack of notebooks, using a main application loop (MainCode.cpp) and two manager classes: SceneManager, which loads textures and materials and draws the scene content, and ViewManager, which is responsible for the camera, mouse/keyboard input, and the projection from 3D scene space to the 2D display.

**Browse the code:** [original](https://github.com/jezzydaboss/jezzydaboss.github.io/tree/main/artifacts/original/3DOpenGLScene) · [enhanced](https://github.com/jezzydaboss/jezzydaboss.github.io/tree/main/artifacts/enhanced/OpenGL_Scene_Enhanced_v3)

## Justification for Inclusion

I selected this artifact because it demonstrates C++ software architecture and low-level graphics programming, skills that don't come through in my other two artifacts. When I sat down to actually perform this milestone's enhancement, I discovered a much more serious problem had been introduced: the camera and keyboard/mouse input logic had been duplicated. ViewManager, the class originally designed to own the camera, had been left with an empty mouse callback and keyboard handling that only checked the Escape key. In its place, a second, completely separate implementation of camera movement, mouse-look, and projection switching had been written from scratch as global variables and free functions at the top of SceneManager.cpp - despite the project already including a complete, working Camera class (camera.h) that was never actually being used. On top of that, MainCode.cpp registered GLFW's mouse callback twice: once to ViewManager's callback, and then again to the SceneManager-based one, which silently overwrote the first. I want to be direct about this - my enhancement work had to start with re-diagnosing the artifact rather than simply executing my original plan.

For this milestone, I performed a full consolidation: all camera and input handling now lives exclusively in ViewManager, built on top of the existing Camera class instead of a hand-rolled reimplementation. I removed roughly ninety lines of duplicated globals and free functions from SceneManager.cpp. SceneManager no longer computes or uploads its own view and projection matrices; it now holds a pointer to the ViewManager instance and asks it for the current matrices once per frame, which also eliminated a second bug I found in the process both classes were pushing the "view" and "projection" shader uniforms every frame, with SceneManager's copy silently overwriting ViewManager's correct one. The camera itself is now owned through a std::unique_ptr rather than a manually managed raw pointer. I also fixed the remaining defects from my original review that were still present: SceneManager::FindMaterial() was still unconditionally returning true regardless of whether a material tag was actually found, and CreateGLTexture() still had no bounds check against the fixed 16-slot texture array. I fixed both. I removed a duplicate call to PrepareScene() in main() that was running the entire scene setup twice on every launch.

Unlike my first attempt at this milestone, I was able to install the project's actual dependencies (GLFW, GLEW, GLM) in my working environment and compile and link every modified file for real, rather than reviewing the changes by eye alone. The full build compiles cleanly with zero errors and zero warnings under both default flags. I want to be precise about what that does and doesn't confirm: it verifies the code is syntactically correct, type-safe, and linkable.

After reviewing my first attempt at this milestone, I recognized that my "Full-Stack App Integration" claim wasn't accurate yet - I had only written frame metrics to a local JSON file, which is not "an accessible web platform" by any reasonable reading, since nothing outside my own filesystem could reach it. I built the missing piece: a MetricsServer class that opens a real TCP listening socket and serves the current frame metrics as JSON over HTTP, running on a background thread so it never blocks the render loop.

### Figure 1 — Defect Fix

![DestroyGLTextures defect fix](assets/screenshots/artifact1_shot1.png)

*The original called `glGenTextures()` (allocating new memory) instead of `glDeleteTextures()` (freeing it), leaking GPU memory on every call.*

### Figure 2 — Camera/Callback Consolidation

![Camera and callback consolidation](assets/screenshots/artifact1_shot2.png)

*GLFW's window user-pointer is used to recover the owning instance in static callbacks, replacing the duplicate/competing callback registration that previously existed in `MainCode.cpp`.*

## Course Outcomes Update

In Module One, I planned for this enhancement to demonstrate progress toward Course Outcomes 3 and 4. I believe I made real progress on both, though the path there was different from what I originally planned. Outcome 4 is reflected directly in replacing a hand-rolled camera reimplementation with the project's own existing, correct Camera class, and in using RAII (std::unique_ptr) and GLFW's window-user-pointer pattern instead of global state. Outcome 3 came through most clearly in deciding how to eliminate the duplicate camera/matrix logic I had to weigh a minimal patch against a full consolidation that removed the duplicate entirely, and chose the latter because leaving two competing camera implementations in the codebase, even if I made one of them functionally correct, would have left the same architectural risk in place for the next person to touch this code. My update to the plan is to scale back some, I did more defect remediation than originally planned, because the actual state of the code required it.

## Reflection on the Enhancement Process

The biggest lesson from this milestone is that a code review and the actual state of the code can drift apart, and an enhancement has to start by re-verifying reality rather than trusting the review. My Milestone One review was accurate for the code I had at the time, but by the time I sat down to implement the enhancement, the working project had changed underneath it into a more tangled form than before. That was a genuinely useful reminder that a review is a snapshot, not a permanent contract, and that going back to actually read the current code before making changes isn't optional.

The main technical challenge was untangling the duplicate camera logic without breaking anything that depended on it. Both implementations were, on their own, mostly reasonable code the SceneManager-based one even correctly implemented mouse-look and WASD movement. I also learned the value of actually compiling code I'm reviewing or changing, rather than reasoning about it purely by reading. Being able to install the real GLFW/GLEW/GLM dependencies and get a real compiler's opinion on my changes caught nothing major this time, since I was careful, and it's the same discipline I'd want to apply as a habit in any future codebase, not just this one.
