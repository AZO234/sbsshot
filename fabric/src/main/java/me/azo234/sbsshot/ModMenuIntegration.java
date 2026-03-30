package me.azo234.sbsshot;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * ModMenu に Config 画面を登録する。
 * entrypoints の "modmenu" に指定する。
 *
 * ModMenu が入っていない環境でもクラスロードされないよう
 * fabric.mod.json の suggests に modmenu を記述し、
 * entrypoints で "modmenu" キーにのみ登録する。
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new StereoConfigScreen(parent, SbsShotModClient.STEREO_CONFIG);
    }
}
