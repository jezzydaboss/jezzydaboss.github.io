///////////////////////////////////////////////////////////////////////////////
// viewmanager.h
// ============
// manage the viewing of 3D objects within the viewport - camera, projection
//
//  AUTHOR: Brian Battersby - SNHU Instructor / Computer Science
//	Created for CS-330-Computational Graphics and Visualization, Nov. 1st, 2023
///////////////////////////////////////////////////////////////////////////////

#pragma once

#include "ShaderManager.h"
#include "camera.h"
#include "MetricsServer.h"

// GLFW library
#include "GLFW/glfw3.h" 

#include <memory>

/***********************************************************
 *  ViewManager
 *
 *  ENHANCEMENT NOTES (Milestone Two, CS 499):
 *
 *  This artifact had accumulated a real architecture problem:
 *  camera and input handling had been duplicated in TWO places.
 *  ViewManager (this class) was the originally-designed owner
 *  of the camera/view, but its Mouse_Position_Callback() had
 *  been left empty and its ProcessKeyboardEvents() only
 *  handled the Escape key. Meanwhile, a full second
 *  implementation of camera movement, mouse look, scroll-based
 *  speed, and projection-mode switching had been written from
 *  scratch as free functions and global variables at the top
 *  of SceneManager.cpp - despite camera.h already providing a
 *  complete, working Camera class with ProcessKeyboard(),
 *  ProcessMouseMovement(), and GetViewMatrix() that was never
 *  actually used. On top of the duplication, MainCode.cpp
 *  registered GLFW's cursor-position callback twice - once to
 *  ViewManager's (no-op) callback, then again to the
 *  SceneManager-based one, silently overwriting the first.
 *
 *  This enhancement consolidates ALL camera and input logic
 *  back into ViewManager - its originally intended single
 *  responsibility - built on top of the existing Camera class
 *  instead of hand-rolled globals. SceneManager no longer
 *  computes or uploads its own view/projection matrices at
 *  all; it asks ViewManager for them once per frame. Camera
 *  ownership uses std::unique_ptr (RAII) instead of a manual
 *  new/delete, and GLFW's window "user pointer" is used to
 *  recover the correct ViewManager instance inside the static
 *  C-style callbacks instead of relying on any global state.
 ***********************************************************/
class ViewManager
{
public:
	// projection mode - previously duplicated as a global enum in
	// SceneManager.cpp; now owned here alongside the rest of the
	// camera/view state it actually controls
	enum class ProjectionMode { Perspective, Orthographic };

	// constructor
	ViewManager(
		ShaderManager* pShaderManager);
	// destructor
	~ViewManager();

	// mouse position callback for mouse interaction with the 3D scene
	static void Mouse_Position_Callback(GLFWwindow* window, double xMousePos, double yMousePos);
	// mouse scroll callback - adjusts camera movement speed
	static void Scroll_Callback(GLFWwindow* window, double xOffset, double yOffset);

	// ENHANCEMENT: exposes the current view/projection matrices so
	// SceneManager can render using the single, authoritative camera
	// state instead of maintaining a second, duplicate camera itself
	glm::mat4 GetViewMatrix() const;
	glm::mat4 GetProjectionMatrix() const;

	// ENHANCEMENT: basic per-frame performance data, now backed by an
	// actual network-reachable HTTP endpoint (see MetricsServer) rather
	// than only a local JSON file - a browser dashboard can fetch()
	// http://localhost:8080/metrics live. This is what makes the
	// "Full-Stack App Integration" claim in the Module One enhancement
	// plan an accurate description of the code rather than a future
	// intention.
	float GetLastFrameTimeMs() const { return m_deltaTime * 1000.0f; }
	float GetCurrentFPS() const { return (m_deltaTime > 0.0f) ? (1.0f / m_deltaTime) : 0.0f; }

private:
	// pointer to shader manager object
	ShaderManager* m_pShaderManager;
	// active OpenGL display window
	GLFWwindow* m_pWindow;

	// ENHANCEMENT: camera is owned via RAII instead of a manually
	// managed global raw pointer, and instead of being reimplemented
	// from scratch with free-floating globals in SceneManager.cpp
	std::unique_ptr<Camera> m_pCamera;

	// ENHANCEMENT: the live HTTP endpoint that serves frame metrics
	// (frame time, FPS, camera position/front navigation vector) as
	// JSON to any HTTP client - see MetricsServer.h for design notes
	std::unique_ptr<MetricsServer> m_pMetricsServer;

	// builds the current JSON metrics payload and pushes it to
	// m_pMetricsServer so the next HTTP request serves fresh data
	void UpdateMetricsPayload();

	// ENHANCEMENT: mouse-tracking and timing state, formerly
	// duplicated as globals in two different .cpp files, now
	// encapsulated as members of the class that actually owns them
	float m_lastX;
	float m_lastY;
	bool m_firstMouse;
	float m_deltaTime;
	float m_lastFrame;

	// current projection mode, toggled by the P / O keys
	ProjectionMode m_projectionMode;

	// process keyboard events for interaction with the 3D scene
	void ProcessKeyboardEvents();

public:
	// create the initial OpenGL display window
	GLFWwindow* CreateDisplayWindow(const char* windowTitle);
	
	// prepare the conversion from 3D object display to 2D scene display
	void PrepareSceneView();
};
