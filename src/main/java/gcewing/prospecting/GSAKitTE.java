// ------------------------------------------------------
//
// ProspectingCraft - Sample Analyser Tile Entity
//
// ------------------------------------------------------

package gcewing.prospecting;

import static gcewing.prospecting.BaseBlockUtils.*;
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
import net.minecraftforge.common.util.Constants;

public class GSAKitTE extends BaseTileInventory implements ITickable {

    protected static final int sampleSlot = 0, litmusPaperSlot = 1, bookSlot = 2, numInventorySlots = 3;

    // Configuration options
    public static boolean debug = false;

    public State state = State.IDLE;
    public BlockPos samplePos;
    public int yMin, yCurrent, yMax;
    public List<Bucket> results;

    protected IInventory inventory;
    protected BucketCollection buckets;
    // protected double totalWeight;

    public static void configure(BaseConfiguration cfg) {
        debug = cfg.getBoolean("GSAKit", "debug", debug);
    }

    public GSAKitTE() {
        super();
        inventory = new InventoryBasic("", false, numInventorySlots);
    }

    @Override
    protected IInventory getInventory() {
        return inventory;
    }

    @Override
    public void readContentsFromNBT(NBTTagCompound nbt) {
        // System.out.printf("GSAKitTE.readContentsFromNBT: %s\n", nbt);
        super.readContentsFromNBT(nbt);
        state = states[nbt.getInteger("state")];
        yMin = nbt.getInteger("yMin");
        yMax = nbt.getInteger("yMax");
        yCurrent = yMin;
        if (nbt.hasKey("samplePos")) samplePos = blockPosFromNBT(nbt.getCompoundTag("samplePos"));
        else samplePos = null;
        // totalWeight = nbt.getDouble("totalWeight");
        NBTTagList weights = nbt.getTagList("resultWeights", Constants.NBT.TAG_DOUBLE);
        NBTTagList names = nbt.getTagList("resultNames", Constants.NBT.TAG_STRING);
        int n = min(weights.tagCount(), names.tagCount());
        results = null;
        if (n > 0) {
            results = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                Bucket b = new Bucket();
                b.weight = weights.func_150309_d(i);
                b.name = names.getStringTagAt(i);
                results.add(b);
            }
        }
    }

    @Override
    public void writeContentsToNBT(NBTTagCompound nbt) {
        super.writeContentsToNBT(nbt);
        nbt.setInteger("state", state.ordinal());
        nbt.setInteger("yMin", yMin);
        nbt.setInteger("yMax", yMax);
        if (samplePos != null) nbt.setTag("samplePos", nbtFromBlockPos(samplePos));
        // nbt.setDouble("totalWeight", totalWeight);
        if (results != null) {
            NBTTagList weights = new NBTTagList();
            NBTTagList names = new NBTTagList();
            for (Bucket b : results) {
                weights.appendTag(new NBTTagDouble(b.weight));
                names.appendTag(new NBTTagString(b.name));
            }
            nbt.setTag("resultWeights", weights);
            nbt.setTag("resultNames", names);
        }
    }

    @Override
    protected void readClientStateFromNBT(NBTTagCompound nbt) {
        super.readClientStateFromNBT(nbt);
        yCurrent = nbt.getInteger("yCurrent");
    }

    @Override
    protected void writeClientStateToNBT(NBTTagCompound nbt) {
        super.writeClientStateToNBT(nbt);
        nbt.setInteger("yCurrent", yCurrent);
    }

    protected boolean getSample() {
        ItemStack stack = getStackInSlot(sampleSlot);
        if (stack != null && stack.stackSize > 0) {
            samplePos = GeologicalSampleItem.getSamplePosition(stack);
            if (samplePos != null) return true;
        }
        return false;
    }

    protected boolean readyToAnalyse() {
        return getSample() && hasLitmusPaper();
    }

    protected boolean hasSample() {
        return hasStackInSlot(sampleSlot);
    }

    protected boolean hasLitmusPaper() {
        return hasStackInSlot(litmusPaperSlot);
    }

    @Override
    public void update() {
        if (!worldObj.isRemote) {
            switch (state) {
                case IDLE:
                    if (readyToAnalyse()) {
                        beginScan();
                        state = State.ANALYSING;
                        markChanged();
                    }
                    break;
                case ANALYSING:
                    if (yCurrent <= yMax) scanLayer(yCurrent++, worldObj, samplePos, buckets);
                    else {
                        endScan();
                        state = State.FINISHED;
                    }
                    markForUpdate();
                    break;
                case FINISHED:
                    if (!hasSample()) {
                        state = State.IDLE;
                        markChanged();
                    }
                    break;
            }
        }
    }

    protected void damageSample() {
        damageStackInSlot(sampleSlot, 1);
    }

    protected void useLitmusPaper() {
        decrStackSize(litmusPaperSlot, 1);
    }

    protected void beginScan() {
        // System.out.printf("GSAKitTE.beginScan\n");
        damageSample();
        useLitmusPaper();
        int y = samplePos.getY();
        yMin = max(0, y - 16);
        yMax = min(255, y + 16);
        yCurrent = yMin;
        buckets = new BucketCollection();
        // totalWeight = 0;
        results = null;
    }

    protected void endScan() {
        buckets.scaleWeights();
        results = buckets.sorted();
        // dumpResults("endScan");
    }

    public static List<Bucket> scanRegion(World world, BlockPos samplePos) {
        int yc = samplePos.getY();
        int y0 = max(0, yc - 16);
        int y1 = min(255, yc + 16);
        BucketCollection buckets = new BucketCollection();
        for (int y = y0; y <= y1; y++) scanLayer(y, world, samplePos, buckets);
        buckets.scaleWeights();
        return buckets.sorted();
    }

    protected static void scanLayer(int y, World world, BlockPos samplePos, BucketCollection buckets) {
        // System.out.printf("GSAKitTE.scanLayer: %s\n", y);
        int xc = samplePos.getX();
        int zc = samplePos.getZ();
        int k = y - samplePos.getY();
        for (int i = -16; i <= 16; i++) {
            for (int j = -16; j <= 16; j++) {
                if ((i | j | k) != 0) {
                    int x = xc + i;
                    int z = zc + j;
                    // double weight = 1.0/sqrt(i*i + j*j + k*k);
                    double weight = 1.0 / (i * i + j * j + k * k);
                    buckets.totalWeight += weight;
                    BlockPos pos = new BlockPos(x, y, z);
                    IBlockState state = getWorldBlockState(world, pos);
                    String ore = getOreName(state);
                    if (ore != null) {
                        if (debug) System.out.printf("ProspectingCraft: GSAKit: %s: %s\n", ore, pos);
                        Bucket b = buckets.get(ore);
                        b.weight += weight;
                    }
                }
            }
        }
    }

    protected void dumpResults(String title) {
        System.out.printf("GSAKitTE.dumpResults: for %s\n", title);
        if (results != null) {
            for (Bucket b : results) System.out.printf("GSAKit.dumpResults: %s %s\n", b.weight, b.name);
        }
    }

    protected static String getOreName(IBlockState state) {
        ItemStack stack = newBlockStack(state);
        if (stack != null) {
            if (SeismicRecorderTE.itemStackIsOfInterest(stack)) return stack.getUnlocalizedName();
        }
        return null;
    }

    // -------------------------------------------------------------------------

    public static enum State {
        IDLE,
        ANALYSING,
        FINISHED;
    }

    protected static State[] states = State.values();

    static class Bucket implements Comparable<Bucket> {

        public String name;
        public double weight;

        public int compareTo(Bucket that) {
            if (this.weight > that.weight) return -1;
            else if (this.weight < that.weight) return 1;
            else return 0;
        }

    }

    static class BucketCollection {

        double totalWeight = 0;
        Map<String, Bucket> map = new HashMap<>();

        public Bucket get(String name) {
            Bucket b = map.get(name);
            if (b == null) {
                b = new Bucket();
                b.name = name;
                map.put(name, b);
            }
            return b;
        }

        public void scaleWeights() {
            for (Bucket b : map.values()) b.weight /= totalWeight;
            totalWeight = 1;
        }

        public List<Bucket> sorted() {
            List<Bucket> list = new ArrayList(map.values());
            Collections.sort(list);
            return list;
        }

    }

}
