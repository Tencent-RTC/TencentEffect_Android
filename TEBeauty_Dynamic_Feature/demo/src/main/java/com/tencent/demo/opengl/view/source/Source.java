package com.tencent.demo.opengl.view.source;

public interface Source {

    //用于创建GL环境
    void initGlContext();

    //开始在GL环境中生产数据
    void start();

    //设置数据的数据接受者
    void setSourceEventListener(SourceEventListener sourceEventListener);

    //通知GL线程，SurfaceView的surfaceCreated方法再次被触发了，这个时候source也将也会将此事件发送到GL线程。
    void surfaceRecreate();

    //暂停GL线程 或 相机
    void pause();

    //销毁Source
    void release();


}
