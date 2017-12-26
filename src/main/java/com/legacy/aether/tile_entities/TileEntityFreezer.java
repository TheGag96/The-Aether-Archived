package com.legacy.aether.tile_entities;

import java.util.Map;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.legacy.aether.api.AetherAPI;
import com.legacy.aether.api.events.AetherHooks;
import com.legacy.aether.api.freezables.AetherFreezable;
import com.legacy.aether.blocks.BlocksAether;
import com.legacy.aether.tile_entities.util.AetherTileEntity;

public class TileEntityFreezer extends AetherTileEntity
{

	public int progress, ticksRequired, powerRemaining;

	private NonNullList<ItemStack> frozenItemStacks = NonNullList.<ItemStack>withSize(3, ItemStack.EMPTY);

	private AetherFreezable currentFreezable;

	public TileEntityFreezer() 
	{
		super("freezer");
	}

	@Override
	public NonNullList<ItemStack> getTileInventory() 
	{
		return this.frozenItemStacks;
	}

	@Override
	public void onSlotChanged(int index)
	{

	}

	@Override
	public void update()
	{
		if (this.powerRemaining > 0)
		{
			this.powerRemaining--;

			if (this.currentFreezable != null)
			{
				if (this.world.getBlockState(this.getPos().down()).getBlock() == BlocksAether.icestone)
				{
					this.progress += 2;
				}
				else
				{
					this.progress++;
				}
			}
		}

		if (this.currentFreezable != null)
		{
			if (this.progress >= this.currentFreezable.getTimeRequired())
			{
				if (!this.world.isRemote)
				{
					ItemStack result = this.currentFreezable.getOutput().copy();

					EnchantmentHelper.setEnchantments(EnchantmentHelper.getEnchantments(this.getStackInSlot(0)), result);

					if (!this.getStackInSlot(2).isEmpty())
					{
						result.setCount(this.getStackInSlot(2).getCount() + 1);

						this.setInventorySlotContents(2, result);
					}
					else
					{
						this.setInventorySlotContents(2, result);
					}

					if (this.getStackInSlot(0).getItem().hasContainerItem(this.getStackInSlot(0)))
					{
						this.setInventorySlotContents(0, this.getStackInSlot(0).getItem().getContainerItem(this.getStackInSlot(0)));
					}
					else
					{
						this.decrStackSize(0, 1);
					}
				}

				this.progress = 0;
				AetherHooks.onItemFreeze(this, this.currentFreezable);
			}

			if (this.getStackInSlot(0).isEmpty() || (!this.getStackInSlot(0).isEmpty() && AetherAPI.getInstance().getFreezable(this.getStackInSlot(0)) != this.currentFreezable))
			{
				this.currentFreezable = null;
				this.progress = 0;
			}

			if (this.powerRemaining <= 0)
			{
				if (!this.getStackInSlot(1).isEmpty() && AetherAPI.getInstance().isFreezableFuel(this.getStackInSlot(1)))
				{
					this.powerRemaining += AetherAPI.getInstance().getFreezableFuel(this.getStackInSlot(1)).getTimeGiven();

					if (!this.world.isRemote)
					{
						this.decrStackSize(1, 1);
					}
				}
				else
				{
					this.currentFreezable = null;
					this.progress = 0;
				}
			}
		}
		else if (!this.getStackInSlot(0).isEmpty())
		{
			ItemStack itemstack = this.getStackInSlot(0);
			AetherFreezable freezable = AetherAPI.getInstance().getFreezable(itemstack);

			if (freezable != null)
			{
				if (this.getStackInSlot(2).isEmpty() || (freezable.getOutput().getItem() == this.getStackInSlot(2).getItem() && freezable.getOutput().getMetadata() == this.getStackInSlot(2).getMetadata()))
				{
					this.currentFreezable = freezable;
					this.ticksRequired = this.currentFreezable.getTimeRequired();
					this.addEnchantmentWeight(itemstack);
					this.ticksRequired = AetherHooks.onSetFreezableTime(this, this.currentFreezable, this.ticksRequired);
				}
			}
		}
	}

	public void addEnchantmentWeight(ItemStack stack)
	{
		Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);

		if (!enchantments.isEmpty())
		{
			for (int levels : enchantments.values())
			{
				this.ticksRequired += (levels * 1250);
			}
		}
	}

	@SideOnly(Side.CLIENT)
	public int getEnchantmentProgressScaled(int i)
	{
		if (this.ticksRequired == 0)
		{
			return 0;
		}
		return (this.progress * i) / this.ticksRequired;
	}

	@SideOnly(Side.CLIENT)
	public int getEnchantmentTimeRemaining(int i)
	{
		return (this.powerRemaining * i) / 500;
	}

	public boolean isBurning()
	{
		return this.powerRemaining > 0;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound)
	{
		super.readFromNBT(nbttagcompound);

		this.progress = nbttagcompound.getInteger("FreezerProgressTime");
		this.ticksRequired = nbttagcompound.getInteger("FreezerTimeRequired");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbttagcompound)
	{
		nbttagcompound.setInteger("FreezerProgressTime", this.progress);
		nbttagcompound.setInteger("FreezerTimeRequired", this.ticksRequired);

		return super.writeToNBT(nbttagcompound);
	}

	@Override
	public boolean isValidSlotItem(int slot, ItemStack stackInSlot)
	{
		if (stackInSlot != null)
		{
			if (AetherAPI.getInstance().hasFreezable(stackInSlot))
			{
				return true;
			}
			else if (slot == 1 && AetherAPI.getInstance().isFreezableFuel(stackInSlot))
			{
				return true;
			}
		}

		return false;
	}

	@Override
	public int[] getSlotsForFace(EnumFacing side)
	{
		return side == EnumFacing.DOWN ? new int[] {2} : new int[] {0, 1};
	}

	@Override
	public int getField(int id)
	{
		if (id == 0)
		{
			return this.progress;
		}
		else if (id == 1)
		{
			return this.powerRemaining;
		}
		else if (id == 2)
		{
			return this.ticksRequired;
		}

		return 0; 
	}

	@Override
	public void setField(int id, int value) 
	{ 
		if (id == 0)
		{
			this.progress = value;
		}
		else if (id == 1)
		{
			this.powerRemaining = value;
		}
		else if (id == 2)
		{
			this.ticksRequired = value;
		}
	}

	@Override
	public int getFieldCount() 
	{
		return 3; 
	}

}