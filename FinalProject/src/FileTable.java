import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * The FileTable class is mainly concerned with linking processes' file descriptors to Inodes
 * that point to the actual data on disk.  It tracks which entries are accessing which files
 * with which permissions, and is responsible for telling all Inodes to write to disk at sync.
 */
public class FileTable {

    private Vector<FileTableEntry> table;         // the actual entity of this file table
    private Directory dir;        // the root directory
    private List<Inode> inodes;   // Track all used Inodes, for updating purposes


    public FileTable( Directory directory, int maxInodes ) { // constructor
        table = new Vector<FileTableEntry>( );     // instantiate a file (structure) table
        dir = directory;           // receive a reference to the Directory
        // from the file system
        inodes = new ArrayList<Inode>(maxInodes);   // ArrayList of used inodes
    }

    public synchronized void sync(){
        for(FileTableEntry fte: table){
            fte.inode.toDisk(fte.iNumber);
        }
    }

    // major public methods
    public synchronized FileTableEntry falloc( String filename, String mode ) {
        // allocate a new file (structure) table entry for this file name
        // allocate/retrieve and register the corresponding inode using dir
        // increment this inode's count
        // immediately write back this inode to the disk
        // return a reference to this file (structure) table entry
        FileTableEntry newFTE = null;
        Inode inode;
        short inumber = dir.namei(filename);

        // Case 1 or 2: file exists on disk
        if(inumber != -1){

            // Create a new Inode based on this inumber, since we know the file should exist
            inode = new Inode(inumber);

            // Check if this Inode is already in use
            // If there is no existing Inode in the inodes list, no other process is accessing it
            for (Inode i: inodes) {
                if (i.equals(inode)) {
                    inode = i;
                    ++inode.count;
                    break;
                }
            }
            // Now we have an Inode instance and an inumber
        } else {

            // Case 3: file does not exist on disk, nobody is accessing it
            // Only allowed if mode is not "r"
            if(mode.compareTo("r") != 0) {
                inumber = dir.ialloc(filename);
            }
            inode = new Inode();


            // Now we have an Inode instance and an inumber
        }

        // If the inumber comes back -1 (invalid / not allowed) or if the
        // Inode exists but is scheduled for deletion, do not allow
        if(inumber != -1 && inode.flag != Inode.DELETE) {
            // Create newFTE with inode and inumber we have gotten
            newFTE = new FileTableEntry(inode, inumber, mode);

            // Record new FTE and, if necessary, the inode
            table.add(newFTE);
            if (!inodes.contains(inode)) {
                inodes.add(inode);
            }
            // Write back inode
            inode.toDisk(inumber);
        }

        return newFTE;
    }

    public synchronized boolean ffree( FileTableEntry e ) {
        // receive a file table entry reference
        // save the corresponding inode to the disk
        // free this file table entry.
        // return true if this file table entry found in my table
        boolean found = false;

        // Check if e is in table; if so, unlink (decrement count) from the Inode,
        // Save back the Inode to disk (using the FTE's iNumber entry).
        if(table.contains(e)){

            Inode inode = e.inode;

            // Decrement the FTE's count, in case it is a copy held by a child
            e.count--;

            // Only actually *remove* the FTE if nobody has any references to it.
            if(e.count<= 0){
                table.remove(e);
            }

            // Decrement inode count
            inode.count--;

            // If nobody has any references to the inode, it can also be retired from active duty
            if(inode.count <= 0){
                inodes.remove(inode);
            }

            // Attempt write-back; only report success if this also succeeds
            if(inode.toDisk(e.iNumber) != Kernel.ERROR){
                found = true;
            }
        }
        return found;
    }

    public synchronized boolean fempty( ) {
        return table.isEmpty( );  // return if table is empty
    }                            // should be called before starting a format
}