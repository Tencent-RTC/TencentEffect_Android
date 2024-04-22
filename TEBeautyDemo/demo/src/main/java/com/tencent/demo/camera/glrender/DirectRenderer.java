package com.tencent.demo.camera.glrender;

import android.opengl.GLES20;

import java.nio.ByteBuffer;

import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1i;

public class DirectRenderer extends BaseRenderer {

    protected int uTexPosition = -1;

    @Override
    public void init() {
        super.init(getVertShader(VERT_SHADER_ROTATE_0_V));
        uTexPosition = glGetUniformLocation(mShaderProgram, "inputImageTexture");
    }
    public void initFlipVertical(){
        super.init();
        uTexPosition = glGetUniformLocation(mShaderProgram, "inputImageTexture");
    }
    public void doRender(int srcTextureId, int desTextureId, int frameWidth, int frameHeight, float[] srcTransformMatrix, float[] destTransformMatrix, ByteBuffer buffer) {
        super.beforeRender(desTextureId, frameWidth, frameHeight, srcTransformMatrix, destTransformMatrix);
        super.setVertex();

        glActiveTexture(GLES20.GL_TEXTURE0);
        glBindTexture(GLES20.GL_TEXTURE_2D, srcTextureId);
        glUniform1i(uTexPosition, 0);

        glDrawArrays(GL_TRIANGLES, 0, 6);

        saveToBuffer(buffer);
    }
}
