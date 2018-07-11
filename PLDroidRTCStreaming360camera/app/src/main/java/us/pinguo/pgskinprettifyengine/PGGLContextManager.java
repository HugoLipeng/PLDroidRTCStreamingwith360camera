//
//  PGGLContextManager.java
//  PGSkinPrettifyEngine
//
//  Created by ZhangJingQi on 16/5/26.
//  Copyright © 2016-2017年 Chengdu PinGuo Technology Co., Ltd. All rights reserved.
//

package us.pinguo.pgskinprettifyengine;

import android.opengl.GLES11Ext;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL11;

public class PGGLContextManager
{
	private static String LOG_TAG = "PGGLContextManager";
	private GL11 m_Gl;
	private EGL10 m_EglInstance;
	private EGLConfig m_EglConfig;
	private EGLDisplay m_EglDisplay;
	private EGLContext m_EglContext;
	private EGLSurface m_EglPBSurface;
    private EGLSurface m_ViewSurface;

	private EGLConfig[] m_aEglConfigs = new EGLConfig[1];
	private int[] m_aEglNumConfig = new int[1];
	private int[] m_aEglVersion = new int[2];
	private int[] m_aContextAttribList = { 0x3098, 2, EGL10.EGL_NONE };
	private int[] m_aPbufferAttribList = { EGL10.EGL_WIDTH, 32, EGL10.EGL_HEIGHT, 32, EGL10.EGL_NONE };
	private int[] m_aEglAttrib = { EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT, EGL10.EGL_RENDERABLE_TYPE, 4,
	        EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_ALPHA_SIZE, 8,
	        EGL10.EGL_NONE };


	public void initGLContext(int iGLVersion)
	{

		m_EglInstance = (EGL10) EGLContext.getEGL();

		m_EglDisplay = m_EglInstance.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
		if (m_EglDisplay == EGL10.EGL_NO_DISPLAY)
		{
			 Log.e(LOG_TAG, "eglGetDisplay Error:" + GLUtils.getEGLErrorString(m_EglInstance.eglGetError()));
		}

		if (!m_EglInstance.eglInitialize(m_EglDisplay, m_aEglVersion))
		{
			 Log.e(LOG_TAG, "eglInitialize Error:" +
			 GLUtils.getEGLErrorString(m_EglInstance.eglGetError()));
		}

		if (!m_EglInstance.eglChooseConfig(m_EglDisplay, m_aEglAttrib, m_aEglConfigs, 1, m_aEglNumConfig))
		{
			 Log.e(LOG_TAG, "eglChooseConfig Error:" +
			 GLUtils.getEGLErrorString(m_EglInstance.eglGetError()));
		}

		m_EglConfig = m_aEglConfigs[0];
		if (m_EglConfig == null)
		{
			 Log.e(LOG_TAG, "eglConfig not initialized");
		}
        m_aContextAttribList[1] = 3;
		m_EglContext = m_EglInstance.eglCreateContext(m_EglDisplay, m_EglConfig, EGL10.EGL_NO_CONTEXT,
		        m_aContextAttribList);
		if (m_EglContext == EGL10.EGL_NO_CONTEXT)
		{

            m_aContextAttribList[1] = 2;
            m_EglContext = m_EglInstance.eglCreateContext(m_EglDisplay, m_EglConfig, EGL10.EGL_NO_CONTEXT,
                    m_aContextAttribList);
            if (m_EglContext == EGL10.EGL_NO_CONTEXT)
            {
                Log.e(LOG_TAG, "eglCreateContext Error:" +
                        GLUtils.getEGLErrorString(m_EglInstance.eglGetError()));
            }
		}

		m_Gl = (GL11) m_EglContext.getGL();

		checkEglError();
	}

    public void releaseContext()
    {
        if (!m_EglInstance.eglMakeCurrent(m_EglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_CONTEXT))
        {
            Log.e(LOG_TAG, "eglMakeCurrent Release Error:" +
                    GLUtils.getEGLErrorString(m_EglInstance.eglGetError()));
        }

        if (!m_EglInstance.eglDestroyContext(m_EglDisplay, m_EglContext))
        {
            Log.e(LOG_TAG, "eglDestroyContext Error:" +
                    GLUtils.getEGLErrorString(m_EglInstance.eglGetError()));
        }

        if (m_EglDisplay != null)
        {
            m_EglInstance.eglTerminate(m_EglDisplay);
            m_EglDisplay = null;
        }

        m_EglContext = null;

    }

    public void addSurface(SurfaceHolder surface)
    {
        if (surface == null) {
            m_EglPBSurface = m_EglInstance.eglCreatePbufferSurface(m_EglDisplay, m_EglConfig, m_aPbufferAttribList);
            if (m_EglPBSurface == EGL10.EGL_NO_SURFACE) {
                Log.e(LOG_TAG, "eglCreatePbufferSurface Error:" +
                        GLUtils.getEGLErrorString(m_EglInstance.eglGetError()));
            }
        }
        else {
            int[] surfaceAttribs = {
                    EGL10.EGL_NONE
            };
            m_ViewSurface = m_EglInstance.eglCreateWindowSurface(m_EglDisplay, m_EglConfig, surface, surfaceAttribs);
            checkEglError();
        }
    }

    public void presentSurface()
    {
        if (m_ViewSurface != null) {
            if (!m_EglInstance.eglSwapBuffers(m_EglDisplay, m_ViewSurface)) {
                Log.e(LOG_TAG, "cannot swap buffers!");
            }
            checkEglError();
        }
    }

    public void releaseSurface()
    {
        if (!m_EglInstance.eglMakeCurrent(m_EglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_CONTEXT))
        {
            Log.e(LOG_TAG, "eglMakeCurrent release Error:" + m_EglInstance.eglGetError());
        }

        if(m_EglPBSurface != null) {
            if (!m_EglInstance.eglDestroySurface(m_EglDisplay, m_EglPBSurface)) {
                Log.e(LOG_TAG, "eglDestroySurface Error:" +
                        GLUtils.getEGLErrorString(m_EglInstance.eglGetError()));
            }
            m_EglPBSurface = null;
        }

        if(m_ViewSurface != null)
        {
            if (!m_EglInstance.eglDestroySurface(m_EglDisplay, m_ViewSurface))
            {
                Log.e(LOG_TAG, "eglDestroySurface Error:" + m_EglInstance.eglGetError());
            }
            m_ViewSurface = null;
        }
    }

	private void checkGlError()
	{
		final int error = m_Gl.glGetError();
		if (error != GL11.GL_NO_ERROR)
		{
			 Log.e(LOG_TAG, "GL error = 0x" + Integer.toHexString(error));
		}
	}

	private void checkEglError()
	{
		final int error = m_EglInstance.eglGetError();
		if (error != EGL10.EGL_SUCCESS)
		{
			 Log.e(LOG_TAG, "EGL error = 0x" + Integer.toHexString(error));
		}
	}

    public void activateOurGLContext()
	{
        if (m_ViewSurface == null) {
            if (!m_EglInstance.eglMakeCurrent(m_EglDisplay, m_EglPBSurface, m_EglPBSurface, m_EglContext)) {
                Log.e(LOG_TAG, "eglMakeCurrent Error:" +
                        GLUtils.getEGLErrorString(m_EglInstance.eglGetError()));
            }
        }
        else
        {
            if (!m_EglInstance.eglMakeCurrent(m_EglDisplay, m_ViewSurface, m_ViewSurface, m_EglContext)) {
                Log.e(LOG_TAG, "eglMakeCurrent Error:" +
                        GLUtils.getEGLErrorString(m_EglInstance.eglGetError()));
            }
        }
	}

    public void deactivateOurGLContext()
	{
		if(m_EglDisplay != null && m_EglPBSurface != null)
		{
			if (!m_EglInstance.eglMakeCurrent(EGL10.EGL_NO_DISPLAY, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT))
			{
				Log.e(LOG_TAG, "eglMakeCurrent Error:" +
						GLUtils.getEGLErrorString(m_EglInstance.eglGetError()));
			}
		}
	}


    public int createGLExtTexture()
    {
        int[] textures = new int[1];
        m_Gl.glGenTextures(1, textures, 0);
        checkGlError();
        int texId = textures[0];

        if (texId <= 0)
        {
            throw new RuntimeException("invalid GL texture id generated");
        }

        m_Gl.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId);
        checkGlError();

        m_Gl.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        m_Gl.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        m_Gl.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP_TO_EDGE);
        m_Gl.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP_TO_EDGE);
        checkGlError();
        return texId;
    }

    public void deleteGLExtTexture(int iTextureID)
    {
        m_Gl.glActiveTexture(GL11.GL_TEXTURE0);
        int[] aTextures = {iTextureID};
        m_Gl.glDeleteTextures(1, aTextures, 0);
        checkGlError();
    }

}



