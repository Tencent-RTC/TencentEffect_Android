package com.tencent.demo.opengl.view.display;

/**
 * Display的状态监听
 */
public interface DisplayStateChangeListener {

    //当Display状态准备好的时候回调，一般在此方法中开始初始化GL环境
    void onReady();

    //只有在SurfaceView的surfaceCreated再次被调用时触发，这个时候需要在GL线程中通知到Display再次创建 WindowSurface对象
    void onSurfaceRecreated();

    //当Display 的大小发生变化的时候触发
    void onSizeChange(int width, int height);

    //当Display销毁的时候触发
    void onDestroy();


}
