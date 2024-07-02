package com.jmatio.types;

import java.util.ArrayList;

public class MLCell extends MLArray
{
    private final ArrayList<MLArray> cells;
    
    public MLCell(String name, int[] dims )
    {
        this( name, dims, MLArray.mxCELL_CLASS, 0);
    }
    
    public MLCell(String name, int[] dims, int type, int attributes)
    {
        super(name, dims, type, attributes);
        
        cells = new ArrayList<>(getM() * getN());
        
        for ( int i = 0; i < getM()*getN(); i++ )
        {
            cells.add( new MLEmptyArray() );
        }
    }    
    public void set(MLArray value, int m, int n)
    {
        cells.set( getIndex(m,n), value );
    }
    public void set(MLArray value, int index)
    {
        cells.set( index, value );
    }
    public MLArray get(int m, int n)
    {
        return cells.get( getIndex(m,n) );
    }
    public MLArray get(int index)
    {
        return cells.get( index );
    }
    public int getIndex(int m, int n)
    {
        return m+n*getM();
    }
    public ArrayList<MLArray> cells()
    {
        return cells;
    }
    public String contentToString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" = \n");
        
        for ( int m = 0; m < getM(); m++ )
        {
           sb.append("\t");
           for ( int n = 0; n < getN(); n++ )
           {
               sb.append( get(m,n) );
               sb.append("\t");
           }
           sb.append("\n");
        }
        return sb.toString();
    }

}
