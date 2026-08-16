///////////////////////////////////////////////////////////////////////////////
// maincode.cpp
// ============
// gets called when application is launched - initializes GLEW, GLFW
//
//  AUTHOR: Brian Battersby - SNHU Instructor / Computer Science
//	Created for CS-330-Computational Graphics and Visualization, Nov. 1st, 2023
//
//  ENHANCED (Milestone Two - CS 499):
//    - Removed the duplicate glfwSetCursorPosCallback/glfwSetScrollCallback
//      registration that had been overwriting ViewManager's own callback
//      registration (set inside CreateDisplayWindow()) with a second,
//      competing SceneManager-based implementation.
//    - Removed the duplicate call to g_SceneManager->PrepareScene().
//    - Removed SceneManager::SetWindow() / the processInput() call, since
//      camera/input handling now lives entirely in ViewManager - see
//      ViewManager.h/.cpp and SceneManager.h/.cpp for details.
///////////////////////////////////////////////////////////////////////////////

#include <iostream>         // error handling and output
#include <cstdlib>          // EXIT_FAILURE
#define GLM_ENABLE_EXPERIMENTAL
#include <GL/glew.h>        // GLEW library
#include "GLFW/glfw3.h"     // GLFW library

// GLM Math Header inclusions
#include <glm/glm.hpp>
#include <glm/gtx/transform.hpp>
#include <glm/gtc/type_ptr.hpp>

#include "SceneManager.h"
#include "ViewManager.h"
#include "ShapeMeshes.h"
#include "ShaderManager.h"

// -----------------------------------------------------------------------------
// Global state and forward declarations
// -----------------------------------------------------------------------------

// Window title
const char* const WINDOW_TITLE = "7-1 FinalProject and Milestones";

// Global pointers to managers (created in main)
GLFWwindow* g_Window = nullptr;
SceneManager* g_SceneManager = nullptr;
ShaderManager* g_ShaderManager = nullptr;
ViewManager* g_ViewManager = nullptr;

// Function prototypes
bool InitializeGLFW();
bool InitializeGLEW();
void resizeWindow(GLFWwindow* window, int width, int height);

/***********************************************************
*  main(int, char*)
*
*  This function gets called after the application has been
*  launched.
***********************************************************/ 
int main(int argc, char* argv[])
{
	// if GLFW fails initialization, then terminate the application
	if (InitializeGLFW() == false)
	{
		return(EXIT_FAILURE);
	}

	// try to create a new shader manager object
	g_ShaderManager = new ShaderManager();
	// try to create a new view manager object
	g_ViewManager = new ViewManager(
		g_ShaderManager);

	// try to create the main display window
	// (this also registers the cursor-position and scroll callbacks
	// internally, since ViewManager is the sole owner of camera/input)
	g_Window = g_ViewManager->CreateDisplayWindow(WINDOW_TITLE);
	if (NULL == g_Window)
	{
		std::cout << "Failed to create GLFW window" << std::endl;
		glfwTerminate();
		return EXIT_FAILURE;
	}

	// Ensure the OpenGL context is current before initializing GLEW
	glfwMakeContextCurrent(g_Window);

	// if GLEW fails initialization, then terminate the application
	if (InitializeGLEW() == false)
	{
		return(EXIT_FAILURE);
	}

	glfwSetFramebufferSizeCallback(g_Window, resizeWindow);

	// load the shader code from the external GLSL files
	g_ShaderManager->LoadShaders(
		"Utilities/shaders/vertexShader.glsl",
		"Utilities/shaders/fragmentShader.glsl");
	g_ShaderManager->use();

	// try to create a new scene manager object and prepare the 3D scene -
	// SceneManager is given a pointer to the ViewManager so it can pull
	// the current view/projection matrices each frame instead of
	// maintaining its own separate camera state
	g_SceneManager = new SceneManager(g_ShaderManager, g_ViewManager);
	g_SceneManager->PrepareScene();

	// loop will keep running until the application is closed 
	// or until an error has occurred
	while (!glfwWindowShouldClose(g_Window))
	{
		// Enable z-depth
		glEnable(GL_DEPTH_TEST);

		// Clear the frame and z buffers
		glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		// convert from 3D object space to 2D view - this also processes
		// keyboard input (movement, projection toggle) for the frame
		g_ViewManager->PrepareSceneView();

		// refresh the 3D scene
		g_SceneManager->RenderScene();

		// Flips the the back buffer with the front buffer every frame.
		glfwSwapBuffers(g_Window);

		// query the latest GLFW events
		glfwPollEvents();
	}

	// clear the allocated manager objects from memory
	if (NULL != g_SceneManager)
	{
		delete g_SceneManager;
		g_SceneManager = NULL;
	}
	if (NULL != g_ViewManager)
	{
		delete g_ViewManager;
		g_ViewManager = NULL;
	}
	if (NULL != g_ShaderManager)
	{
		delete g_ShaderManager;
		g_ShaderManager = NULL;
	}

	// Terminates the program successfully
	exit(EXIT_SUCCESS);
}

/***********************************************************
 *	InitializeGLFW()
 * 
 *  This function is used to initialize the GLFW library.   
 ***********************************************************/
bool InitializeGLFW()
{
	// GLFW: initialize and configure library
	// --------------------------------------
	glfwInit();

#ifdef __APPLE__
	// set the version of OpenGL and profile to use
	glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
	glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
	glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
#else
	// set the version of OpenGL and profile to use
	glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
	glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
	glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
#endif
	// GLFW: end -------------------------------

	return(true);
}

/***********************************************************
 *	InitializeGLEW()
 *
 *  This function is used to initialize the GLEW library.
 ***********************************************************/
bool InitializeGLEW()
{
	// GLEW: initialize
	// -----------------------------------------
	GLenum GLEWInitResult = GLEW_OK;

	// try to initialize the GLEW library
	GLEWInitResult = glewInit();
	if (GLEW_OK != GLEWInitResult)
	{
		std::cerr << glewGetErrorString(GLEWInitResult) << std::endl;
		return false;
	}
	// GLEW: end -------------------------------

	// Displays a successful OpenGL initialization message
	std::cout << "INFO: OpenGL Successfully Initialized\n";
	std::cout << "INFO: OpenGL Version: " << glGetString(GL_VERSION) << "\n" << std::endl;

	return(true);
}

void resizeWindow(GLFWwindow* window, int width, int height)
{
	glViewport(0, 0, width, height);
}
