import java.util.ArrayList;
import java.util.List;

/**
 * The Superblock class keeps track of how many blocks and Inodes are contained in the filesystem,
 * but most importantly, keeps a pointer to the next free block as well as the total free block count.
 */
class Superblock {
    private final static int[] fieldSizes = {4, 4, 4, 4}; // Size of fields needed to store Superblock on disk

    public int totalBlocks; // the number of disk blocks
    public int totalInodes; // the number of inodes
    public int freeList;    // the block number of the free list's head
    public int totalFreeBlocks; // the number of free disk blocks


    public Superblock( int diskSize ) {
        this.totalBlocks = diskSize;
        this.totalInodes = 48;
        // First free block should be first block after Inodes
        this.freeList = (int)Math.ceil(this.totalInodes / 16.0) + 1;
        this.totalFreeBlocks = totalBlocks - freeList;
    }

    public Superblock( int diskSize, int inodeCount ) {
        this.totalBlocks = diskSize;
        this.totalInodes = inodeCount;
        // First free block should be first block after Inodes
        this.freeList = (int)Math.ceil(this.totalInodes / 16.0) + 1;
        this.totalFreeBlocks = totalBlocks - freeList;
    }

    public Superblock( short block) {
        // Prep to load
        List<Object> fields = SysLib.disk2List(block, 0, fieldSizes);

        if(fields.get(0) != null) { // read-in from disk has succeeded
            this.totalBlocks = (int) fields.get(0);
            this.totalInodes = (int) fields.get(1);
            this.freeList = (int) fields.get(2);
            this.totalFreeBlocks = (int) fields.get(3);
        }
        else{ // Something has gone wrong; initialize with useless values
            this.totalBlocks = 0;
            this.totalInodes = 0;
            this.freeList = 1;
            this.totalFreeBlocks = 0;
        }
    }

    public synchronized short getNextFree(){
        int [] header = {2};
        short next = (short)freeList;

        short nextNext = (short)(SysLib.disk2List(next, 0, header)).get(0);
        freeList = nextNext;
        --this.totalFreeBlocks;

        return next;
    }

    // Assumes blockNum has been deleted already
    public synchronized int returnBlock(short blockNum){

        short next = (short)freeList;

        int [] header = {2};
        List<Object> fields = new ArrayList<Object>(1);
        fields.add(0, next);

        // Log new next free block
        freeList = blockNum;
        // Increase count of free blocks
        ++totalFreeBlocks;

        // Write "next" block number to the freed blockNum
        return SysLib.list2Disk(fields, header, blockNum, 0);

    }

    public int toDisk(){
        List<Object> fields = new ArrayList<Object>(fieldSizes.length);
        fields.add(0, this.totalBlocks);
        fields.add(1, this.totalInodes);
        fields.add(2, this.freeList);
        fields.add(3, this.totalFreeBlocks);

        return SysLib.list2Disk(fields, fieldSizes, (short)0, 0);
    }
}