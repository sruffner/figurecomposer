package com.jmatio.types;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class represents Matlab's Structure object (structure array).
 * <p>
 * Note: array of structures can contain only structures of the same type
 * , that means structures that have the same field names.
 * 
 * @author Wojciech Gradkowski <wgradkowski@gmail.com>
 */
public class MLStructure extends MLArray
{
    /**
     * A Set that keeps structure field names
     */
    private final Set<String> keys;
    /**
     * Array of structures
     */
    private final List< Map<String,MLArray> > mlStructArray;
    /**
     * Current structure pointer for bulk insert 
     */
    private int currentIndex = 0;
    
    public MLStructure(String name, int[] dims)
    {
        this(name, dims, MLArray.mxSTRUCT_CLASS, 0 );
    }
    
    public MLStructure(String name, int[] dims, int type, int attributes)
    {
        super(name, dims, type, attributes);
        
        mlStructArray = new ArrayList<>(dims[0] * dims[1]);
        keys = new LinkedHashSet<>();
    }
    /**
     * Sets field for current structure
     * 
     * @param name - name of the field
     * @param value - <code>MLArray</code> field value
     */
    public void setField(String name, MLArray value)
    {
        //fields.put(name, value);
        setField(name, value, currentIndex);
    }
    /**
     * Sets field for (m,n)'th structure in struct array
     * 
     * @param name - name of the field
     * @param value - <code>MLArray</code> field value
     * @param m Row index.
     * @param n Column index.
     */
    public void setField(String name, MLArray value, int m, int n)
    {
        setField(name, value, getIndex(m,n) );
    }
    /**
     * Sets filed for structure described by index in struct array
     * 
     * @param name - name of the field
     * @param value - <code>MLArray</code> field value
     * @param index Index into column-packed backing buffer.
     */
    public void setField(String name, MLArray value, int index)
    {
        keys.add(name);
        currentIndex = index;
        
        if ( mlStructArray.isEmpty() || mlStructArray.size() <= index )
        {
            mlStructArray.add(index, new LinkedHashMap<>() );
        }
        mlStructArray.get(index).put(name, value);
    }
    
    /**
     * Gets the maximum length of field descriptor
     * 
     * @return The max length.
     */
    public int getMaxFieldLenth()
    {
        //get max field name
        int maxLen = 0;
        for ( String s : keys )
        {
            maxLen = Math.max(s.length(), maxLen);
        }
        return maxLen+1;
        
    }
    
    /**
     * Dumps field names to byte array. Field names are written as Zero End Strings
     * 
     * @return The structure field names packed into a byte array.
     */
    public byte[] getKeySetToByteArray() 
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        char[] buffer = new char[getMaxFieldLenth()];
        
        try
        {
            for ( String s : keys )
            {
                Arrays.fill(buffer, (char)0);
                System.arraycopy( s.toCharArray(), 0, buffer, 0, s.length() );
                dos.writeBytes( new String(buffer) );
            }
        }
        catch  (IOException e)
        {
            System.err.println("Could not write Structure key set to byte array: " + e );
            return new byte[0];
        }
        return baos.toByteArray();
        
    }
    /**
     * Gets all field from sruct array as flat list of fields.
     * 
     * @return Array of fields in structure.
     */
    public Collection<MLArray> getAllFields()
    {
        ArrayList<MLArray> fields = new ArrayList<>();
        
        for ( Map<String, MLArray> struct : mlStructArray )
        {
            fields.addAll( struct.values() );
        }
        return fields;
    }
    /**
     * Returns the {@link Collection} of keys for this structure.
     * @return the {@link Collection} of keys for this structure
     */
    public Collection<String> getFieldNames()
    {

       return new LinkedHashSet<>(keys);
        
    }
    /**
     * Gets a value of the field described by name from current struct
     * in struct array or null if the field doesn't exist.
     * 
     * @param name Field name.
     * @return Field value.
     */
    public MLArray getField(String name)
    {
        return getField(name, currentIndex);
    }
    /**
     * Gets a value of the field described by name from (m,n)'th struct
     * in struct array or null if the field doesn't exist.
     * 
     * @param name Field name.
     * @param m Row index.
     * @param n Column index.
     * @return Value of specified field at location (m,n) in structure matrix.
     */
    public MLArray getField(String name, int m, int n)
    {
        return getField(name, getIndex(m,n) );
    }
    /**
     * Gets a value of the field described by name from index'th struct
     * in struct array or null if the field doesn't exist.
     * 
     * @param name Field name.
     * @param index Index into structure array.
     * @return value of the field or null if the field doesn't exist
     */
    public MLArray getField(String name, int index)
    {
    	if (mlStructArray.isEmpty()) {
    		return null;
    	}
        return mlStructArray.get(index).get(name);
    }
    /* (non-Javadoc)
     * @see com.paradigmdesigner.matlab.types.MLArray#contentToString()
     */
    public String contentToString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" = \n");
        
        if ( getM()*getN() == 1 )
        {
            for ( String key : keys )
            {
                sb.append("\t").append(key).append(" : ").append(getField(key)).append("\n");
            }
        }
        else
        {
            sb.append("\n");
            sb.append(getM()).append("x").append(getN());
            sb.append(" struct array with fields: \n");
            for ( String key : keys)
            {
                sb.append("\t").append(key).append("\n");
            }
        }
        return sb.toString();
    }

}
