package com.legacy.aether.client;

import java.util.List;

import com.legacy.aether.client.gui.GuiCustomizationScreen;
import com.legacy.aether.client.gui.GuiEnterAether;
import com.legacy.aether.client.gui.button.GuiCustomizationScreenButton;
import com.legacy.aether.client.gui.button.GuiGlowButton;
import com.legacy.aether.client.gui.button.GuiHaloButton;
import com.legacy.aether.client.gui.menu.AetherMainMenu;
import com.legacy.aether.client.gui.menu.GuiMenuToggleButton;
import com.legacy.aether.player.perks.AetherRankings;
import cpw.mods.fml.client.FMLClientHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.client.event.RenderPlayerEvent.SetArmorModel;

import com.legacy.aether.AetherConfig;
import com.legacy.aether.client.gui.AetherLoadingScreen;
import com.legacy.aether.client.gui.button.GuiAccessoryButton;
import com.legacy.aether.client.renders.entity.PlayerAetherRenderer;
import com.legacy.aether.entities.EntitiesAether;
import com.legacy.aether.items.ItemAetherSpawnEgg;
import com.legacy.aether.items.ItemsAether;
import com.legacy.aether.items.armor.ItemAetherArmor;
import com.legacy.aether.network.AetherGuiHandler;
import com.legacy.aether.network.AetherNetwork;
import com.legacy.aether.network.packets.PacketOpenContainer;
import com.legacy.aether.player.PlayerAether;

import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class AetherClientEvents {

	private static boolean wasInAether = false;

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) throws Exception {
		Minecraft mc = Minecraft.getMinecraft();
		TickEvent.Phase phase = event.phase;
		TickEvent.Type type = event.type;

		if (phase == TickEvent.Phase.END) {
			if (type.equals(TickEvent.Type.CLIENT)) {
				if (!AetherConfig.triviaDisabled()) {
					if (!(mc.loadingScreen instanceof AetherLoadingScreen)) {
						mc.loadingScreen = new AetherLoadingScreen(mc);
					}
				}

				if (mc.thePlayer != null && !(mc.thePlayer.movementInput instanceof AetherMovementInput)) {
					mc.thePlayer.movementInput = new AetherMovementInput(mc, mc.gameSettings);
				}
			}
		}

		if (phase == TickEvent.Phase.START) {
			if (type.equals(TickEvent.Type.CLIENT)) {
				if (mc.currentScreen == null || mc.currentScreen.allowUserInput) {
					if (!mc.thePlayer.isUsingItem()) {
						if (GameSettings.isKeyDown(mc.gameSettings.keyBindPickBlock)) {
							this.sendPickupPacket(mc);
						}
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onOpenGui(GuiOpenEvent event)
	{
		Minecraft mc = FMLClientHandler.instance().getClient();

		if (mc.thePlayer != null && event.gui instanceof GuiDownloadTerrain)
		{
			GuiEnterAether enterAether = new GuiEnterAether(true);
			GuiEnterAether exitAether = new GuiEnterAether(false);

			if (mc.thePlayer.dimension == AetherConfig.getAetherDimensionID())
			{
				event.gui = enterAether;
				wasInAether = true;
			}

			else if (wasInAether)
			{
				event.gui = exitAether;
				wasInAether = false;
			}
		}
	}


	private void sendPickupPacket(Minecraft mc) {
		if (mc.objectMouseOver != null) {
			if (!this.onPickEntity(mc.objectMouseOver, mc.thePlayer, mc.theWorld)) {
				return;
			}

			if (mc.thePlayer.capabilities.isCreativeMode) {
				int index = mc.thePlayer.inventoryContainer.inventorySlots.size() - 9 + mc.thePlayer.inventory.currentItem;

				mc.playerController.sendSlotPacket(mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem), index);
			}
		}
	}

	private boolean onPickEntity(MovingObjectPosition target, EntityPlayer player, World world) {
		ItemStack result = null;
		boolean isCreative = player.capabilities.isCreativeMode;

		if (!isCreative) {
			return false;
		}

		if (target.entityHit != null) {
			int id = EntitiesAether.getEntityID(target.entityHit);

			if (id >= 0 && ItemAetherSpawnEgg.entityEggs.containsKey(id)) {
				result = new ItemStack(ItemsAether.aether_spawn_egg, 1, id);
			}
		}

		if (result == null) {
			return false;
		}

		for (int x = 0; x < 9; x++) {
			ItemStack stack = player.inventory.getStackInSlot(x);

			if (stack != null && stack.isItemEqual(result) && ItemStack.areItemStackTagsEqual(stack, result)) {
				player.inventory.currentItem = x;

				return true;
			}
		}

		int slot = player.inventory.getFirstEmptyStack();

		if (slot < 0 || slot >= 9) {
			slot = player.inventory.currentItem;
		}

		player.inventory.setInventorySlotContents(slot, result);
		player.inventory.currentItem = slot;

		return true;
	}

	@SubscribeEvent
	public void onBowPulled(FOVUpdateEvent event) {
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;

		if (player == null || (player != null && player.getCurrentEquippedItem() == null)) {
			return;
		}

		Item item = player.getCurrentEquippedItem().getItem();

		if (item == ItemsAether.phoenix_bow) {
			int i = player.getItemInUseDuration();
			float f1 = (float) i / 20.0F;

			if (f1 > 1.0F) {
				f1 = 1.0F;
			} else {
				f1 = f1 * f1;
			}

			float original = event.fov;

			original *= 1.0F - f1 * 0.15F;

			event.newfov = original;
		}
	}

	private static final GuiAccessoryButton ACCESSORY_BUTTON = new GuiAccessoryButton(0, 0);

	private static final GuiMenuToggleButton MAIN_MENU_BUTTON = new GuiMenuToggleButton(0, 0);

	private static int previousSelectedTabIndex = -1;

	@SubscribeEvent
	@SuppressWarnings("unchecked")
	public void onGuiOpened(GuiScreenEvent.InitGuiEvent.Post event) {
		if (event.gui instanceof GuiContainer) {
			EntityPlayer player = Minecraft.getMinecraft().thePlayer;
			Class<?> clazz = event.gui.getClass();

			int guiLeft = ObfuscationReflectionHelper.getPrivateValue(GuiContainer.class, (GuiContainer) event.gui, "guiLeft", "field_147003_i");
			int guiTop = ObfuscationReflectionHelper.getPrivateValue(GuiContainer.class, (GuiContainer) event.gui, "guiTop", "field_147009_r");

			if (player.capabilities.isCreativeMode) {
				if (event.gui instanceof GuiContainerCreative) {
					if (((GuiContainerCreative) event.gui).func_147056_g() == CreativeTabs.tabInventory.getTabIndex()) {
						event.buttonList.add(ACCESSORY_BUTTON.setPosition(guiLeft + 28, guiTop + 38));
						previousSelectedTabIndex = CreativeTabs.tabInventory.getTabIndex();
					}
				}
			} else if (clazz == GuiInventory.class) {
				event.buttonList.add(ACCESSORY_BUTTON.setPosition(guiLeft + 26, guiTop + 65));
			}
		}

		if (AetherConfig.config.get("Misc", "Enables the Aether Menu toggle button", false).getBoolean() && event.gui instanceof GuiMainMenu)
		{
			event.buttonList.add(MAIN_MENU_BUTTON.setPosition(event.gui.width - 24, 4));
		}

		if (AetherConfig.config.get("Misc", "Enables the Aether Menu", false).getBoolean() && event.gui.getClass() == GuiMainMenu.class)
		{
			Minecraft.getMinecraft().displayGuiScreen(new AetherMainMenu());
		}

		if (event.gui.getClass() == GuiOptions.class)
		{
			if (Minecraft.getMinecraft().thePlayer != null)
			{
				if (AetherRankings.isRankedPlayer(Minecraft.getMinecraft().thePlayer.getUniqueID()))
				{
					event.buttonList.add(new GuiCustomizationScreenButton(545, event.gui.width / 2 - 155, event.gui.height / 6 + 48 - 6, 150, 20, I18n.format("gui.options.perk_customization")));
				}
			}
		}
	}

	@SubscribeEvent
	public void onMouseClicked(DrawScreenEvent.Post event) {
		if (Minecraft.getMinecraft().currentScreen instanceof GuiContainerCreative) {
			GuiContainerCreative guiScreen = (GuiContainerCreative) Minecraft.getMinecraft().currentScreen;

			if (previousSelectedTabIndex != guiScreen.func_147056_g()) {
				List<GuiButton> buttonList = ObfuscationReflectionHelper.getPrivateValue(GuiScreen.class, (GuiScreen) guiScreen, 4);

				if (guiScreen.func_147056_g() == CreativeTabs.tabInventory.getTabIndex() && !buttonList.contains(ACCESSORY_BUTTON)) {
					int guiLeft = ObfuscationReflectionHelper.getPrivateValue(GuiContainer.class, (GuiContainer) guiScreen, "guiLeft", "field_147003_i");
					int guiTop = ObfuscationReflectionHelper.getPrivateValue(GuiContainer.class, (GuiContainer) guiScreen, "guiTop", "field_147009_r");

					buttonList.add(ACCESSORY_BUTTON.setPosition(guiLeft + 28, guiTop + 38));
				} else if (previousSelectedTabIndex == CreativeTabs.tabInventory.getTabIndex()) {
					buttonList.remove(ACCESSORY_BUTTON);
				}

				previousSelectedTabIndex = guiScreen.func_147056_g();
			}
		}
	}

	@SubscribeEvent
	public void onDrawGui(GuiScreenEvent.DrawScreenEvent.Pre event)
	{
		if (!AetherConfig.config.get("Misc", "Enables the Aether Menu", false).getBoolean() && event.gui.getClass() == AetherMainMenu.class)
		{
			Minecraft.getMinecraft().displayGuiScreen(new GuiMainMenu());
		}
	}

	@SubscribeEvent
	public void onButtonPressed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
		Class<?> clazz = event.gui.getClass();

		if ((clazz == GuiInventory.class || clazz == GuiContainerCreative.class) && event.button.id == 18067) {
			AetherNetwork.sendToServer(new PacketOpenContainer(AetherGuiHandler.accessories));
		}

		if (event.button.getClass() == GuiCustomizationScreenButton.class)
		{
			Minecraft.getMinecraft().displayGuiScreen(new GuiCustomizationScreen(event.gui));
		}
	}

	@SubscribeEvent
	public void onRenderInvisibility(RenderPlayerEvent.Pre event) {
		EntityPlayer player = event.entityPlayer;
		PlayerAether playerAether = PlayerAether.get(player);

		if (playerAether != null) {
			if (playerAether.getAccessoryInventory().wearingAccessory(new ItemStack(ItemsAether.invisibility_cape))) {
				event.setCanceled(true);
			}
		}

		PlayerAetherRenderer.instance().setPartialTicks(event.partialRenderTick);
	}

	@SubscribeEvent
	public void onRenderAetherCape(RenderPlayerEvent.Specials.Pre event) {
		event.renderCape = !PlayerAetherRenderer.instance().isCapeRendering();
	}

	@SubscribeEvent
	public void onRenderAetherArmor(SetArmorModel event) {
		if (event.stack != null && event.stack.getItem() instanceof ItemAetherArmor) {
			event.result = PlayerAetherRenderer.instance().renderAetherArmor(PlayerAether.get(event.entityPlayer), event.renderer, event.stack, 3 - event.slot);
		}
	}

	@SubscribeEvent
	public void onRenderAccessories(RenderLivingEvent.Post event) {
		if (event.entity instanceof EntityPlayer) {
			PlayerAether playerAether = PlayerAether.get((EntityPlayer) event.entity);

			if (event.renderer instanceof RenderPlayer)
			{
				PlayerAetherRenderer.instance().renderAccessories(playerAether, (RenderPlayer) event.renderer, event.x, event.y, event.z, PlayerAetherRenderer.instance().getPartialTicks());
			}
		}
	}

}