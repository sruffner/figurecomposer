package com.srscicomp.fc.uibase;

import com.srscicomp.common.util.Utilities;

/**
 * This interface collects various common constants in one place that would otherwise have to be defined in every
 * <i>Figure Composer</i> GUI component that used them.
 * @author sruffner
 */
public interface FCUIConstants
{
   /** Mnemonic for the Command (Mac) or Control key (otherwise); for composing hot key mnemonics. */
   String MODCMD = Utilities.isMacOS() ? "\u2318" : "Ctrl-";
   /** Mnemonic for the Shift key; for composing hot key mnemonics. */
   String MODSHIFT = Utilities.isMacOS() ? "\u21E7" : "Shift-";
   /** Mnemonic for the Ctrl+Shift keys; for composing hot key mnemonics. */
   String MODCTRLSHIFT = Utilities.isMacOS() ? "\u2303\u21E7" : "Ctrl-Shift-";
   /** Mnemonic for the Alt key (Option key on Mac OSX); for composing hot key mnemonics. */
   String MODALT = Utilities.isMacOS() ? "\u2325" : "Alt-";

}
