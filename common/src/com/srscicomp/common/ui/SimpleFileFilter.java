package com.srscicomp.common.ui;

import java.io.File;

import javax.swing.filechooser.FileFilter;

import com.srscicomp.common.util.Utilities;


/**
 * A file filter for {@link javax.swing.JFileChooser JFileChooser} which accepts all directories and only filenames 
 * with the extensions specified in the filter's constructor.  All extension strings are forced to lowercase.
 * 
 * @author 	sruffner
 */
public class SimpleFileFilter extends FileFilter
{
	private String[] allowedExts = null;
	private String fileDescription = null;

	/** 
	 * Construct a simple file filter for JFileChooser that admits any file having one of the specified extensions, plus 
	 * any directory.
	 * 
	 * @param 	extensions a list of allowed extensions.  All extensions are forced to lowercase letters.
	 * @param 	fileDesc a human-readable description of the category of files having one of these extensions. this 
	 * 	description is appended with a parenthetic list of the allowed extensions.
	 */
	public SimpleFileFilter( String[] extensions, String fileDesc )
	{
		allowedExts = new String[extensions.length];
		for( int i=0; i<extensions.length; i++ ) allowedExts[i] = extensions[i].toLowerCase();
			
		fileDescription = fileDesc + " (";
		for( int i=0; i<allowedExts.length; i++ )
		{
			fileDescription += "*." + allowedExts[i];
			if( i < allowedExts.length - 1 ) fileDescription += ";";
		}
		fileDescription += ")";
	}

	/**
	 * Accepts all existing directories and any file with the extensions allowed by this SimpleFileFilter.
	 * 
	 * @see FileFilter#accept(File)
	 */
	public boolean accept( File f )
	{
		if( f.isDirectory() ) return( true );

		boolean ok = false;
		for( int i=0; i < allowedExts.length && !ok; i++ )
		{
			if( allowedExts[i].equals( Utilities.getExtension(f) ) ) ok = true;
		}

		return( ok );
	}

	/**
	 * @see FileFilter#getDescription()
	 */
	public String getDescription()
	{
		return( fileDescription );
	}

	/**
	 * Return a new abstract pathname that represents the absolute path of the specified abstract pathname appended with 
	 * one of the extensions allowed by this SimpleFileFilter.  If the specified pathname already has a valid extension, 
	 * then the pathname itself is returned.  Otherwise, the method returns a new abstract pathname with the corrected 
	 * extension.  The first entry in the extension list passed to the constructor will be used when any correction is 
	 * made.  Case is ignored when checking extension strings.
	 * 
	 * @param 	f The abstract pathname to be checked.
	 * @return	If it is already valid, the argument is returned.  Otherwise, a new abstract pathname with a valid 
	 * 	extension is returned.
	 */
	public File validateExtension( File f )
	{
		String ext = Utilities.getExtension(f);
		for( int i=0; i<allowedExts.length; i++ )
		{
			if( allowedExts[i].equalsIgnoreCase(ext) ) return( f );
		}

		String base = f.getAbsolutePath();
		if( base == null ) base = ""; 
		int i = base.lastIndexOf('.');
		if( i >= 0 ) base = base.substring(0,i);
		return( new File( base + "." + allowedExts[0] ) );
	}
}

