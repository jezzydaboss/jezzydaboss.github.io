///////////////////////////////////////////////////////////////////////////////
// viewmanager.cpp
// ============
// manage the viewing of 3D objects within the viewport - camera, projection
//
//  AUTHOR: Brian Battersby - SNHU Instructor / Computer Science
//	Created for CS-330-Computational Graphics and Visualization, Nov. 1st, 2023
//
//  ENHANCED (Milestone Two - CS 499): all camera/input logic that had been
//  duplicated as globals in SceneManager.cpp is now consolidated here, built
//  on top of the existing Camera class instead of a hand-rolled reimplementation.
//  See ViewManager.h for full enhancement notes.
///////////////////////////////////////////////////////////////////////////////
#define GLM_ENABLE_EXPERIMENTAL
#include "ViewManager.h"

// GLM Math Header inclusions
#include <glm/glm.hpp>
#include <glm/gtx/transform.hpp>
#include <glm/gtc/type_ptr.hpp>

// declaration of variables that are genuinely compile-time constants /
// shader uniform names, shared read-only across all instances
namespace
{
	const int WINDOW_WIDTH = 1000;
	const int WINDOW_HEIGHT = 800;
	const char* g_ViewName = "view";
	const char* g_ProjectionName = "projection";
}

/***********************************************************
 *  ViewManager()
 *
 *  The constructor for the class
 ***********************************************************/
ViewManager::ViewManager(
	ShaderManager *pShaderManager)
{
	m_pShaderManager = pShaderManager;
	m_pWindow = NULL;

	// ENHANCEMENT: camera is owned via unique_ptr (RAII) instead of a
	// manually new'd/delete'd global raw pointer
	m_pCamera = std::make_unique<Camera>();
	m_pCamera->Position = glm::vec3(0.0f, 5.0f, 12.0f);
	m_pCamera->Front = glm::vec3(0.0f, -0.5f, -2.0f);
	m_pCamera->Up = glm::vec3(0.0f, 1.0f, 0.0f);
	m_pCamera->Zoom = 45.0f;
	m_pCamera->MovementSpeed = 2.5f;

	m_lastX = WINDOW_WIDTH / 2.0f;
	m_lastY = WINDOW_HEIGHT / 2.0f;
	m_firstMouse = true;
	m_deltaTime = 0.0f;
	m_lastFrame = 0.0f;
	m_projectionMode = ProjectionMode::Perspective;

	// ENHANCEMENT: start the live metrics HTTP endpoint. Port 8080 is
	// used as a simple, commonly-open default; if it's already in use
	// on this machine, Start() logs an error and returns false, but the
	// rest of the application continues running normally either way -
	// the endpoint is an enhancement, not a hard dependency for the
	// scene to render.
	m_pMetricsServer = std::make_unique<MetricsServer>(8080);
	m_pMetricsServer->Start();
}

/***********************************************************
 *  ~ViewManager()
 *
 *  The destructor for the class
 ***********************************************************/
ViewManager::~ViewManager()
{
	// ENHANCEMENT: explicitly stop the server (joins its background
	// thread) before the unique_ptr destroys it, and before m_pCamera
	// and other members go away - Stop() is also safe to call
	// implicitly via ~MetricsServer() alone, but being explicit here
	// keeps the shutdown order obvious.
	if (m_pMetricsServer)
	{
		m_pMetricsServer->Stop();
	}

	// m_pCamera and m_pMetricsServer are std::unique_ptr and release
	// themselves automatically here - no manual delete/NULL-check required.
	m_pShaderManager = NULL;
	m_pWindow = NULL;
}

/***********************************************************
 *  CreateDisplayWindow()
 *
 *  This method is used to create the main display window.
 ***********************************************************/
GLFWwindow* ViewManager::CreateDisplayWindow(const char* windowTitle)
{
	GLFWwindow* window = nullptr;

	window = glfwCreateWindow(
		WINDOW_WIDTH,
		WINDOW_HEIGHT,
		windowTitle,
		NULL, NULL);
	if (window == NULL)
	{
		std::cout << "Failed to create GLFW window" << std::endl;
		glfwTerminate();
		return NULL;
	}
	glfwMakeContextCurrent(window);

	// ENHANCEMENT: store a pointer back to this ViewManager instance on
	// the window itself. GLFW's callback API only accepts free
	// functions/static methods, so this is how the static callbacks
	// below recover the correct owning instance without any global state.
	glfwSetWindowUserPointer(window, this);

	// tell GLFW to capture all mouse events
	glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

	// ENHANCEMENT: both the cursor-position and scroll callbacks are now
	// registered here, once, in the class that owns the camera they
	// drive. Previously the cursor callback was registered here AND a
	// second time in MainCode.cpp with a different, competing function,
	// with the second registration silently overwriting this one.
	glfwSetCursorPosCallback(window, &ViewManager::Mouse_Position_Callback);
	glfwSetScrollCallback(window, &ViewManager::Scroll_Callback);

	// enable blending for supporting tranparent rendering
	glEnable(GL_BLEND);
	glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

	m_pWindow = window;

	return(window);
}

/***********************************************************
 *  Mouse_Position_Callback()
 *
 *  This method is automatically called from GLFW whenever
 *  the mouse is moved within the active GLFW display window.
 ***********************************************************/
void ViewManager::Mouse_Position_Callback(GLFWwindow* window, double xMousePos, double yMousePos)
{
	ViewManager* pView = static_cast<ViewManager*>(glfwGetWindowUserPointer(window));
	if (pView == nullptr)
	{
		return;
	}

	if (pView->m_firstMouse)
	{
		pView->m_lastX = static_cast<float>(xMousePos);
		pView->m_lastY = static_cast<float>(yMousePos);
		pView->m_firstMouse = false;
	}

	float xOffset = static_cast<float>(xMousePos) - pView->m_lastX;
	float yOffset = pView->m_lastY - static_cast<float>(yMousePos); // reversed since y-coordinates go from bottom to top

	pView->m_lastX = static_cast<float>(xMousePos);
	pView->m_lastY = static_cast<float>(yMousePos);

	pView->m_pCamera->ProcessMouseMovement(xOffset, yOffset);
}

/***********************************************************
 *  Scroll_Callback()
 *
 *  ENHANCEMENT: previously reimplemented from scratch as a free
 *  function operating on a global movementSpeed variable in
 *  SceneManager.cpp. Camera::ProcessMouseScroll() already does
 *  exactly this, correctly clamped, so this callback just forwards
 *  to it.
 ***********************************************************/
void ViewManager::Scroll_Callback(GLFWwindow* window, double xOffset, double yOffset)
{
	(void)xOffset; // unused - only vertical scroll drives movement speed

	ViewManager* pView = static_cast<ViewManager*>(glfwGetWindowUserPointer(window));
	if (pView == nullptr)
	{
		return;
	}

	pView->m_pCamera->ProcessMouseScroll(static_cast<float>(yOffset));
}

/***********************************************************
 *  ProcessKeyboardEvents()
 *
 *  This method is called to process any keyboard events
 *  that may be waiting in the event queue.
 ***********************************************************/
void ViewManager::ProcessKeyboardEvents()
{
	// close the window if the escape key has been pressed
	if (glfwGetKey(m_pWindow, GLFW_KEY_ESCAPE) == GLFW_PRESS)
	{
		glfwSetWindowShouldClose(m_pWindow, true);
	}

	// ENHANCEMENT: camera movement now goes through Camera::ProcessKeyboard(),
	// the working implementation that was already available via camera.h,
	// instead of the hand-rolled position math that had been duplicated in
	// SceneManager.cpp's processInput().
	if (glfwGetKey(m_pWindow, GLFW_KEY_W) == GLFW_PRESS)
	{
		m_pCamera->ProcessKeyboard(FORWARD, m_deltaTime);
	}
	if (glfwGetKey(m_pWindow, GLFW_KEY_S) == GLFW_PRESS)
	{
		m_pCamera->ProcessKeyboard(BACKWARD, m_deltaTime);
	}
	if (glfwGetKey(m_pWindow, GLFW_KEY_A) == GLFW_PRESS)
	{
		m_pCamera->ProcessKeyboard(LEFT, m_deltaTime);
	}
	if (glfwGetKey(m_pWindow, GLFW_KEY_D) == GLFW_PRESS)
	{
		m_pCamera->ProcessKeyboard(RIGHT, m_deltaTime);
	}
	if (glfwGetKey(m_pWindow, GLFW_KEY_Q) == GLFW_PRESS)
	{
		m_pCamera->ProcessKeyboard(UP, m_deltaTime);
	}
	if (glfwGetKey(m_pWindow, GLFW_KEY_E) == GLFW_PRESS)
	{
		m_pCamera->ProcessKeyboard(DOWN, m_deltaTime);
	}

	// projection mode toggle - previously a separate free function
	// (processProjectionKeys) operating on a global enum
	if (glfwGetKey(m_pWindow, GLFW_KEY_P) == GLFW_PRESS)
	{
		m_projectionMode = ProjectionMode::Perspective;
	}
	if (glfwGetKey(m_pWindow, GLFW_KEY_O) == GLFW_PRESS)
	{
		m_projectionMode = ProjectionMode::Orthographic;
	}
}

/***********************************************************
 *  GetViewMatrix() / GetProjectionMatrix()
 *
 *  ENHANCEMENT: these are the single authoritative source for
 *  the scene's view/projection matrices. SceneManager::RenderScene()
 *  previously recomputed its own copies of both every frame from a
 *  second, separate set of camera globals - this eliminates that
 *  duplication entirely.
 ***********************************************************/
glm::mat4 ViewManager::GetViewMatrix() const
{
	return m_pCamera->GetViewMatrix();
}

glm::mat4 ViewManager::GetProjectionMatrix() const
{
	int fbWidth = WINDOW_WIDTH;
	int fbHeight = WINDOW_HEIGHT;
	if (m_pWindow != NULL)
	{
		glfwGetFramebufferSize(m_pWindow, &fbWidth, &fbHeight);
	}
	const float aspect = (fbHeight > 0) ? (float)fbWidth / (float)fbHeight : (float)WINDOW_WIDTH / (float)WINDOW_HEIGHT;

	if (m_projectionMode == ProjectionMode::Perspective)
	{
		return glm::perspective(glm::radians(m_pCamera->Zoom), aspect, 0.1f, 100.0f);
	}

	const float orthoScale = 6.0f;
	const float right = orthoScale * aspect;
	const float left = -right;
	const float top = orthoScale;
	const float bottom = -top;
	return glm::ortho(left, right, bottom, top, 0.1f, 100.0f);
}

/***********************************************************
 *  UpdateMetricsPayload()
 *
 *  ENHANCEMENT: builds the current frame metrics as a JSON
 *  document and pushes it to the MetricsServer so the next
 *  HTTP request to /metrics serves fresh data. Includes frame
 *  timing and the camera's current position/front vector (the
 *  "navigation vector" data referenced in the enhancement
 *  plan). Collision-event tracking is intentionally NOT
 *  included here - there is no collision detection anywhere
 *  in this codebase yet, so reporting collision data would be
 *  fabricating a field with no real source; that remains
 *  explicitly unbuilt/future work rather than being
 *  represented as done.
 ***********************************************************/
void ViewManager::UpdateMetricsPayload()
{
	if (!m_pMetricsServer)
	{
		return;
	}

	const glm::vec3& pos = m_pCamera->Position;
	const glm::vec3& front = m_pCamera->Front;

	std::string json = "{\n";
	json += "  \"frameTimeMs\": " + std::to_string(GetLastFrameTimeMs()) + ",\n";
	json += "  \"fps\": " + std::to_string(GetCurrentFPS()) + ",\n";
	json += "  \"navigationVector\": {\n";
	json += "    \"position\": [" + std::to_string(pos.x) + ", " + std::to_string(pos.y) + ", " + std::to_string(pos.z) + "],\n";
	json += "    \"front\": [" + std::to_string(front.x) + ", " + std::to_string(front.y) + ", " + std::to_string(front.z) + "]\n";
	json += "  },\n";
	json += "  \"projectionMode\": \"" + std::string(m_projectionMode == ProjectionMode::Perspective ? "perspective" : "orthographic") + "\"\n";
	json += "}\n";

	m_pMetricsServer->UpdatePayload(json);
}

/***********************************************************
 *  PrepareSceneView()
 *
 *  This method is used for preparing the 3D scene by loading
 *  the shapes, textures in memory to support the 3D scene 
 *  rendering
 ***********************************************************/
void ViewManager::PrepareSceneView()
{
	// per-frame timing
	float currentFrame = static_cast<float>(glfwGetTime());
	m_deltaTime = currentFrame - m_lastFrame;
	m_lastFrame = currentFrame;

	// process any keyboard events that may be waiting in the queue
	ProcessKeyboardEvents();

	// keep the GL viewport synced to the current framebuffer size
	// every frame (covers the case where the window was resized)
	if (m_pWindow != NULL)
	{
		int fbWidth = 0, fbHeight = 0;
		glfwGetFramebufferSize(m_pWindow, &fbWidth, &fbHeight);
		glViewport(0, 0, fbWidth, fbHeight);
	}

	glm::mat4 view = GetViewMatrix();
	glm::mat4 projection = GetProjectionMatrix();

	if (NULL != m_pShaderManager)
	{
		m_pShaderManager->setMat4Value(g_ViewName, view);
		m_pShaderManager->setMat4Value(g_ProjectionName, projection);
		m_pShaderManager->setVec3Value("viewPosition", m_pCamera->Position);
	}

	// ENHANCEMENT: push fresh metrics to the live HTTP endpoint every
	// frame so a connected dashboard always sees current data
	UpdateMetricsPayload();
}
