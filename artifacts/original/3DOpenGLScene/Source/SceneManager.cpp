///////////////////////////////////////////////////////////////////////////////
// shadermanager.cpp
// ============
// manage the loading and rendering of 3D scenes
//
//  AUTHOR: Brian Battersby - SNHU Instructor / Computer Science
//	Created for CS-330-Computational Graphics and Visualization, Nov. 1st, 2023
///////////////////////////////////////////////////////////////////////////////
#define GLM_ENABLE_EXPERIMENTAL
#include "SceneManager.h"

#ifndef STB_IMAGE_IMPLEMENTATION
#define STB_IMAGE_IMPLEMENTATION
#include "stb_image.h"
#endif

#include <glm/gtx/transform.hpp>
#include <GLFW/glfw3.h>
#include "ViewManager.h"
#include <iostream> // Added for std::cout

// --- GLOBAL VARIABLES FOR CAMERA & INPUT ---
// Note: In a production app, these would be class members, but globals work for this scope.
glm::vec3 cameraPos = glm::vec3(0.0f, 2.0f, 10.0f); // Adjusted start pos
glm::vec3 cameraFront = glm::vec3(0.0f, 0.0f, -1.0f);
glm::vec3 cameraUp = glm::vec3(0.0f, 1.0f, 0.0f);

float movementSpeed = 2.5f;
float deltaTime = 0.0f;
float lastFrame = 0.0f;

// Orientation state
float yaw = -90.0f; // start looking -Z
float pitch = 0.0f;
float sensitivity = 0.1f;

// Projection State
enum class ProjectionMode { Perspective, Orthographic };
ProjectionMode gProjMode = ProjectionMode::Perspective;

// --- CALLBACK FUNCTIONS ---
// These must be registered in your Main.cpp using glfwSetCursorPosCallback / glfwSetScrollCallback

void mouse_callback(GLFWwindow* window, double xpos, double ypos)
{
	static float lastX = 400.0f, lastY = 300.0f;
	static bool firstMouse = true;

	if (firstMouse) {
		lastX = static_cast<float>(xpos);
		lastY = static_cast<float>(ypos);
		firstMouse = false;
	}

	float xoffset = static_cast<float>(xpos) - lastX;
	float yoffset = lastY - static_cast<float>(ypos); // invert Y
	lastX = static_cast<float>(xpos);
	lastY = static_cast<float>(ypos);

	xoffset *= sensitivity;
	yoffset *= sensitivity;

	yaw += xoffset;
	pitch += yoffset;

	// clamp to avoid flip
	if (pitch > 89.0f) pitch = 89.0f;
	if (pitch < -89.0f) pitch = -89.0f;

	glm::vec3 front;
	front.x = cos(glm::radians(yaw)) * cos(glm::radians(pitch));
	front.y = sin(glm::radians(pitch));
	front.z = sin(glm::radians(yaw)) * cos(glm::radians(pitch));
	cameraFront = glm::normalize(front);
}
void scroll_callback(GLFWwindow* window, double xoffset, double yoffset)
{
	movementSpeed += (float)yoffset;         // speed up/down movement
	if (movementSpeed < 1.0f) movementSpeed = 1.0f;
}

 void processProjectionKeys(GLFWwindow* window)
{
	if (glfwGetKey(window, GLFW_KEY_P) == GLFW_PRESS) gProjMode = ProjectionMode::Perspective;
	if (glfwGetKey(window, GLFW_KEY_O) == GLFW_PRESS) gProjMode = ProjectionMode::Orthographic;
}

void processInput(GLFWwindow* window)
{
	if (!window) return;

	// Use your global timing/camera variables (must be visible here)
	const float velocity = movementSpeed * deltaTime;

	// Forward / Back
	if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) cameraPos += velocity * cameraFront;
	if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) cameraPos -= velocity * cameraFront;

	// Left / Right (strafe)
	glm::vec3 right = glm::normalize(glm::cross(cameraFront, cameraUp));
	if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) cameraPos -= right * velocity;
	if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) cameraPos += right * velocity;

	// Up / Down
	if (glfwGetKey(window, GLFW_KEY_Q) == GLFW_PRESS) cameraPos += cameraUp * velocity;
	if (glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS) cameraPos -= cameraUp * velocity;

	// Optional: projection toggles (call into ViewManager if you implement it)
	if (glfwGetKey(window, GLFW_KEY_P) == GLFW_PRESS) { /* set perspective */ }
	if (glfwGetKey(window, GLFW_KEY_O) == GLFW_PRESS) { /* set orthographic */ }
}
void SceneManager::SetWindow(GLFWwindow* window)
{
	m_pWindow = window;
}
// declaration of global variables
namespace
{
	const char* g_ModelName = "model";
	const char* g_ColorValueName = "objectColor";
	const char* g_TextureValueName = "objectTexture";
	const char* g_UseTextureName = "bUseTexture";
	const char* g_UseLightingName = "bUseLighting";
}

/***********************************************************
 *  SceneManager()
 *
 *  The constructor for the class
 ***********************************************************/
SceneManager::SceneManager(ShaderManager *pShaderManager)
{
	m_pShaderManager = pShaderManager;
	m_basicMeshes = new ShapeMeshes();
	m_loadedTextures = 0;

}

/***********************************************************
 *  ~SceneManager()
 *
 *  The destructor for the class
 ***********************************************************/
SceneManager::~SceneManager()
{
	m_pShaderManager = NULL;
	delete m_basicMeshes;
	m_basicMeshes = NULL;
}

/***********************************************************
 *  CreateGLTexture()
 *
 *  This method is used for loading textures from image files,
 *  configuring the texture mapping parameters in OpenGL,
 *  generating the mipmaps, and loading the read texture into
 *  the next available texture slot in memory.
 ***********************************************************/
bool SceneManager::CreateGLTexture(const char* filename, std::string tag)
{
	int width = 0;
	int height = 0;
	int colorChannels = 0;
	GLuint textureID = 0;

	// indicate to always flip images vertically when loaded
	stbi_set_flip_vertically_on_load(true);

	// try to parse the image data from the specified image file
	unsigned char* image = stbi_load(
		filename,
		&width,
		&height,
		&colorChannels,
		0);

	// if the image was successfully read from the image file
	if (image)
	{
		std::cout << "Successfully loaded image:" << filename << ", width:" << width << ", height:" << height << ", channels:" << colorChannels << std::endl;

		glGenTextures(1, &textureID);
		glBindTexture(GL_TEXTURE_2D, textureID);

		// set the texture wrapping parameters
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		// set texture filtering parameters
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

		// if the loaded image is in RGB format
		if (colorChannels == 3)
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, image);
		// if the loaded image is in RGBA format - it supports transparency
		else if (colorChannels == 4)
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
		else
		{
			std::cout << "Not implemented to handle image with " << colorChannels << " channels" << std::endl;
			return false;
		}

		// generate the texture mipmaps for mapping textures to lower resolutions
		glGenerateMipmap(GL_TEXTURE_2D);

		// free the image data from local memory
		stbi_image_free(image);
		glBindTexture(GL_TEXTURE_2D, 0); // Unbind the texture

		// register the loaded texture and associate it with the special tag string
		m_textureIDs[m_loadedTextures].ID = textureID;
		m_textureIDs[m_loadedTextures].tag = tag;
		m_loadedTextures++;

		return true;
	}

	std::cout << "Could not load image:" << filename << std::endl;

	// Error loading the image
	return false;
}

/***********************************************************
 *  BindGLTextures()
 *
 *  This method is used for binding the loaded textures to
 *  OpenGL texture memory slots.  There are up to 16 slots.
 ***********************************************************/
void SceneManager::BindGLTextures()
{
	for (int i = 0; i < m_loadedTextures; i++)
	{
		// bind textures on corresponding texture units
		glActiveTexture(GL_TEXTURE0 + i);
		glBindTexture(GL_TEXTURE_2D, m_textureIDs[i].ID);
	}
}

/***********************************************************
 *  DestroyGLTextures()
 *
 *  This method is used for freeing the memory in all the
 *  used texture memory slots.
 ***********************************************************/
void SceneManager::DestroyGLTextures()
{
	for (int i = 0; i < m_loadedTextures; i++)
	{
		glDeleteTextures(1, &m_textureIDs[i].ID); // free texture memory
	}
	m_loadedTextures = 0; // reset count
}
/***********************************************************
 *  FindTextureID()
 *
 *  This method is used for getting an ID for the previously
 *  loaded texture bitmap associated with the passed in tag.
 ***********************************************************/
int SceneManager::FindTextureID(std::string tag)
{
	int textureID = -1;
	int index = 0;
	bool bFound = false;

	while ((index < m_loadedTextures) && (bFound == false))
	{
		if (m_textureIDs[index].tag.compare(tag) == 0)
		{
			textureID = m_textureIDs[index].ID;
			bFound = true;
		}
		else
			index++;
	}

	return(textureID);
}

/***********************************************************
 *  FindTextureSlot()
 *
 *  This method is used for getting a slot index for the previously
 *  loaded texture bitmap associated with the passed in tag.
 ***********************************************************/
int SceneManager::FindTextureSlot(std::string tag)
{
	int textureSlot = -1;
	int index = 0;
	bool bFound = false;

	while ((index < m_loadedTextures) && (bFound == false))
	{
		if (m_textureIDs[index].tag.compare(tag) == 0)
		{
			textureSlot = index;
			bFound = true;
		}
		else
			index++;
	}

	return(textureSlot);
}

/***********************************************************
 *  FindMaterial()
 *
 *  This method is used for getting a material from the previously
 *  defined materials list that is associated with the passed in tag.
 ***********************************************************/
bool SceneManager::FindMaterial(std::string tag, OBJECT_MATERIAL& material)
{
	if (m_objectMaterials.size() == 0)
	{
		return(false);
	}

	int index = 0;
	bool bFound = false;
	while ((index < (int)m_objectMaterials.size()) && (bFound == false))
	{
		if (m_objectMaterials[index].tag.compare(tag) == 0)
		{
			bFound = true;
			material.ambientColor = m_objectMaterials[index].ambientColor;
			material.ambientStrength = m_objectMaterials[index].ambientStrength;
			material.diffuseColor = m_objectMaterials[index].diffuseColor;
			material.specularColor = m_objectMaterials[index].specularColor;
			material.shininess = m_objectMaterials[index].shininess;
		}
		else
		{
			index++;
		}
	}

	return(true);
}

/***********************************************************
 *  SetTransformations()
 *
 *  This method is used for setting the transform buffer
 *  using the passed in transformation values.
 ***********************************************************/
void SceneManager::SetTransformations(
	glm::vec3 scaleXYZ,
	float XrotationDegrees,
	float YrotationDegrees,
	float ZrotationDegrees,
	glm::vec3 positionXYZ)
{
	// variables for this method
	glm::mat4 modelView;
	glm::mat4 scale;
	glm::mat4 rotationX;
	glm::mat4 rotationY;
	glm::mat4 rotationZ;
	glm::mat4 translation;

	// set the scale value in the transform buffer
	scale = glm::scale(scaleXYZ);
	// set the rotation values in the transform buffer
	rotationX = glm::rotate(glm::radians(XrotationDegrees), glm::vec3(1.0f, 0.0f, 0.0f));
	rotationY = glm::rotate(glm::radians(YrotationDegrees), glm::vec3(0.0f, 1.0f, 0.0f));
	rotationZ = glm::rotate(glm::radians(ZrotationDegrees), glm::vec3(0.0f, 0.0f, 1.0f));
	// set the translation value in the transform buffer
	translation = glm::translate(positionXYZ);

	modelView = translation * rotationZ * rotationY * rotationX * scale;

	if (NULL != m_pShaderManager)
	{
		m_pShaderManager->setMat4Value(g_ModelName, modelView);
	}
}

/***********************************************************
 *  SetShaderColor()
 *
 *  This method is used for setting the passed in color
 *  into the shader for the next draw command
 ***********************************************************/
void SceneManager::SetShaderColor(
	float redColorValue,
	float greenColorValue,
	float blueColorValue,
	float alphaValue)
{
	// variables for this method
	glm::vec4 currentColor;

	currentColor.r = redColorValue;
	currentColor.g = greenColorValue;
	currentColor.b = blueColorValue;
	currentColor.a = alphaValue;

	if (NULL != m_pShaderManager)
	{
		m_pShaderManager->setIntValue(g_UseTextureName, false);
		m_pShaderManager->setVec4Value(g_ColorValueName, currentColor);
	}
}


/***********************************************************
 *  SetShaderTexture()
 *
 *  This method is used for setting the texture data
 *  associated with the passed in ID into the shader.
 ***********************************************************/
void SceneManager::SetShaderTexture(
	std::string textureTag)
{
	if (NULL != m_pShaderManager)
	{
		m_pShaderManager->setIntValue(g_UseTextureName, true);

		int textureID = -1;
		textureID = FindTextureSlot(textureTag);
		m_pShaderManager->setSampler2DValue(g_TextureValueName, textureID);
	}
}

/***********************************************************
 *  SetTextureUVScale()
 *
 *  This method is used for setting the texture UV scale
 *  values into the shader.
 ***********************************************************/
void SceneManager::SetTextureUVScale(float u, float v)
{
	if (NULL != m_pShaderManager)
	{
		m_pShaderManager->setVec2Value("UVscale", glm::vec2(u, v));
	}
}

/***********************************************************
 *  SetShaderMaterial()
 *
 *  This method is used for passing the material values
 *  into the shader.
 ***********************************************************/
void SceneManager::SetShaderMaterial(
	std::string materialTag)
{
	if (m_objectMaterials.size() > 0)
	{
		OBJECT_MATERIAL material;
		bool bReturn = false;

		bReturn = FindMaterial(materialTag, material);
		if (bReturn == true)
		{
			m_pShaderManager->setVec3Value("material.ambientColor", material.ambientColor);
			m_pShaderManager->setFloatValue("material.ambientStrength", material.ambientStrength);
			m_pShaderManager->setVec3Value("material.diffuseColor", material.diffuseColor);
			m_pShaderManager->setVec3Value("material.specularColor", material.specularColor);
			m_pShaderManager->setFloatValue("material.shininess", material.shininess);
		}
	}
}

/**************************************************************/
/*** STUDENTS CAN MODIFY the code in the methods BELOW for  ***/
/*** preparing and rendering their own 3D replicated scenes.***/
/*** Please refer to the code in the OpenGL sample project  ***/
/*** for assistance.                                        ***/
/**************************************************************/
void SceneManager::LoadSceneTextures()
{
	/*** STUDENTS - add the code BELOW for loading the textures that ***/
	/*** will be used for mapping to objects in the 3D scene. Up to  ***/
	/*** 16 textures can be loaded per scene. Refer to the code in   ***/
	/*** the OpenGL Sample for help.                                 ***/
	bool bReturn = false;

	bReturn = CreateGLTexture(
		"Utilities/textures/pavers.jpg",
		"floor");

	bReturn = CreateGLTexture(
		"Utilities/textures/gold-seamless-texture.jpg",
		"cylinder");

	bReturn = CreateGLTexture(
		"Utilities/textures/circular-brushed-gold-texture.jpg",
		"cylinder_top");

	bReturn = CreateGLTexture(
		"Utilities/textures/rustcwood.jpg",
		"plank");

	bReturn = CreateGLTexture(
		"Utilities/textures/tilesf2.jpg",
		"box");

	bReturn = CreateGLTexture(
		"Utilities/textures/stainedglass.jpg",
		"ball");

	bReturn = CreateGLTexture(
		"Utilities/textures/abstract.jpg",
		"cone");
	bReturn = CreateGLTexture(
		"Utilities/textures/screen.jpg",
		"screen");

	bReturn = CreateGLTexture(
		"Utilities/textures/woodgrain.jpg",
		"woodgrain");


	// after the texture image data is loaded into memory, the
	// loaded textures need to be bound to texture slots - there
	// are a total of 16 available slots for scene textures
	BindGLTextures();
}

/***********************************************************
 *  PrepareScene()
 *
 *  This method is used for preparing the 3D scene by loading
 *  the shapes, textures in memory to support the 3D scene 
 *  rendering
 ***********************************************************/
void SceneManager::PrepareScene()
{	// load the textures that will be used in the 3D scene
	LoadSceneTextures();
	DefineObjectMaterials();
	SetupSceneLights();

	// only one instance of a particular mesh needs to be
	// loaded in memory no matter how many times it is drawn
	// in the rendered 3D scene

	m_basicMeshes->LoadPlaneMesh();
	m_basicMeshes->LoadCylinderMesh();
	m_basicMeshes->LoadTorusMesh();
	m_basicMeshes->LoadBoxMesh();
}
/***********************************************************
 *  DefineObjectMaterials()
 *
 *  This method is used for defining the object materials
 *  that will be used in the 3D scene rendering
 ***********************************************************/
void SceneManager::DefineObjectMaterials() {
	OBJECT_MATERIAL woodgrain;
	woodgrain.ambientColor = glm::vec3(0.3f, 0.2f, 0.1f); // Warm brown ambient
	woodgrain.ambientStrength = 0.4f;
	woodgrain.diffuseColor = glm::vec3(0.6f, 0.4f, 0.2f); // Brown diffuse
	woodgrain.specularColor = glm::vec3(0.2f, 0.2f, 0.2f); // Low grey shine (varnish)
	woodgrain.shininess = 16.0f;
	woodgrain.tag = "woodgrain";
	m_objectMaterials.push_back(woodgrain);

	OBJECT_MATERIAL cylinder;
	cylinder.ambientColor = glm::vec3(0.15f, 0.15f, 0.18f);
	cylinder.ambientStrength = 0.30f;
	cylinder.diffuseColor = glm::vec3(0.25f, 0.25f, 0.30f);
	cylinder.specularColor = glm::vec3(0.05f, 0.05f, 0.05f);
	cylinder.shininess = 8.0f;
	cylinder.tag = "cylinder";
	m_objectMaterials.push_back(cylinder);

	OBJECT_MATERIAL cylinder_top;
	cylinder_top.ambientColor = glm::vec3(0.05f, 0.05f, 0.05f);
	cylinder_top.ambientStrength = 0.25f;
	cylinder_top.diffuseColor = glm::vec3(0.70f, 0.70f, 0.75f);
	cylinder_top.specularColor = glm::vec3(0.20f, 0.20f, 0.20f);
	cylinder_top.shininess = 32.0f;
	cylinder_top.tag = "cylinder_top";
	m_objectMaterials.push_back(cylinder_top);

	OBJECT_MATERIAL plank;
	plank.ambientColor = glm::vec3(0.20f, 0.15f, 0.15f);
	plank.ambientStrength = 0.35f;
	plank.diffuseColor = glm::vec3(0.70f, 0.55f, 0.55f);
	plank.specularColor = glm::vec3(0.10f, 0.10f, 0.10f);
	plank.shininess = 16.0f;
	plank.tag = "plank";
	m_objectMaterials.push_back(plank);

	OBJECT_MATERIAL box;
	box.ambientColor = glm::vec3(0.25f, 0.25f, 0.25f);
	box.ambientStrength = 0.30f;
	box.diffuseColor = glm::vec3(0.70f, 0.70f, 0.65f);
	box.specularColor = glm::vec3(0.05f, 0.05f, 0.05f);
	box.shininess = 8.0f;
	box.tag = "box";
	m_objectMaterials.push_back(box);

	OBJECT_MATERIAL ball;
	ball.ambientColor = glm::vec3(0.20f, 0.20f, 0.22f);
	ball.ambientStrength = 0.35f;
	ball.diffuseColor = glm::vec3(0.80f, 0.30f, 0.30f);
	ball.specularColor = glm::vec3(0.15f, 0.15f, 0.15f);
	ball.shininess = 24.0f;
	ball.tag = "ball";
	m_objectMaterials.push_back(ball);

	OBJECT_MATERIAL cone;
	cone.ambientColor = glm::vec3(0.10f, 0.10f, 0.12f);
	cone.ambientStrength = 0.25f;
	cone.diffuseColor = glm::vec3(0.20f, 0.20f, 0.25f);
	cone.specularColor = glm::vec3(0.05f, 0.05f, 0.05f);
	cone.shininess = 8.0f;
	cone.tag = "cone";
	m_objectMaterials.push_back(cone);

	OBJECT_MATERIAL screenMat;
	screenMat.ambientColor = glm::vec3(0.2f, 0.2f, 0.2f);
	screenMat.ambientStrength = 0.4f;
	screenMat.diffuseColor = glm::vec3(0.9f, 0.9f, 0.9f); // Bright white base
	screenMat.specularColor = glm::vec3(0.8f, 0.8f, 0.8f); // Shiny reflection
	screenMat.shininess = 50.0f;
	screenMat.tag = "screenMat";
	m_objectMaterials.push_back(screenMat);
}
void SceneManager::SetupSceneLights()
{
	m_pShaderManager->setVec3Value("lightSources[0].position", 3.0f, 14.0f, 0.0f);
	m_pShaderManager->setVec3Value("lightSources[0].ambientColor", 0.01f, 0.01f, 0.01f);
	m_pShaderManager->setVec3Value("lightSources[0].diffuseColor", 0.4f, 0.4f, 0.4f);
	m_pShaderManager->setVec3Value("lightSources[0].specularColor", 0.0f, 0.0f, 0.0f);
	m_pShaderManager->setFloatValue("lightSources[0].focalStrength", 32.0f);
	m_pShaderManager->setFloatValue("lightSources[0].specularIntensity", 0.05f);

	m_pShaderManager->setVec3Value("lightSources[1].position", -3.0f, 14.0f, 0.0f);
	m_pShaderManager->setVec3Value("lightSources[1].ambientColor", 0.01f, 0.01f, 0.01f);
	m_pShaderManager->setVec3Value("lightSources[1].diffuseColor", 0.4f, 0.4f, 0.4f);
	m_pShaderManager->setVec3Value("lightSources[1].specularColor", 0.0f, 0.0f, 0.0f);
	m_pShaderManager->setFloatValue("lightSources[1].focalStrength", 32.0f);
	m_pShaderManager->setFloatValue("lightSources[1].specularIntensity", 0.05f);

	m_pShaderManager->setVec3Value("lightSources[2].position", 0.6f, 5.0f, 6.0f);
	m_pShaderManager->setVec3Value("lightSources[2].ambientColor", 0.01f, 0.01f, 0.01f);
	m_pShaderManager->setVec3Value("lightSources[2].diffuseColor", 0.3f, 0.3f, 0.3f);
	m_pShaderManager->setVec3Value("lightSources[2].specularColor", 0.3f, 0.3f, 0.3f);
	m_pShaderManager->setFloatValue("lightSources[2].focalStrength", 12.0f);
	m_pShaderManager->setFloatValue("lightSources[2].specularIntensity", 0.5f);
}

/***********************************************************
 *  RenderScene()
 *
 *  This method is used for rendering the 3D scene by 
 *  transforming and drawing the basic 3D shapes
 ***********************************************************/
void SceneManager::RenderScene()
{
	// Per-frame timing (start of RenderScene)
	float currentFrame = glfwGetTime();
	deltaTime = currentFrame - lastFrame;
	lastFrame = currentFrame;

	// 2. Process Input (Camera move, Persp/Ortho toggle)
	processInput(m_pWindow);
	processProjectionKeys(m_pWindow);

	// Compute view
	glm::mat4 view = glm::lookAt(cameraPos, cameraPos + cameraFront, cameraUp);

	// Compute aspect from framebuffer
	int fbw = 0, fbh = 0;
	glfwGetFramebufferSize(m_pWindow, &fbw, &fbh);
	glViewport(0, 0, fbw, fbh); // keep viewport synced with framebuffer
	const float aspect = (fbh > 0) ? (float)fbw / (float)fbh : (800.0f / 600.0f);

	// Choose projection
	glm::mat4 projection;
	if (gProjMode == ProjectionMode::Perspective) {
		projection = glm::perspective(glm::radians(45.0f), aspect, 0.1f, 100.0f);
	}
	else {
		// Ortho bounds scaled to aspect to avoid stretching; camera should look directly at the object.
		const float orthoScale = 6.0f;
		const float right = orthoScale * aspect, left = -right;
		const float top = orthoScale, bottom = -top;
		projection = glm::ortho(left, right, bottom, top, 0.1f, 100.0f);
	}

	// Upload to shader
	m_pShaderManager->setMat4Value("view", view);
	m_pShaderManager->setMat4Value("projection", projection);
	// Declare transformation variables once at the top
	glm::vec3 scaleXYZ;
	float XrotationDegrees = 0.0f;
	float YrotationDegrees = 0.0f;
	float ZrotationDegrees = 0.0f;
	glm::vec3 positionXYZ;

	/// ****************************************************************
	//  THE DESK SURFACE (Plane)
	// ****************************************************************
	scaleXYZ = glm::vec3(20.0f, 1.0f, 10.0f);
	positionXYZ = glm::vec3(0.0f, 0.0f, 0.0f);
	SetTransformations(scaleXYZ, 0.0f, 0.0f, 0.0f, positionXYZ);
	SetShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
	SetShaderMaterial("woodgrain");
	SetShaderTexture("woodgrain");

	SetTextureUVScale(1.0f, 1.0f);
	m_basicMeshes->DrawPlaneMesh();


	// ****************************************************************
	//  THE LAPTOP (Composite: Box Base + Box Screen)
	// ****************************************************************

	// --- Laptop Base ---
	scaleXYZ = glm::vec3(4.0f, 0.2f, 3.0f);
	positionXYZ = glm::vec3(0.0f, 0.11f, 2.0f);
	SetTransformations(scaleXYZ, 0.0f, 0.0f, 0.0f, positionXYZ);
	SetShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
	SetShaderMaterial("cylinder"); // Use shiny material for gold laptop
	SetShaderTexture("cylinder");
	SetTextureUVScale(1.0f, 1.0f);
	m_basicMeshes->DrawBoxMesh();

	// --- Laptop Screen ---
	scaleXYZ = glm::vec3(4.0f, 3.0f, 0.1f);
	positionXYZ = glm::vec3(0.0f, 1.5f, 0.5f);
	SetTransformations(scaleXYZ, -15.0f, 0.0f, 0.0f, positionXYZ);
	SetShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
	SetShaderMaterial("cylinder_top"); // Brushed metal material
	SetShaderTexture("cylinder_top");
	m_basicMeshes->DrawBoxMesh();

	// ****************************************************************
	//  LAPTOP DISPLAY (The LCD Panel)
	// ****************************************************************

	// This box is slightly smaller than the lid and positioned 
	// just a tiny bit forward so it sits "on top" of the lid surface.

	scaleXYZ = glm::vec3(3.5f, 2.5f, 0.05f); // Slightly smaller than lid (4.0 x 3.0)

	// Position Logic: 
	// The lid is at (0.0, 1.5, 0.5) with -15 degree tilt.
	// We move this slightly forward (+Z) and up (+Y) to match the tilt.
	positionXYZ = glm::vec3(0.0f, 1.52f, 0.56f);

	SetTransformations(scaleXYZ, -15.0f, 0.0f, 0.0f, positionXYZ); // Match rotation

	SetShaderColor(1.0f, 1.0f, 1.0f, 1.0f); // Pure white so texture colors show correctly

	SetShaderMaterial("screenMat"); // Bright material
	SetShaderTexture("screen");     // The screen image you loaded

	m_basicMeshes->DrawBoxMesh();

	// --- Monitor Base (Flat stand) ---
	scaleXYZ = glm::vec3(2.0f, 0.1f, 1.5f);
	positionXYZ = glm::vec3(-7.5f, 0.06f, 3.0f); // Far left of desk
	// Rotated 30 degrees to face the center
	SetTransformations(scaleXYZ, 0.0f, 30.0f, 0.0f, positionXYZ);
	SetShaderColor(0.2f, 0.2f, 0.2f, 1.0f); // Dark Grey
	SetShaderMaterial("cylinder"); // Use existing dark material
	SetShaderTexture("cylinder");  // Dark texture
	m_basicMeshes->DrawBoxMesh();

	// --- Monitor Neck (Cylinder) ---
	scaleXYZ = glm::vec3(0.25f, 2.0f, 0.25f);
	positionXYZ = glm::vec3(-7.5f, 1.0f, 2.8f);
	SetTransformations(scaleXYZ, 0.0f, 0.0f, 0.0f, positionXYZ);
	SetShaderColor(0.2f, 0.2f, 0.2f, 1.0f);
	SetShaderTexture("cylinder");
	SetTextureUVScale(1.0f, 1.0f);
	m_basicMeshes->DrawCylinderMesh();

	// --- Monitor Housing (The Bezel/Back) ---
	scaleXYZ = glm::vec3(4.0f, 2.5f, 0.2f);
	positionXYZ = glm::vec3(-7.5f, 2.0f, 3.0f);
	SetTransformations(scaleXYZ, 0.0f, 30.0f, 0.0f, positionXYZ); // Match rotation
	SetShaderColor(0.2f, 0.2f, 0.2f, 1.0f);
	SetShaderMaterial("cylinder");
	m_basicMeshes->DrawBoxMesh();

	// --- Monitor Screen (The Lit Part) ---
	scaleXYZ = glm::vec3(3.6f, 2.1f, 0.05f);
	// Calculated Offset: We shift X and Z slightly so the screen sits 
	// exactly on the front face of the rotated housing.
	positionXYZ = glm::vec3(-7.43f, 2.0f, 3.11f);
	SetTransformations(scaleXYZ, 0.0f, 30.0f, 0.0f, positionXYZ); // Match rotation

	SetShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
	SetShaderMaterial("screenMat"); // Bright glowing material
	SetShaderTexture("screen");     // The screen image
	m_basicMeshes->DrawBoxMesh();

	// ****************************************************************
	// THE COFFEE MUG 
	// ****************************************************************

	// --- Mug Body ---
	scaleXYZ = glm::vec3(1.0f, 2.0f, 1.0f);
	positionXYZ = glm::vec3(3.5f, 0.0f, 3.0f);
	SetTransformations(scaleXYZ, 0.0f, 0.0f, 0.0f, positionXYZ);
	SetShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

	SetShaderMaterial("ball"); // Use the 'ball' material (red/shiny)
	SetShaderTexture("ball");
	SetTextureUVScale(1.0f, 1.0f);
	m_basicMeshes->DrawCylinderMesh();

	// --- Mug Handle ---
	scaleXYZ = glm::vec3(0.5f, 0.5f, 0.5f);
	positionXYZ = glm::vec3(4.3f, 1.0f, 3.0f);
	SetTransformations(scaleXYZ, 0.0f, 90.0f, 0.0f, positionXYZ);
	SetShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

	SetShaderMaterial("ball");
	SetShaderTexture("ball");
	m_basicMeshes->DrawTorusMesh();

	// --- Pen 1 (The original one, slightly tilted) ---
	scaleXYZ = glm::vec3(0.1f, 2.5f, 0.1f);   // Thin cylinder
	positionXYZ = glm::vec3(3.5f, 1.5f, 3.2f); // Center of mug
	// Tilt slightly randomly (X=5, Z=-5)
	SetTransformations(scaleXYZ, 5.0f, 0.0f, -5.0f, positionXYZ);
	SetShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
	SetShaderMaterial("cone");
	SetShaderTexture("cone"); // Abstract texture
	m_basicMeshes->DrawCylinderMesh();

	// --- Pen 2 (Tilted left and forward, Gold texture) ---
	scaleXYZ = glm::vec3(0.1f, 2.5f, 0.1f);
	positionXYZ = glm::vec3(3.4f, 1.5f, 3.1f); // Shifted position
	// Stronger tilt (X=15, Z=10)
	SetTransformations(scaleXYZ, 15.0f, 45.0f, 10.0f, positionXYZ);
	SetShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
	// Use a different material/texture for variety
	SetShaderMaterial("cylinder");
	SetShaderTexture("cylinder"); // Gold texture
	m_basicMeshes->DrawCylinderMesh();

	// --- Pen 3 (Tilted right and back, Red tint) ---
	scaleXYZ = glm::vec3(0.1f, 2.4f, 0.1f); // Slightly shorter
	positionXYZ = glm::vec3(3.6f, 1.45f, 3.3f); // Shifted position
	// Tilt opposite direction (X=-10, Z=-15)
	SetTransformations(scaleXYZ, -10.0f, -30.0f, -15.0f, positionXYZ);
	// Tint this one red
	SetShaderColor(1.0f, 0.5f, 0.5f, 1.0f);
	SetShaderMaterial("cone");
	SetShaderTexture("cone");
	m_basicMeshes->DrawCylinderMesh();


	// ****************************************************************
	//  STACK OF NOTEBOOKS (Boxes)
	// ****************************************************************

	// --- Notebook 1 (Bottom) ---
	scaleXYZ = glm::vec3(2.5f, 0.15f, 3.5f);
	positionXYZ = glm::vec3(-4.0f, 0.08f, 3.0f);
	SetTransformations(scaleXYZ, 0.0f, 20.0f, 0.0f, positionXYZ);
	SetShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

	SetShaderMaterial("plank"); // Wood/Paper material
	SetShaderTexture("plank");
	SetTextureUVScale(1.0f, 1.0f);
	m_basicMeshes->DrawBoxMesh();

	// --- Notebook 2 (Top) ---
	scaleXYZ = glm::vec3(2.3f, 0.15f, 3.3f);
	positionXYZ = glm::vec3(-4.0f, 0.24f, 3.0f);
	SetTransformations(scaleXYZ, 0.0f, 35.0f, 0.0f, positionXYZ);

	SetShaderMaterial("box"); // Basic box material
	SetShaderTexture("box");
	m_basicMeshes->DrawBoxMesh();
}