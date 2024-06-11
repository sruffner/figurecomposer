package com.srscicomp.common.util;

/**
 * <code>ASCII85Encoder</code> is a utility class that transforms binary data (a sequence of bytes) into an
 * ASCII85Encoder-encoded character sequence. It was developed to in order to encode binary image data in a Postscript file.
 *
 * <p>ASCII85Encoder is a printable binary-to-text encoding that is more compact than base64 -- it is only 25% larger than the
 * original binary sequence vs 33% for base64. (NOTE that it cannot be easily used in XML because it requires some
 * characters that are reserved in XML and would thus have to be escaped.) When encoding, each group of 4 bytes is taken
 * as a 32-bit binary number, most significant byte first (ASCII85Encoder uses a big-endian convention). This is converted, by
 * repeatedly dividing by 85 and taking the remainder, into 5 radix-85 digits. These digits (again, most significant
 * first) are then encoded as printable characters by adding 33 to them, giving the ASCII characters 33 ("!") to 117
 * ("u"). One exception to this rule: if the 4 bytes are all zeros and are NOT at the end of the byte sequence, the zero
 * group is encoded as a single 'z' rather than "!!!!!", which helps to compress data streams containing lots of zeros.
 * If the byte stream length is not a multiple of 4, the extra N=[1..3] bytes at the end are padded with zeros, converted
 * to a 5-digit radix-85 number in the usual way, and then only the most significant N+1 ASCII85Encoder characters are included
 * in the output. Care must be taken when decoding such a truncated block, since truncation can have a rounding down
 * effect. To correctly decode such a block, simply pad it with 'u' out to 5 characters and decode as usual.</p>
 *
 * <p>CREDITS: The ASCII85Encoder coding is well-described at wikipedia.com. The implementation here was adapted from open
 * source C code found <a href="http://www.stillhq.com/cgi-bin/cvsweb/ascii85">here</a>.</p>
 * @author sruffner
 */
public class ASCII85Encoder 
{
   /**
    * Construct a <code>ASCII85Encoder</code> which writes the encoded output into the specified buffer. An initial 
    * carriage-return-linefeed pair is appended to the buffer before the first encoded character, after every 80
    * characters thereafter, and after the final character in the sequence (upon termination of the encoder).
    * @param buffer The buffer to receive the ASCII85-encoded character sequence.
    */
   public ASCII85Encoder(StringBuffer buffer)
   {
      this.buffer = buffer;
      this.buffer.append(CRLF);
   }

   /**
    * Construct a <code>ASCII85Encoder</code> which writes the encoded output into the specified buffer.
    * @param buffer The buffer to receive the ASCII85-encoded character sequence.
    * @param maxLineLen If this is non-positive, no carriage-return linefeeds (CRLF) are inserted into the encoded 
    * character sequence whatsoever. Otherwise, a CRLF is inserted after every <code>maxLineLen</code> characters.
    */
   public ASCII85Encoder(StringBuffer buffer, int maxLineLen)
   {
      this.buffer = buffer;
      this.maxLineLen = maxLineLen;
   }
   
   /** The buffer to which the encoded character sequence is appended. */
   private final StringBuffer buffer;
   
   /** Accumulate the next 4 bytes in the binary sequence. The MSbyte is the first byte and the LSByte is the 4th. */
   private long tuple = 0;
   
   /** Number of bytes accumulated thus far in the next 4-tuple. */
   private int count = 0;
   
   /** Flag set once encoder has been terminated -- no further encoding is allowed. */
   private boolean terminated = false;
   
   /** Current character position within the current line. */
   private int linePos = 0;
   
   /** 
    * The maximum number of encoded characters per "line" in the output. If non-positive, characters are appended to
    * the output buffer without formatting.
    */
   private int maxLineLen = 80;

   /** A carriage-return linefeed pair, for inserting linebreaks in encoded output. */
   private final static String CRLF = "\r\n";
   
   /**
    * Encode one byte of binary data.
    * @param b The next byte in the binary data sequence being encoded.
    * @throws IllegalStateException if this encoder has already been terminated.
    */
   public void put(byte b)
   {
      if(terminated) throw new IllegalStateException("Encoder has been terminated. No further encoding possible.");
      int iByte = ((int)b) & 0x000000ff;
      ++count;
      switch(count) 
      {
         case 1: tuple |= 0x00000000ffffffffL & ((long) iByte << 24); break;
         case 2: tuple |= (iByte << 16); break;
         case 3: tuple |= (iByte <<  8); break;
         case 4: tuple |= iByte; writeTuple(); break;
      }
   }
   
   /** 
    * Terminate the encoding operation, flushing any remaining encoded characters into the output buffer. If line
    * formatting is enabled, a final carriage-return linefeed pair is appended. Once this method is called, no further 
    * encoding is possible.
    * <p>Note that <code>ASCII85Encoder</code> does NOT append the ASCII85 end-of-data marker "~>"; that is left to the
    * caller.</p>
    */
   public void terminate()
   {
      if(terminated) return;
      writeTuple();
      terminated = true;
   }
   
   /** Used to construct the 5-digit ASCII85 encoding of the current 4-tuple. */
   private final char[] cbuf = new char[5];
   
   /** 
    * Helper method encodes the current 4-tuple in ASCII85 and appends the encoded string to the output buffer. It
    * handles the special case in which the 4-tuple is identically zero. If invoked when there are N=[1..3] bytes 
    * accumulated thus far, it will append an truncated ASCII85 block of N+1 characters; this happens only when the
    * encoder terminates. If line formatting is enabled, carriage-return linefeeds are inserted as needed.
    */
   private void writeTuple()
   {
      if(count == 0) return;
      if(count == 4 && tuple == 0)
      {
         buffer.append("z");
         incrementLinePos();
         count = 0;
         return;
      }
      
      int i;
      i = cbuf.length - 1;
      do {
         cbuf[i] = (char) ((tuple % 85) + '!');
         tuple /= 85;
      } while (--i >= 0);
      
      for(i=0; i<count+1; i++)
      {
         buffer.append(cbuf[i]);
         incrementLinePos();
      }
      
      count = 0;
      tuple = 0;
   }
   
   /** 
    * If line formatting is enabled, increment current line position. If we've reached the end of a line, append a
    * CRLF to the output buffer and reset the line position.
    */
   private void incrementLinePos()
   {
      if(maxLineLen > 0)
      {
         ++linePos;
         if(linePos == maxLineLen)
         {
            buffer.append(CRLF);
            linePos = 0;
         }
      }
   }
   
   /** 
    * A simple test program.
    * @param args Command-line arguments (ignored).
    */
   public static void main(String[] args)
   {
      StringBuffer buffer = new StringBuffer();
      ASCII85Encoder encoder = new ASCII85Encoder(buffer);
      for(int i=0; i<24; i++) encoder.put((byte) (i*10));
      encoder.put((byte)255);
      encoder.terminate();
      System.out.println("Encode result: " + buffer);
      buffer.setLength(0);
      for(int i=0; i<85; i++)
      {
         char c = (char) (i + '!');
         buffer.append(c);
      }
      System.out.println("ASCII85 chars: " + buffer);
   }
}