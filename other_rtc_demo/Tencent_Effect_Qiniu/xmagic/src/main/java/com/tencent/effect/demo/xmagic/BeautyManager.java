package com.tencent.effect.demo.xmagic;

public class BeautyManager {
    private BeautyManager(){}

    static class ClassHolder {
        static final BeautyManager BEAUTY_MANAGER = new BeautyManager();
    }

    public static BeautyManager getInstance(){
        return ClassHolder.BEAUTY_MANAGER;
    }



    public void onCreate(){

    }


    public void onDestroy(){

    }






}
