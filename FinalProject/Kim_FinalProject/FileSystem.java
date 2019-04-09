/**
 * The main hub of filesystem control.
 * This class provides all functions of a filesystem, which SysLib (via the Kernel) exposes to users.
 * It is responsible for initializing a disk with the correct file system information,
 * for tracking files for deletion, and for consolidating Inodes' data blocks for reading and writing.
 */
public class FileSystem {
    private Superblock superblock;
    private Directory directory;
    private FileTable filetable;
    private int dataBlockCount;

    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

    // Constants that constrain how far writes can proceed
    private final int MAX_BLOCKS = 267; // 11 direct + 256 indirect
    private final int MAX_BYTES = MAX_BLOCKS * 512;

    public FileSystem( int diskBlocks ){
        // default superblock creation and formatting
        superblock = new Superblock( diskBlocks );

        // create new Directory and set root "/" as first entry
        directory = new Directory( superblock.totalInodes );

        // Link directory to new FileTable
        filetable = new FileTable( directory, superblock.totalInodes );

        // Recreate the root entry if one exists
        FileTableEntry dirEnt = open( "/", "r");
        int dirSize = fsize( dirEnt );
        if ( dirSize > 0 ) {
            byte[] dirData = new byte[dirSize];
            read(dirEnt, dirData);
            directory.bytes2directory(dirData);
        }
        close( dirEnt );

        // calculate the total number of blocks available for data from the disk blocks and inode count
        dataBlockCount = (diskBlocks - (1 + (int)Math.ceil(superblock.totalInodes/16.0)));
    }

    // Sync data to "disk" so that the "disk" can be saved to the actual disk
    public void sync( ) {

        // Do this first as it may take up more blocks now
        FileTableEntry dirEnt = open( "/", "w");
        byte [] dirData = directory.directory2bytes();
        write(dirEnt, dirData);

        superblock.toDisk();
        filetable.sync();

    }

    private synchronized boolean initializeSuperblock(Superblock sb){
        boolean initialized = false;
        try{
            initialized = ((sb.toDisk() == Kernel.OK));
        }
        catch (Exception e){
            SysLib.cerr(e.toString() + "");
        }

        return initialized;
    }

    private synchronized boolean initializeDirectory(Directory dir){
        boolean initialized = false;

        try{
            // allocate byte array for the new directory's byte representation
            byte [] dirAsBytes = dir.directory2bytes();
            byte [] buffer = new byte[512];
            int remLength, directCount;
            int indirectCount = 0;

            // Allocate and configure the very first Inode for use by the directory
            Inode dirI = new Inode();
            dirI.length = dirAsBytes.length;    // Total length of the initial dir, *should* be 256 + 1

            // Figure out how many data blocks, of both direct and indirect type, are needed
            directCount = (1 + (dirI.length/512)); // Total number of blocks needed; will be at least 1
            if(directCount > 11){
                indirectCount = directCount - 11;
                directCount = 11;
            }

            // Prepare to write dir to disk.  It *should* be smaller than 512b, but that's not
            // guaranteed.  So we need an approximation of the Inode write-out process (may move this later)
            // For every block required to store the Directory's initial status, write out 512 bytes
            // in a free data block (and update the freeList)
            for(int i = 0; i < directCount; ++i){
                // Assign next free block to this Inode
                dirI.direct[i] = superblock.getNextFree();

                // Check remaining length of dirAsBytes to prevent underrun Exceptions
                remLength = (dirI.length-(i*512) >= 512) ? 512 : dirI.length - (i * 512);

                // Read next section of dirAsBytes into buffer for writing;
                System.arraycopy(dirAsBytes, 512 * i, buffer, 0, remLength);

                // Write out the current 512-byte chunk
                SysLib.rawwrite(dirI.direct[i], buffer);
            }

            // If there is (somehow) a need for indirect data blocks for the directory, we must
            // A) Allocate a data block # for the indirect block
            // B) Allocate a buffer to store entries for that indirect block
            // C) Allocate one (or more) data block numbers for the data blocks the indirect block points at
            if(indirectCount != 0){
                dirI.indirect = superblock.getNextFree();

                byte[] indirectBlock = new byte[512];       // Allocate a block to hold all indirect data blocks

                short nextDataBlock;

                for(int j = 0; j < indirectCount; ++j){

                    // C) allocate another data block for the indirectly-pointed-to data
                    nextDataBlock = superblock.getNextFree();

                    // Load the new data block location into indirectBlock;
                    SysLib.short2bytes(nextDataBlock, indirectBlock, j*2);

                    // We still need to track remaining length, keeping in mind that we've filled 11 blocks already
                    remLength = (dirI.length-(5632 + (j*512)) >= 512) ? 512 : dirI.length - (5632 + (j * 512));

                    System.arraycopy(dirAsBytes, (5632+(j*512)), buffer, 0, remLength);

                    SysLib.rawwrite(nextDataBlock, buffer);
                }

                // Finally, write out the indirect block
                SysLib.rawwrite(dirI.indirect, indirectBlock);

            }
            // write inode 0 back to disk
            dirI.toDisk((short)0);
            // If we got this far, Directory is fully initialized
            initialized = true;
        }
        catch (Exception e){
            SysLib.cerr(e.toString() + "");
        }

        return initialized;
    }

    private synchronized boolean initializeInodes(int files){
        boolean initialized = false;
        // create an Inode that can "stamp" values onto the remaining Inode positions after 0
        Inode stamp = new Inode();
        stamp.count = 0;
        stamp.flag = Inode.UNUSED;

        try {
            // "Stamp" the default values onto every Inode# within the specified Inode blocks
            // This is slow due to reading, adjusting and writing back the block once for each Inode.
            for (short i = 1; i < superblock.totalInodes; ++i) {
                stamp.toDisk(i);
            }

            initialized = true;
        }
        catch (Exception e){
            SysLib.cerr(e.toString() + "");
        }

        return initialized;
    }

    private synchronized boolean initializeData( int files, int diskBlocks){
        boolean initialized = false;
        byte [] buffer = new byte[512];
        try {
            // Initialize every data block but the last with the block # of the *next* block
            for (int i = 0; i < diskBlocks - 1; ++i) {

                // Add the next block's block number to the beginning of this block
                SysLib.short2bytes((short) (i + 1), buffer, 0);
                // Write new block to disk
                SysLib.rawwrite((short) i, buffer);
            }

            // Last block (currently) has no successor
            SysLib.short2bytes((short) (-1), buffer, 0);
            SysLib.rawwrite((diskBlocks - 1), buffer);

            initialized = true;

        }
        catch (Exception e){
            SysLib.cerr(e.toString() + "\n");
        }
        return initialized;

    }

    public boolean format( int files) {
        boolean formatted = false;
        try {
            int diskBlocks = superblock.totalBlocks;

            // Create default FS components
            superblock = new Superblock(diskBlocks, files);
            directory = new Directory(files);
            filetable = new FileTable(directory, files);

            // Write new superblock to disk
            formatted &= this.initializeData( files, diskBlocks );
            formatted &= this.initializeSuperblock(superblock);
            formatted &= this.initializeDirectory(directory);
            formatted &= this.initializeInodes(files);
            formatted &= (SysLib.sync() == Kernel.OK);

        }
        catch (Exception e){
            SysLib.cerr(e.toString() + "");
        }

        return formatted;
    }

    public FileTableEntry open( String filename, String mode){

        FileTableEntry fte = filetable.falloc(filename, mode);
        if(fte != null) {

            if (mode.compareTo("w") == 0) {
                deallocaAllBlocks(fte);
            }

            fte.inode.count++;
        }

        return fte;
    }

    public int close( FileTableEntry ftEnt ) {
        int closed = Kernel.ERROR;

        if(filetable.ffree(ftEnt)){
            if(ftEnt.inode.count == 0 && ftEnt.inode.flag == Inode.DELETE){
                deallocaAllBlocks(ftEnt);
                ftEnt.inode.flag=Inode.UNUSED;
                ftEnt.inode.toDisk(ftEnt.iNumber);
            }
            closed = Kernel.OK;
        }

        return closed;
    }

    public int fsize( FileTableEntry ftEnt ) {
        return ftEnt.inode.length;
    }

    public int read( FileTableEntry fte, byte[] buffer ){
        int bytesRead = Kernel.ERROR;

        // Trivial case
        if(buffer.length == 0 || fte.inode.length == fte.seekPtr){
            return 0;
        }

        int firstBlock = fte.seekPtr / 512; // Block # within the file at which reads must begin
        int lBlocks;                        // Length of buffer in blocks
        int currentBlock = firstBlock;      // block-level cursor within iNode's total blocks
        int currentNewBlock = 0;
        int length = buffer.length;
        Inode fInode = fte.inode;

        short [] allNecessaryBlocks; // Will hold block addresses of all data blocks needed to write buffer
        // taking into account the seekPtr position, maximum number of blocks addressable,
        // etc.
        byte[] iBlock = new byte[512];   // stores indirect block contents (shorts as 2-byte chunks * 256 )

        // Adjust total length to fit within the maximum allowable file size
        if (fte.seekPtr + length > MAX_BYTES){
            length = MAX_BYTES - fte.seekPtr;
        }
        // # of data blocks necessary to store the buffer (1-512 bytes = 1, 513 = 2, etc)
        lBlocks = (int)Math.ceil(((fte.seekPtr % 512) + buffer.length) / 512.0);

        // Adjust total number of blocks needed to fit within the maximum number of blocks
        if(lBlocks + firstBlock > MAX_BLOCKS){
            lBlocks = MAX_BLOCKS - firstBlock;
        }

        // We now know how many blocks are *necessary*, so we can create the array to hold block addresses.
        allNecessaryBlocks = new short [lBlocks];

        // Now, fill in allNecessaryBlocks.
        // There *must* be some number of bytes past the seekPtr, or we wouldn't be here
        if(fInode.length > fte.seekPtr){
            int alreadyAllocatedBlocks = (int)Math.ceil((fInode.length - fte.seekPtr) / 512.0); // if there is one more
                                                                                                // byte, 1 block, etc.

            // Prepare for reading in block #s from the Indirect block, if necessary.
            if(fInode.indirect != -1){
                SysLib.rawread(fInode.indirect, iBlock);
            }

            // i is the index into fInode.direct[] *and* the indirect block, so some gymnastics are necessary
            for (int i = firstBlock, j = 0; i < (firstBlock + alreadyAllocatedBlocks) && j < allNecessaryBlocks.length;
                 ++i, ++j){
                if(i < 11){
                    // copy ith direct block to jth aNB index (e.g. 5 -> 0, 6->1, 7->2, etc.
                    allNecessaryBlocks[j] = fInode.direct[i];
                }
                else{
                    // Copy shorts from indirect block, where the 0th element is actually the 11th Inode block
                    allNecessaryBlocks[j] = SysLib.bytes2short(iBlock, (i-11)*2);
                }
                currentBlock++;     // We have advanced from firstBlock; this tracks how far in the Inode's blocks
                currentNewBlock++;  // We have also advanced within allNecessaryBlocks.
            }
        }

        // allNecessaryBlocks should now be filled with the block numbers needed to read in the buffer
        int cursorOnBuffer = 0;                 // We need to track location within the buffer
        length = buffer.length;                 // Also need to track size of current source chunk
        bytesRead = 0;                          // Having reached this point, assume we are reading N bytes, no error
        int advanced;

        for(int i = 0; i < allNecessaryBlocks.length; ++i){

            // This reads in one block
            advanced = readBlock(allNecessaryBlocks[i], buffer, fte.seekPtr%512, length, cursorOnBuffer);
            if(advanced != 0){
                length -= advanced;
                fte.seekPtr += advanced;
                cursorOnBuffer += advanced;
                bytesRead += advanced;
            }
            else{
                bytesRead = Kernel.ERROR;
                break;
            }
        }

        return bytesRead;
    }

    private int readBlock(short blockAdd, byte [] buffer, int start, int length, int cursor){

        int advanced = 0;
        byte [] newBlock = new byte[512];   // buffer for XOR-ing appropriate chunk of buffer onto a disk block

        if(length == 0){
            return advanced;
        }

        if(start == 0 && length >= 512){
            SysLib.rawread(blockAdd, newBlock);

            System.arraycopy(newBlock, 0, buffer, cursor, 512);
            advanced = 512;
        }
        else{
            // if the length, from the start position, exceeds the block end, only advance up to the block end.
            int span = (start + length <= 512) ? length : 512 - start;
            // Read in on-disk block
            SysLib.rawread(blockAdd, newBlock);
            // Overlay only the relevant chunk of buffer - can start at 0 ~ 510, can end at 1 ~ 511.
            System.arraycopy(newBlock, start, buffer, cursor, span);
            advanced = span;
        }

        return advanced;

    }

    private short[] allocateBlocksForWrite(FileTableEntry fte, byte[] buffer){

        int firstBlock = fte.seekPtr / 512; // Block # within the file at which writes must begin
        int lBlocks;                        // Length of buffer in blocks
        int currentBlock = firstBlock;      // block-level cursor within iNode's total blocks
        int currentNewBlock = 0;
        int length = buffer.length;
        Inode fInode = fte.inode;

        short [] allNecessaryBlocks; // Will hold block addresses of all data blocks needed to write buffer
        // taking into account the seekPtr position, maximum number of blocks addressable,
        // etc.
        byte[] iBlock = new byte[512];   // stores indirect block contents (shorts as 2-byte chunks * 256 )

        // Adjust total length to fit within the maximum allowable file size
        if (fte.seekPtr + length > MAX_BYTES){
            length = MAX_BYTES - fte.seekPtr;
        }

        // # of data blocks necessary to store the buffer (1-512 bytes = 1, 513 = 2, etc)
        // Need to be cognizant of crossing block boundaries as well.
        lBlocks = (int)Math.ceil(((fte.seekPtr % 512) + buffer.length) / 512.0);

        // Adjust total number of blocks needed to write buffer to fit within the maximum number of blocks
        if(lBlocks + firstBlock > MAX_BLOCKS){
            lBlocks = MAX_BLOCKS - firstBlock;
        }

        // If we need more new blocks than there are free blocks,
        if(lBlocks > superblock.totalFreeBlocks){
            lBlocks = superblock.totalFreeBlocks;
        }

        // We now know how many blocks are *necessary*, so we can create the array to hold block addresses.
        allNecessaryBlocks = new short [lBlocks];

        // Now, fill in allNecessaryBlocks.
        // First, check to see if the Inode already has access to blocks in the range
        if(fInode.length % 512 > 0 && fInode.length >= fte.seekPtr){
            int alreadyAllocatedBlocks = 1 +((fInode.length - fte.seekPtr) / 512); // if there is one more byte,
                                                                                   // 1 block, etc.

            // Prepare for reading in block #s from the Indirect block, if necessary.
            if(fInode.indirect != -1){
                SysLib.rawread(fInode.indirect, iBlock);
            }

            // i is the index into fInode.direct[] *and* the indirect block, so some gymnastics are necessary
            for (int i = firstBlock, j = 0; i < (firstBlock + alreadyAllocatedBlocks) && j < allNecessaryBlocks.length;
                 ++i, ++j){
                if(i < 11){
                    // copy ith direct block to jth aNB index (e.g. 5 -> 0, 6->1, 7->2, etc.
                    allNecessaryBlocks[j] = fInode.direct[i];
                }
                else{
                    // Copy shorts from indirect block, where the 0th element is actually the 11th Inode block
                    allNecessaryBlocks[j] = SysLib.bytes2short(iBlock, (i-11)*2);
                }
                currentBlock++;     // We have advanced from firstBlock; this tracks how far in the Inode's blocks
                currentNewBlock++;  // We have also advanced within allNecessaryBlocks.
            }
        }

        // Now, begin allocating new blocks.
        // Start with direct, if any are needed
        if(currentBlock < 11){
            for(int i = currentBlock, j = currentNewBlock; i < 11 && j < lBlocks; ++i, ++j){
                fInode.direct[i] = superblock.getNextFree();
                allNecessaryBlocks[j] = fInode.direct[i];
                currentBlock++;
                currentNewBlock++;
            }
        }

        // Then assign any required indirect blocks
        if(currentBlock >= 11 && currentNewBlock < lBlocks) {
            // Allocate a block to store indirect blocks if necessary
            if(fInode.indirect == -1){
                fInode.indirect = superblock.getNextFree();
            }
            else{
                SysLib.rawread(fInode.indirect, iBlock);
            }

            for (int i = currentBlock - 11, j = currentNewBlock; j < lBlocks; ++i, ++j) {
                short next = superblock.getNextFree();
                SysLib.short2bytes(next, iBlock, i*2);
                allNecessaryBlocks[j] = next;
                currentBlock++;
                currentNewBlock++;
            }
            // If everything went well, we have exactly as many blocks as we need
            // Save back iBlock to disk for safekeeping.
            SysLib.rawwrite(fInode.indirect, iBlock);

        }

        // Write back the Inode for safekeeping
        fInode.toDisk(fte.iNumber);

        // We should now have an array of all the data blocks that we will write to, in order.
        return allNecessaryBlocks;
    }

    private int xorBlock(short blockAdd, byte [] buffer, int start, int length, int cursor){
        int advanced = 0;
        byte [] newBlock = new byte[512];   // buffer for XOR-ing appropriate chunk of buffer onto a disk block

        if(length == 0){
            return advanced;
        }

        if(start == 0 && length >= 512){
            System.arraycopy(buffer, cursor, newBlock, 0, 512);
            advanced = 512;
        }
        else{
            // if the length, from the start position, exceeds the block end, only advance up to the block end.
            int span = (start + length <= 512) ? length : 512 - start;
            // Read in on-disk block
            SysLib.rawread(blockAdd, newBlock);
            // Overlay only the relevant chunk of buffer - can start at 0 ~ 510, can end at 1 ~ 511.
            System.arraycopy(buffer, cursor, newBlock, start, span);
            advanced = span;
        }
        // Finally, write updated block back to disk
        SysLib.rawwrite(blockAdd, newBlock);

        return advanced;
    }

    public int write( FileTableEntry ftEnt, byte[] buffer ){
        int wrote = 0;

        if(ftEnt.mode.compareTo("r") == 0){
            return Kernel.ERROR;
        }
        else if(buffer.length == 0){
            return buffer.length;
        }

        try{
            int originalSize = ftEnt.inode.length;
            int startPos = ftEnt.seekPtr;
            int cursorOnBuffer = 0;     // We need to track location within the buffer
            int length = buffer.length;                 // Also need to track size of current source chunk
            int advanced;
            short [] writeBlocks = allocateBlocksForWrite(ftEnt, buffer); // get a list of blocks to write into

            for(int i = 0; i < writeBlocks.length; ++i){

                // This writes back one block
                advanced = xorBlock(writeBlocks[i], buffer, ftEnt.seekPtr % 512, length, cursorOnBuffer );
                if(advanced != 0){
                    length -= advanced;
                    ftEnt.seekPtr += advanced;
                    cursorOnBuffer += advanced;
                    wrote += advanced;
                }
                else{
                    wrote = Kernel.ERROR;
                    break;
                }
            }
            ftEnt.inode.length = (startPos + wrote > originalSize ) ? startPos + wrote : originalSize;
        }
        catch (Exception e){
            SysLib.cerr(e.toString() + "\n");
        }

        return wrote;
    }

    private short[] getAllHeldBlocks(Inode inode){

        int firstBlock = 0; // Block # within the file at which reads must begin
        int lBlocks;                        // Length of buffer in blocks

        short [] allNecessaryBlocks; // Will hold block addresses of all data blocks needed to write buffer
        // taking into account the seekPtr position, maximum number of blocks addressable,
        // etc.
        byte[] iBlock = new byte[512];   // stores indirect block contents (shorts as 2-byte chunks * 256 )

        lBlocks = (int)Math.ceil(inode.length / 512.0); // # of data blocks necessary to store the buffer
                                                        // (1-512 bytes = 1, 513 = 2, etc)

        // We now know how many blocks are *necessary*, so we can create the array to hold block addresses.
        allNecessaryBlocks = new short [lBlocks];

        // Now, fill in allNecessaryBlocks.
        int alreadyAllocatedBlocks = lBlocks; // if there is one more byte, 1 block, etc.

        // Prepare for reading in block #s from the Indirect block, if necessary.
        if(inode.indirect != -1){
            SysLib.rawread(inode.indirect, iBlock);
        }

        // i is the index into fInode.direct[] *and* the indirect block, so some gymnastics are necessary
        for (int i = firstBlock, j = 0; i < (firstBlock + alreadyAllocatedBlocks) && j < allNecessaryBlocks.length;
             ++i, ++j){
            if(i < 11){
                // copy ith direct block to jth aNB index (e.g. 5 -> 0, 6->1, 7->2, etc.
                allNecessaryBlocks[j] = inode.direct[i];
            }
            else{
                // Copy shorts from indirect block, where the 0th element is actually the 11th Inode block
                allNecessaryBlocks[j] = SysLib.bytes2short(iBlock, (i-11)*2);
            }
        }

        return allNecessaryBlocks;

    }

    // Clears all data blocks (used for "w" mode and deletion) but does not
    // free the inode or FTE.
    private synchronized boolean deallocaAllBlocks( FileTableEntry ftEnt ){

        boolean deallocated = true;
        byte [] buffer = new byte[512];
        short[] blocks = getAllHeldBlocks(ftEnt.inode);
        short next = (short)superblock.freeList;

        // Return blocks in reverse order to avoid extensive traversal
        for(int i = blocks.length -1; i >= 0; --i){
            // Reset blocks
            deallocated &= (superblock.returnBlock(blocks[i]) == Kernel.OK);
        }

        // Reset inode state to nearly-pristine
        ftEnt.inode.length = 0;
        for(int i = 0; i<11; ++i){
            ftEnt.inode.direct[i] = -1;
        }
        ftEnt.inode.indirect = -1;
        ftEnt.inode.toDisk(ftEnt.iNumber);

        ftEnt.seekPtr = 0;

        return deallocated;
    }

    public boolean delete( String filename ){
        boolean setDelete = false;
        FileTableEntry fte = filetable.falloc(filename, "r");
        if(fte != null){
            fte.inode.flag = Inode.DELETE;
            directory.ifree(fte.iNumber);
            setDelete = true;
            filetable.ffree(fte);
        }

        return setDelete;
    }

    public int seek( FileTableEntry ftEnt, int offset, int whence){
        int newPos;
        boolean clamped = false;
        // Choose how to offset the seekPtr based on whence
        switch(whence){
            case SEEK_SET:
                newPos = 0 + offset;
                break;
            case SEEK_CUR:
                newPos = ftEnt.seekPtr + offset;
                break;
            case SEEK_END:
                newPos = ftEnt.inode.length + offset;
                break;
            default:
                newPos = ftEnt.seekPtr;
                break;
        }

        // Clamp extraneous values
        if(newPos < 0){
            clamped = true;
            newPos = 0;
        }
        else if(newPos > ftEnt.inode.length){
            clamped = true;
            newPos = ftEnt.inode.length;
        }
        // Update seekPtr
        ftEnt.seekPtr = newPos;

        if(clamped){
            return Kernel.OK;
        }
        else{
            return newPos;
        }
    }
}