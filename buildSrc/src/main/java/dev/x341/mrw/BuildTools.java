package dev.x341.mrw;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-Minecraft-version configuration resolver — the single place that maps the active
 * {@code minecraftVersion} (from gradle.properties) to every version-specific dependency
 * coordinate. This is what makes the build "multi-version": change one property and every
 * loader, mapping, API and toolchain version follows.
 *
 * <p>Fills the role of the per-version property tables in Joban-Client-Mod's gradle.properties
 * (yarn_mappings_1.20.4=..., forge_version_1.20.4=..., ...), collapsed into one Java table.</p>
 *
 * <p>Usage from a subproject build.gradle (the root build.gradle constructs one instance per
 * subproject and exposes it as the {@code buildTools} ext property):
 * <pre>{@code
 *   mappings "net.fabricmc:yarn:${buildTools.yarnVersion}:v2"
 * }</pre></p>
 *
 * <p><b>IMPORTANT — verify these coordinates.</b> The version strings below are the correct
 * <i>shape</i> and sensible defaults, but exact build numbers drift over time. Before a real
 * build, confirm each against:
 * <ul>
 *   <li>Yarn / Fabric loader / Fabric API → https://fabricmc.net/develop (or https://modmuss50.me/fabric.html)</li>
 *   <li>Mod Menu → https://modrinth.com/mod/modmenu/versions</li>
 *   <li>Forge → https://files.minecraftforge.net/</li>
 * </ul>
 * The mapping jar itself (Minecraft-Mappings-&lt;loader&gt;-&lt;version&gt;-0.0.1.jar) is selected
 * automatically by the subproject build via the {@code libs} flatDir repository.</p>
 */
public final class BuildTools {

    private final String minecraftVersion;
    private final String loader;
    private final VersionConfig config;

    public BuildTools(String minecraftVersion, String loader, Object project) {
        this.minecraftVersion = minecraftVersion;
        this.loader = loader;
        this.config = CONFIGS.get(minecraftVersion);
        if (this.config == null) {
            throw new IllegalArgumentException("Unsupported minecraftVersion '" + minecraftVersion
                    + "'. Supported: " + CONFIGS.keySet());
        }
    }

    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    public String getLoader() {
        return loader;
    }

    /**
     * The Minecraft version collapsed into a fixed-width digit string, e.g. 1.20.1 → "12001",
     * 1.16.5 → "11605". This is the value handed to the Manifold preprocessor as
     * {@code MC_VERSION}; because every value has the same length, lexicographic string
     * comparison ({@code #if MC_VERSION >= "11903"}) matches numeric ordering.
     */
    public String getMergedMinecraftVersion() {
        final String[] split = minecraftVersion.split("\\.");
        final String minor = split.length > 2 ? String.format("%02d", Integer.parseInt(split[2])) : "00";
        return split[0] + split[1] + minor;
    }

    public int getMergedMinecraftVersionInt() {
        return Integer.parseInt(getMergedMinecraftVersion());
    }

    public String getYarnVersion() {
        return config.yarn;
    }

    /** Fabric loader version. Named {@code getFabricVersion} to match upstream call sites. */
    public String getFabricVersion() {
        return config.fabricLoader;
    }

    public String getFabricApiVersion() {
        return config.fabricApi;
    }

    public String getModMenuVersion() {
        return config.modMenu;
    }

    public String getForgeVersion() {
        return config.forge;
    }

    /** Bytecode target ({@code options.release}) — what the shipped jar must run on. */
    public int getJavaRelease() {
        return config.java;
    }

    /**
     * JDK used to run javac. Never below 16 because the Manifold preprocessor plugin needs a
     * modern compiler; the emitted bytecode still targets {@link #getJavaRelease()}.
     */
    public int getJavaLanguageVersion() {
        return Math.max(config.java, 16);
    }

    /** Mixin config compatibilityLevel matching the bytecode target. */
    public String getMixinCompatibilityLevel() {
        return "JAVA_" + config.java;
    }

    /** Resource pack_format for pack.mcmeta (Forge needs one; Fabric supplies its own). */
    public int getPackFormat() {
        return config.packFormat;
    }

    // Lean scaffold: optional-integration hooks default to off. Flip per-version if you add support.
    public boolean hasJadeSupport() {
        return false;
    }

    public boolean hasWthitSupport() {
        return false;
    }

    private static final class VersionConfig {
        final String yarn;
        final String fabricLoader;
        final String fabricApi;
        final String modMenu;
        final String forge;
        final int java;
        final int packFormat;

        VersionConfig(String yarn, String fabricLoader, String fabricApi, String modMenu, String forge, int java, int packFormat) {
            this.yarn = yarn;
            this.fabricLoader = fabricLoader;
            this.fabricApi = fabricApi;
            this.modMenu = modMenu;
            this.forge = forge;
            this.java = java;
            this.packFormat = packFormat;
        }
    }

    private static final Map<String, VersionConfig> CONFIGS = new HashMap<>();

    static {
        //          MC        yarn                     loader      fabric-api          modmenu    forge     java pack
        CONFIGS.put("1.16.5", new VersionConfig("1.16.5+build.10", "0.16.14", "0.42.0+1.16",   "1.16.23", "36.2.42", 8,  6));
        CONFIGS.put("1.17.1", new VersionConfig("1.17.1+build.65", "0.16.14", "0.46.1+1.17",   "2.0.17",  "37.1.1",  16, 7));
        CONFIGS.put("1.18.2", new VersionConfig("1.18.2+build.4",  "0.16.14", "0.77.0+1.18.2", "3.2.5",   "40.2.21", 17, 8));
        CONFIGS.put("1.19.2", new VersionConfig("1.19.2+build.28", "0.16.14", "0.77.0+1.19.2", "4.1.2",   "43.3.13", 17, 9));
        CONFIGS.put("1.19.4", new VersionConfig("1.19.4+build.2",  "0.16.14", "0.87.2+1.19.4", "6.3.1",   "45.3.0",  17, 13));
        CONFIGS.put("1.20.1", new VersionConfig("1.20.1+build.10", "0.16.14", "0.92.9+1.20.1", "7.2.2",   "47.4.0",  17, 15));
        CONFIGS.put("1.20.4", new VersionConfig("1.20.4+build.3",  "0.16.14", "0.97.2+1.20.4", "9.2.0",   "49.1.0",  17, 22));
    }
}
