///////////////////////////////////////////////////////////////////////////////
// scenemanager.h
// ============
// manage the loading and rendering of 3D scenes
//
//  AUTHOR: Brian Battersby - SNHU Instructor / Computer Science
//	Created for CS-330-Computational Graphics and Visualization, Nov. 1st, 2023
///////////////////////////////////////////////////////////////////////////////
#define GLM_ENABLE_EXPERIMENTAL
#pragma once

#include "ShaderManager.h"
#include "ShapeMeshes.h"
#include "ViewManager.h"
#include <string>
#include <vector>

/***********************************************************
 *  SceneManager
 *
 *  This class contains the code for preparing and rendering
 *  3D scenes, including the shader settings.
 *
 *  ENHANCEMENT NOTE (Milestone Two, CS 499): this class no
 *  longer stores a GLFWwindow* or does any camera/input work
 *  of its own. Previously it held its own window pointer
 *  (SetWindow()) and RenderScene() recomputed a completely
 *  separate copy of the view/projection matrices from a set
 *  of camera globals defined at the top of SceneManager.cpp,
 *  duplicating ViewManager's job. SceneManager now takes a
 *  pointer to the ViewManager that owns the camera and asks
 *  it for the current view/projection matrices once per
 *  frame, restoring a clean separation of responsibility:
 *  ViewManager owns the camera/view, SceneManager owns scene
 *  content.
 ***********************************************************/
class SceneManager
{
public:
	// constructor
	SceneManager(ShaderManager *pShaderManager, ViewManager* pViewManager);
	// destructor
	~SceneManager();

	// The following methods are for the students to 
	// customize for their own 3D scene
	void PrepareScene();
	void DefineObjectMaterials();
	void SetupSceneLights();
	void RenderScene();

private:
	// maximum number of textures that can be loaded at once -
	// OpenGL implementations commonly guarantee at least 16
	// active texture units, so this is our hard capacity limit
	static const int MAX_TEXTURES = 16;

	struct TEXTURE_INFO
	{
		std::string tag;
		uint32_t ID;
	};

	struct OBJECT_MATERIAL
	{
		float ambientStrength;
		glm::vec3 ambientColor;
		glm::vec3 diffuseColor;
		glm::vec3 specularColor;
		float shininess;
		std::string tag;
	};

	// pointer to shader manager object
	ShaderManager* m_pShaderManager;
	// pointer to the view manager that owns the camera/view - used to
	// pull the current view/projection matrices each frame instead of
	// SceneManager maintaining its own duplicate camera state
	ViewManager* m_pViewManager;
	// pointer to basic shapes object
	ShapeMeshes* m_basicMeshes;
	// total number of loaded textures
	int m_loadedTextures;
	// loaded textures info
	TEXTURE_INFO m_textureIDs[MAX_TEXTURES];
	// defined object materials
	std::vector<OBJECT_MATERIAL> m_objectMaterials;

	// load texture images and convert to OpenGL texture data
	bool CreateGLTexture(const char* filename, std::string tag);
	// bind loaded OpenGL textures to slots in memory
	void BindGLTextures();
	// free the loaded OpenGL textures
	void DestroyGLTextures();
	// find a loaded texture by tag
	int FindTextureID(std::string tag);
	int FindTextureSlot(std::string tag);
	// find a defined material by tag
	bool FindMaterial(std::string tag, OBJECT_MATERIAL& material);

	// set the transformation values 
	// into the transform buffer
	void SetTransformations(
		glm::vec3 scaleXYZ,
		float XrotationDegrees,
		float YrotationDegrees,
		float ZrotationDegrees,
		glm::vec3 positionXYZ);

	// set the color values into the shader
	void SetShaderColor(
		float redColorValue,
		float greenColorValue,
		float blueColorValue,
		float alphaValue);

	// set the texture data into the shader
	void SetShaderTexture(
		std::string textureTag);

	// set the UV scale for the texture mapping
	void SetTextureUVScale(
		float u, float v);

	// set the object material into the shader
	void SetShaderMaterial(
		std::string materialTag);
	void LoadSceneTextures();
};
