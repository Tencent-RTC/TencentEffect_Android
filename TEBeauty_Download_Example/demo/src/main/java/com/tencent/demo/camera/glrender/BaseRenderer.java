package com.tencent.demo.camera.glrender;

import static android.opengl.GLES20.GL_FLOAT;
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
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glFinish;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetError;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.text.TextUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

class BaseRenderer {

    protected static final String VERT_SHADER_ROTATE_START =
            "attribute vec4 position;\n" +
                    "vec4 verticalFlipPosition;\n" +
                    "attribute vec4 inputTexCoord;\n" +
                    "uniform mat4 uInputMatrix;\n" +
                    "uniform mat4 uRotateMatrix;\n" +
                    "uniform mat4 uScreenMatrix;\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "void main()\n" +
                    "{\n" +
                    "    textureCoordinate = (uInputMatrix * inputTexCoord).xy;\n" +
                    "    verticalFlipPosition = vec4(";

    protected static final String VERT_SHADER_ROTATE_END =
            ", position.z, 1);\n" +
                    "    gl_Position = verticalFlipPosition * uRotateMatrix * uScreenMatrix;\n" +
                    "}";

    public static String getVertShader(String shader) {
        if (TextUtils.isEmpty(shader)) {
            return null;
        }
        return VERT_SHADER_ROTATE_START + shader + VERT_SHADER_ROTATE_END;
    }

    protected static final String VERT_SHADER_ROTATE_0 = "position.x, position.y\n";
    protected static final String VERT_SHADER_ROTATE_0_H = "-position.x, position.y\n";
    protected static final String VERT_SHADER_ROTATE_0_V = "position.x, -position.y\n";
    protected static final String VERT_SHADER_ROTATE_0_HV = "-position.x, -position.y\n";


    protected static final String VERT_SHADER_ROTATE_90 = "-position.y, position.x\n";
    protected static final String VERT_SHADER_ROTATE_90_H = "position.y, position.x\n";
    protected static final String VERT_SHADER_ROTATE_90_V = "-position.y, -position.x\n";
    protected static final String VERT_SHADER_ROTATE_90_HV = "position.y, -position.x\n";

    protected static final String VERT_SHADER_ROTATE_180 = VERT_SHADER_ROTATE_0_HV;
    protected static final String VERT_SHADER_ROTATE_180_H = VERT_SHADER_ROTATE_0_V;
    protected static final String VERT_SHADER_ROTATE_180_V = VERT_SHADER_ROTATE_0_H;
    protected static final String VERT_SHADER_ROTATE_180_HV = VERT_SHADER_ROTATE_0;

    protected static final String VERT_SHADER_ROTATE_270 = VERT_SHADER_ROTATE_90_HV;
    protected static final String VERT_SHADER_ROTATE_270_H = VERT_SHADER_ROTATE_90_V;
    protected static final String VERT_SHADER_ROTATE_270_V = VERT_SHADER_ROTATE_90_H;
    protected static final String VERT_SHADER_ROTATE_270_HV = VERT_SHADER_ROTATE_90;

    protected static final String DEFAULT_FRAG_SHADER =
            "precision highp float;\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "uniform sampler2D inputImageTexture;\n" +
                    "void main() \n" +
                    "{\n" +
                    "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                    "}";

    protected int vertexShader = -1;
    protected int fragmentShader = -1;
    protected int mShaderProgram = -1;

    protected int mDesTextureId = -1;

    protected int mFrameWidth = 0;
    protected int mFrameHeight = 0;
    protected int rotateDegree = 0;
    protected int flipHorizontal = 0;
    protected int flipVertical = 0;

    protected int[] mFBOIdHolder = {-1};

    protected int aPosition = -1;
    protected int aInputTexCoord = -1;
    protected int uInputMat = -1;
    protected int uRotateMat = -1;
    protected int uScreenMat = -1;

    protected float[] mInputMatrix = new float[16];
    protected float[] mRotateMatrix = new float[16];
    protected float[] mScreenMatrix = new float[16];

    private float[] vertexData = {
            1f, 1f, 1f, 1f,
            -1f, 1f, 0f, 1f,
            -1f, -1f, 0f, 0f,
            1f, 1f, 1f, 1f,
            -1f, -1f, 0f, 0f,
            1f, -1f, 1f, 0f
    };

    private FloatBuffer vertexBuffer;


    public void init() {
        this.init(VERT_SHADER_ROTATE_0, DEFAULT_FRAG_SHADER);
    }

    public void init(String vertexShaderR) {
        this.init(vertexShaderR, DEFAULT_FRAG_SHADER);
    }

    public void init(String vertexShaderR, String fragmentShaderR) {
        vertexShader = loadShader(GL_VERTEX_SHADER, vertexShaderR);
        fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentShaderR);
        mShaderProgram = linkProgram(vertexShader, fragmentShader);

        glGenFramebuffers(1, mFBOIdHolder, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        vertexBuffer = createBuffer(vertexData);

        aPosition = glGetAttribLocation(mShaderProgram, "position");
        aInputTexCoord = glGetAttribLocation(mShaderProgram, "inputTexCoord");
        uRotateMat = glGetUniformLocation(mShaderProgram, "uRotateMatrix");
        uInputMat = glGetUniformLocation(mShaderProgram, "uInputMatrix");
        uScreenMat = glGetUniformLocation(mShaderProgram, "uScreenMatrix");

        Matrix.setIdentityM(mInputMatrix, 0);
        Matrix.setIdentityM(mScreenMatrix, 0);
        Matrix.setIdentityM(mRotateMatrix, 0);
    }

    // destination: 0 for screen, 1 for RGBATexture
    public void beforeRender(int desTextureId, int frameWidth, int frameHeight, float[] srcTransformMatrix, float[] destTransformMatrix) {
        mFrameWidth = frameWidth;
        mFrameHeight = frameHeight;
        mDesTextureId = desTextureId;

        setScreenMatrix(destTransformMatrix);
        setTextureMatrix(srcTransformMatrix);

        glUseProgram(mShaderProgram);

        GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        GLES20.glViewport(0, 0, mFrameWidth, mFrameHeight);
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        if (mDesTextureId != -1) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFBOIdHolder[0]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mDesTextureId, 0);
        }

        glUniformMatrix4fv(uInputMat, 1, false, mInputMatrix, 0);
        glUniformMatrix4fv(uScreenMat, 1, false, mScreenMatrix, 0);
        glUniformMatrix4fv(uRotateMat, 1, false, mRotateMatrix, 0);
    }

    public void setVertex() {
        vertexBuffer.position(0);
        glEnableVertexAttribArray(aPosition);
        glVertexAttribPointer(aPosition, 2, GL_FLOAT, false, 16, vertexBuffer);

        vertexBuffer.position(2);
        glEnableVertexAttribArray(aInputTexCoord);
        glVertexAttribPointer(aInputTexCoord, 2, GL_FLOAT, false, 16, vertexBuffer);
    }

    public void setRotateAndFlip(int degree, int flipVertical, int flipHorizontal) {
        boolean vertArrayUpdated = false;
        if (rotateDegree != degree) {
            rotateDegree = degree;
            Matrix.setIdentityM(mRotateMatrix, 0);
            Matrix.rotateM(mRotateMatrix, 0, rotateDegree, 0f, 0f, 1f);
        }
        if (this.flipVertical != flipVertical) {
            vertArrayUpdated = true;
            this.flipVertical = flipVertical;
            if (flipVertical == 1) {
                vertexData[1] = -1f;
                vertexData[5] = -1f;
                vertexData[9] = 1f;
                vertexData[13] = -1f;
                vertexData[17] = 1f;
                vertexData[21] = 1f;
            } else {
                vertexData[1] = 1f;
                vertexData[5] = 1f;
                vertexData[9] = -1f;
                vertexData[13] = 1f;
                vertexData[17] = -1f;
                vertexData[21] = -1f;
            }
        }
        if (this.flipHorizontal != flipHorizontal) {
            vertArrayUpdated = true;
            this.flipHorizontal = flipHorizontal;
            if (flipHorizontal == 1) {
                vertexData[0] = -1f;
                vertexData[4] = 1f;
                vertexData[8] = 1f;
                vertexData[12] = -1f;
                vertexData[16] = 1f;
                vertexData[20] = -1f;
            } else {
                vertexData[0] = 1f;
                vertexData[4] = -1f;
                vertexData[8] = -1f;
                vertexData[12] = 1f;
                vertexData[16] = -1f;
                vertexData[20] = 1f;
            }
        }
        vertexBuffer = vertArrayUpdated ? createBuffer(vertexData) : vertexBuffer;
    }

    public void setScreenMatrix(float[] screenMatrix) {
        if (screenMatrix != null && screenMatrix.length == mScreenMatrix.length) {
            System.arraycopy(screenMatrix, 0, mScreenMatrix, 0, mScreenMatrix.length);
        } else {
            Matrix.setIdentityM(mScreenMatrix, 0);
        }
    }

    public void setTextureMatrix(float[] textureMatrix) {
        if (textureMatrix != null && textureMatrix.length == mInputMatrix.length) {
            System.arraycopy(textureMatrix, 0, mInputMatrix, 0, mInputMatrix.length);
        } else {
            Matrix.setIdentityM(mInputMatrix, 0);
        }
    }

    protected void saveToBuffer(ByteBuffer buffer) {
        if (buffer != null) {
            glFinish();
            buffer.rewind();
            if (buffer.capacity() != mFrameHeight * mFrameWidth * 4) {
                buffer = ByteBuffer.allocateDirect(mFrameHeight * mFrameWidth * 4);
            }
            GLES20.glPixelStorei(3333, 1);
            GLES20.glReadPixels(0, 0, mFrameWidth, mFrameHeight, 6408, 5121, buffer);
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void release() {
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        glDeleteProgram(mShaderProgram);
        glDeleteFramebuffers(1, mFBOIdHolder, 0);
    }


    protected int loadShader(int type, String shaderSource) {
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

    protected int linkProgram(int verShader, int fragShader) {
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

    protected FloatBuffer createBuffer(float[] vertexData) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.put(vertexData, 0, vertexData.length).position(0);
        return buffer;
    }
}
