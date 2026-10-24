package com.desastres.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class DisasterConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue BLOCK_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_PERMISSION;
    public static final ForgeConfigSpec.DoubleValue DURATION_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue MAX_ACTIVE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("general");
        BLOCK_DAMAGE = b
                .comment("Si es false, los desastres NO modifican bloques (solo efectos visuales y empuje/dano a entidades).")
                .define("blockDamage", true);
        REQUIRE_PERMISSION = b
                .comment("Si es true, solo operadores (nivel 2) o jugadores en creativo pueden usar el control.",
                        "Ponlo en false si quieres que cualquiera con el item pueda usarlo (por ejemplo en supervivencia).")
                .define("requirePermission", true);
        DURATION_MULTIPLIER = b
                .comment("Multiplicador de duracion de los desastres (1.0 = normal).")
                .defineInRange("durationMultiplier", 1.0D, 0.1D, 5.0D);
        MAX_ACTIVE = b
                .comment("Maximo de desastres activos al mismo tiempo (los meteoritos sueltos no cuentan).")
                .defineInRange("maxActiveDisasters", 6, 1, 50);
        b.pop();
        SPEC = b.build();
    }

    private DisasterConfig() {}
}
