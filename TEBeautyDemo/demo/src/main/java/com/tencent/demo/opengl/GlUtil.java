package com.tencent.demo.opengl;

import static android.widget.ImageView.ScaleType.FIT_XY;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import android.util.Size;
import android.widget.ImageView;

import com.tencent.effect.beautykit.utils.LogUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;


/**
 * Some OpenGL utility functions.
 */
public abstract class GlUtil {
    /**
     * The constant TAG.
     */
    public static final String TAG = GlUtil.class.getSimpleName();

    /**
     * The constant NO_TEXTURE.
     */
    public static final int NO_TEXTURE = -1;


    /**
     * The constant x_scale.
     */
    public static final float X_SCALE = 1.0f;
    /**
     * The constant y_scale.
     */
    public static final float Y_SCALE = 1.0f;


    private static final int SIZEOF_FLOAT = 4;


    private GlUtil() {
        throw new IllegalStateException("Can not new GlUtil");
    }     // do not instantiate

    /**
     * Creates a new program from the supplied vertex and fragment shaders.
     *
     * @param vertexSource   the vertex source
     * @param fragmentSource the fragment source
     * @return A handle to the program, or 0 on failure.
     */
    public static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        checkGlError("glCreateProgram");
        if (program == 0) {
            LogUtils.e(TAG, "Could not create program");
        }
        GLES20.glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        GLES20.glAttachShader(program, pixelShader);
        checkGlError("glAttachShader");
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            LogUtils.e(TAG, "Could not link program: ");
            LogUtils.e(TAG, GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }

    /**
     * Compiles the provided shader source.
     *
     * @param shaderType the shader type
     * @param source     the source
     * @return A handle to the shader, or 0 on failure.
     */
    public static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        checkGlError("glCreateShader type=" + shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            LogUtils.e(TAG, "Could not compile shader " + shaderType + ":");
            LogUtils.e(TAG, " " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }


    public static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            LogUtils.e(TAG, msg);
        }
    }


    public static void checkLocation(int location, String label) {
        if (location < 0) {
            LogUtils.e(TAG, "Unable to locate '" + label + "' in program");
        }
    }


    public static FloatBuffer createFloatBuffer(float[] coords) {
        // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * SIZEOF_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }


    public static int createOESTexture() {
        int[] texture = new int[1];

        GLES20.glGenTextures(1, texture, 0);
        // Bind the texture handle to the 2D texture target.
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        // Configure min/mag filtering, i.e. what scaling method do we use if what we're rendering
        // is smaller or larger than the source image.
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        return texture[0];
    }

    public static int createTexture(int width, int height, int config) {
        int[] texture = new int[1];

        GLES20.glGenTextures(1, texture, 0);

        Log.e("createTexture",""+texture[0]);
        // Bind the texture handle to the 2D texture target.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        // Configure min/mag filtering, i.e. what scaling method do we use if what we're rendering
        // is smaller or larger than the source image.
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, config, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        return texture[0];
    }


    public static void bindTexture(Bitmap bitmap, int textureId) {
        if (bitmap != null && !bitmap.isRecycled()) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            //根据以上指定的参数，生成一个2D纹理
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        }
    }

    public static void clearTexture(int textureId, int width, int height) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }


    /**
     * Delete texture id.
     *
     * @param textureId the texture id
     */
    public static void deleteTextureId(int[] textureId) {
        if (textureId != null && textureId.length > 0) {
            GLES20.glDeleteTextures(textureId.length, textureId, 0);
        }
    }

    /**
     * Delete texture id.
     *
     * @param textureId the texture id
     */
    public static void deleteTextureId(int textureId) {
        int[] textures = new int[1];
        textures[0] = textureId;
        GLES20.glDeleteTextures(textures.length, textures, 0);

    }


    /**
     * Gets show matrix.
     *
     * @param matrix   the matrix
     * @param type     the type
     * @param textureW the texture width
     * @param textureH the texture height
     * @param targetW  target width
     * @param targetH  target height
     * @return size after applying crop Matrix
     */
    public static Size cropSize(float[] matrix, ImageView.ScaleType type, int textureW, int textureH, int targetW,
                                int targetH) {
        int resultWidth = 0;
        int resultHeight = 0;
        if (textureH > 0 && textureW > 0 && targetW > 0 && targetH > 0) {
            float[] projection = new float[16];
            float[] camera = new float[16];
            if (type == FIT_XY) {
                resultWidth = textureW;
                resultHeight = textureH;
                Matrix.orthoM(projection, 0, -1, 1, -1, 1, 1, 3);
                Matrix.setLookAtM(camera, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
                Matrix.multiplyMM(matrix, 0, projection, 0, camera, 0);
            }
            float sWhView = (float) targetW / targetH;
            float sWhImg = (float) textureW / textureH;
            if (sWhImg > sWhView) {
                switch (type) {
                    case CENTER:
                        float cropRatio = ((float) targetW) / ((float) textureW);
                        resultWidth = (int) (textureW * cropRatio);
                        resultHeight = (int) (textureH * cropRatio);
                        Matrix.orthoM(projection, 0, -cropRatio, cropRatio, -cropRatio, cropRatio, 1, 3);
                        Matrix.scaleM(projection, 0, X_SCALE, Y_SCALE, 1);
                        break;
                    case CENTER_CROP:
                        resultWidth = (int) (textureW * (sWhView / sWhImg));
                        resultHeight = textureH;
                        Matrix.orthoM(projection, 0, -sWhView / sWhImg, sWhView / sWhImg, -1, 1, 1, 3);
                        Matrix.scaleM(projection, 0, X_SCALE, Y_SCALE, 1);
                        break;
                    case CENTER_INSIDE:
                        resultWidth = textureW;
                        resultHeight = (int) (textureH * (sWhImg / sWhView));
                        Matrix.orthoM(projection, 0, -1, 1, -sWhImg / sWhView, sWhImg / sWhView, 1, 3);
                        break;
                    case FIT_START:
                        resultWidth = textureW;
                        resultHeight = (int) (textureH * (sWhImg / sWhView));
                        Matrix.orthoM(projection, 0, -1, 1, 1 - 2 * sWhImg / sWhView, 1, 1, 3);
                        break;
                    case FIT_END:
                        resultWidth = textureW;
                        resultHeight = (int) (textureH * (sWhImg / sWhView));
                        Matrix.orthoM(projection, 0, -1, 1, -1, 2 * sWhImg / sWhView - 1, 1, 3);
                        break;
                    default:
                        // do nothing
                }
            } else {
                switch (type) {
                    case CENTER:
                        float cropRatio = ((float) targetW) / ((float) textureW);
                        resultWidth = (int) (textureW * cropRatio);
                        resultHeight = (int) (textureH * cropRatio);
                        Matrix.orthoM(projection, 0, -cropRatio, cropRatio, -cropRatio, cropRatio, 1, 3);
                        Matrix.scaleM(projection, 0, X_SCALE, Y_SCALE, 1);
                        break;
                    case CENTER_CROP:
                        resultWidth = textureW;
                        resultHeight = (int) (textureH * (sWhImg / sWhView));
                        Matrix.orthoM(projection, 0, -1, 1, -sWhImg / sWhView, sWhImg / sWhView, 1, 3);
                        Matrix.scaleM(projection, 0, X_SCALE, Y_SCALE, 1);
                        break;
                    case CENTER_INSIDE:
                        resultWidth = (int) (textureW * (sWhView / sWhImg));
                        resultHeight = textureH;
                        Matrix.orthoM(projection, 0, -sWhView / sWhImg, sWhView / sWhImg, -1, 1, 1, 3);
                        break;
                    case FIT_START:
                        resultWidth = (int) (textureW * (sWhImg / sWhView));
                        resultHeight = textureH;
                        Matrix.orthoM(projection, 0, -1, 2 * sWhView / sWhImg - 1, -1, 1, 1, 3);
                        break;
                    case FIT_END:
                        resultWidth = (int) (textureW * (sWhImg / sWhView));
                        resultHeight = textureH;
                        Matrix.orthoM(projection, 0, 1 - 2 * sWhView / sWhImg, 1, -1, 1, 1, 3);
                        break;
                    default:
                        // do nothing
                }
            }
            Matrix.setLookAtM(camera, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
            Matrix.multiplyMM(matrix, 0, projection, 0, camera, 0);
        }
        return new Size(resultWidth, resultHeight);
    }


    /**
     * Prefer OpenGL ES 3.0, otherwise 2.0
     *
     * @param context the context
     * @return support gl version
     */
    public static int getSupportGLVersion(Context context) {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        int version = configurationInfo.reqGlEsVersion >= 0x30000 ? 3 : 2;
        String glEsVersion = configurationInfo.getGlEsVersion();
        LogUtils.d(TAG, "reqGlEsVersion: " + Integer.toHexString(configurationInfo.reqGlEsVersion)
                + ", glEsVersion: " + glEsVersion + ", return: " + version);
        return version;
    }


    public static float[] rotate(float[] m, float angle) {
        Matrix.rotateM(m, 0, angle, 0, 0, 1);
        return m;
    }

    public static float[] flip(float[] m, boolean x, boolean y) {
        if (x || y) {
            Matrix.scaleM(m, 0, x ? -1 : 1, y ? -1 : 1, 1);
        }
        return m;
    }


    public static float[] scale(float[] m, float x, float y) {
        Matrix.scaleM(m, 0, x, y, 1);
        return m;
    }


    public static Bitmap readTexture(int texture, int width, int height) {
        long s = System.currentTimeMillis();

        int[] frame = new int[1];
        GLES20.glGenFramebuffers(1, frame, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frame[0]);
        checkGlError("glBindFramebuffer");

        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, texture, 0);
        checkGlError("glFramebufferTexture2D");

        byte[] data = new byte[width * height * 4];
        ByteBuffer buffer = ByteBuffer.wrap(data);
        GLES20.glPixelStorei(GLES20.GL_PACK_ALIGNMENT, GLES20.GL_TRUE);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        checkGlError("glReadPixels");
        // LightLogUtil.v(TAG, "readTexture: " + (System.currentTimeMillis() - s) + "ms");

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glDeleteFramebuffers(1, frame, 0);
        checkGlError("glBindFramebuffer");

        return bitmap;
    }

}
