package com.jmatio.types;

import java.nio.ByteBuffer;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class MLSparse extends MLNumericArray<Double>
{
    final int nzmax;
    private SortedSet<IndexMN> indexSet;
    private SortedMap<IndexMN, Double> real;  
    private SortedMap<IndexMN, Double> imaginary;  
    
    /**
     * @param name Array name.
     * @param dims Array dimesions.
     * @param attributes Array attributes.
     * @param nzmax Max number of nonzero values.
     */
    public MLSparse(String name, int[] dims, int attributes, int nzmax )
    {
        super(name, dims, MLArray.mxSPARSE_CLASS, attributes);
        this.nzmax = nzmax;
    }
    
    protected void allocate()
    {
        real = new TreeMap<>();
        if ( isComplex() )
        {
            imaginary = new TreeMap<>();
        }
        indexSet = new TreeSet<>();
    }
    
    /**
     * Gets maximum number of non-zero values
     * 
     * @return Max number of non-zeros.
     */
    public int getMaxNZ()
    {
        return nzmax;
    }
    /**
     * Gets row indices
     * <p>
     * <tt>ir</tt> points to an integer array of length nzmax containing the row indices of
     * the corresponding elements in <tt>pr</tt> and <tt>pi</tt>.
     */
    public int[] getIR()
    {
        int[] ir = new int[nzmax];
        int i = 0;
        for ( IndexMN index : indexSet )
        {
            ir[i++] = index.m;
        }
        return ir;
    }
    /**
     * Gets column indices. 
     * <p>
     * <tt>jc</tt> points to an integer array of length N+1 that contains column index information.
     * For j, in the range <tt>0&lt;=j&lt;=N�1</tt>, <tt>jc[j]</tt> is the index in ir and <tt>pr</tt> (and <tt>pi</tt>
     * if it exists) of the first nonzero entry in the jth column and <tt>jc[j+1]�1</tt> index
     * of the last nonzero entry. As a result, <tt>jc[N]</tt> is also equal to nnz, the number
     * of nonzero entries in the matrix. If nnz is less than nzmax, then more nonzero
     * entries can be inserted in the array without allocating additional storage
     * 
     * @return Column indices (see description).
     */
    public int[] getJC()
    {
        int[] jc = new int[getN()+1];
        // jc[j] is the number of nonzero elements in all preceeding columns
        for ( IndexMN index : indexSet )
        {
            for (int column = index.n + 1; column < jc.length; column++)
            {
                jc[column]++;
            }
        }
        return jc;
    }
    
    /* (non-Javadoc)
     * @see com.paradigmdesigner.matlab.types.GenericArrayCreator#createArray(int, int)
     */
    public Double[] createArray(int m, int n)
    {
        return null;
    }
    /* (non-Javadoc)
     * @see com.paradigmdesigner.matlab.types.MLNumericArray#getReal(int, int)
     */
    public Double getReal(int m, int n)
    {
        IndexMN i = new IndexMN(m,n);
        if ( real.containsKey(i) )
        {
            return real.get(i);
        }
        return (double) 0;
    }
    
    /* (non-Javadoc)
     * @see com.jmatio.types.MLNumericArray#getReal(int)
     */
    public Double getReal ( int index )
    {
        throw new IllegalArgumentException("Can't get Sparse array elements by index. " +
        "Please use getReal(int index) instead.");
    }
    /**
     * @param value The real value to set.
     * @param m row index
     * @param n column index
     */
    public void setReal(Double value, int m, int n)
    {
        IndexMN i = new IndexMN(m,n);
        indexSet.add(i);
        real.put(i, value );
    }
    /**
     * @param value The real value to set.
     * @param index The index into column-packed backing buffer.
     */
    public void setReal(Double value, int index)
    {
        throw new IllegalArgumentException("Can't set Sparse array elements by index. " +
                "Please use setReal(Double value, int m, int n) instead.");
    }
    /**
     * @param value The imaginary value to set.
     * @param m The row index.
     * @param n The column index.
     */
    public void setImaginary(Double value, int m, int n)
    {
        IndexMN i = new IndexMN(m,n);
        indexSet.add(i);
        imaginary.put(i, value );
    }
    /**
     * @param value The imaginary value to set.
     * @param index The index into column-packed backing buffer.
     */
    public void setImaginary(Double value, int index)
    {
        throw new IllegalArgumentException("Can't set Sparse array elements by index. " +
        "Please use setImaginary(Double value, int m, int n) instead.");
    }
    /* (non-Javadoc)
     * @see com.paradigmdesigner.matlab.types.MLNumericArray#getImaginary(int, int)
     */
    public Double getImaginary(int m, int n)
    {
        IndexMN i = new IndexMN(m,n);
        if ( imaginary.containsKey(i) )
        {
            return imaginary.get(i);
        }
        return (double) 0;
    }
    /* (non-Javadoc)
     * @see com.jmatio.types.MLNumericArray#getImaginary(int)
     */
    public Double getImaginary( int index )
    {
        throw new IllegalArgumentException("Can't get Sparse array elements by index. " +
        "Please use getImaginary(int index) instead.");
    }
    
    /**
     * Returns the real part (PR) array. PR has length number-of-nonzero-values.
     *
     * @return real part
     */
    public Double[] exportReal()
    {
        Double[] ad = new Double[indexSet.size()];
        int i = 0;
        for (IndexMN index: indexSet) {
           ad[i] = real.getOrDefault(index, 0.0);
            i++;
        }
        return ad;
    }
    
    /**
     * Returns the imaginary part (PI) array. PI has length number-of-nonzero-values.
     *
     * @return The imaginary part array.
     */
    public Double[] exportImaginary()
    {
        Double[] ad = new Double[indexSet.size()];
        int i = 0;
        for (IndexMN index: indexSet) {
           ad[i] = imaginary.getOrDefault(index, 0.0);
            i++;
        }
        return ad;
    }
    /* (non-Javadoc)
     * @see com.paradigmdesigner.matlab.types.MLArray#contentToString()
     */
    public String contentToString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" = \n");
        
        for ( IndexMN i : indexSet )
        {
            sb.append("\t(");
            sb.append(i.m).append(",").append(i.n);
            sb.append(")");
            sb.append("\t").append(getReal(i.m, i.n));
            if ( isComplex() )
            {
                sb.append("+").append(getImaginary(i.m, i.n));
            }
            sb.append("\n");
            
        }
        
        return sb.toString();
    }
    
    /**
     * Matrix index (m,n)
     * 
     * @author Wojciech Gradkowski <wgradkowski@gmail.com>
     */
    private class IndexMN implements Comparable<IndexMN>
    {
        final int m;
        final int n;
        
        public IndexMN( int m, int n )
        {
            this.m = m;
            this.n = n;
        }
        
        
        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(IndexMN anOtherIndex) {
            return getIndex(m,n) - getIndex(anOtherIndex.m,anOtherIndex.n);
        }

        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object o)
        {
            if (o instanceof IndexMN )
            {
                return m == ((IndexMN)o).m && n == ((IndexMN)o).n;
            }
            return super.equals(o);
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString()
        {
            return "{" +
                 "m=" + m +
                 ", " +
                 "n=" + n +
                 "}";
        }
    }

    public int getBytesAllocated()
    {
        return Double.SIZE << 3;
    }
    public Double buldFromBytes(byte[] bytes)
    {
        if ( bytes.length != getBytesAllocated() )
        {
            throw new IllegalArgumentException( 
                        "To build from byte array I need array of size: " 
                                + getBytesAllocated() );
        }
        return ByteBuffer.wrap( bytes ).getDouble();
        
    }
    public byte[] getByteArray(Double value)
    {
        int byteAllocated = getBytesAllocated();
        ByteBuffer buff = ByteBuffer.allocate( byteAllocated );
        buff.putDouble( value );
        return buff.array();
    }
    
    public Class<Double> getStorageClazz()
    {
        return Double.class;
    }


}
