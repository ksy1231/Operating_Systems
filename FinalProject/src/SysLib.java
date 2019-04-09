import java.util.*;

public class SysLib {
    public static int exec( String args[] ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
                Kernel.EXEC, 0, args );
    }

    public static int join( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
                Kernel.WAIT, 0, null );
    }

    public static int boot( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
                Kernel.BOOT, 0, null );
    }

    public static int exit( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
                Kernel.EXIT, 0, null );
    }

    public static int sleep( int milliseconds ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
                Kernel.SLEEP, milliseconds, null );
    }

    public static int disk( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_DISK,
                0, 0, null );
    }

    public static int cin( StringBuffer s ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
                Kernel.READ, 0, s );
    }

    public static int cout( String s ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
                Kernel.WRITE, 1, s );
    }

    public static int cerr( String s ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
                Kernel.WRITE, 2, s );
    }

    public static int rawread( int blkNumber, byte[] b ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
                Kernel.RAWREAD, blkNumber, b );
    }

    public static int rawwrite( int blkNumber, byte[] b ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
                Kernel.RAWWRITE, blkNumber, b );
    }

    public static int sync( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
                Kernel.SYNC, 0, null );
    }

    public static int cread( int blkNumber, byte[] b ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
                Kernel.CREAD, blkNumber, b );
    }

    public static int cwrite( int blkNumber, byte[] b ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
                Kernel.CWRITE, blkNumber, b );
    }

    public static int flush( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
                Kernel.CFLUSH, 0, null );
    }

    public static int csync( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
                Kernel.CSYNC, 0, null );
    }

    public static String[] stringToArgs( String s ) {
        StringTokenizer token = new StringTokenizer( s," " );
        String[] progArgs = new String[ token.countTokens( ) ];
        for ( int i = 0; token.hasMoreTokens( ); i++ ) {
            progArgs[i] = token.nextToken( );
        }
        return progArgs;
    }

    public static void short2bytes( short s, byte[] b, int offset ) {
        b[offset] = (byte)( s >> 8 );
        b[offset + 1] = (byte)s;
    }

    public static short bytes2short( byte[] b, int offset ) {
        short s = 0;
        s += b[offset] & 0xff;
        s <<= 8;
        s += b[offset + 1] & 0xff;
        return s;
    }

    public static void int2bytes( int i, byte[] b, int offset ) {
        b[offset] = (byte)( i >> 24 );
        b[offset + 1] = (byte)( i >> 16 );
        b[offset + 2] = (byte)( i >> 8 );
        b[offset + 3] = (byte)i;
    }

    public static int bytes2int( byte[] b, int offset ) {
        int n = ((b[offset] & 0xff) << 24) + ((b[offset+1] & 0xff) << 16) +
                ((b[offset+2] & 0xff) << 8) + (b[offset+3] & 0xff);
        return n;
    }

    public static int open(String fname, String mode) {
        String[] args = new String[]{fname, mode};
        return Kernel.interrupt(1, 14, 0, args);
    }

    public static int close( int fd) {
        return Kernel.interrupt(1, 15, fd, null);
    }

    public static int read(int fd, byte[] buffer) {
        return Kernel.interrupt(1, 8, fd, buffer);
    }

    public static int write(int fd, byte[] buffer) {
        return Kernel.interrupt(1, 9, fd, buffer);
    }

    public static int seek(int fd, int offset, int whence) {
        int[]  args = new int[]{offset, whence};
        return Kernel.interrupt(1, 17, fd, args);
    }

    public static int fsize(int fd) {
        return Kernel.interrupt(1, 16, fd, null);
    }

    public static int delete(String fname) {
        return Kernel.interrupt(1, 19, 0, fname);
    }

    public static int format( int files ) {
        return Kernel.interrupt(1, 18, files, null);
    }

    // Given a disk block #, an offset within the block, and a array of field sizes,
    // reads the data back from the block and converts it to an ArrayList of properly-formatted objects
    public static List<Object> disk2List(short block, int offset, int[] sizes) {
        // Initial values will be null, so no need to intialize
        List<Object> values = new ArrayList<Object>(sizes.length);
        // Keep cursor within block
        int cursor;

        // If the block or offset are invalid, return a list of just null values.
        if ((block < 0 || block >= 1000) || (offset < 0 || offset >= 512)) {
            return values;
        } else { // If valid, extract
            cursor = offset;

            byte[] buffer = new byte[512];
            rawread(block, buffer);

            // Attempt to fill in the object list "values" with sets of data from the selected block
            try {
                for(int i = 0; i < sizes.length; ++i){
                    // Depending on what size each field is, load a different number of bytes into
                    // the corresponding values field.
                    switch(sizes[i]){
                        case 4: values.add(i, bytes2int(buffer, cursor));
                            cursor += 4;
                            break;
                        case 2: values.add(i, bytes2short(buffer, cursor));
                            cursor += 2;
                            break;
                        case 1: values.add(i, buffer[cursor]);
                            cursor++;
                            break;
                        default:
                            // No defined behavior for any other field size.
                            break;
                    }
                    // If the selected sizes have somehow put the cursor outside of the block,
                    // discontinue reading fields and return what's been read.
                    if(cursor >= 512){
                        break;
                    }
                }
            }
            catch (IndexOutOfBoundsException e){
                cerr(e.toString());
            }
            return values;
        }
    }

    // Given a list of Objects, a list of field sizes, and a block and offset within the block, attempts
    // to write the data back to disk.
    public static int list2Disk(List<Object> fields, int[] sizes, short block, int offset){
        int retval;
        byte[] buffer = new byte[512];
        int cursor;

        // If block or offset data is out of range, or fields' size doesn't equal sizes.length, return an error
        if ((block < 0 || block >= 1000) || (offset < 0 || offset >= 512) || (fields.size() != sizes.length)) {
            retval = Kernel.ERROR;
        } else { // If valid, read in the specified block, then update the correct number of bytes and write back
            cursor = offset;
            retval = rawread(block, buffer);

            if (retval != Kernel.ERROR) {
                try {
                    for (int i = 0; i < sizes.length; ++i) {
                        switch (sizes[i]) {
                            case 4:
                                int2bytes((int) fields.get(i), buffer, cursor);
                                cursor += 4;
                                break;
                            case 2:
                                short2bytes((short) fields.get(i), buffer, cursor);
                                cursor += 2;
                                break;
                            case 1:
                                buffer[cursor] = (byte) fields.get(i);
                                cursor++;
                                break;
                            default:    // Behavior undefined for any other sizes
                                break;
                        }
                        // If the selected sizes have somehow put the cursor outside of the block,
                        // discontinue reading fields and return what's been read.
                        if (cursor >= 512) {
                            break;
                        }

                    }
                }
                catch (IndexOutOfBoundsException e){
                    cerr(e.toString());
                }
                retval = Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
                        Kernel.RAWWRITE, block, buffer );
            }
        }
        return retval;
    }
}