// ------------------------------------------------------
//
// ProspectingCraft - Main
//
// ------------------------------------------------------

package gcewing.prospecting;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.*;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraftforge.common.*;

import com.github.bluefalconeer1.prospectingcraft.Tags;

import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gcewing.prospecting.survey.ProspectingSurveyCommand;
import gcewing.prospecting.survey.SurveyMapStyleConfig;
import gcewing.prospecting.survey.SurveyTier;
import gcewing.prospecting.survey.network.SurveyChunkServerHandler;

@Mod(
    modid = Info.modID,
    name = Info.modName,
    version = Tags.VERSION,
    acceptableRemoteVersions = Info.versionBounds,
    acceptedMinecraftVersions = Info.acceptedMinecraftVersions,
    dependencies = "required-after:navigator@[1.1.6,);after:hbm")

public class ProspectingCraft extends BaseMod<ProspectingCraftClient> {

    public static ProspectingCraft mod;
    public static BaseDataChannel channel = new BaseDataChannel("prospectingcraft");

    //
    // Blocks and Items
    //

    public static Item[] itemInk = new Item[4];

    // Gui IDs
    public static int seismicRecorderGui, gsaKitGui;

    public ProspectingCraft() {
        super();
        mod = this;
        channel.handlers.add(SurveyChunkServerHandler.INSTANCE);
        creativeTab = new CreativeTabs("prospectingcraft:all") {

            public Item getTabIconItem() {
                return Item.getItemFromBlock(blockSeismicRecorder);
            }
        };
    }

    @Override
    void configure() {
        super.configure();
        GSAKitTE.configure(config);
        SurveyTier.configure(config);
        SurveyMapStyleConfig.configure(config);
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        super.preInit(e);
        // integrateWithMod("ThermalExpansion", "gcewing.prospecting.TXIntegration");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        super.init(e);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent e) {
        super.postInit(e);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new ProspectingSurveyCommand());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) SurveyChunkServerHandler.INSTANCE.processRequests();
    }

    @Override
    ProspectingCraftClient initClient() {
        return new ProspectingCraftClient(this);
    }

    public static BaseBlock blockSeismicRecorder, blockSteelSeismicRecorder, blockDeshSeismicRecorder, blockGeophone,
        blockGSAKit;

    @Override
    protected void registerBlocks() {
        blockSeismicRecorder = newBlock("seismic_recorder", SeismicRecorderBlock.class);
        blockSteelSeismicRecorder = addBlock("seismic_recorder_steel", new SeismicRecorderBlock(SurveyTier.STEEL));
        blockDeshSeismicRecorder = addBlock("seismic_recorder_desh", new SeismicRecorderBlock(SurveyTier.DESH));
        blockSteelSeismicRecorder.setBlockTextureName(Info.modID + ":seismic_recorder");
        blockDeshSeismicRecorder.setBlockTextureName(Info.modID + ":seismic_recorder");
        blockGeophone = addBlock("geophone", new BaseBlock(Material.wood, GeophoneTE.class));
        blockGeophone.setModelAndTextures("geophone.smeg", "minecraft:jukebox_side", "minecraft:redstone_torch_on");
        blockGeophone.setOpaque(false);
        blockGSAKit = addBlock(
            "gsa_kit",
            new BaseBlock(Material.wood, BaseOrientation.orient4WaysByState, GSAKitTE.class));
        blockGSAKit.setCreativeTab(null);
        blockGSAKit.setOpaque(false);
        blockGSAKit.setModelAndTextures("gsakit_open.smeg", "gsa_kit", "minecraft:glass_white");
    }

    public static Item itemSeismicSurvey, itemSowsEar, itemGeologicalSample, itemProspectingPick, itemLitmusPaper,
        itemRedtoothTransmitter, itemRedtoothReceiver, itemTestTube, itemEyeglass, itemMagnetometer,
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
        itemSowsEar.setCreativeTab(null);
        itemGeologicalSample = newItem("geological_sample", GeologicalSampleItem.class);
        itemGeologicalSample.setMaxDamage(16);
        itemGeologicalSample.setCreativeTab(null);
        itemProspectingPick = newItem("prospecting_pick", ProspectingPickItem.class);
        itemProspectingPick.setCreativeTab(null);
        itemLitmusPaper = newItem("litmus_paper");
        itemLitmusPaper.setCreativeTab(null);
        itemRedtoothTransmitter = newItem("redtooth_transmitter");
        itemRedtoothTransmitter.setCreativeTab(null);
        itemRedtoothReceiver = newItem("redtooth_receiver");
        itemRedtoothReceiver.setCreativeTab(null);
        itemTestTube = newItem("test_tube");
        itemTestTube.setCreativeTab(null);
        itemEyeglass = newItem("eyeglass", EyeglassItem.class);
        itemEyeglass.setCreativeTab(null);
        itemMagnetometer = newItem("magnetometer", MagnetometerItem.class);
        itemMagnetometer.setCreativeTab(null);
        itemNaquadahDetector = newItem("naquadah_detector", NaquadahDetectorItem.class);
        itemNaquadahDetector.setCreativeTab(null);
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
        newRecipe(blockGeophone, 1, "t", "l", 't', Blocks.redstone_torch, 'l', Items.leather);
        newRecipe(
            blockSeismicRecorder,
            1,
            "igi",
            "wfw",
            "www",
            'i',
            "ingotIron",
            'g',
            blockGeophone,
            'w',
            "plankWood",
            'f',
            Items.feather);
        Item hbmCircuit = findItem("hbm:item.circuit");
        if (hbmCircuit != null) {
            newRecipe(
                blockSteelSeismicRecorder,
                1,
                "scs",
                "wbw",
                "www",
                's',
                "ingotSteel",
                'c',
                new ItemStack(hbmCircuit, 1, 8),
                'b',
                blockSeismicRecorder,
                'w',
                "plankWood");
            newRecipe(
                blockDeshSeismicRecorder,
                1,
                "dcd",
                "wsw",
                "www",
                'd',
                "ingotDesh",
                'c',
                new ItemStack(hbmCircuit, 1, 9),
                's',
                blockSteelSeismicRecorder,
                'w',
                "plankWood");
        } else {
            FMLLog.warning(
                "[%s] HBM circuit item not found; Steel and Desh Surveyor recipes were not registered",
                Info.modID);
        }
        newShapelessRecipe(
            itemInk[0],
            1,
            "dyeBlack", // new ItemStack(Items.dye, 1, 0),
            Items.potionitem);
        newShapelessRecipe(
            itemInk[1],
            1,
            "dyeMagenta", // new ItemStack(Items.dye, 1, 13),
            Items.potionitem);
        newShapelessRecipe(
            itemInk[2],
            1,
            "dyeYellow", // new ItemStack(Items.dye, 1, 11),
            Items.potionitem);
        newShapelessRecipe(
            itemInk[3],
            1,
            "dyeCyan", // new ItemStack(Items.dye, 1, 6),
            Items.potionitem);
    }

    @Override
    protected void registerContainers() {
        seismicRecorderGui = addContainer(SeismicRecorderContainer.class, SeismicRecorderTE.class);
        gsaKitGui = addContainer(GSAKitContainer.class, GSAKitTE.class);
    }

}
