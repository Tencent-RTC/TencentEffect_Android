package com.tencent.effect.agora.demo;

import android.app.Application;

import com.tencent.effect.beautykit.TEBeautyKit;
import com.tencent.effect.beautykit.config.TEUIConfig;
import com.tencent.effect.agora.demo.beauty.LicenseConstant;
import com.tencent.effect.beautykit.model.TEPanelDataModel;
import com.tencent.effect.beautykit.model.TEPanelViewResModel;
import com.tencent.effect.beautykit.model.TEUIProperty;

import java.io.File;
import java.util.List;

public class DemoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        this.initXmagic();
    }


    private void initXmagic(){
        TEBeautyKit.setResPath((new File(getFilesDir(), "xmagic_dir").getAbsolutePath()));

        List<TEPanelDataModel> panelDataModels = TEUIConfig.getInstance().getPanelDataList();
        panelDataModels.clear();
        //根据套餐功能添加对应的JSON
        TEPanelDataModel template = new TEPanelDataModel("beauty_panel/beauty_template.json", TEUIProperty.UICategory.BEAUTY_TEMPLATE);
        TEPanelDataModel beauty1 = new TEPanelDataModel("beauty_panel/beauty.json", TEUIProperty.UICategory.BEAUTY);
        TEPanelDataModel beauty2 = new TEPanelDataModel("beauty_panel/beauty_image.json", TEUIProperty.UICategory.BEAUTY);
        TEPanelDataModel beauty4 = new TEPanelDataModel("beauty_panel/beauty_shape.json", TEUIProperty.UICategory.BEAUTY);
        TEPanelDataModel beauty3 = new TEPanelDataModel("beauty_panel/beauty_makeup.json", TEUIProperty.UICategory.BEAUTY);


        TEPanelDataModel lut = new TEPanelDataModel("beauty_panel/lut.json", TEUIProperty.UICategory.LUT);
        TEPanelDataModel lightMakeup = new TEPanelDataModel("beauty_panel/light_makeup.json",
                TEUIProperty.UICategory.LIGHT_MAKEUP);
        TEPanelDataModel makeup = new TEPanelDataModel("beauty_panel/makeup.json", TEUIProperty.UICategory.MAKEUP);
        TEPanelDataModel motion = new TEPanelDataModel("beauty_panel/motion_2d.json", TEUIProperty.UICategory.MOTION);
        TEPanelDataModel motion2 = new TEPanelDataModel("beauty_panel/motion_3d.json", TEUIProperty.UICategory.MOTION);
        TEPanelDataModel motion_gesture = new TEPanelDataModel("beauty_panel/motion_gesture.json",
                TEUIProperty.UICategory.MOTION);
        TEPanelDataModel seg = new TEPanelDataModel("beauty_panel/segmentation.json", TEUIProperty.UICategory.SEGMENTATION);


        panelDataModels.add(template);
        panelDataModels.add(beauty1);
        panelDataModels.add(beauty2);
        panelDataModels.add(beauty4);
        panelDataModels.add(beauty3);
        panelDataModels.add(lut);

        panelDataModels.add(lightMakeup);
        panelDataModels.add(makeup);

        panelDataModels.add(motion);
        panelDataModels.add(motion2);
        panelDataModels.add(motion_gesture);
        panelDataModels.add(seg);

        TEBeautyKit.setTELicense(this, LicenseConstant.mXMagicLicenceUrl,LicenseConstant.mXMagicKey,null);
    }
}
