//------------------------------------------------------
//
//   ProspectingCraft - Main
//
//------------------------------------------------------

package gcewing.prospecting;

import com.github.bluefalconeer1.prospectingcraft.Tags;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.*;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.world.*;

import net.minecraftforge.common.*;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

@Mod(
	modid = Info.modID,
	name = Info.modName,
	version = Tags.VERSION,
	acceptableRemoteVersions = Info.versionBounds,
	acceptedMinecraftVersions = Info.acceptedMinecraftVersions,
	dependencies = "required-after:navigator@[1.1.6,)")

public class ProspectingCraft extends BaseMod<ProspectingCraftClient> {

	public static ProspectingCraft mod;
	public static BaseDataChannel channel = new BaseDataChannel("prospectingcraft");

	//
	//   Blocks and Items
	//

    public static Item[] itemInk = new Item[4];
    
    // Gui IDs
    public static int
        seismicRecorderGui,
        gsaKitGui;

	public ProspectingCraft() {
		super();
		mod = this;
		creativeTab = new CreativeTabs("prospectingcraft:all") {
		    public Item getTabIconItem() {return itemProspectingPick;}
		};
	}
	
	@Override
	void configure() {
	    super.configure();
	    GSAKitTE.configure(config);
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		super.preInit(e);
// 		integrateWithMod("ThermalExpansion", "gcewing.prospecting.TXIntegration");
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent e) {
		super.init(e);
	}
	
	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent e) {
		super.postInit(e);
		SeismicRecorderTE.init();
	}

	@Override
	ProspectingCraftClient initClient() {
		return new ProspectingCraftClient(this);
	}

    public static BaseBlock
        blockSeismicRecorder,
        blockGeophone,
        blockGSAKit;
    
	@Override
	protected void registerBlocks() {
		blockSeismicRecorder = newBlock("seismic_recorder", SeismicRecorderBlock.class);
		blockGeophone = addBlock("geophone", new BaseBlock(Material.wood, GeophoneTE.class));
		blockGeophone.setModelAndTextures("geophone.smeg",
		    "minecraft:jukebox_side", "minecraft:redstone_torch_on");
		blockGeophone.setOpaque(false);
		blockGSAKit = addBlock("gsa_kit",
		    new BaseBlock(Material.wood, BaseOrientation.orient4WaysByState, GSAKitTE.class));
        blockGSAKit.setOpaque(false);
        blockGSAKit.setModelAndTextures("gsakit_open.smeg",
            "gsa_kit", "minecraft:glass_white");
    }
    
    public static Item
        itemSeismicSurvey,
        itemSowsEar,
        itemGeologicalSample,
        itemProspectingPick,
        itemLitmusPaper,
        itemRedtoothTransmitter,
        itemRedtoothReceiver,
        itemTestTube,
        itemEyeglass,
        itemMagnetometer,
        itemNaquadahDetector;
    
    @Override
    protected void registerItems() {
        itemSeismicSurvey = newItem("seismic_survey", SeismicSurveyItem.class);
        itemSeismicSurvey.setCreativeTab(null);
        itemInk[0] = newInkItem("ink_bottle_black");
        itemInk[1] = newInkItem("ink_bottle_magenta");
        itemInk[2] = newInkItem("ink_bottle_yellow");
        itemInk[3] = newInkItem("ink_bottle_cyan");
        itemSowsEar = newItem("sows_ear");
        itemGeologicalSample = newItem("geological_sample", GeologicalSampleItem.class);
        itemGeologicalSample.setMaxDamage(16);
        itemGeologicalSample.setCreativeTab(null);
        itemProspectingPick = newItem("prospecting_pick", ProspectingPickItem.class);
        itemLitmusPaper = newItem("litmus_paper");
        itemRedtoothTransmitter = newItem("redtooth_transmitter");
        itemRedtoothReceiver = newItem("redtooth_receiver");
        itemTestTube = newItem("test_tube");
        itemEyeglass = newItem("eyeglass", EyeglassItem.class);
        itemMagnetometer = newItem("magnetometer", MagnetometerItem.class);
        itemNaquadahDetector = newItem("naquadah_detector", NaquadahDetectorItem.class);
    }
    
    protected Item newInkItem(String name) {
        return newItem(name, InkBottleItem.class);
    }
    
    @Override
    protected void registerOres() {
        addOre("flowerRed", new ItemStack(Blocks.red_flower, 1, 0)); // poppy
        addOre("flowerRed", new ItemStack(Blocks.red_flower, 1, 4)); // red tulip
        addOre("flowerRed", new ItemStack(Blocks.double_plant, 1, 4)); // rose
        addOre("flowerBlue", new ItemStack(Blocks.red_flower, 1, 1)); // blue orchid
        addOre("flowerBlue", new ItemStack(Blocks.red_flower, 1, 3)); // azure bluet
        addOre("flowerPurple", new ItemStack(Blocks.double_plant, 1, 1)); // lilac
        addOre("flowerPurple", new ItemStack(Blocks.red_flower, 1, 2)); // allium
    }
    
    @Override
    protected void registerRecipes() {
        newRecipe(blockGeophone, 1,
            "t",
            "e",
                't', itemRedtoothTransmitter,
                'e', itemSowsEar,
                'w', "plankWood");
        newRecipe(blockSeismicRecorder, 1,
            "iri",
            "wfw",
            "www",
                'i', "ingotIron",
                'r', itemRedtoothReceiver,
                'w', "plankWood",
                'f', Items.feather);
        newRecipe(blockGSAKit, 1,
            "ttt",
            " c ",
                't', itemTestTube,
                'c', Blocks.chest);
        newRecipe(itemTestTube, 3,
            "g",
            "g",
                'g', "blockGlass");
        newRecipe(itemRedtoothTransmitter, 1,
            "t",
            "W",
                't', Blocks.redstone_torch,
                'W', "plankWood");
        newRecipe(itemRedtoothReceiver, 1,
            "Wt",
                't', Blocks.redstone_torch,
                'W', "plankWood");
        newRecipe(itemProspectingPick, 1,
            "CC",
            " /",
                'C', "cobblestone",
                '/', Items.stick);
        newRecipe(itemEyeglass, 1,
            " G",
            "/ ",
                'G', "blockGlass",
                '/', Items.stick);
        newRecipe(itemMagnetometer, 1,
            "WGW",
            "WIW",
            "W@W",
                'W', "plankWood",
                'G', "blockGlass",
                'I', "ingotIron",
                '@', Items.string);
        newRecipe(itemMagnetometer, 1,
            "WGW",
            "WCW",
            "W@W",
                'W', "plankWood",
                'G', "blockGlass",
                'C', "ingotCopper",
                '@', Items.string);
        newRecipe(itemNaquadahDetector, 1,
            "gG*",
            "yey",
            "SrS",
                'g', "dyeGreen",
                'G', "blockGlass",
                '*', "dustGlowstone",
                'y', "dyeYellow",
                'e', Items.spider_eye,
                'S', Blocks.stone,
                'r', "dustRedstone");
        newShapelessRecipe(itemInk[0], 1,
            "dyeBlack", //new ItemStack(Items.dye, 1, 0),
            Items.potionitem);
        newShapelessRecipe(itemInk[1], 1,
            "dyeMagenta", //new ItemStack(Items.dye, 1, 13),
            Items.potionitem);
        newShapelessRecipe(itemInk[2], 1,
            "dyeYellow", //new ItemStack(Items.dye, 1, 11),
            Items.potionitem);
        newShapelessRecipe(itemInk[3], 1,
            "dyeCyan", //new ItemStack(Items.dye, 1, 6),
            Items.potionitem);
        newShapelessRecipe(itemLitmusPaper, 64,
            Items.paper, "flowerRed", "flowerBlue");
        newShapelessRecipe(itemLitmusPaper, 64,
            Items.paper, "flowerPurple");
    }

    @Override
    protected void registerContainers() {
        seismicRecorderGui = addContainer(SeismicRecorderContainer.class, SeismicRecorderTE.class);
        gsaKitGui = addContainer(GSAKitContainer.class, GSAKitTE.class);
    }
    
    @SubscribeEvent
    public void onLivingDropsEvent(LivingDropsEvent event) {
        if (event.source instanceof EntityDamageSource) {
            Entity srcEntity = ((EntityDamageSource)event.source).getEntity();
            if (srcEntity != null && srcEntity instanceof EntityPlayerMP) {
                EntityPlayerMP srcPlayer = (EntityPlayerMP)srcEntity;
                ItemStack weapon = srcPlayer.getCurrentEquippedItem();
                if (weapon != null && weapon.getItem() instanceof ItemAxe)
                    event.entity.dropItem(itemSowsEar, 2);
            }
        }
    }

}
