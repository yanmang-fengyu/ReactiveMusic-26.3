package circuitlord.reactivemusic.config;

import net.minecraft.network.chat.Component;


import circuitlord.reactivemusic.RMSongpackLoader;
import circuitlord.reactivemusic.ReactiveMusic;
//import circuitlord.reactivemusic.SongLoader;
import circuitlord.reactivemusic.SongpackZip;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.phys.Vec3;

import static dev.isxander.yacl3.platform.YACLPlatform.getConfigDir;



public class ModConfig {

    public static final ValueFormatter<ChatFormatting> FORMATTING_FORMATTER = formatting -> Component.literal(StringUtils.capitalize(formatting.toString().replaceAll("_", " ")));


    public static ModConfig getConfig() {
        return GSON.instance();
    }

    public static void saveConfig() {
        GSON.save();
    }

    public static final ConfigClassHandler<ModConfig> GSON = ConfigClassHandler.createBuilder(ModConfig.class)
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(getConfigDir().resolve("ReactiveMusic.json5"))
                    .setJson5(true)
                    //.appendGsonBuilder(GsonBuilder::setPrettyPrining)
                    .build())
            .build();



    @SerialEntry
    public MusicDelayLength musicDelayLength2 = MusicDelayLength.SONGPACK_DEFAULT;

    @SerialEntry
    public MusicSwitchSpeed musicSwitchSpeed2 = MusicSwitchSpeed.SONGPACK_DEFAULT;

    /**
     * 自定义歌曲间隔（秒），-1 表示使用预设 / 音乐包默认值。
     */
    @SerialEntry
    public int customSongDelaySeconds = -1;

    /**
     * 自定义切歌等待（秒），事件失效后多久开始淡出，-1 表示使用预设 / 音乐包默认值。
     */
    @SerialEntry
    public int customSongSwitchSeconds = -1;

    /**
     * 淡出时长（秒）。
     */
    @SerialEntry
    public float fadeOutSeconds = 7.5f;

    /**
     * 暂停游戏或打开菜单时音乐保留的音量百分比（0-100）。
     */
    @SerialEntry
    public int pausedMusicVolumePercent = 70;

    @SerialEntry
    public boolean debugModeEnabled = false;

    //@SerialEntry
    //public boolean treatAsWhitelist = false;

    //@SerialEntry
    //public double confirmationResetDelay = 1.0;

    @SerialEntry
    public String loadedUserSongpack = "";

    @SerialEntry
    public List<String> blacklistedDimensions = new ArrayList<>();

    @SerialEntry
    public HashMap<String, Vec3> savedHomePositions = new HashMap<>();

    @SerialEntry
    public List<String> soundsMuteMusic = new ArrayList<>();

    @SerialEntry
    public boolean hasForcedInitialVolume = false;





    private static Component secondsText(int seconds) {
        if (seconds < 0) {
            return Component.translatable("reactive_music.value.preset_default");
        }
        return Component.translatable("reactive_music.value.seconds", seconds);
    }

    private static Component secondsText(float seconds) {
        return Component.translatable("reactive_music.value.seconds_decimal", String.format(Locale.ROOT, "%.1f", seconds));
    }

    private static Component percentText(int percent) {
        return Component.translatable("reactive_music.value.percent", percent);
    }

    private static Component enumText(String key, String name) {
        return Component.translatable("reactive_music.enum." + key + "." + name);
    }


    public static Screen createScreen(Screen parent) {

        //SongLoader.fetchAvailableSongpacks();
        RMSongpackLoader.fetchAvailableSongpacks();

        return YetAnotherConfigLib.create(ModConfig.GSON, ((defaults, config, builder) -> {



            var songpacksBuilder = ConfigCategory.createBuilder();
            songpacksBuilder.name(Component.translatable("reactive_music.config.category.songpacks"));


            for (var songpackZip : RMSongpackLoader.availableSongpacks) {

                boolean isLoaded = false;

                if (ReactiveMusic.currentSongpack != null) {

                    isLoaded = Objects.equals(ReactiveMusic.currentSongpack.config.name, songpackZip.config.name);
                }

                if (songpackZip.blockLoading) {
                    songpacksBuilder.option(ButtonOption.createBuilder()
                            .name(Component.translatable("reactive_music.config.songpack.failed",
                                    songpackZip.config == null ? Component.translatable("reactive_music.config.songpack.unknown")
                                            : Component.literal(songpackZip.config.name)))
                            .description(
                                    OptionDescription.createBuilder()
                                            .text(Component.translatable("reactive_music.config.songpack.failed.desc",
                                                    songpackZip.errorString))
                                            .build()
                            )

                            .available(false)
                            .text(Component.empty())
                            .action((yaclScreen, buttonOption) -> {

                            })
                            .build());

                }
                else {

                    String name = songpackZip.config.name;
                    String description = songpackZip.config.description + "\n\n" +
                            Component.translatable("reactive_music.config.songpack.credits").getString() + "\n" +
                            songpackZip.config.credits;


                    boolean allowedToShowErrors =
                            getConfig().debugModeEnabled ||
                            songpackZip.isv05OldSongpack ||
                            (songpackZip.path != null && !songpackZip.path.toString().endsWith(".zip"));


                    if (allowedToShowErrors && !songpackZip.errorString.isEmpty()) {
                        name = Component.translatable("reactive_music.config.songpack.warning_prefix").getString() + name;
                        description = Component.translatable("reactive_music.config.songpack.warning_desc",
                                songpackZip.errorString).getString() + "\n\n" + description;
                    }

                    String finalName = name;
                    songpacksBuilder.option(ButtonOption.createBuilder()
                            .name(Component.literal(finalName))
                            .description(
                                    OptionDescription.createBuilder()
                                            .text(Component.literal(description))
                                            .build()
                            )

                            .available(!isLoaded)

                            .text(Component.translatable(isLoaded ? "reactive_music.config.songpack.loaded" : "reactive_music.config.songpack.load"))


                            .action((yaclScreen, buttonOption) -> {
                                setActiveSongpack(songpackZip);

                                Minecraft.getInstance().setScreenAndShow(ModConfig.createScreen(parent));
                            })



                            .build());
                }

            }


            builder.category(songpacksBuilder.build());




            builder
                    .title(Component.translatable("reactive_music.title"))


                    .category(ConfigCategory.createBuilder()
                            .name(Component.translatable("reactive_music.config.category.general"))

                            .option(Option.<MusicDelayLength>createBuilder()
                                    .name(Component.translatable("reactive_music.config.option.music_delay"))
                                    .binding(defaults.musicDelayLength2, () -> config.musicDelayLength2, newVal -> config.musicDelayLength2 = newVal )
                                    .controller(opt -> EnumControllerBuilder.create(opt).enumClass(MusicDelayLength.class)
                                            .valueFormatter(v -> enumText("delay", v.name())))
                                    .description(
                                            OptionDescription.createBuilder()
                                                    .text(Component.translatable("reactive_music.config.option.music_delay.desc"))
                                                    .build()
                                    )

                                    .build())

                            .option(Option.<MusicSwitchSpeed>createBuilder()
                                    .name(Component.translatable("reactive_music.config.option.music_switch_speed"))
                                    .binding(defaults.musicSwitchSpeed2, () -> config.musicSwitchSpeed2, newVal -> config.musicSwitchSpeed2 = newVal )
                                    .controller(opt -> EnumControllerBuilder.create(opt).enumClass(MusicSwitchSpeed.class)
                                            .valueFormatter(v -> enumText("switch", v.name())))
                                    .description(
                                            OptionDescription.createBuilder()
                                                    .text(Component.translatable("reactive_music.config.option.music_switch_speed.desc"))
                                                    .build()
                                    )

                                    .build())

                            .option(Option.<Integer>createBuilder()
                                    .name(Component.translatable("reactive_music.config.option.custom_delay"))
                                    .description(OptionDescription.createBuilder()
                                            .text(Component.translatable("reactive_music.config.option.custom_delay.desc"))
                                            .build())
                                    .binding(defaults.customSongDelaySeconds, () -> config.customSongDelaySeconds, newVal -> config.customSongDelaySeconds = newVal)
                                    .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-1, 300).step(1)
                                            .valueFormatter(ModConfig::secondsText))
                                    .build())

                            .option(Option.<Integer>createBuilder()
                                    .name(Component.translatable("reactive_music.config.option.custom_switch"))
                                    .description(OptionDescription.createBuilder()
                                            .text(Component.translatable("reactive_music.config.option.custom_switch.desc"))
                                            .build())
                                    .binding(defaults.customSongSwitchSeconds, () -> config.customSongSwitchSeconds, newVal -> config.customSongSwitchSeconds = newVal)
                                    .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(-1, 300).step(1)
                                            .valueFormatter(ModConfig::secondsText))
                                    .build())

                            .option(Option.<Float>createBuilder()
                                    .name(Component.translatable("reactive_music.config.option.fade_out"))
                                    .description(OptionDescription.createBuilder()
                                            .text(Component.translatable("reactive_music.config.option.fade_out.desc"))
                                            .build())
                                    .binding(defaults.fadeOutSeconds, () -> config.fadeOutSeconds, newVal -> config.fadeOutSeconds = newVal)
                                    .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.5f, 30f).step(0.5f)
                                            .valueFormatter(ModConfig::secondsText))
                                    .build())

                            .option(Option.<Integer>createBuilder()
                                    .name(Component.translatable("reactive_music.config.option.paused_volume"))
                                    .description(OptionDescription.createBuilder()
                                            .text(Component.translatable("reactive_music.config.option.paused_volume.desc"))
                                            .build())
                                    .binding(defaults.pausedMusicVolumePercent, () -> config.pausedMusicVolumePercent, newVal -> config.pausedMusicVolumePercent = newVal)
                                    .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 100).step(5)
                                            .valueFormatter(ModConfig::percentText))
                                    .build())

                            .build())


                    .category(ConfigCategory.createBuilder()
                            .name(Component.translatable("reactive_music.config.category.debug"))
                            .tooltip(Component.translatable("reactive_music.config.category.debug.tooltip"))


                            .option(Option.<Boolean>createBuilder()
                                    .name(Component.translatable("reactive_music.config.option.debug_mode_enabled"))
                                    .description(OptionDescription.createBuilder()
                                            .text(Component.translatable("reactive_music.config.option.debug_mode_enabled.desc"))
                                            .build())
                                    .binding(defaults.debugModeEnabled, () -> config.debugModeEnabled, newVal -> config.debugModeEnabled = newVal )
                                    .controller(TickBoxControllerBuilder::create)
                                    .build())




                            .build())




            .build();





            return builder;

        })).generateScreen(parent);
    }





    public static void setActiveSongpack(SongpackZip songpack) {

        if (songpack.embedded) {
            getConfig().loadedUserSongpack = "";
        }
        else {
            getConfig().loadedUserSongpack = songpack.config.name;
        }

        GSON.save();

        ReactiveMusic.setActiveSongpack(songpack);

    }


    public static <E extends Enum<E>> Function<Option<E>, ControllerBuilder<E>> getEnumDropdownControllerFactory(ValueFormatter<E> formatter) {
        return opt -> EnumDropdownControllerBuilder.create(opt).formatValue(formatter);
    }

}
