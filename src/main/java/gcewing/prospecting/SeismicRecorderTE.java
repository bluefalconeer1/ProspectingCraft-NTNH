// ------------------------------------------------------
//
// ProspectingCraft - Seismic Recorder Tile Entity
//
// ------------------------------------------------------

package gcewing.prospecting;

import static gcewing.prospecting.BaseUtils.*;

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
import net.minecraftforge.oredict.OreDictionary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gcewing.prospecting.survey.OreDictionaryResolver;
import gcewing.prospecting.survey.PlayerSurveyData;
import gcewing.prospecting.survey.SurveyArea;
import gcewing.prospecting.survey.SurveyTier;
import gcewing.prospecting.survey.network.SurveyChunkServerHandler;

/**
 * Server-authoritative Surveyor state machine and inventory.
 *
 * <p>
 * A scan creates an exact 128-by-128 {@link SurveyArea}, then processes one X/Z row per server tick without forcing
 * unloaded chunks to load. Exact results remain server-side; tier limiting occurs later when map snapshots are built.
 */
public class SeismicRecorderTE extends BaseTileInventory implements ITickable {

    private static final Logger LOG = LogManager.getLogger("ProspectingCraft Surveyor");

    protected static int paperSlot = 0, blackInkSlot = 1, magentaInkSlot = 2, yellowInkSlot = 3, cyanInkSlot = 4,
        gunpowderSlot = 5, numInventorySlots = 7;

    // Keep the old seventh slot loadable so legacy survey results are recoverable by breaking the block.
    protected static int legacyResultSlot = 6;

    protected static int linkCheckInterval = 5 * 20; // ticks

    public static enum ReadyStatus {

        RDY(false, "Ready"),
        RUN(false, "Analyzing..."),
        NEG(true, "Too few geophones"),
        GTC(true, "Geophones too close"),
        OOP(true, "Out of Paper"),
        OOG(true, "Out of Gunpowder"),
        OOI0(true, "Out of Black Ink"),
        OOI1(true, "Out of Magenta Ink"),
        OOI2(true, "Out of Yellow Ink"),
        OOI3(true, "Out of Cyan Ink"),
        OWN(true, "Open Surveyor to claim ownership");

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
    protected int numGeophones;
    protected boolean geophonesOkay;
    protected int linkTimer;
    protected UUID ownerId;
    protected UUID activeSurveyId;

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
        numGeophones = nbt.getInteger("numGeophones");
        geophonesOkay = nbt.getBoolean("geophonesOkay");
        ownerId = readUuid(nbt, "ownerId");
        activeSurveyId = readUuid(nbt, "activeSurveyId");
    }

    @Override
    public void writeContentsToNBT(NBTTagCompound nbt) {
        super.writeContentsToNBT(nbt);
        nbt.setBoolean("signalled", signalled);
        nbt.setBoolean("activated", activated);
        nbt.setInteger("phase", phase);
        nbt.setInteger("numGeophones", numGeophones);
        nbt.setBoolean("geophonesOkay", geophonesOkay);
        if (ownerId != null) nbt.setString("ownerId", ownerId.toString());
        if (activeSurveyId != null) nbt.setString("activeSurveyId", activeSurveyId.toString());
    }

    protected UUID readUuid(NBTTagCompound nbt, String key) {
        if (!nbt.hasKey(key)) return null;
        try {
            return UUID.fromString(nbt.getString(key));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
        markChanged();
    }

    public void inputSignal(boolean signal) {
        // System.out.printf(
        // "SeismicRecorderTE.inputSignal: signal = %s, signalled = %s, activated = %s, ready = %s\n",
        // signal, signalled, activated, isReadyToActivate());
        if (signal && !signalled) tryBeginScan();
        signalled = signal;
    }

    protected boolean hasPaper() {
        ItemStack stack = getStackInSlot(paperSlot);
        return stack != null && stack.getItem() == getPaperItem();
    }

    protected int emptyInkNo() {
        for (int i = blackInkSlot; i <= cyanInkSlot; i++)
            if (!isExpectedInkBottle(i, getStackInSlot(i))) return i - blackInkSlot;
        return -1;
    }

    protected boolean hasGunpowder() {
        ItemStack stack = getStackInSlot(gunpowderSlot);
        return stack != null && stack.getItem() == getGunpowderItem();
    }

    protected boolean isReadyToActivate() {
        return getReadyStatus() == ReadyStatus.RDY;
    }

    public ReadyStatus getReadyStatus() {
        if (ownerId == null) return ReadyStatus.OWN;
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

    protected boolean isExpectedInkBottle(int slot, ItemStack stack) {
        int inkIndex = slot - blackInkSlot;
        return inkIndex >= 0 && inkIndex < 4 && stack != null && stack.getItem() == getExpectedInkItem(inkIndex);
    }

    protected Item getPaperItem() {
        return Items.paper;
    }

    protected Item getGunpowderItem() {
        return Items.gunpowder;
    }

    protected Item getExpectedInkItem(int inkIndex) {
        return ProspectingCraft.itemInk[inkIndex];
    }

    protected void useInk() {
        for (int i = blackInkSlot; i <= cyanInkSlot; i++) {
            ItemStack stack = getStackInSlot(i);
            if (isInkBottle(stack)) {
                ItemStack damagedStack = stack.copy();
                int damage = damagedStack.getItemDamage() + 1;
                damagedStack.setItemDamage(damage);
                if (damage >= damagedStack.getMaxDamage())
                    setInventorySlotContents(i, new ItemStack(Items.glass_bottle, 1));
                else setInventorySlotContents(i, damagedStack);
            }
        }
    }

    protected void consumeSurveySupplies() {
        usePaper();
        useInk();
        decrStackSize(gunpowderSlot, 1);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack == null) return false;
        if (slot == paperSlot) return stack.getItem() == getPaperItem();
        if (slot >= blackInkSlot && slot <= cyanInkSlot) return isExpectedInkBottle(slot, stack);
        if (slot == gunpowderSlot) return stack.getItem() == getGunpowderItem();
        return false;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return isItemValidForSlot(slot, stack);
    }

    public boolean tryBeginScan() {
        if (!isReadyToActivate()) return false;
        beginScan();
        return true;
    }

    protected void beginScan() {
        // System.out.printf("SeismicRecorderTE.beginScan\n");
        if (ownerId == null) return;
        SurveyArea surveyArea = new SurveyArea(
            getSurveyTier(),
            BaseUtils.getWorldDimensionId(worldObj),
            getX() - 64,
            getZ() - 64,
            128,
            128);
        PlayerSurveyData.get(worldObj, ownerId)
            .addSurveyArea(surveyArea);
        activeSurveyId = surveyArea.getSurveyId();
        consumeSurveySupplies();
        fireGunpowder();
        activated = true;
        phase = 0;
        markChanged();
    }

    protected SurveyTier getSurveyTier() {
        if (worldObj == null) return SurveyTier.BASE;
        return surveyTierForBlock(worldObj.getBlock(getX(), getY(), getZ()));
    }

    static SurveyTier surveyTierForBlock(Block block) {
        if (block instanceof SeismicRecorderBlock) return ((SeismicRecorderBlock) block).getSurveyTier();
        return SurveyTier.BASE;
    }

    protected void fireGunpowder() {
        // System.out.printf("Bang!\n");
        worldObj.newExplosion(null, getX(), getY(), getZ(), 0, false, true);
    }

    @Override
    public void update() {
        if (!worldObj.isRemote) {
            if (linkTimer++ >= linkCheckInterval) {
                linkTimer = 0;
                checkForLink();
            }
            if (activated) updateActiveScan();
        }
    }

    protected void updateActiveScan() {
        PlayerSurveyData playerSurveyData = getPlayerSurveyData();
        SurveyArea activeSurvey = playerSurveyData == null ? null : playerSurveyData.getSurveyArea(activeSurveyId);
        if (activeSurvey == null) {
            LOG.warn(
                "Aborted Surveyor scan at " + getX()
                    + ", "
                    + getY()
                    + ", "
                    + getZ()
                    + " because its active survey record is missing");
            activated = false;
            phase = 0;
            activeSurveyId = null;
            markChanged();
            return;
        }

        if (phase < 128) {
            // System.out.printf("SeismicRecorderTE.update: Scanning row %s\n", phase);
            scanRow(phase, playerSurveyData, activeSurvey);
            phase += 1;
        } else {
            activated = false;
            // System.out.printf("SeismicRecorderTE.update: Resetting phase\n");
            phase = 0;
            notifySurveyChanged(activeSurvey);
            activeSurveyId = null;
        }
        markChanged();
    }

    public void cancelActiveScan() {
        if (!activated || activeSurveyId == null || worldObj != null && worldObj.isRemote) return;
        PlayerSurveyData playerSurveyData = getPlayerSurveyData();
        SurveyArea removedSurvey = playerSurveyData == null ? null : playerSurveyData.removeSurveyArea(activeSurveyId);
        activated = false;
        phase = 0;
        activeSurveyId = null;
        markChanged();
        if (removedSurvey != null) notifySurveyChanged(removedSurvey);
    }

    protected void notifySurveyChanged(SurveyArea surveyArea) {
        SurveyChunkServerHandler.INSTANCE.notifySurveyChanged(ownerId, surveyArea);
    }

    protected void checkForLink() {
        // System.out.printf("SeismicRecorderTE.checkForLink: Searchihg for geophones\n");
        BlockPos recorderPos = getPos();
        List<BlockPos> geophonePositions = findNearbyGeophonePositions(recorderPos);
        int ng = geophonePositions.size();
        boolean ok = hasWellSeparatedTrio(recorderPos, geophonePositions);
        if (numGeophones != ng || geophonesOkay != ok) {
            numGeophones = ng;
            geophonesOkay = ok;
            markForUpdate();
            // System.out.printf("SeismicRecorderTE.checkForLink: numGeophones = %s, geophonesOkay = %s\n",
            // numGeophones, geophonesOkay);
        }
    }

    protected List<BlockPos> findNearbyGeophonePositions(BlockPos recorderPos) {
        List<BlockPos> positions = new ArrayList<BlockPos>();
        int cx0 = (recorderPos.x - 15) >> 4, cz0 = (recorderPos.z - 15) >> 4;
        int cx1 = (recorderPos.x + 15) >> 4, cz1 = (recorderPos.z + 15) >> 4;
        for (int cz = cz0; cz <= cz1; cz++) for (int cx = cx0; cx <= cx1; cx++) {
            if (!worldObj.getChunkProvider()
                .chunkExists(cx, cz)) continue;
            Chunk chunk = worldObj.getChunkFromChunkCoords(cx, cz);
            for (Object obj : getChunkTileEntityMap(chunk).values()) if (obj instanceof GeophoneTE) {
                GeophoneTE te = (GeophoneTE) obj;
                BlockPos geophonePos = te.getPos();
                if (isInsideGeophoneSearchArea(recorderPos, geophonePos)) positions.add(geophonePos);
            }
        }
        return positions;
    }

    static boolean isInsideGeophoneSearchArea(BlockPos recorderPos, BlockPos geophonePos) {
        return Math.abs(geophonePos.x - recorderPos.x) <= 15 && Math.abs(geophonePos.z - recorderPos.z) <= 15;
    }

    static boolean hasWellSeparatedTrio(BlockPos recorderPos, List<BlockPos> geophonePositions) {
        for (int first = 0; first < geophonePositions.size(); first++) {
            BlockPos firstPos = geophonePositions.get(first);
            if (!isWellSeparated(recorderPos, firstPos)) continue;
            for (int second = first + 1; second < geophonePositions.size(); second++) {
                BlockPos secondPos = geophonePositions.get(second);
                if (!isWellSeparated(recorderPos, secondPos) || !isWellSeparated(firstPos, secondPos)) continue;
                for (int third = second + 1; third < geophonePositions.size(); third++) {
                    BlockPos thirdPos = geophonePositions.get(third);
                    if (isWellSeparated(recorderPos, thirdPos) && isWellSeparated(firstPos, thirdPos)
                        && isWellSeparated(secondPos, thirdPos)) return true;
                }
            }
        }
        return false;
    }

    private static boolean isWellSeparated(BlockPos first, BlockPos second) {
        long dx = first.x - second.x;
        long dy = first.y - second.y;
        long dz = first.z - second.z;
        return dx * dx + dy * dy + dz * dz >= 7.5 * 7.5;
    }

    protected void scanRow(int j, PlayerSurveyData playerSurveyData, SurveyArea surveyArea) {
        int r = 64;
        int x0 = getX();
        int z0 = getZ();
        int z = z0 - r + j;
        World world = worldObj;
        for (int i = 0; i < 128; i++) {
            int x = x0 - r + i;
            if (world.getChunkProvider()
                .chunkExists(x >> 4, z >> 4)) scanCoords(world, x, z, surveyArea);
        }
        playerSurveyData.markDirty();
    }

    protected void scanCoords(World world, int x, int z, SurveyArea surveyArea) {
        surveyArea.markSurveyed(x, z);
        for (int y = 0; y < getY(); y++) {
            Block block = world.getBlock(x, y, z);
            int meta = world.getBlockMetadata(x, y, z);
            Item item = Item.getItemFromBlock(block);
            if (item != null) {
                ItemStack stack = new ItemStack(item, 1, itemMetadataForScannedBlock(block, meta));
                for (String oreName : OreDictionaryResolver.getCanonicalOreNames(stack))
                    surveyArea.recordOre(x, z, oreName, 1);
            }
        }
    }

    protected static int itemMetadataForScannedBlock(Block block, int blockMetadata) {
        return block.damageDropped(blockMetadata);
    }

    protected PlayerSurveyData getPlayerSurveyData() {
        if (ownerId == null) return null;
        return PlayerSurveyData.get(worldObj, ownerId);
    }

    public static boolean itemStackIsOfInterest(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        if (stack.getItem() == Item.getItemFromBlock(Blocks.obsidian)) return true;
        for (int oreId : OreDictionary.getOreIDs(stack)) if (OreDictionary.getOreName(oreId)
            .toLowerCase(Locale.ROOT)
            .contains("ore")) return true;
        return stack.getUnlocalizedName()
            .toLowerCase(Locale.ROOT)
            .contains("ore");
    }

}
