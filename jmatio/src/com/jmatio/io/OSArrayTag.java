package com.jmatio.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Tiny class that represents MAT-file TAG 
 * It simplifies writing data. Automates writing padding for instance.
 */
class OSArrayTag extends MatTag
{
    private final ByteBuffer data;

    /**
     * Creates TAG and sets its <code>size</code> as size of byte array
     * 
     * @param type Matlab data type.
     * @param data The byte array.
     */
    public OSArrayTag(int type, byte[] data )
    {
        this ( type, ByteBuffer.wrap( data ) );
    }
    /**
     * Creates TAG from supplied byte buffer.
     * 
     * @param type Matlab data type.
     * @param data The byte buffer.
     */
    public OSArrayTag(int type, ByteBuffer data )
    {
        super( type, data.limit() );
        this.data = data;
        data.rewind();
    }

    
    /**
     * Writes tag and data to <code>DataOutputStream</code>. Wites padding if neccesary.
     * 
     * @param os The output stream
     * @throws IOException if an IO error occurs.
     */
    public void writeTo(DataOutputStream os) throws IOException
    {
    
    	int padding;
		if (size<=4 && size>0) {
			// Use small data element format (Page 1-10 in "MATLAB 7 MAT-File Format", September 2010 revision)
    		os.writeShort(size);
    		os.writeShort(type);
            padding = getPadding(data.limit(), true);
    	} else {
    		os.writeInt(type);
    		os.writeInt(size);
            padding = getPadding(data.limit(), false);
    	}
        
        int maxBuffSize = 1024;
        int writeBuffSize = Math.min(data.remaining(), maxBuffSize);
        byte[] tmp = new byte[writeBuffSize]; 
        while ( data.remaining() > 0 )
        {
            int length = Math.min(data.remaining(), tmp.length);
            data.get( tmp, 0, length);
            os.write(tmp, 0, length);
        }
        
        if ( padding > 0 )
        {
            os.write( new byte[padding] );
        }
    }
}