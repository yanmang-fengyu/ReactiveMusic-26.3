package circuitlord.reactivemusic;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

public class VersionHelper {



    // 1.21.4


    // 1.20

/*
    public static Entity GetRidingEntity(ClientPlayerEntity player) {
        return player.getControllingVehicle();
    }
*/




    // -------------------------------------


    // 1.19

    public static Entity GetRidingEntity(LocalPlayer player) {
        return player.getVehicle();
    }



}
