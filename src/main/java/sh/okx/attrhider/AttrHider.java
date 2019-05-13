package sh.okx.attrhider;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;

import java.util.List;

public class AttrHider extends JavaPlugin {
  @Override
  public void onEnable() {
    saveDefaultConfig();

    ProtocolManager manager = ProtocolLibrary.getProtocolManager();

    if (getConfig().getBoolean("hide-item-meta")) {
      manager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.ENTITY_EQUIPMENT) {
        @Override
        public void onPacketSending(PacketEvent event) {
          PacketContainer packet = event.getPacket();
          if (event.getPlayer().hasPermission("attrhider.bypass")) {
            return;
          }

          ItemStack item = packet.getItemModifier().read(0);
          if (item == null) {
            return;
          }
          if (!shouldBeObfuscated(item.getType())) {
            // remove some meta information for all items
            ItemMeta meta = item.getItemMeta();
            meta.setLore(null);
            meta.setDisplayName(null);
            meta.setUnbreakable(false);
            item.setItemMeta(meta);
            packet.getItemModifier().write(0, item);
            return;
          }

          // more thorough obfuscation
          Color colour = null;
          ItemMeta meta = item.getItemMeta();
          if (meta instanceof LeatherArmorMeta) {
            LeatherArmorMeta lam = (LeatherArmorMeta) item.getItemMeta();
            colour = lam.getColor();
          }
          ItemStack newItem = new ItemStack(item.getType(), 1, item.getDurability());
          if (meta.hasEnchants()) {
            newItem.addEnchantment(Enchantment.DURABILITY, 1);
          }
          if (colour != null) {
            LeatherArmorMeta lam = (LeatherArmorMeta) item.getItemMeta();
            lam.setColor(colour);
            newItem.setItemMeta(lam);
          }
          packet.getItemModifier().write(0, newItem);
        }
      });
    }
    if (getConfig().getBoolean("hide-potion-meta")) {
      manager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.ENTITY_EQUIPMENT) {
        @Override
        public void onPacketSending(PacketEvent event) {
          PacketContainer packet = event.getPacket();
          ItemStack item = packet.getItemModifier().read(0);

          if (item == null
              || !isPotion(item.getType())
              || event.getPlayer().hasPermission("attrhider.bypass")) {
            return;
          }

          // only show the potion type
          PotionMeta meta = (PotionMeta) item.getItemMeta();
          meta.clearCustomEffects();
          PotionData base = meta.getBasePotionData();
          meta.setBasePotionData(new PotionData(base.getType()));
          item.setItemMeta(meta);

          packet.getItemModifier().write(0, item);
        }
      });
    }
    if (getConfig().getBoolean("hide-potion-effects")) {
      manager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.ENTITY_EFFECT) {
        @Override
        public void onPacketSending(PacketEvent event) {
          PacketContainer packet = event.getPacket();
          if (event.getPlayer().hasPermission("attrhider.bypass")) {
            return;
          }
          StructureModifier<Integer> ints = packet.getIntegers();
          if (event.getPlayer().getEntityId() == ints.read(0)) {
            return;
          }
          // set amplifier to 0
          packet.getBytes().write(1, (byte) 0);
          // set duration to 0
          ints.write(1, 0);
        }
      });
    }
    if (getConfig().getBoolean("hide-health")) {
      manager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.ENTITY_METADATA) {
        @Override
        public void onPacketSending(PacketEvent event) {
          PacketContainer packet = event.getPacket();
          Player player = event.getPlayer();
          if (player.hasPermission("attrhider.bypass")) {
            return;
          }
          Entity entity = event.getPacket().getEntityModifier(event).read(0);
          StructureModifier<List<WrappedWatchableObject>> modifier = packet.getWatchableCollectionModifier();
          List<WrappedWatchableObject> read = modifier.read(0);

          if (player == entity
              || !(entity instanceof LivingEntity)
              || entity instanceof EnderDragon
              || entity instanceof Wither
              || entity.getPassengers().contains(player)) {
            return;
          }

          for (WrappedWatchableObject obj : read) {
            if (obj.getIndex() == 7) {
              float value = (float) obj.getValue();
              if (value > 0) {
                obj.setValue(1f);
              }
            }
          }
        }
      });
    }
  }

  private boolean shouldBeObfuscated(Material type) {
    return type == Material.DIAMOND_HELMET
        || type == Material.DIAMOND_CHESTPLATE
        || type == Material.DIAMOND_LEGGINGS
        || type == Material.DIAMOND_BOOTS
        || type == Material.IRON_HELMET
        || type == Material.IRON_CHESTPLATE
        || type == Material.IRON_LEGGINGS
        || type == Material.IRON_BOOTS
        || type == Material.GOLD_HELMET
        || type == Material.GOLD_CHESTPLATE
        || type == Material.GOLD_LEGGINGS
        || type == Material.GOLD_BOOTS
        || type == Material.LEATHER_HELMET
        || type == Material.LEATHER_CHESTPLATE
        || type == Material.LEATHER_LEGGINGS
        || type == Material.LEATHER_BOOTS
        || type == Material.DIAMOND_SWORD
        || type == Material.GOLD_SWORD
        || type == Material.IRON_SWORD
        || type == Material.STONE_SWORD
        || type == Material.WOOD_SWORD
        || type == Material.DIAMOND_AXE
        || type == Material.GOLD_AXE
        || type == Material.IRON_AXE
        || type == Material.STONE_AXE
        || type == Material.WOOD_AXE
        || type == Material.DIAMOND_PICKAXE
        || type == Material.GOLD_PICKAXE
        || type == Material.IRON_PICKAXE
        || type == Material.STONE_PICKAXE
        || type == Material.WOOD_PICKAXE
        || type == Material.DIAMOND_SPADE
        || type == Material.GOLD_SPADE
        || type == Material.IRON_SPADE
        || type == Material.STONE_SPADE
        || type == Material.WOOD_SPADE
        || type == Material.FIREWORK
        || type == Material.WRITTEN_BOOK
        || type == Material.ENCHANTED_BOOK;
  }

  private boolean isPotion(Material type) {
    return type == Material.POTION
        || type == Material.LINGERING_POTION
        || type == Material.SPLASH_POTION;
  }
}
