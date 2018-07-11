//
//  PGSkinPrettifyEngine.java
//  PGSkinPrettifyEngine
//
//  Created by ZhangJingQi on 16/5/26.
//  Copyright © 2016-2017年 Chengdu PinGuo Technology Co., Ltd. All rights reserved.
//

package us.pinguo.pgskinprettifyengine;

import java.nio.ByteBuffer;

import android.content.Context;
import android.opengl.Matrix;

public class PGSkinPrettifyEngine
{
	private float[] m_pModelMatrix = new float[16];
    private float[] m_pViewMatrix = new float[16];
    private float[] m_pProjectionMatrix = new float[16];

    private float[] CalcFinalMatrix() {
        float[] pMVPMatrix = new float[16];
        Matrix.multiplyMM(pMVPMatrix, 0, m_pViewMatrix, 0, m_pModelMatrix, 0);
        Matrix.multiplyMM(pMVPMatrix, 0, m_pProjectionMatrix, 0, pMVPMatrix, 0);
        return pMVPMatrix;
    }

    public enum PG_SoftenAlgorithm
    {
        PG_SoftenAlgorithmDenoise(0),        // 降噪磨皮
        PG_SoftenAlgorithmContrast(1),       // 细节保留磨皮
        PG_SoftenAlgorithmSkinDetect(2);     // 带肤色检测的磨皮

        private int index;

        PG_SoftenAlgorithm(int idx) {
            this.index = idx;
        }

        public int getIndex()
        {
            return index;
        }
    }


    public enum PG_Orientation {
        PG_OrientationNormal(0),            /* 原样输出 */
        PG_OrientationRightRotate90(1),     /* 右旋90度输出（注意改变输出宽高） */
        PG_OrientationRightRotate180(2),    /* 右旋180度输出 */
        PG_OrientationRightRotate270(3),    /* 右旋270度输出（注意改变输出宽高） */
        PG_OrientationFlippedMirrored(4),   /* 翻转并镜像输出 */
        PG_OrientationFlipped(5),           /* 上下翻转输出 */
        PG_OrientationMirrored(6),          /* 左右镜像输出 */
        PG_OrientationRightRotate90Mirrored(7), /*右旋90并左右镜像输出*/
        PG_OrientationRightRotate180Mirrored(8), /*右旋180并左右镜像输出*/
        PG_OrientationRightRotate270Mirrored(9); /*右旋270并左右镜像输出*/

        private int index;

        PG_Orientation(int idx) {
            this.index = idx;
        }

        public float[] getMatrix() {
            float[] pModelMatrix = new float[16];
            switch (index) {
                case 0: {
                    Matrix.setIdentityM(pModelMatrix, 0);
                }
                break;

                case 1: {
                    Matrix.setIdentityM(pModelMatrix, 0);
                    Matrix.rotateM(pModelMatrix, 0, 90, 0, 0, 1);
                }
                break;

                case 2: {
                    Matrix.setIdentityM(pModelMatrix, 0);
                    Matrix.rotateM(pModelMatrix, 0, 180, 0, 0, 1);
                }
                break;

                case 3: {
                    Matrix.setIdentityM(pModelMatrix, 0);
                    Matrix.rotateM(pModelMatrix, 0, 270, 0, 0, 1);
                }
                break;

                case 4: {
                    Matrix.setIdentityM(pModelMatrix, 0);
                    Matrix.rotateM(pModelMatrix, 0, 180, 0, 0, 1);
                }
                break;

                case 5: {
                    Matrix.setIdentityM(pModelMatrix, 0);
                    Matrix.rotateM(pModelMatrix, 0, 180, 1, 0, 0);
                }
                break;

                case 6: {
                    Matrix.setIdentityM(pModelMatrix, 0);
                    Matrix.rotateM(pModelMatrix, 0, 180, 0, 1, 0);
                }
                break;

                case 7: {
                    Matrix.setIdentityM(pModelMatrix, 0);
                    Matrix.rotateM(pModelMatrix, 0, 90, 0, 0, 1);
                    Matrix.rotateM(pModelMatrix, 0, 180, 1, 0, 0);
                }
                break;

                case 8: {
                    Matrix.setIdentityM(pModelMatrix, 0);
                    Matrix.rotateM(pModelMatrix, 0, 180, 0, 0, 1);
                    Matrix.rotateM(pModelMatrix, 0, 180, 0, 1, 0);
                }
                break;

                case 9: {
                    Matrix.setIdentityM(pModelMatrix, 0);
                    Matrix.rotateM(pModelMatrix, 0, 270, 0, 0, 1);
                    Matrix.rotateM(pModelMatrix, 0, 180, 1, 0, 0);
                }
                break;
            }

            return pModelMatrix;
        }
    }


    public enum PG_PixelFormat
	{
		PG_Pixel_RGBA(0),		/*输出 RGBA 格式 */
		PG_Pixel_BGRA(1),		/*输出 BGRA 格式 */
		PG_Pixel_NV21(2),		/*输出 YUV420 格式(NV21格式) */
        PG_Pixel_YV12(3),       /*输出 YUV420 格式(YV12格式) */
        PG_Pixel_I420(4);       /*输出 YUV420 格式(I420格式) */
		private int index;

		PG_PixelFormat(int idx)
		{
			this.index = idx;
		}

		public int getIndex()
		{
			return index;
		}
	}

    public enum PG_BlendMode {
        PG_BlendNormal(1),      /* 正常 */
        PG_BlendScreen(2),      /* 滤色 */
        PG_BlendDifference(3),  /* 差值 */
        PG_BlendMultiply(4),    /* 排除 */
        PG_BlendOverlay(5);     /* 叠加 */
        private int index;

        PG_BlendMode(int idx) {
            this.index = idx;
        }

        public int getIndex() {
            return index;
        }
    }

	static
	{
		System.loadLibrary("PGSkinPrettifyEngine");
	}
	
	private long m_pEngine = 0;

	public native static long NewPGSkinPrettifyEngine(Context contxt, String key, boolean bInitEGL);
	public native static boolean RunEngine(long pEngine);
	public native static boolean DestroyEngine(long pEngine);

    public native static boolean SetInputFrameByNV21(long pEngine, byte []pFrameData, int iFrameWidth, int iFrameHeight);
    public native static boolean SetInputFrameByYV12(long pEngine, byte []pFrameData, int iFrameWidth, int iFrameHeight);
    public native static boolean SetInputFrameByI420(long pEngine, byte []pFrameData, int iFrameWidth, int iFrameHeight);
    public native static boolean SetInputFrameByTexture(long pEngine, int iTextureID, int iTextureWidth, int iTextureHeight);
    public native static boolean SetMatrixForAdjustInput(long pEngine, float[] pMatrix);
	public native static boolean SetMatrixForAdjustOutput(long pEngine, float[] pMatrix);
    public native static boolean SetMatrixForAdjustDisplay(long pEngine, float[] pMatrix);
    public native static boolean SetSkinSoftenAlgorithm(long pEngine, int iAlgorithm);
    public native static boolean SetColorFilterByName(long pEngine, String pName);
    public native static boolean SetColorFilterStrength(long pEngine, int iStrength);

	public native static boolean SetSizeForAdjustInput(long pEngine, int iAdjustWidth, int iAdjustHeight);
	public native static boolean SetSkinSoftenStrength(long pEngine, int iSoftenStrength);
	public native static boolean SetSkinColor(long pEngine, float fPinking, float fWhitening, float fRedden);
	public native static boolean SetOutputFormat(long pEngine, int eOutFormat);
	public native static boolean SetDisplayMirroredEnable(long pEngine, boolean bDislpayMirrored);
    public native static boolean SetWatermarkStrength(long pEngine, int iBlendStrength);
    public native static boolean SetWatermarkByPath(long pEngine, String pImagePath, int iMode);
    public native static boolean SetParamForAdjustWatermark(long pEngine, float fLeft, float fTop, float fWidth, float fHeight, float fFlipped, float fMirrored);

	public native static boolean GetOutputToScreen(long pEngine, int iScreenWidth, int iScreenHeight);
	public native static ByteBuffer GetSkinPrettifyResult(long pEngine);
    public native static int GetActualOutputWidth(long pEngine);
    public native static int GetActualOutputHeight(long pEngine);
    public native static int GetOutputTextureID(long pEngine);

    public native static ByteBuffer GetSkinPrettifyResultByEGLImage(long pEngine);

	/*
	 描述：初始化引擎
	 返回值：成功返回 true，失败或已经初始化过了返回 false
	 参数：bInitEGL - 是否在 Native 层自己初始化 EGLContex
	*/
	public boolean InitialiseEngine(Context context, String key, boolean bInitEGL)
	{
		m_pEngine = NewPGSkinPrettifyEngine(context, key, bInitEGL);
		if (m_pEngine == 0)
        {
	        return false;
        }
        Matrix.setIdentityM(m_pModelMatrix, 0);
        Matrix.setIdentityM(m_pViewMatrix, 0);
        Matrix.setIdentityM(m_pProjectionMatrix, 0);
		return true;
	}



	/*
	 描述：销毁引擎
	 返回值：无
	 参数：无
	 */
	public void DestroyEngine()
	{
		DestroyEngine(m_pEngine);
	}

	/*
	 描述：根据所设置的参数，运行引擎
	 返回值：成功返回 YES, 失败返回 NO
	 参数：无
	 */
	public void RunEngine()
	{
		RunEngine(m_pEngine);
	}

	/*
	 描述：设置输入帧
	 返回值：成功返回 true，失败返回 false
	 参数：iTextureID - 相机回调所给的预览帧，iTextureWidth - 帧宽度，iTextureHeight - 帧高度
	 */
    public boolean SetInputFrameByTexture(int iTextureID, int iTextureWidth, int iTextureHeight)
    {
        return SetInputFrameByTexture(m_pEngine, iTextureID, iTextureWidth, iTextureHeight);
    }

    /*
     描述：设置输入帧
     返回值：成功返回 true，失败返回 false
     参数：pFrameData - 相机回调所给的预览帧，iFrameWidth - 帧宽度，iFrameHeight - 帧高度
     */
    public boolean SetInputFrameByNV21(byte []pFrameData, int iFrameWidth, int iFrameHeight)
    {
        return SetInputFrameByNV21(m_pEngine, pFrameData, iFrameWidth, iFrameHeight);
    }

    /*
     描述：设置输入帧
     返回值：成功返回 true，失败返回 false
     参数：pFrameData - 相机回调所给的预览帧，iFrameWidth - 帧宽度，iFrameHeight - 帧高度
     */
    public boolean SetInputFrameByYV12(byte []pFrameData, int iFrameWidth, int iFrameHeight)
    {
        return SetInputFrameByYV12(m_pEngine, pFrameData, iFrameWidth, iFrameHeight);
    }

    /*
     描述：设置输入帧
     返回值：成功返回 true，失败返回 false
     参数：pFrameData - 相机回调所给的预览帧，iFrameWidth - 帧宽度，iFrameHeight - 帧高度
     */
    public boolean SetInputFrameByI420(byte []pFrameData, int iFrameWidth, int iFrameHeight)
    {
        return SetInputFrameByI420(m_pEngine, pFrameData, iFrameWidth, iFrameHeight);
    }

	/*
	 描述：设置一个方向，用于校正输入的预览帧
	 返回值：成功返回 true，失败返回 false
	 参数：eAdjustInputOrient - 方向值
	 */
    public boolean SetOrientForAdjustInput(PG_Orientation eAdjustInputOrient) {
        m_pModelMatrix = eAdjustInputOrient.getMatrix();
        return SetMatrixForAdjustInput(m_pEngine, CalcFinalMatrix());
    }

	/*
	 描述：设置一个尺寸，用于调整输入帧的宽高，也是最终输出帧的宽高
	 返回值：成功返回 true，失败返回 false
	 参数：iAdjustWidth - 输出帧宽度，iAdjustHeight - 输出帧高度
	 */
    public boolean SetSizeForAdjustInput(int iAdjustWidth, int iAdjustHeight)
    {
        return SetSizeForAdjustInput(m_pEngine, iAdjustWidth, iAdjustHeight);
    }

	/*
	 描述：设置美肤步骤中磨皮的强度
	 返回值：成功返回 true，失败返回 false
	 参数：iSoftenStrength - 磨皮强度，范围 0 - 100
	 */
    public boolean SetSkinSoftenStrength(int iSoftenStrength)
    {
        return SetSkinSoftenStrength(m_pEngine, iSoftenStrength);
    }

    /*
     描述：设置一个矩阵用于控制显示画面的变换
     返回值：成功返回 true
     参数：pMatrix - MVP矩阵
     */
    public boolean SetMatrixForAdjustDisplay(float[] pMatrix)
    {
        return SetMatrixForAdjustDisplay(m_pEngine, pMatrix);
    }

    /*
     描述：设置美肤算法
     返回值：成功返回 true
     参数：eSoftenAlgorithm - 美肤算法类型
     */
    public boolean SetSkinSoftenAlgorithm(PG_SoftenAlgorithm iAlgorithm)
    {
        return SetSkinSoftenAlgorithm(m_pEngine, iAlgorithm.getIndex());
    }

    /*
     描述：设置调色滤镜
     返回值：成功返回 true
     参数：pName - 滤镜名称
     */
    public boolean SetColorFilterByName(String pName)
    {
        return SetColorFilterByName(m_pEngine, pName);
    }

    /*
     描述：设置调色滤镜强度
     返回值：成功返回 true
     参数：iStrength - 调色滤镜强度，范围 0 - 100
     */
    public boolean SetColorFilterStrength(int iStrength)
    {
        return SetColorFilterStrength(m_pEngine, iStrength);
    }


	/*
	 描述：设置美肤步骤中的肤色调整参数
	 返回值：成功返回 true，失败返回 false
	 参数：fPinking - 粉嫩程度， fWhitening - 白晰程度，fRedden - 红润程度，范围都是0.0 - 1.0
	 */
    public boolean SetSkinColor(float fPinking, float fWhitening, float fRedden)
    {
        return SetSkinColor(m_pEngine, fPinking, fWhitening, fRedden);
    }

	/*
	 描述：设置美肤结果的输出方向
	 返回值：成功返回 true，失败返回 false
	 参数：eOutputOrient - 方向值
	 */
    public boolean SetOutputOrientation(PG_Orientation eOutputOrient)
    {
        return SetMatrixForAdjustOutput(m_pEngine, eOutputOrient.getMatrix());
    }

	/*
	 描述：设置美肤结果的输出格式
	 返回值：成功返回 true，失败返回 false
	 参数：eOutFormat - 输出的色彩格式
	 */
    public boolean SetOutputFormat(PG_PixelFormat eOutFormat)
    {
        return SetOutputFormat(m_pEngine, eOutFormat.getIndex());
    }

	/*
	 描述：将显示内容左右镜像
	 返回值：成功返回 true，失败返回 false
	 参数：bDislpayMirrored - 为 true 时显示内容会左右镜像
	 */
    public boolean SetDisplayMirroredEnable(boolean bDislpayMirrored)
    {
        return SetDisplayMirroredEnable(m_pEngine, bDislpayMirrored);
    }

    /*
     描述：从路径设置水印图像，支持 jpeg 和 png
     返回值：成功返回 true，失败返回 false
     参数：pImagePath - 图像路径，iMode - 水印混合模式
     */
    public boolean SetWatermarkByPath(String pImagePath, PG_BlendMode iMode) {
        return SetWatermarkByPath(m_pEngine, pImagePath, iMode.getIndex());
    }

    /*
     描述：设置水印的位置及翻转和镜像参数，坐标系是左下角为原点，横向为x轴，纵向为y轴，范围均为 0 - 1
     返回值：成功返回 true，失败返回 false
     参数：fLeft, fTop - 左上角坐标， fWidth, fHeight - 宽和高， fFlipped, fMirrored - 上下翻转和左右镜像
     */
    public boolean SetParamForAdjustWatermark(float fLeft, float fTop, float fWidth, float fHeight, float fFlipped, float fMirrored) {
        return SetParamForAdjustWatermark(m_pEngine, fLeft, fTop, fWidth, fHeight, fFlipped, fMirrored);
    }

    /*
     描述：设置水印不透明度
     返回值：成功返回 true，失败返回 false
     参数：iBlendStrength - 水印不透明度，范围 0 - 100
     */
    public boolean SetWatermarkStrength(int iBlendStrength) {
        return SetWatermarkStrength(m_pEngine, iBlendStrength);
    }
	/*
	 描述：将美肤结果刷新到 Surface
	 返回值：成功返回 true，失败返回 false
	 参数：iScreenWidth - Surface的宽，iScreenHeight - Surface的高
	 */
    public boolean GetOutputToScreen(int iScreenWidth, int iScreenHeight)
    {
        return GetOutputToScreen(m_pEngine, iScreenWidth, iScreenHeight);
    }

	/*
	 描述：手动获取美肤结果，支持OpenGL ES 3.0的机型使用PBO的方式读取，否则自动退化为OpenGL ES 2.0的glReadPixels
	 返回值：用于存放结果的缓冲区，数据格式为初始化美肤引擎所设置的格式
	 参数：无 
	 */
	public ByteBuffer SkinSoftenGetResult()
	{
		return GetSkinPrettifyResult(m_pEngine);
	}

    /*
	 描述：基于Android graphics buffer快速获取GPU结果此方法获取结果十分高效，但兼容性和稳定性在部分设备上较差，上层需要根据实际机型适配
	 返回值：null: 表示结果获取失败(可能设备不支持EGLImage) 非空:获取成功,部分机型也可能返回数据全为0,根据实际机型适配
	 备注：EGLImage有个通病前几帧(通常前两帧)数据全为0，部分机型(特别是x86机型)支持得不够好，另外android 6.0后已经不能支持了
	 */
    public ByteBuffer SkinSoftenGetResultByEGLImage(){return GetSkinPrettifyResultByEGLImage(m_pEngine);}

    /*
     描述：获取实际输出宽高，如果所设置的输入宽高大于了GPU支持的宽高，引擎内部会进行裁剪，可以使用此接口获取实际的输出宽高
     返回值：实际输出帧的宽
     参数：无
     */
    public int GetActualOutputWidth() {
        return GetActualOutputWidth(m_pEngine);
    }

    /*
     描述：获取实际输出宽高，如果所设置的输入宽高大于了GPU支持的宽高，引擎内部会进行裁剪，可以使用此接口获取实际的输出宽高
     返回值：实际输出帧的高
     参数：无
     */
    public int GetActualOutputHeight() {
        return GetActualOutputHeight(m_pEngine);
    }

    /*
     描述：获取输出纹理的ID
     返回值：输出纹理的ID
     参数：无
     */
    public int GetOutputTextureID() {
        return GetOutputTextureID(m_pEngine);
    }


}
