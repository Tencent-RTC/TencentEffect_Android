package com.tencent.demo.camera.glrender;

import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDrawArrays;

import android.opengl.GLES20;

import java.nio.ByteBuffer;

public class RotateRenderer extends BaseRenderer {


    private static final String TAG = "RotateRenderer";

    /**
     * This method will load the vertex shader for the rotation direction.
     */
    @Override
    public void init() {
        initRotate(270, false, true);
    }


    /**
     * This method will load the vertex shader that does not perform rotation direction.
     */
    public void initRotate(int degree, boolean flipV, boolean flipH) {
        String vertShader = null;
        switch (degree % 360) {
            case 0:
                if (flipH && flipV) {
                    vertShader = VERT_SHADER_ROTATE_0_HV;
                } else if (flipH) {
                    vertShader = VERT_SHADER_ROTATE_0_H;
                } else if (flipV) {
                    vertShader = VERT_SHADER_ROTATE_0_V;
                } else {
                    vertShader = VERT_SHADER_ROTATE_0;
                }
                break;
            case 90:
                if (flipH && flipV) {
                    vertShader = VERT_SHADER_ROTATE_90_HV;
                } else if (flipH) {
                    vertShader = VERT_SHADER_ROTATE_90_H;
                } else if (flipV) {
                    vertShader = VERT_SHADER_ROTATE_90_V;
                } else {
                    vertShader = VERT_SHADER_ROTATE_90;
                }

                break;
            case 180:
                if (flipH && flipV) {
                    vertShader = VERT_SHADER_ROTATE_180_HV;
                } else if (flipH) {
                    vertShader = VERT_SHADER_ROTATE_180_H;
                } else if (flipV) {
                    vertShader = VERT_SHADER_ROTATE_180_V;
                } else {
                    vertShader = VERT_SHADER_ROTATE_180;
                }
                break;
            case 270:
                if (flipH && flipV) {
                    vertShader = VERT_SHADER_ROTATE_270_HV;
                } else if (flipH) {
                    vertShader = VERT_SHADER_ROTATE_270_H;
                } else if (flipV) {
                    vertShader = VERT_SHADER_ROTATE_270_V;
                } else {
                    vertShader = VERT_SHADER_ROTATE_270;
                }
                break;
        }
        super.init(getVertShader(vertShader));
    }


    public void doRender(int srcTextureId, int desTextureId, int frameWidth, int frameHeight,
                         float[] srcTransformMatrix, float[] destTransformMatrix, ByteBuffer buffer) {
        super.beforeRender(desTextureId, frameWidth, frameHeight, srcTransformMatrix, destTransformMatrix);
        super.setVertex();

        glActiveTexture(GLES20.GL_TEXTURE0);
        glBindTexture(GLES20.GL_TEXTURE_2D, srcTextureId);
//        glUniform1i(uTexPosition, 0);

        glDrawArrays(GL_TRIANGLES, 0, 6);

        saveToBuffer(buffer);
    }

}
