package com.jmatio.io;

import java.io.IOException;
import java.nio.ByteBuffer;

interface DataOutputStream 
{
    /**
     * Returns the current size of this stream.
     * 
     * @return the current size of this stream.
     * @throws IOException if an IO error occurs.
     */
    int size() throws IOException;

    /**
     * Returns the current {@link ByteBuffer} mapped on the target file.
     * <p>
     * Note: the {@link ByteBuffer} has <strong>READ ONLY</strong> access.
     * 
     * @return the {@link ByteBuffer}
     * @throws IOException if an IO error occurs.
     */
    ByteBuffer getByteBuffer() throws IOException;

    /**
     * Writes a sequence of bytes to this stream from the given buffer.
     * 
     * @param byteBuffer
     *            the source {@link ByteBuffer}
     * @throws IOException if an IO error occurs.
     */
    void write(ByteBuffer byteBuffer) throws IOException;

}