package com.tencent.demo.opengl.render;


import com.tencent.demo.opengl.GlUtil;

import java.nio.FloatBuffer;

/**
 * Base class for stuff we like to draw.
 */
public class Drawable2d {
    private static final int SIZEOF_FLOAT = 4;
    /**
     * The constant COORDS_PER_VERTEX.
     */
    public static final int COORDS_PER_VERTEX = 2;

    /**
     * The constant TEXTURE_COORD_STRIDE.
     */
    public static final int TEXTURE_COORD_STRIDE = COORDS_PER_VERTEX * SIZEOF_FLOAT;
    /**
     * The constant VERTEXTURE_STRIDE.
     */
    public static final int VERTEXTURE_STRIDE = COORDS_PER_VERTEX * SIZEOF_FLOAT;


    /**
     * A "full" square, extending from -1 to +1 in both dimensions.  When the model/view/projection
     * matrix is identity, this will exactly cover the viewport.
     * <p>
     * The texture coordinates are Y-inverted relative to RECTANGLE.  (This seems to work out
     * right with external textures from SurfaceTexture.)
     */
    private static final float[] FULL_RECTANGLE_COORDS = {
            -1.0f, -1.0f,   // 0 bottom left
            1.0f, -1.0f,   // 1 bottom right
            -1.0f, 1.0f,   // 2 top left
            1.0f, 1.0f,   // 3 top right
    };

    /**
     * The coordinate system of the FrameBuffer and the screen is mirrored vertically, so when drawing the texture to a FrameBuffer or screen
     * , the vertex coordinates of the texture they use are different, which needs attention.
     * <p>FrameBuffer 与屏幕的坐标系是垂直镜像的，所以在将纹理绘制到一个 FrameBuffer 或屏幕上
     * 的时候，他们用的纹理顶点坐标是不同的，需要注意。
     */
    private static final float[] FULL_RECTANGLE_TEX_COORDS = {
            0.0f, 1.0f,     // 0 bottom left
            1.0f, 1.0f,     // 1 bottom right
            0.0f, 0.0f,     // 2 top left
            1.0f, 0.0f      // 3 top right
    };

    private static final float[] FULL_RECTANGLE_TEX_COORDS1 = {
            0.0f, 0.0f,     // 0 bottom left
            1.0f, 0.0f,     // 1 bottom right
            0.0f, 1.0f,     // 2 top left
            1.0f, 1.0f      // 3 top right
    };
    private static final FloatBuffer FULL_RECTANGLE_BUF =
            GlUtil.createFloatBuffer(FULL_RECTANGLE_COORDS);
    private static final FloatBuffer FULL_RECTANGLE_TEX_BUF =
            GlUtil.createFloatBuffer(FULL_RECTANGLE_TEX_COORDS);
    private static final FloatBuffer FULL_RECTANGLE_TEX_BUF1 =
            GlUtil.createFloatBuffer(FULL_RECTANGLE_TEX_COORDS1);


    private FloatBuffer mVertexArray;
    private FloatBuffer mTexCoordArray;
    private FloatBuffer mTexCoordArrayFB;
    private int mVertexCount;
    private int mCoordsPerVertex;


    public Drawable2d() {

        mVertexArray = FULL_RECTANGLE_BUF;
        mTexCoordArray = FULL_RECTANGLE_TEX_BUF;
        mTexCoordArrayFB = FULL_RECTANGLE_TEX_BUF1;
        mCoordsPerVertex = 2;
        mVertexCount = FULL_RECTANGLE_COORDS.length / mCoordsPerVertex;
    }

    /**
     * Returns the array of vertices.
     * <p>
     * To avoid allocations, this returns internal state.  The caller must not modify it.
     *
     * @return the vertex array
     */
    public FloatBuffer getVertexArray() {
        return mVertexArray;
    }

    /**
     * Returns the array of texture coordinates.
     * <p>
     * To avoid allocations, this returns internal state.  The caller must not modify it.
     *
     * @return the tex coord array
     */
    public FloatBuffer getTexCoordArray() {
        return mTexCoordArray;
    }


    /**
     * Gets tex coor array fb.
     *
     * @return the tex coor array fb
     */
    public FloatBuffer getTexCoordArrayFB() {
        return mTexCoordArrayFB;
    }

    /**
     * Returns the number of vertices stored in the vertex array.
     *
     * @return the vertex count
     */
    public int getVertexCount() {
        return mVertexCount;
    }


}
