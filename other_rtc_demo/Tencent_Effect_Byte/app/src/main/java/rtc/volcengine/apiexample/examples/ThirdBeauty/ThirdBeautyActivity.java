package rtc.volcengine.apiexample.examples.ThirdBeauty;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.Intent;
import android.os.Bundle;

import com.tencent.effect.beautykit.TEBeautyKit;
import rtc.volcengine.apiexample.common.LicenseConstant;
import com.tencent.effect.beautykit.config.TEUIConfig;
import com.tencent.effect.beautykit.model.TEPanelDataModel;
import com.tencent.effect.beautykit.model.TEPanelViewResModel;
import com.tencent.effect.beautykit.model.TEUIProperty;

import java.util.List;

import rtc.volcengine.apiexample.R;
import rtc.volcengine.apiexample.common.annotations.ApiExample;

import rtc.volcengine.apiexample.examples.ThirdBeauty.xmagic.XmagicActivity;

@ApiExample(name = "第三方美颜", order = 9, category = "重要组件")
public class ThirdBeautyActivity extends AppCompatActivity {

    AppCompatButton btnByteBeauty;
    AppCompatButton btn_xmagic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_third_beauty);
        setTitle(R.string.title_third_beauty);


        btn_xmagic = findViewById(R.id.btn_xmagic);
        btn_xmagic.setOnClickListener(v -> initXmagic());
    }

    private void initXmagic() {
        this.initPanelConfig();
        //setupSDK 是一个聚合方法，内部包含了 鉴权和复制资源（只在第一次执行的时候复制资源，成功之后，不升级SDK版本，不会在复制资源，只会执行鉴权的流程）
        //只有在此方法回调成功之后才能使用美颜，要不会出现美颜没有效果
        //setupSDK is an aggregation method that includes authentication and resource copying (resources are only copied during the first execution. After success, the SDK version will not be upgraded, resources will not be copied, and the authentication process will only be executed).
        //Beautification can only be used after the callback of this method is successful, otherwise the beautification will have no effect.
        TEBeautyKit.setupSDK(getApplicationContext(), LicenseConstant.mXMagicLicenceUrl, LicenseConstant.mXMagicKey, new TEBeautyKit.SetupSDKCallback() {
            @Override
            public void onResult(int errorCode, String msg) {
                if (errorCode == LicenseConstant.AUTH_STATE_SUCCEED) {
                    startActivity();
                }
            }
        });
    }


    private void startActivity() {
        startActivity(new Intent(ThirdBeautyActivity.this, XmagicActivity.class));
    }


    private void initPanelConfig() {
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
    }
}