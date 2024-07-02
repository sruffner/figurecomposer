package com.jmatio.io;

import java.nio.ByteBuffer;

import com.jmatio.common.MatDataTypes;
import com.jmatio.types.ByteStorageSupport;

/**
 * MAT-file input stream class. 
 * 
 * @author Wojciech Gradkowski <wgradkowski@gmail.com>
 */
class MatFileInputStream
{
    private final int type;
    private final ByteBuffer buf;
    
    /**
     * Attach MAT-file input stream to a byte buffer
     * 
     * @param buf The byte buffer for input stream.
     * @param type Type of data in the stream.
     * @see MatDataTypes
     */
    public MatFileInputStream( ByteBuffer buf, int type )
    {
        this.type = type;
        this.buf = buf;
    }
    
    /**
     * Reads data (number of bytes red is determined by <i>data type</i>)
     * from the stream to <code>int</code>.
     * 
     * @return Integer datum.
     */
    public int readInt()
    {
        switch ( type )
        {
            case MatDataTypes.miUINT8:
                return buf.get() & 0xFF;
            case MatDataTypes.miINT8:
                return buf.get();
            case MatDataTypes.miUINT16:
                return buf.getShort() & 0xFFFF;
            case MatDataTypes.miINT16:
                return buf.getShort();
            case MatDataTypes.miUINT32:
            case MatDataTypes.miINT32:
                return buf.getInt();
            case MatDataTypes.miUINT64:
            case MatDataTypes.miINT64:
                return (int) buf.getLong();
            case MatDataTypes.miDOUBLE:
                return (int) buf.getDouble();
            default:
                throw new IllegalArgumentException("Unknown data type: " + type);
        }
    }
    /**
     * Reads data (number of bytes red is determined by <i>data type</i>)
     * from the stream to <code>char</code>.
     * 
     * @return - char
     */
    public char readChar()
    {
        switch ( type )
        {
            case MatDataTypes.miUINT8:
                return (char)( buf.get() & 0xFF);
            case MatDataTypes.miINT8:
            case MatDataTypes.miUTF8:
                return (char) buf.get();
            case MatDataTypes.miUINT16:
                return (char)( buf.getShort() & 0xFFFF);
            case MatDataTypes.miINT16:
                return (char) buf.getShort();
            case MatDataTypes.miUINT32:
            case MatDataTypes.miINT32:
                return (char)(buf.getInt());
            case MatDataTypes.miDOUBLE:
                return (char) buf.getDouble();
            default:
                throw new IllegalArgumentException("Unknown data type: " + type);
        }
    }
    /**
     * Reads data (number of bytes red is determined by <i>data type</i>)
     * from the stream to <code>double</code>.
     * 
     * @return - double
     */
    public double readDouble()
    {
        switch ( type )
        {
            case MatDataTypes.miUINT8:
                return buf.get() & 0xFF;
            case MatDataTypes.miINT8:
                return buf.get();
            case MatDataTypes.miUINT16:
                return buf.getShort() & 0xFFFF;
            case MatDataTypes.miINT16:
                return buf.getShort();
            case MatDataTypes.miUINT32:
            case MatDataTypes.miINT32:
                return buf.getInt();
            case MatDataTypes.miDOUBLE:
                return buf.getDouble();
            default:
                throw new IllegalArgumentException("Unknown data type: " + type);
        }
    }

    public byte readByte()
    {
        switch ( type )
        {
            case MatDataTypes.miUINT8:
                return (byte)( buf.get() & 0xFF);
            case MatDataTypes.miINT8:
            case MatDataTypes.miUTF8:
            case MatDataTypes.miUTF16:
            case MatDataTypes.miUTF32:
                return buf.get();
            case MatDataTypes.miUINT16:
                return (byte)( buf.getShort() & 0xFFFF);
            case MatDataTypes.miINT16:
                return (byte) buf.getShort();
            case MatDataTypes.miUINT32:
            case MatDataTypes.miINT32:
                return (byte)(buf.getInt());
            case MatDataTypes.miDOUBLE:
                return (byte) buf.getDouble();
            default:
                throw new IllegalArgumentException("Unknown data type: " + type);
        }
    }

    /**
     * Reads the data into a <code>{@link ByteBuffer}</code>. This method is
     * only supported for arrays with backing ByteBuffer (<code>{@link ByteStorageSupport}</code>).
     * 
     * @param dest
     *            the destination <code>{@link ByteBuffer}</code>
     * @param elements
     *            the number of elements to read into a buffer
     * @param storage
     *            the backing <code>{@link ByteStorageSupport}</code> that
     *            gives information how data should be interpreted
     * @return reference to the destination <code>{@link ByteBuffer}</code>
     */
    @SuppressWarnings("UnusedReturnValue")
    public ByteBuffer readToByteBuffer(ByteBuffer dest, int elements,
                                       ByteStorageSupport<?> storage)
    {
        
        int bytesAllocated = storage.getBytesAllocated();
        int size = elements * storage.getBytesAllocated();
        
        //direct buffer copy
        if ( MatDataTypes.sizeOf(type) == bytesAllocated && buf.order().equals(dest.order()) )
        {
            int bufMaxSize = 1024;
            int bufSize = Math.min(buf.remaining(), bufMaxSize);
            int bufPos = buf.position();
            
            byte[] tmp = new byte[ bufSize ];
            
            while ( dest.remaining() > 0 )
            {
                int length = Math.min(dest.remaining(), tmp.length);
                buf.get( tmp, 0, length );
                dest.put( tmp, 0, length );
            }
            buf.position( bufPos + size );
        }
        else
        {
            //because Matlab writes data not respectively to the declared
            //matrix type, the reading is not straight forward (as above)
            Class<?> clazz = storage.getStorageClazz();
            while ( dest.remaining() > 0 )
            {
                if ( clazz.equals( Double.class) )
                {
                    dest.putDouble( readDouble() );
                }
                else if ( clazz.equals( Byte.class) )
                {
                    dest.put( readByte() );
                }
                else if ( clazz.equals( Integer.class) )
                {
                    dest.putInt( readInt() );
                }
                else if ( clazz.equals( Long.class) )
                {
                    dest.putLong( readLong() );
                }
                else  if ( clazz.equals( Float.class) )
                {
                    dest.putFloat( readFloat() );
                }
                else if ( clazz.equals( Short.class) )
                {
                    dest.putShort( readShort() );
                }
                else
                {
                    throw new RuntimeException("Not supported buffer reader for " + clazz );
                }
            }
        }
        dest.rewind();
        return dest;
    }

    private float readFloat()
    {
        switch ( type )
        {
            case MatDataTypes.miUINT8:
                return (float)( buf.get() & 0xFF);
            case MatDataTypes.miINT8:
                return buf.get();
            case MatDataTypes.miUINT16:
                return (float)( buf.getShort() & 0xFFFF);
            case MatDataTypes.miINT16:
                return buf.getShort();
            case MatDataTypes.miUINT32:
            case MatDataTypes.miINT32:
                return (float)(buf.getInt());
            case MatDataTypes.miSINGLE:
                return buf.getFloat();
            case MatDataTypes.miDOUBLE:
                return (float) buf.getDouble();
            default:
                throw new IllegalArgumentException("Unknown data type: " + type);
        }
    }
    private short readShort()
    {
        switch ( type )
        {
            case MatDataTypes.miUINT8:
                return (short)( buf.get() & 0xFF);
            case MatDataTypes.miINT8:
                return buf.get();
            case MatDataTypes.miUINT16:
                return (short)( buf.getShort() & 0xFFFF);
            case MatDataTypes.miINT16:
                return buf.getShort();
            case MatDataTypes.miUINT32:
            case MatDataTypes.miINT32:
                return (short)(buf.getInt());
            case MatDataTypes.miUINT64:
            case MatDataTypes.miINT64:
                return (short) buf.getLong();
            case MatDataTypes.miDOUBLE:
                return (short) buf.getDouble();
            default:
                throw new IllegalArgumentException("Unknown data type: " + type);
        }
    }
    private long readLong()
    {
        switch ( type )
        {
            case MatDataTypes.miUINT8:
                return buf.get() & 0xFF;
            case MatDataTypes.miINT8:
                return buf.get();
            case MatDataTypes.miUINT16:
                return buf.getShort() & 0xFFFF;
            case MatDataTypes.miINT16:
                return buf.getShort();
            case MatDataTypes.miUINT32:
            case MatDataTypes.miINT32:
                return buf.getInt();
            case MatDataTypes.miUINT64:
            case MatDataTypes.miINT64:
                return buf.getLong();
            case MatDataTypes.miDOUBLE:
                return (long) buf.getDouble();
            default:
                throw new IllegalArgumentException("Unknown data type: " + type);
        }
    }

	public void skip(int padding) 
	{
		buf.position( buf.position() + padding );
	}
    

}
