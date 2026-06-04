package com.tencent.effect.agora.demo.observer;

public class AgoraVideoFrameObserverHelper {


    public enum BeautyProcessType {
        /**
         * Unknown
         *
         * @constructor Create empty Unknown
         */
        UNKNOWN,

        /**
         * Texture Oes
         *
         * @constructor Create empty Texture Oes
         */
        TEXTURE_OES,

        /**
         * Texture 2d
         *
         * @constructor Create empty Texture 2d
         */
        TEXTURE_2D,

        /**
         * I420
         *
         * @constructor Create empty I420
         */
        I420
    }

    public enum AgoraVideoFrameConvertState {
        INIT, START_DESTROY, DESTROYED,
    }

    public enum MirrorMode {

        // 没有镜像正常画面的定义：前置拍到画面和手机看到画面是左右不一致的，后置拍到画面和手机看到画面是左右一致的

        /**
         * Mirror Local Remote
         *
         * @constructor Create empty Mirror Local Remote
         */
        MIRROR_LOCAL_REMOTE, //本地远端都镜像，前置默认，本地和远端贴纸都正常

        /**
         * Mirror Local Only
         *
         * @constructor Create empty Mirror Local Only
         */
        MIRROR_LOCAL_ONLY, // 仅本地镜像，远端不镜像，，远端贴纸正常，本地贴纸镜像。用于打电话场景，电商直播场景(保证电商直播后面的告示牌文字是正的)；这种模式因为本地远端是反的，所以肯定有一边的文字贴纸方向会是反的

        /**
         * Mirror Remote Only
         *
         * @constructor Create empty Mirror Remote Only
         */
        MIRROR_REMOTE_ONLY, // 仅远端镜像，本地不镜像，远端贴纸正常，本地贴纸镜像

        /**
         * Mirror None
         *
         * @constructor Create empty Mirror None
         */
        MIRROR_NONE // 本地远端都不镜像，后置默认，本地和远端贴纸都正常
    }

    public static class CameraConfig {
        MirrorMode frontMirror = MirrorMode.MIRROR_LOCAL_REMOTE;// 前置默认镜像：本地远端都镜像
        MirrorMode backMirror = MirrorMode.MIRROR_NONE; // 后置默认镜像：本地远端都不镜像
    }


    public static boolean[] getCaptureAndRenderMirrorState(boolean isFrontCamera, CameraConfig cameraConfig) {
        boolean cMirror = false;
        if (isFrontCamera) {
            switch (cameraConfig.frontMirror) {
                case MIRROR_LOCAL_REMOTE:
                case MIRROR_REMOTE_ONLY:
                    cMirror = true;
                    break;
                case MIRROR_LOCAL_ONLY:
                case MIRROR_NONE:
                    cMirror = false;
                    break;
            }
        } else {
            switch (cameraConfig.backMirror) {
                case MIRROR_LOCAL_REMOTE:
                case MIRROR_REMOTE_ONLY:
                    cMirror = true;
                    break;
                case MIRROR_LOCAL_ONLY:
                case MIRROR_NONE:
                    cMirror = false;
                    break;
            }

        }
        boolean rMirror = false;
        if (isFrontCamera) {
            switch (cameraConfig.frontMirror) {
                case MIRROR_LOCAL_ONLY:
                case MIRROR_REMOTE_ONLY:
                    rMirror = true;
                    break;
                case MIRROR_LOCAL_REMOTE:
                case MIRROR_NONE:
                    rMirror = false;
                    break;
            }

        } else {
            switch (cameraConfig.backMirror) {
                case MIRROR_LOCAL_ONLY:
                case MIRROR_REMOTE_ONLY:
                    rMirror = true;
                    break;
                case MIRROR_LOCAL_REMOTE:
                case MIRROR_NONE:
                    rMirror = false;
                    break;
            }

        }

        return new boolean[]{cMirror, rMirror};

    }
}
