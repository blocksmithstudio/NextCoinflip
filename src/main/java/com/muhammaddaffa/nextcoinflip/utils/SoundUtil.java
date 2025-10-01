package com.muhammaddaffa.nextcoinflip.utils;

import com.muhammaddaffa.mdlib.utils.Common;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtil {

    public static void click(Player player) {
        Common.playSound(player, Sound.UI_BUTTON_CLICK);
    }

    public static void villagerNo(Player player) {
        Common.playSound(player, Sound.ENTITY_VILLAGER_NO);
    }

    public static void pling(Player player) {
        Common.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
    }

}
