package us.pinguo.pgskinprettifyengine;

import android.content.Context;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by wh on 2017/03/12.
 */

public class PGSkinUtils {

    private final String LOG_TAG = "PGSkinUtils";

    public static final String SDK_KEY_NEW = "";

    PGSkinPrettifyEngine mPGSkinPrettifyEngine;
    private int mSurfaceWidth, mSurfaceHeight;
    private int mPreviewWidth, mPreviewHeight;

    //default value
    private float mPinkValue = 0.6f;
    private float mWhitenValue = 0.5f;
    private float mReddenValue = 0.6f;
    private int mSoftenValue = 70;
    private String mFilterName = "";
    private int mFilterStrength = 100;
    private PGSkinPrettifyEngine.PG_SoftenAlgorithm mAlgorithm;

    private boolean mFilterChange;
    private boolean mSoftenChange;
    private boolean mSkinChange;
    private boolean mFilterStrengthChange;
    private boolean mAlgorithmChange;

    public PGSkinUtils() {
        mPGSkinPrettifyEngine = new PGSkinPrettifyEngine();
    }


    public void SetColorFilterStrength(int progress) {
        mFilterStrengthChange = true;
        mFilterStrength = progress;
    }


    public void SetSkinColor(float fPinking, float fWhitening, float fRedden) {
        mSkinChange = true;
        mPinkValue = fPinking;
        mWhitenValue = fWhitening;
        mReddenValue = fRedden;
    }

    public void SetSkinSoftenStrength(int iSoftenStrength) {
        mSoftenChange = true;
        mSoftenValue = iSoftenStrength;
    }

    public void SetColorFilterByName(String pName) {
        mFilterChange = true;
        mFilterName = pName;
    }

    public void setSkinSoftenAlgorithm(PGSkinPrettifyEngine.PG_SoftenAlgorithm algorithm) {
        mAlgorithm = algorithm;
        mAlgorithmChange = true;
    }


    public void setScreenChange(int width, int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
    }

    public void setPreviewSize(int width, int height) {
        mPreviewWidth = width;
        mPreviewHeight = height;
    }

    /**
     * step 1
     * 初始化
     */

    public void initEngine(Context context) {
        mPGSkinPrettifyEngine.InitialiseEngine(context, SDK_KEY_NEW, false);//初始化引擎
        mPGSkinPrettifyEngine.SetSizeForAdjustInput(mPreviewWidth, mPreviewHeight/*height,width*/);
        mPGSkinPrettifyEngine.SetOutputFormat(PGSkinPrettifyEngine.PG_PixelFormat.PG_Pixel_NV21);
        mPGSkinPrettifyEngine.SetSkinSoftenStrength(mSoftenValue);
        mPGSkinPrettifyEngine.SetSkinColor(mPinkValue, mWhitenValue, mReddenValue);
        mPGSkinPrettifyEngine.SetSkinSoftenAlgorithm(PGSkinPrettifyEngine.PG_SoftenAlgorithm.PG_SoftenAlgorithmContrast);//磨皮算法设置
        mPGSkinPrettifyEngine.SetOutputOrientation(PGSkinPrettifyEngine.PG_Orientation.PG_OrientationNormal);
    }

    /**
     * step 2
     * 美颜 run
     * param  date 数据流， textureid 外部纹理id，
     */

    public void frameProcess(Context context, byte[] data, int textureId, boolean isFirstFrame) {

        if (isFirstFrame) initEngine(context);//  在第一帧视频到来时，初始化，指定需要的输出大小以及方向


        if (mFilterChange) {
            mPGSkinPrettifyEngine.SetColorFilterByName(mFilterName);//滤镜
            mFilterChange = false;
        }
        if (mFilterStrengthChange) {
            mPGSkinPrettifyEngine.SetColorFilterStrength(mFilterStrength);//滤镜强度
            mFilterStrengthChange = false;
        }
        if (mSoftenChange) {
            mPGSkinPrettifyEngine.SetSkinSoftenStrength(mSoftenValue);
            mSoftenChange = false;
        }
        if (mSkinChange) {
            mPGSkinPrettifyEngine.SetSkinColor(mPinkValue, mWhitenValue, mReddenValue);
            mSkinChange = false;
        }
        if (mAlgorithmChange) {
            mPGSkinPrettifyEngine.SetSkinSoftenAlgorithm(mAlgorithm);//磨皮算法设置
            mAlgorithmChange = false;
        }

        if (data != null) {
            mPGSkinPrettifyEngine.SetInputFrameByNV21(data, mPreviewWidth, mPreviewHeight);
        } else if (textureId > 0) {
            mPGSkinPrettifyEngine.SetInputFrameByTexture(textureId, mPreviewWidth, mPreviewHeight);
            //mPGSkinPrettifyEngine.SetInputFrameByTexture(textureId, mPreviewWidth, mPreviewHeight,1);//普通纹理id
        }

        mPGSkinPrettifyEngine.RunEngine();

    }


    /**
     * step 3
     * 获取美颜后数据 1.buffer 2.byte 3.textureID
     */

    public ByteBuffer SkinSoftenGetResult() {
        return mPGSkinPrettifyEngine.SkinSoftenGetResult();
    }


    public byte[] getSkinSoftenByte() {
        ByteBuffer buffer = mPGSkinPrettifyEngine.SkinSoftenGetResult();
        buffer.clear();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes, 0, bytes.length);
        return bytes;
    }

    public int getSkinSoftenTextureId() {
        return mPGSkinPrettifyEngine.GetOutputTextureID();
    }


    public void pause() {
        if (mPGSkinPrettifyEngine != null) {
            Log.i(LOG_TAG, "releasing mPGSkinPrettifyEngine");
            mPGSkinPrettifyEngine.DestroyEngine();
            mPGSkinPrettifyEngine = null;
        }
    }


    public void onresume() {
        if (mPGSkinPrettifyEngine == null) mPGSkinPrettifyEngine = new PGSkinPrettifyEngine();
    }

}
