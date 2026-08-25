// ------------------------------------------------------
//
// ProspectingCraft - Seismic Recorder Tile Entity
//
// ------------------------------------------------------

package gcewing.prospecting;

import static gcewing.prospecting.BaseUtils.*;
import static java.lang.Math.*;

import java.util.*;

import net.minecraft.block.*;
import net.minecraft.block.material.*;
import net.minecraft.init.*;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.oredict.OreDictionary;

import com.google.common.primitives.Ints;

public class SeismicRecorderTE extends BaseTileInventory implements ITickable {

    protected static int paperSlot = 0, blackInkSlot = 1, magentaInkSlot = 2, yellowInkSlot = 3, cyanInkSlot = 4,
        gunpowderSlot = 5, resultSlot = 6, numInventorySlots = 7;

    protected static int linkCheckInterval = 5 * 20; // ticks

    public static enum ReadyStatus {

        RDY(false, "Ready"),
        RUN(false, "Analyzing..."),
        FIN(false, "Finished"),
        NEG(true, "Too few geophones"),
        GTC(true, "Geophones too close"),
        OOP(true, "Out of Paper"),
        OOG(true, "Out of Gunpowder"),
        OOI0(true, "Out of Black Ink"),
        OOI1(true, "Out of Magenta Ink"),
        OOI2(true, "Out of Yellow Ink"),
        OOI3(true, "Out of Cyan Ink");

        public boolean error;
        public String message;

        ReadyStatus(boolean error, String message) {
            this.error = error;
            this.message = message;
        }
    }

    protected static ReadyStatus[] OOI = { ReadyStatus.OOI0, ReadyStatus.OOI1, ReadyStatus.OOI2, ReadyStatus.OOI3 };

    protected IInventory inventory;

    protected boolean signalled;
    public boolean activated;
    public int phase;
    protected int mapId;
    protected boolean colorMode;
    protected int numGeophones;
    protected boolean geophonesOkay;
    protected int linkTimer;

    public SeismicRecorderTE() {
        super();
        inventory = new InventoryBasic("", false, numInventorySlots);
    }

    @Override
    protected IInventory getInventory() {
        return inventory;
    }

    @Override
    public void readContentsFromNBT(NBTTagCompound nbt) {
        super.readContentsFromNBT(nbt);
        signalled = nbt.getBoolean("signalled");
        activated = nbt.getBoolean("activated");
        phase = nbt.getInteger("phase");
        mapId = nbt.getInteger("mapId");
        colorMode = nbt.getBoolean("colorMode");
        numGeophones = nbt.getInteger("numGeophones");
        geophonesOkay = nbt.getBoolean("geophonesOkay");
    }

    @Override
    public void writeContentsToNBT(NBTTagCompound nbt) {
        super.writeContentsToNBT(nbt);
        nbt.setBoolean("signalled", signalled);
        nbt.setBoolean("activated", activated);
        nbt.setInteger("phase", phase);
        nbt.setInteger("mapId", mapId);
        nbt.setBoolean("colorMode", colorMode);
        nbt.setInteger("numGeophones", numGeophones);
        nbt.setBoolean("geophonesOkay", geophonesOkay);
    }

    public void inputSignal(boolean signal) {
        // System.out.printf(
        // "SeismicRecorderTE.inputSignal: signal = %s, signalled = %s, activated = %s, ready = %s\n",
        // signal, signalled, activated, isReadyToActivate());
        if (signal && !signalled && isReadyToActivate()) beginScan();
        signalled = signal;
    }

    protected boolean hasPaper() {
        return getStackInSlot(paperSlot) != null;
    }

    protected boolean hasInk() {
        return true;
        // return emptyInkNo() < 0;
    }

    protected int emptyInkNo() {
        int n = numInksRequired();
        for (int i = 0; i < n; i++) if (!isInkBottle(getStackInSlot(blackInkSlot + i))) return i;
        return -1;
    }

    protected int numInksRequired() {
        if (colorMode) return 4;
        else return 1;
    }

    protected boolean hasGunpowder() {
        return getStackInSlot(gunpowderSlot) != null;
    }

    protected boolean hasResult() {
        return getStackInSlot(resultSlot) != null;
    }

    protected boolean isReadyToActivate() {
        /// System.out.printf(
        // "SeismicRecorder.isReadyToActivate: isReadyToPrint = %s, hasGunpowder = %s, hasResult = %s\n",
        // isReadyToPrint(), hasGunpowder(), hasResult());
        // return !activated && isReadyToPrint() && hasGunpowder() && !hasResult();
        return getReadyStatus() == ReadyStatus.RDY;
    }

    protected boolean isReadyToPrint() {
        return hasPaper() && hasInk();
    }

    public ReadyStatus getReadyStatus() {
        if (hasResult()) return ReadyStatus.FIN;
        if (activated) return ReadyStatus.RUN;
        if (numGeophones < 3) return ReadyStatus.NEG;
        if (!geophonesOkay) return ReadyStatus.GTC;
        if (!hasPaper()) return ReadyStatus.OOP;
        if (!hasGunpowder()) return ReadyStatus.OOG;
        int i = emptyInkNo();
        if (i >= 0) return OOI[i];
        return ReadyStatus.RDY;
    }

    public boolean blink() {
        return (phase & 8) != 0;
    }

    protected void usePaper() {
        decrStackSize(paperSlot, 1);
    }

    protected boolean isInkBottle(ItemStack stack) {
        return stack != null && stack.getItem() instanceof InkBottleItem;
    }

    protected void useInk() {
        for (int i = blackInkSlot; i <= cyanInkSlot; i++) {
            ItemStack stack = getStackInSlot(i);
            if (isInkBottle(stack)) {
                int damage = stack.getItemDamage() + 1;
                stack.setItemDamage(damage);
                if (damage >= stack.getMaxDamage()) setInventorySlotContents(i, new ItemStack(Items.glass_bottle, 1));
            }
        }
    }

    protected void beginScan() {
        // System.out.printf("SeismicRecorderTE.beginScan\n");
        fireGunpowder();
        activated = true;
        phase = 0;
        mapId = worldObj.getUniqueDataId("map");
        MapData data = getMapData();
        data.xCenter = getX();
        data.zCenter = getZ();
        data.scale = 0;
        data.dimension = BaseUtils.getWorldDimensionId(worldObj);
        markForUpdate();
    }

    protected void fireGunpowder() {
        // System.out.printf("Bang!\n");
        decrStackSize(gunpowderSlot, 1);
        worldObj.newExplosion(null, getX(), getY(), getZ(), 0, false, true);
    }

    @Override
    public void update() {
        if (!worldObj.isRemote) {
            if (linkTimer++ >= linkCheckInterval) {
                linkTimer = 0;
                checkForLink();
            }
            if (activated) {
                if (phase < 128) {
                    // System.out.printf("SeismicRecorderTE.update: Scanning row %s\n", phase);
                    scanRow(phase);
                    phase += 1;
                } else {
                    activated = false;
                    // System.out.printf("SeismicRecorderTE.update: Resetting phase\n");
                    phase = 0;
                    if (isReadyToPrint()) {
                        usePaper();
                        useInk();
                        produceResult();
                    }
                }
                markForUpdate();
            }
        }
    }

    protected void checkForLink() {
        // System.out.printf("SeismicRecorderTE.checkForLink: Searchihg for geophones\n");
        BaseTileEntity[] teList = new BaseTileEntity[4];
        int numTEs = findRelevantTileEntities(teList);
        int ng = numTEs - 1;
        boolean ok = tileEntitiesWellSeparated(teList, numTEs);
        if (numGeophones != ng || geophonesOkay != ok) {
            numGeophones = ng;
            geophonesOkay = ok;
            markForUpdate();
            // System.out.printf("SeismicRecorderTE.checkForLink: numGeophones = %s, geophonesOkay = %s\n",
            // numGeophones, geophonesOkay);
        }
    }

    protected int findRelevantTileEntities(BaseTileEntity[] teList) {
        int numTEs = 0;
        teList[numTEs++] = this;
        BlockPos p = getPos();
        int cx0 = (p.x - 15) >> 4, cz0 = (p.z - 15) >> 4;
        int cx1 = (p.x + 15) >> 4, cz1 = (p.z + 15) >> 4;
        for (int cz = cz0; cz <= cz1; cz++) for (int cx = cx0; cx <= cx1; cx++) {
            Chunk chunk = worldObj.getChunkFromChunkCoords(cx, cz);
            for (Object obj : getChunkTileEntityMap(chunk).values()) if (obj instanceof GeophoneTE) {
                GeophoneTE te = (GeophoneTE) obj;
                // System.out.printf("SeismicRecorderTE.checkForLink: Found geophone at %s\n",
                // te.getPos());
                teList[numTEs++] = te;
                if (numTEs == teList.length) return numTEs;
            }
        }
        return numTEs;
    }

    protected boolean tileEntitiesWellSeparated(BaseTileEntity[] teList, int numTEs) {
        for (int i = 0; i < numTEs; i++) {
            Vector3 p1 = new Vector3(teList[i].getPos());
            for (int j = i + 1; j < numTEs; j++) {
                Vector3 p2 = new Vector3(teList[j].getPos());
                if (p1.distance(p2) < 7.5) return false;
            }
        }
        return true;
    }

    protected MapData getMapData() {
        String s = "map_" + mapId;
        MapData mapdata = (MapData) worldObj.loadItemData(MapData.class, s);
        if (mapdata == null) {
            mapdata = new MapData(s);
            worldObj.setItemData(s, mapdata);
        }
        return mapdata;
    }

    protected void scanRow(int j) {
        int r = 64;
        MapData data = getMapData();
        int x0 = data.xCenter;
        int z0 = data.zCenter;
        int z = z0 - r + j;
        int i0 = j * 128;
        World world = worldObj;
        for (int i = 0; i < 128; i++) {
            int x = x0 - r + i;
            data.colors[i0 + i] = scanCoords(world, x, z);
        }
        data.markDirty();
    }

    protected void produceResult() {
        MapData data = getMapData();
        Item item = ProspectingCraft.itemSeismicSurvey;
        ItemStack stack = new ItemStack(item, 1, mapId);
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("xCenter", data.xCenter);
        nbt.setInteger("zCenter", data.zCenter);
        stack.setTagCompound(nbt);
        setInventorySlotContents(resultSlot, stack);
    }

    protected byte scanCoords(World world, int x, int z) {
        int bestPriority = 0;
        float maxHardness = -1;
        int bestByte = 0;
        ItemStack bestStack = null;
        for (int y = 0; y < getY(); y++) {
            Block block = world.getBlock(x, y, z);
            int meta = world.getBlockMetadata(x, y, z);
            float hardness = block.getBlockHardness(world, x, y, z);
            Item item = Item.getItemFromBlock(block);
            ItemStack stack = null;
            int b = -1;
            if (item != null) {
                stack = new ItemStack(item, 1, meta);
                // System.out.printf("SeismicRecorderTE.scanCoords: (%s, %s) block %s stack %s\n", x, z, block, stack);
                b = getMapDataByteForStack(stack);
            }
            int priority;
            if (b >= 0) priority = 2;
            else {
                priority = 1;
                b = getMapDataByteForHardness(hardness);
            }
            if (priority > bestPriority || (priority == bestPriority && hardness > maxHardness)) {
                bestPriority = priority;
                maxHardness = hardness;
                bestByte = b;
                bestStack = stack;
            }
        }
        // System.out.printf("SeismicRecorderTE.scanCoords: (%s, %s) --> prio %s hard %s stack %s = %s\n",
        // x, z, bestPriority, maxHardness, bestStack, bestByte);
        if (!colorMode) bestByte = colorByteToGreyByte(bestByte);
        return (byte) bestByte;
    }

    public static boolean itemStackIsOfInterest(ItemStack stack) {
        return getMapDataByteForStack(stack) >= 0;
    }

    protected int getMapDataByteForHardness(float h) {
        return getMapDataByteFromArray(hardnessToMapDataByte, h, 4.0f);
    }

    protected int getMapDataByteFromArray(int[] a, float x, float xMax) {
        int i = iround(a.length * (x / xMax));
        return a[clampIndex(i, a.length)];
    }

    protected static int getMapDataByteForStack(ItemStack stack) {
        int[] ids = OreDictionary.getOreIDs(stack);
        boolean oreLike = false;
        String oreName = null;
        for (int id : ids) {
            String name = OreDictionary.getOreName(id);
            if (oreIdToMapDataByte.containsKey(id)) {
                int b = oreIdToMapDataByte.get(id);
                // System.out.printf("SeismicRecorderTE.getMapDataByteForStack: known ore %s = %s\n", name, b);
                return b;
            }
            if (name.toLowerCase()
                .contains("ore")) {
                oreLike = true;
                oreName = name;
            }
        }
        if (!oreLike) {
            oreName = stack.getUnlocalizedName();
            oreLike = oreName.toLowerCase()
                .contains("ore");
        }
        if (oreLike) {
            int b = 4 * (16 + oreName.hashCode() % (30 - 16));
            // System.out.printf("SeismicRecorderTE.getMapDataByteForStack: ore-like %s = %s\n", oreName, b);
            return b;
        }
        return -1;
    }

    // protected int defaultMapDataByteForStack(ItemStack stack) {
    // return 4 * (16 + stack.getUnlocalizedName().hashCode() % (30 - 16));
    // }

    // ----------------------------------------------------------------------------

    protected static int[] hardnessToMapDataByte = new int[4];

    static {
        for (int i = 0; i < 4; i++) hardnessToMapDataByte[i] = (MapColor.greenColor.colorIndex * 4) + ((i - 1) & 3);
    }

    protected static Map<Integer, Integer> oreIdToMapDataByte = new HashMap<>();
    protected static Set<Integer> ignoreOreIds = new HashSet<>();

    public static void init() {
        OreDictionary.registerOre("blockObsidian", Blocks.obsidian);
        OreDictionary.registerOre("cobblestone", Blocks.mossy_cobblestone);
        OreDictionary.registerOre("stone", Blocks.monster_egg);
        addOre("oreGold", MapColor.goldColor, 3);
        addOre("oreIron", MapColor.brownColor, 3);
        addOre("oreCoal", MapColor.grayColor, 0);
        addOre("oreLapis", MapColor.lapisColor, 3);
        addOre("oreDiamond", MapColor.diamondColor, 3);
        addOre("oreRedstone", MapColor.redColor, 3);
        addOre("oreEmerald", MapColor.emeraldColor, 3);
        addOre("oreQuartz", MapColor.quartzColor, 3);
        addOre("oreCopper", MapColor.brownColor, 3);
        addOre("oreTin", MapColor.silverColor, 2);
        addOre("oreLead", MapColor.grayColor, 2);
        addOre("oreNickel", MapColor.silverColor, 1);
        addOre("oreSilver", MapColor.silverColor, 3);
        addOre("orePlatinum", MapColor.silverColor, 2);
        addSingleOre("blockObsidian", MapColor.blackColor, 0);
        // addSingleOre("cobblestoneMossy", MapColor.greenColor, 1);
        // ignoreOre("stone");
        // ignoreOre("cobblestone");
    }

    protected static void addOre(String name, MapColor color, int bright) {
        addSingleOre(name, color, bright);
        addSingleOre("dense" + name, color, bright);
    }

    protected static void addSingleOre(String name, MapColor color, int bright) {
        int id = OreDictionary.getOreID(name);
        int b = (color.colorIndex * 4) + ((bright - 1) & 3);
        // System.out.printf("SeismicRecorderTE.addOre: %s %s --> %s (%s, %s)\n",
        // name, id, b, color.colorIndex, bright);
        oreIdToMapDataByte.put(id, b);
    }

    // protected static void ignoreOre(String name) {
    // int id = OreDictionary.getOreID(name);
    // System.out.printf("SeismicRecorderTE.ignoreOre: %s %s\n", name, id);
    // ignoreOreIds.add(id);
    // }

    // ----------------------------------------------------------------------------

    protected static int[] multipliers = { 180, 220, 255, 135 };

    protected static int[] greyScaleByte = new int[256];

    protected static void initGreyScale() {
        int[] shadeToByte = new int[256];
        List<Integer> shades = new ArrayList<>();
        for (MapColor mc : MapColor.mapColorArray) {
            if (mc != null) {
                int r = (mc.colorValue >> 16) & 0xff;
                int g = (mc.colorValue >> 8) & 0xff;
                int b = (mc.colorValue) & 0xff;
                if (r == g && g == b) {
                    // System.out.printf("SeismicRecorderTE: Map color %s = 0x%06x\n",
                    // mc.colorIndex, mc.colorValue);
                    for (int i = 0; i < 4; i++) {
                        int shade = r * multipliers[i] / 255;
                        if (!shades.contains(shade)) {
                            shadeToByte[shade] = mc.colorIndex * 4 + i;
                            shades.add(shade);
                        }
                    }
                }
            }
        }
        int[] a = Ints.toArray(shades);
        Arrays.sort(a);
        for (int shade : a) {
            // System.out.printf("SeismicRecorderTE: Grey scale shade 0x%02x = %s\n",
            // shade, shadeToByte[shade]);
        }
        int k = 0;
        for (int shade : a) while (k <= shade) greyScaleByte[k++] = shadeToByte[shade];
        // for (int i = 0; i < 256; i++)
        // System.out.printf("SeismicRecorderTE: greyScaleByte[0x%02x] = %s\n", i, greyScaleByte[i]);
    }

    static {
        initGreyScale();
    }

    protected int colorByteToGreyByte(int i) {
        int color = mapColorForShade(MapColor.mapColorArray[i >> 2], (i & 3));
        int r = (color >> 16) & 0xff;
        int g = (color >> 8) & 0xff;
        int b = (color) & 0xff;
        // int j = (r + g + b) / 3;
        int j = max(r, max(g, b));
        return greyScaleByte[j];
    }

    protected int mapColorForShade(MapColor mc, int shade) {
        short short1 = 220;
        if (shade == 3) short1 = 135;
        if (shade == 2) short1 = 255;
        if (shade == 1) short1 = 220;
        if (shade == 0) short1 = 180;
        int j = (mc.colorValue >> 16 & 255) * short1 / 255;
        int k = (mc.colorValue >> 8 & 255) * short1 / 255;
        int l = (mc.colorValue & 255) * short1 / 255;
        return -16777216 | j << 16 | k << 8 | l;
    }

}
