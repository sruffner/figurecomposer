package com.srscicomp.common.util;

/**
 * <code>NeverOccursException</code> is an unchecked exception that should be used to wrap any checked exception that
 * "should never occur". A classic example of such an exception is the <code>UnsupportedEncodingException</code> thrown
 * by <code>java.lang.String.getByte(encoding)</code>, when the character encoding used is one that is guaranteed to
 * be supported on all conforming JVMs.
 * 
 * @author sruffner
 */
public class NeverOccursException extends RuntimeException
{
   public NeverOccursException(String msg) { super(msg); }
   public NeverOccursException(Throwable cause) { super(cause); }
}
