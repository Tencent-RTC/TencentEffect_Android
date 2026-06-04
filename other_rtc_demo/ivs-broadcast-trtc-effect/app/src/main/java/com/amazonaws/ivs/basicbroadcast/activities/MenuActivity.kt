package com.amazonaws.ivs.basicbroadcast.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.ivs.basicbroadcast.LicenseConstant
import com.amazonaws.ivs.basicbroadcast.databinding.ActivityMenuBinding
import com.tencent.effect.beautykit.TEBeautyKit
import com.tencent.effect.beautykit.config.TEUIConfig
import com.tencent.effect.beautykit.model.TEPanelDataModel
import com.tencent.effect.beautykit.model.TEUIProperty

class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.menuCustomMediaSources.setOnClickListener {
            onClick()
        }



        val panelDataModels = TEUIConfig.getInstance().getPanelDataList()
        panelDataModels.clear()

        //根据套餐功能添加对应的JSON
        val template = TEPanelDataModel("beauty_panel/beauty_template.json", TEUIProperty.UICategory.BEAUTY_TEMPLATE)
        val beauty1 = TEPanelDataModel("beauty_panel/beauty.json", TEUIProperty.UICategory.BEAUTY)
        val beauty2 = TEPanelDataModel("beauty_panel/beauty_image.json", TEUIProperty.UICategory.BEAUTY)
        val beauty4 = TEPanelDataModel("beauty_panel/beauty_shape.json", TEUIProperty.UICategory.BEAUTY)
        val beauty3 = TEPanelDataModel("beauty_panel/beauty_makeup.json", TEUIProperty.UICategory.BEAUTY)


        val lut = TEPanelDataModel("beauty_panel/lut.json", TEUIProperty.UICategory.LUT)
        val lightMakeup = TEPanelDataModel(
            "beauty_panel/light_makeup.json",
            TEUIProperty.UICategory.LIGHT_MAKEUP
        )
        val makeup = TEPanelDataModel("beauty_panel/makeup.json", TEUIProperty.UICategory.MAKEUP)
        val motion = TEPanelDataModel("beauty_panel/motion_2d.json", TEUIProperty.UICategory.MOTION)
        val motion2 = TEPanelDataModel("beauty_panel/motion_3d.json", TEUIProperty.UICategory.MOTION)
        val motion_gesture = TEPanelDataModel(
            "beauty_panel/motion_gesture.json",
            TEUIProperty.UICategory.MOTION
        )
        val seg = TEPanelDataModel("beauty_panel/segmentation.json", TEUIProperty.UICategory.SEGMENTATION)


        panelDataModels.add(template)
        panelDataModels.add(beauty1)
        panelDataModels.add(beauty2)
        panelDataModels.add(beauty4)
        panelDataModels.add(beauty3)
        panelDataModels.add(lut)

        panelDataModels.add(lightMakeup)
        panelDataModels.add(makeup)

        panelDataModels.add(motion)
        panelDataModels.add(motion2)
        panelDataModels.add(motion_gesture)
        panelDataModels.add(seg)
    }


    fun onClick() {

        TEBeautyKit.setupSDK(
            this,
            LicenseConstant.mXMagicLicenceUrl,
            LicenseConstant.mXMagicKey
        ) { errorCode, msg ->
            if (errorCode == 0) {
                startActivity(
                    Intent(
                        this@MenuActivity,
                        CustomSourceActivity::class.java
                    )
                )
            } else {
                Log.e("MenuActivity", "auth failed " + msg)
            }
        }
    }
}
