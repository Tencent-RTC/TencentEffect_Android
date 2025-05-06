package com.tencent.demo.opengl.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.util.Size;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tencent.demo.R;
import com.tencent.demo.opengl.gles.EglCore;
import com.tencent.demo.opengl.view.display.Display;
import com.tencent.demo.opengl.view.display.DisplayImp;
import com.tencent.demo.opengl.view.display.DisplayStateChangeListener;
import com.tencent.demo.opengl.view.process.BitmapProcessor;
import com.tencent.demo.opengl.view.source.BitmapSource;
import com.tencent.demo.opengl.view.source.SourceEventListener;

public class GLImageView extends FrameLayout {


    private Bitmap bitmap = null;
    private boolean isSurfaceView = true;

    private BitmapSource bitmapSource = null;
    private BitmapProcessor processor = null;
    private Display display = null;


    public GLImageView(@NonNull Context context) {
        this(context, null);
    }

    public GLImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GLImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.getAttributes(context, attrs);
        this.initView();
    }


    private void getAttributes(@NonNull Context context, @Nullable AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.te_beauty_GLImageView);
        int imageResId = typedArray.getResourceId(R.styleable.te_beauty_GLImageView_src_image, 0);
        this.isSurfaceView = typedArray.getBoolean(R.styleable.te_beauty_GLImageView_surface, true);
        typedArray.recycle();
        if (imageResId != 0) {
            this.bitmap = BitmapFactory.decodeResource(getResources(), imageResId);
        }
    }


    private void initView() {
        if (this.isSurfaceView) {
            this.display = new DisplayImp(new SurfaceView(getContext()));
        } else {
            this.display = new DisplayImp(new TextureView(getContext()));
        }
        this.display.setDisplayStateChangeListener(new DisplayStateChangeListener() {
            @Override
            public void onReady() {
                if (bitmapSource != null) {
                    bitmapSource.initGlContext();
                }
            }

            @Override
            public void onSurfaceRecreated() {
                if (bitmapSource != null) {
                    bitmapSource.surfaceRecreate();
                }
            }


            @Override
            public void onSizeChange(int width, int height) {
                if (bitmapSource != null) {
                    bitmapSource.updateDisplaySize(new Size(width, height));
                }
            }

            @Override
            public void onDestroy() {
            }
        });
        this.processor = new BitmapProcessor();
        this.processor.setDisplay(this.display);
        this.bitmapSource = new BitmapSource(getContext());
        this.bitmapSource.setSourceEventListener(new SourceEventListener() {
            @Override
            public void onGLContextCreated(EglCore eglCore) {
                processor.onGLContextCreated(eglCore);
                display.onGLContextCreated(eglCore);
            }

            @Override
            public void onSurfaceRecreated(EglCore eglCore) {
                //由于surfaceView在不可见到可见的时候会再次调用surfaceCreated方法，导致需要重新创建新的windowSurface，所以此处需要重新创建 WindowSurface
                display.onGLContextCreated(eglCore);
            }

            @Override
            public void onReceiveFrameTexture(EglCore eglCore, FrameTexture frameTexture) {
                processor.process(eglCore, frameTexture);
            }

            @Override
            public void onGLContextDestroy() {
                processor.onGLContextDestroy();
                display.onGLContextDestroy();
            }
        });
        if (this.bitmap != null) {
            this.bitmapSource.setData(bitmap);
        }
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.CENTER;
        this.addView(this.display.getDisplayView(), layoutParams);
    }

    public void setData(Bitmap bitmap) {
        this.bitmap = bitmap;
        if (this.bitmapSource != null) {
            this.bitmapSource.setData(bitmap);
        }
        this.display.getDisplayView().post(() -> adjustViewSizeToMatchBitmap());
    }

    private void adjustViewSizeToMatchBitmap() {
        View displayView = this.display.getDisplayView();
        if (bitmap == null || displayView == null) {
            return;
        }
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        int displayWidth = displayView.getWidth();
        if (displayWidth <= 0) {
            postDelayed(this::adjustViewSizeToMatchBitmap, 1000);
            return;
        }
        // 根据bitmap的宽高比计算displayView应该设置的高度
        int displayHeight = (int) (displayWidth * (bitmapHeight / (float) bitmapWidth));
        // 设置displayView的新尺寸
        ViewGroup.LayoutParams params = displayView.getLayoutParams();
        params.width = displayWidth;
        params.height = displayHeight;
        displayView.setLayoutParams(params);
    }



    public void clearBitmap() {
        this.bitmap = null;
        if (this.bitmapSource != null) {
            this.bitmapSource.setData(null);
        }
    }

    public void setCustomTextureProcessor(CustomTextureProcessor mCustomTextureProcessor) {
        if (this.processor != null) {
            this.processor.setCustomTextureProcessor(mCustomTextureProcessor);
        }
    }

    public void start() {
        if (this.bitmapSource != null) {
            this.bitmapSource.start();
        }
    }


    public void pause() {
        if (this.bitmapSource != null) {
            this.bitmapSource.pause();
        }
    }

    public void release() {
        if (this.bitmapSource != null) {
            this.bitmapSource.release();
        }
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
    }
}
