import java.util.concurrent.ConcurrentHashMap;

/**
 * The Directory class is mainly responsible for linking file names to Inodes,
 * so that a text-based lookup can be performed when attempting to read, write, open,
 * or delete a set of data.  It also regulates filename size and keeps track of the next
 * free Inode (out of the limited supply).
 */
public class Directory {
    private static int maxChars = 30; // max characters of each file name
    private int freeInodes;     // Track how many inodes remain
    private short nextFreeInode;  // Keep track of the next free Inode

    // Directory entries
    private int fnsizes[];        // each element stores a different file name size.
    private char fnames[][];    // each element stores a different file name.
    private ConcurrentHashMap map;  // Map each string filename to its inode number in O(1) lookup data type

    public Directory( int maxInumber ) { // directory constructor

        freeInodes = maxInumber - 1;        // Remaining free inodes, after the directory's is used
        nextFreeInode = 1;                  // Track the next available Inode

        fnsizes = new int[maxInumber];     // maxInumber = max files
        for ( int i = 0; i < maxInumber; i++ ) {
            fnsizes[i] = 0;                 // all file name sizes initialized to 0
        }

        fnames = new char[maxInumber][maxChars];
        String root = "/";                // entry(inode) 0 is "/"
        fnsizes[0] = root.length( );        // fnsizes[0] is the size of "/".
        root.getChars( 0, fnsizes[0], fnames[0], 0 ); // fnames[0] includes "/"

        // Set mapping for root "/" value
        map = new ConcurrentHashMap(maxInumber);
        map.put(root, (short)0);
    }

    public int bytes2directory( byte data[] ) {
        // assumes data[] received directory information from disk
        // initializes the Directory instance with this data[]
        int success = Kernel.ERROR;

        try{
            freeInodes = fnsizes.length;

            // Copy first (4 * maxInumber) = probably 192 bytes into fnsizes;
            // this should work as this (the Directory instance) must have been instantiated with
            // the correct number of Inodes... or else everything will break!
            for(int i = 0; i < fnsizes.length; ++i ){
                fnsizes[i] = SysLib.bytes2int(data, i * 4);
            }

            // Create new temporary byte array to hold the remainder; this allows us to work
            // without a nasty offset addition every iteration.
            byte[] names = new byte[(data.length - (fnsizes.length * 4))];
            System.arraycopy(data, fnsizes.length * 4, names,0, names.length);

            // track offset within names[] so that we can feed subsections to arraycopy simply
            int offset = 0;
            int length;
            String name;

            // Traverse fnsizes and extract appropriate strings (or nothing for zero length)
            for(int i = 0; i < fnsizes.length; ++i){
                if(fnsizes[i] != 0){

                    // Read next filename size from fnsizes; read this into a string and then store that string
                    // back in fnames at the appropriate inumber index.
                    length = fnsizes[i];
                    name = new String(names, offset, length);
                    name.getChars(0, length, fnames[i], 0);
                    map.put(name, i);

                    // advance offset w/in names by the appropriate amount to reach the next stored filename
                    // (ignores 0-length fnsizes because they don't actually matter)
                    offset += length;

                    // For every filename we find, there should be one fewer free inode on disk
                    --freeInodes;
                }
            }
            // Return to the start of the index and find the first free (unused) inumber index.
            // This will be returned the next time ialloc() is called
            nextFreeInode = findNextFreeInode((short)0);

            success = Kernel.OK;

        }
        catch (Exception e){
            SysLib.cerr(e.toString());
        }
        return success;
    }

    public synchronized byte[] directory2bytes( ) {
        // converts and return Directory information into a plain byte array
        // this byte array will be written back to disk
        // note: only meaningful directory information should be converted
        // into bytes.
        byte[] dirData;                      // byte array to store serialized directory information
        int length = fnsizes.length * 4;     // At the very least, we need enough bytes to store fnsizes
        int fnlength = length;               // We also want to keep the length of fnsizes handy for later
        int offset = length;                 // filename section starts here, but we need to update offset on the fly


        // Determine number of bytes needed for every directory entry that is used
        // (making no assumptions about the root entry's composition at this point
        for(int i = 0; i < fnsizes.length; ++i){
            length += fnsizes[i];            // each fnsizes entry is that inode's associated filename's length in bytes
        }
        // Instantiate dirData, hopefully now large enough to store all entries
        dirData = new byte[length];

        try {
            // copy in each fnsizes entry and its associated filename, if any
            for (int i = 0; i < fnsizes.length; ++i) {

                // int2bytes places the converted 4-byte rendition of the integer directly into our byte[]
                SysLib.int2bytes(fnsizes[i], dirData, i * 4);

                // Copy the recorded number of chars (bytes) into the filenames section starting at <offset>
                // (should do nothing if the filename's length is 0
                for(int j = 0; j < fnsizes[i]; ++j){
                    dirData[offset+j] = (byte) fnames[i][j];
                }
                //System.arraycopy(fnames[i], 0, dirData, offset, fnsizes[i]);

                // Advance offset to the start of the next name (which may be 0 bytes away and 0 length, so
                // it's based on the length read from fnsizes.
                offset += fnsizes[i];
            }
        }
        catch (IndexOutOfBoundsException e){
            SysLib.cerr(e.toString());
        }

        return dirData;
    }

    public synchronized short ialloc( String filename ) {
        // filename is the one of a file to be created.
        // allocates a new inode number for this filename
        short inumber = -1;
        // This works because we can't actually modify a parameter; instead we get a local copy
        filename = (filename.length() <= maxChars) ? filename : filename.substring(0,maxChars);

        // Can only assign a new Inode if any are available
        if(nextFreeInode != -1 && freeInodes >= 0) {

            // Only allow a new Inode allocation if the file doesn't already exist
            if (!map.containsKey(filename)) {
                inumber = nextFreeInode;
                --freeInodes;
                nextFreeInode = findNextFreeInode(nextFreeInode);

                // register new filename in fnsize, fnames, and map
                fnsizes[inumber] = filename.length();
                filename.getChars(0, fnsizes[inumber], fnames[inumber], 0);
                map.put(filename, (short)inumber );
            }
        }

        return inumber;
    }

    private short findNextFreeInode(short lastFree){
        short next = -1;
        if(freeInodes > 0){
            for(int i = lastFree + 1; i < fnsizes.length; ++i){
                if(fnsizes[i] == 0){
                    next = (short)i;
                    break;
                }
            }
            if(next == -1){
                freeInodes = 0;
            }
        }
        return next;
    }

    public synchronized boolean ifree( short iNumber ) {
        // deallocates this inumber (inode number)
        // the corresponding file will be deleted.
        boolean found = false;

        if(map.containsValue((short)iNumber) && fnsizes[iNumber] != 0){
            found = true;

            // Remove all information about this entry from Directory fields
            String key = new String(fnames[iNumber]);
            fnsizes[iNumber] = 0;
            fnames[iNumber] = new char[maxChars];
            map.remove(key, iNumber);
            map.remove(key);

            if( (++freeInodes) > fnsizes.length)
                freeInodes = fnsizes.length;
            nextFreeInode = iNumber;
        }

        return found;
    }

    // returns the inumber corresponding to this filename
    public short namei( String filename ) {
        short inumber;
        // This works because we can't actually modify a parameter; instead we get a local copy
        filename = (filename.length() <= maxChars) ? filename : filename.substring(0,maxChars);

        // All filenames should be contained in the map, including the root "/" dir.
        if(map.containsKey(filename)){
            inumber = (short)map.get(filename);
        }
        else{
            inumber = -1;
        }

        return inumber;
    }
}