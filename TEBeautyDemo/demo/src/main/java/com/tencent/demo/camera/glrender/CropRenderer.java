package com.tencent.demo.camera.glrender;

import android.opengl.GLES20;
import android.util.Log;

import com.tencent.demo.utils.GlUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteFramebuffers;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glFinish;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glGetError;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glUseProgram;

// TODO 与 OESRender 合并, 可减少一次渲染操作
public class CropRenderer {

    protected static final String DEFAULT_VERT_SHADER =
            "attribute vec4 a_Position;\n" +
                    "attribute vec4 a_TexCoordinate;\n" +
                    "uniform mat4 u_CropMatrix;\n" +
                    "varying vec4 v_TexCoord;\n" +
                    "void main()\n" +
                    "{\n" +
                    "    v_TexCoord = u_CropMatrix * a_TexCoordinate;\n" +
                    "    gl_Position =  a_Position;\n" +
                    "}";

    protected static final String DEFAULT_FRAG_SHADER =
            // "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
                    // "uniform samplerExternalOES u_sampler;\n" +
                    "uniform sampler2D u_sampler;\n" +
                    "varying vec4 v_TexCoord;\n" +
                    "void main()\n" +
                    "{\n" +
                    "    gl_FragColor = texture2D(u_sampler, v_TexCoord.xy);\n" +
                    "}";

    protected int vertexShader = -1;
    protected int fragmentShader = -1;
    protected int mShaderProgram = -1;

    protected int[] mFBOIdHolder = {-1};

    private float[] vertexData = {//顶点坐标
            -1.0f, 1.0f, 0.0f,  // top left
            -1.0f, -1.0f, 0.0f,  // bottom left
            1.0f, -1.0f, 0.0f,  // bottom right
            1.0f, 1.0f, 0.0f  // top right
    };
    private float[] texData = {//纹理坐标
            0f, 1f,// top left
            0f, 0f,// bottom left
            1f, 0f, // bottom right
            1f, 1f // top right
    };
    private short[] indexData = {
            3, 2, 0, 0, 1, 2
    };

    private FloatBuffer vertexBuffer;
    private FloatBuffer texBuffer;
    private ShortBuffer indexBuffer;
    private int positionLoc;
    private int texCoordLoc;
    private int samplerLoc;
    private int cropMatrixLoc;

    public void init() {
        init(DEFAULT_VERT_SHADER, DEFAULT_FRAG_SHADER);
    }

    public void init(String vertexShaderR, String fragmentShaderR) {
        vertexShader = loadShader(GL_VERTEX_SHADER, vertexShaderR);
        fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentShaderR);
        mShaderProgram = linkProgram(vertexShader, fragmentShader);

        glGenFramebuffers(1, mFBOIdHolder, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        vertexBuffer = createBuffer(vertexData);
        texBuffer = createBuffer(texData);
        indexBuffer = createBuffer(indexData);

        positionLoc = GLES20.glGetAttribLocation(mShaderProgram, "a_Position");
        texCoordLoc = GLES20.glGetAttribLocation(mShaderProgram, "a_TexCoordinate");
        samplerLoc = GLES20.glGetUniformLocation(mShaderProgram, "u_sampler");
        cropMatrixLoc = GLES20.glGetUniformLocation(mShaderProgram, "u_CropMatrix");

        GlUtil.checkGlError("OesCropRenderer");
    }

    public void doRender(int srcTextureId, int desTextureId, int targetW, int targetH, float[] cropTexture, ByteBuffer buffer) {
        glUseProgram(mShaderProgram);
        GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        GLES20.glViewport(0, 0, targetW, targetH);
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        if (desTextureId != -1) {//如果存在目标纹理, 使之与 FBO 关联
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFBOIdHolder[0]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, desTextureId, 0);
        }

        GlUtil.checkGlError("OesCropRenderer");

        //设置顶点数据
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(positionLoc);
        GLES20.glVertexAttribPointer(positionLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        //设置纹理顶点数据
        texBuffer.position(0);
        GLES20.glEnableVertexAttribArray(texCoordLoc);
        GLES20.glVertexAttribPointer(texCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texBuffer);
        //设置纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, srcTextureId);
        GLES20.glUniform1i(samplerLoc, 0);
        //设置矩阵
        GLES20.glUniformMatrix4fv(cropMatrixLoc, 1, false, cropTexture, 0);

        GlUtil.checkGlError("OesCropRenderer");

        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, indexData.length,
                GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GlUtil.checkGlError("OesCropRenderer");

        // GLES20.glDrawArrays(GL_TRIANGLES, 0, 6);

        saveToBuffer(buffer, targetW, targetH);
    }

    protected static void saveToBuffer(ByteBuffer buffer, int imgW, int imgH) {
        if (buffer != null) {
            glFinish();
            buffer.rewind();
            int imgSize = imgW * imgH * 4;
            if (buffer.capacity() != imgSize) {
                buffer = ByteBuffer.allocateDirect(imgSize);
            }
            GLES20.glPixelStorei(3333, 1);
            GLES20.glReadPixels(0, 0, imgW, imgH, 6408, 5121, buffer);
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void release() {
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        glDeleteProgram(mShaderProgram);
        glDeleteFramebuffers(1, mFBOIdHolder, 0);
    }

    protected static int loadShader(int type, String shaderSource) {
        int shader = glCreateShader(type);
        if (shader == 0) {
            throw new RuntimeException("Create Shader Failed!" + glGetError());
        }
        glShaderSource(shader, shaderSource);
        glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e("loadshader", "loadShader: " + glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    protected static int linkProgram(int verShader, int fragShader) {
        int program = glCreateProgram();
        if (program == 0) {
            throw new RuntimeException("Create Program Failed!" + glGetError());
        }
        glAttachShader(program, verShader);
        glAttachShader(program, fragShader);
        glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e("link", "linkProgram: " + glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }

    protected static FloatBuffer createBuffer(float[] array) {
        FloatBuffer buffer = ByteBuffer
                .allocateDirect(array.length * Float.SIZE / Byte.SIZE)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.put(array, 0, array.length).position(0);
        return buffer;
    }

    protected static ShortBuffer createBuffer(short[] array) {
        ShortBuffer buffer = ByteBuffer.allocateDirect(array.length * Short.SIZE / Byte.SIZE)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        buffer.put(array, 0, array.length).position(0);
        return buffer;
    }
}
