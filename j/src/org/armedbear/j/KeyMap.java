/*
 * KeyMap.java
 *
 * Copyright (C) 1998-2003 Peter Graves
 * $Id: KeyMap.java,v 1.3 2003-06-09 17:12:28 piso Exp $
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.armedbear.j;

import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.KeyStroke;

public final class KeyMap implements Constants
{
    private static KeyMap globalKeyMap;
    private static KeyMap globalOverrides;
    private static File globalKeyMapFile;

    private ArrayList mappings = new ArrayList();

    public KeyMap()
    {
    }

    public static synchronized KeyMap getGlobalKeyMap()
    {
        if (globalKeyMap == null) {
            String filename =
                Editor.preferences().getStringProperty(Property.GLOBAL_KEY_MAP);
            if (filename != null) {
                globalKeyMapFile = File.getInstance(filename);
                if (globalKeyMapFile != null) {
                    globalKeyMap = new KeyMap();
                    if (globalKeyMap.load(globalKeyMapFile))
                        return globalKeyMap;
                    globalKeyMapFile = null;
                }
            }
            globalKeyMap = new KeyMap();
            globalKeyMap.setGlobalDefaults();
        }
        return globalKeyMap;
    }

    public static synchronized final KeyMap getGlobalOverrides()
    {
        return globalOverrides;
    }

    public static synchronized final KeyMap getGlobalOverrides(boolean create)
    {
        if (create && globalOverrides == null)
            globalOverrides = new KeyMap();
        return globalOverrides;
    }

    public static synchronized final File getGlobalKeyMapFile()
    {
        return globalKeyMapFile;
    }

    public static synchronized final void resetGlobalKeyMap()
    {
        globalKeyMap = null;
    }

    public synchronized KeyMapping[] getMappings()
    {
        KeyMapping[] array = new KeyMapping[mappings.size()];
        return (KeyMapping[]) mappings.toArray(array);
    }

    public synchronized boolean load(File file)
    {
        boolean error = false;
        String message = null;
        if (!file.isRemote() && file.isFile()) {
            int lineNumber = 0;
            try {
                BufferedReader in =
                    new BufferedReader(new InputStreamReader(file.getInputStream()));
                while (true) {
                    String s = in.readLine();
                    if (s == null) {
                        // Reached end of file.
                        break;
                    }
                    ++lineNumber;
                    s = s.trim();
                    // Ignore blank lines.
                    if (s.trim().length() == 0)
                        continue;
                    // Ignore comment lines.
                    if (s.charAt(0) == '#')
                        continue;
                    if (!mapKey(s)) {
                        error = true;
                        message = "Error loading key map from " +
                            file.canonicalPath() +
                            " (line " + lineNumber + "); will use defaults.";
                        break;
                    }
                }
            }
            catch (IOException e) {
                Log.error(e);
                message = "Error loading key map from " +
                    file.canonicalPath() + "; will use defaults.";
            }
        } else {
            error = true;
            message = "Error loading key map from " + file.canonicalPath() +
                " (file not found); will use defaults.";
        }
        if (error)
            MessageDialog.showMessageDialog(message, "Error");
        return !error;
    }

    private void setGlobalDefaults()
    {
        // File menu.
        mapKey(KeyEvent.VK_O, CTRL_MASK, "openFile");
        mapKey(KeyEvent.VK_O, CTRL_MASK | ALT_MASK, "openFileInOtherWindow");
        mapKey(KeyEvent.VK_O, CTRL_MASK | SHIFT_MASK, "openFileInOtherFrame");
        mapKey(KeyEvent.VK_N, CTRL_MASK, "newBuffer");
        mapKey(KeyEvent.VK_R, ALT_MASK, "recentFiles");
        mapKey(KeyEvent.VK_S, CTRL_MASK, "save");
        mapKey(KeyEvent.VK_S, CTRL_MASK | SHIFT_MASK, "saveAs");
        mapKey(KeyEvent.VK_S, CTRL_MASK | ALT_MASK, "saveCopy");
        mapKey(KeyEvent.VK_F2, 0, "saveAll");
        mapKey(KeyEvent.VK_F4, CTRL_MASK, "killBuffer");
        mapKey(KeyEvent.VK_W, CTRL_MASK, "killBuffer");
        mapKey(KeyEvent.VK_P, ALT_MASK, "properties");
        mapKey(KeyEvent.VK_KP_RIGHT, ALT_MASK, "nextBuffer");
        mapKey(KeyEvent.VK_RIGHT, ALT_MASK, "nextBuffer");
        mapKey(KeyEvent.VK_KP_LEFT, ALT_MASK, "prevBuffer");
        mapKey(KeyEvent.VK_LEFT, ALT_MASK, "prevBuffer");
        mapKey(KeyEvent.VK_N, CTRL_MASK | SHIFT_MASK, "newFrame");
        mapKey(KeyEvent.VK_X, ALT_MASK, "executeCommand");
        mapKey(KeyEvent.VK_P, CTRL_MASK, "print");
        mapKey(KeyEvent.VK_Q, CTRL_MASK | SHIFT_MASK, "saveAllExit");
        mapKey(KeyEvent.VK_Q, CTRL_MASK, "quit");

        // Edit menu.
        mapKey(KeyEvent.VK_BACK_SPACE, ALT_MASK, "undo");
        mapKey(KeyEvent.VK_Z, CTRL_MASK, "undo");
        mapKey(KeyEvent.VK_Y, CTRL_MASK, "redo");
        mapKey(KeyEvent.VK_DELETE, SHIFT_MASK, "killRegion");
        mapKey(KeyEvent.VK_X, CTRL_MASK, "killRegion");
        mapKey(KeyEvent.VK_X, CTRL_MASK | SHIFT_MASK, "killAppend");
        mapKey(KeyEvent.VK_C, CTRL_MASK, "copyRegion");
        mapKey(KeyEvent.VK_C, CTRL_MASK | SHIFT_MASK, "copyAppend");
        mapKey(KeyEvent.VK_V, CTRL_MASK, "paste");
        mapKey(KeyEvent.VK_V, CTRL_MASK | SHIFT_MASK, "cyclePaste");
        mapKey(KeyEvent.VK_T, ALT_MASK, "cycleTabWidth");

        // Goto menu.
        mapKey(KeyEvent.VK_J, CTRL_MASK, "jumpToLine");
        mapKey(KeyEvent.VK_J, CTRL_MASK | SHIFT_MASK, "jumpToColumn");
        mapKey(KeyEvent.VK_M, CTRL_MASK, "findMatchingChar");
        mapKey(KeyEvent.VK_KP_UP, CTRL_MASK | ALT_MASK, "findFirstOccurrence");
        mapKey(KeyEvent.VK_UP, CTRL_MASK | ALT_MASK, "findFirstOccurrence");
        mapKey(KeyEvent.VK_KP_UP, ALT_MASK, "findPrevWord");
        mapKey(KeyEvent.VK_UP, ALT_MASK, "findPrevWord");
        mapKey(KeyEvent.VK_KP_DOWN, ALT_MASK, "findNextWord");
        mapKey(KeyEvent.VK_DOWN, ALT_MASK, "findNextWord");
        mapKey(KeyEvent.VK_N, CTRL_MASK | ALT_MASK, "nextChange");
        mapKey(KeyEvent.VK_P, CTRL_MASK | ALT_MASK, "previousChange");
        mapKey(KeyEvent.VK_F5, 0, "pushPosition");
        mapKey(KeyEvent.VK_F5, SHIFT_MASK, "popPosition");

        // Search menu.
        mapKey(KeyEvent.VK_F3, ALT_MASK, "find");

        if (Editor.preferences().getBooleanProperty(Property.USE_INCREMENTAL_FIND))
            mapKey(KeyEvent.VK_F, CTRL_MASK, "incrementalFind");
        else
            mapKey(KeyEvent.VK_F, CTRL_MASK, "find");

        mapKey(KeyEvent.VK_L, ALT_MASK, "listOccurrences");
        mapKey(KeyEvent.VK_L, CTRL_MASK | ALT_MASK, "listOccurrencesOfPatternAtDot");

        mapKey(KeyEvent.VK_G, CTRL_MASK, "findNext");
        mapKey(KeyEvent.VK_F3, 0, "findNext");
        mapKey(KeyEvent.VK_H, CTRL_MASK, "findPrev");
        mapKey(KeyEvent.VK_F3, SHIFT_MASK, "findPrev");
        mapKey(KeyEvent.VK_F6, 0, "findInFiles");
        mapKey(KeyEvent.VK_F, CTRL_MASK | SHIFT_MASK, "findInFiles");
        mapKey(KeyEvent.VK_L, CTRL_MASK, "listFiles");
        mapKey(KeyEvent.VK_L, CTRL_MASK | SHIFT_MASK, "listFiles");
        mapKey(KeyEvent.VK_R, CTRL_MASK, "replace");
        mapKey(KeyEvent.VK_R, CTRL_MASK | SHIFT_MASK, "replaceInFiles");

        mapKey(KeyEvent.VK_K, CTRL_MASK, "killLine");
        mapKey(KeyEvent.VK_DELETE, CTRL_MASK, "deleteWordRight");

        mapKey(KeyEvent.VK_HOME, 0, "home");
        mapKey(KeyEvent.VK_END, 0, "end");
        mapKey(KeyEvent.VK_HOME, SHIFT_MASK, "selectHome");
        mapKey(KeyEvent.VK_END, SHIFT_MASK, "selectEnd");
        mapKey(KeyEvent.VK_HOME, CTRL_MASK, "bob");
        mapKey(KeyEvent.VK_HOME, CTRL_MASK | SHIFT_MASK, "selectBob");
        mapKey(KeyEvent.VK_END, CTRL_MASK, "eob");
        mapKey(KeyEvent.VK_END, CTRL_MASK | SHIFT_MASK, "selectEob");
        mapKey(KeyEvent.VK_UP, 0, "up");
        mapKey(KeyEvent.VK_KP_UP, 0, "up");
        mapKey(KeyEvent.VK_DOWN, 0, "down");
        mapKey(KeyEvent.VK_KP_DOWN, 0, "down");
        mapKey(KeyEvent.VK_UP, SHIFT_MASK, "selectUp");
        mapKey(KeyEvent.VK_KP_UP, SHIFT_MASK, "selectUp");
        mapKey(KeyEvent.VK_DOWN, SHIFT_MASK, "selectDown");
        mapKey(KeyEvent.VK_KP_DOWN, SHIFT_MASK, "selectDown");
        mapKey(KeyEvent.VK_LEFT, 0, "left");
        mapKey(KeyEvent.VK_KP_LEFT, 0, "left");
        mapKey(KeyEvent.VK_RIGHT, 0, "right");
        mapKey(KeyEvent.VK_KP_RIGHT, 0, "right");
        mapKey(KeyEvent.VK_LEFT, SHIFT_MASK, "selectLeft");
        mapKey(KeyEvent.VK_KP_LEFT, SHIFT_MASK, "selectLeft");
        mapKey(KeyEvent.VK_RIGHT, SHIFT_MASK, "selectRight");
        mapKey(KeyEvent.VK_KP_RIGHT, SHIFT_MASK, "selectRight");
        mapKey(KeyEvent.VK_UP, CTRL_MASK, "windowUp");
        mapKey(KeyEvent.VK_KP_UP, CTRL_MASK, "windowUp");
        mapKey(KeyEvent.VK_DOWN, CTRL_MASK, "windowDown");
        mapKey(KeyEvent.VK_KP_DOWN, CTRL_MASK, "windowDown");
        mapKey(KeyEvent.VK_PAGE_UP, 0, "pageUp");
        mapKey(KeyEvent.VK_PAGE_UP, SHIFT_MASK, "selectPageUp");
        mapKey(KeyEvent.VK_PAGE_DOWN, 0, "pageDown");
        mapKey(KeyEvent.VK_PAGE_DOWN, SHIFT_MASK, "selectPageDown");
        mapKey(KeyEvent.VK_PAGE_UP, CTRL_MASK, "top");
        mapKey(KeyEvent.VK_PAGE_DOWN, CTRL_MASK, "bottom");
        mapKey(KeyEvent.VK_LEFT, CTRL_MASK, "wordLeft");
        mapKey(KeyEvent.VK_KP_LEFT, CTRL_MASK, "wordLeft");
        mapKey(KeyEvent.VK_RIGHT, CTRL_MASK, "wordRight");
        mapKey(KeyEvent.VK_KP_RIGHT, CTRL_MASK, "wordRight");
        mapKey(KeyEvent.VK_LEFT, CTRL_MASK | SHIFT_MASK, "selectWordLeft");
        mapKey(KeyEvent.VK_KP_LEFT, CTRL_MASK | SHIFT_MASK, "selectWordLeft");
        mapKey(KeyEvent.VK_RIGHT, CTRL_MASK | SHIFT_MASK, "selectWordRight");
        mapKey(KeyEvent.VK_KP_RIGHT, CTRL_MASK | SHIFT_MASK, "selectWordRight");
        mapKey(KeyEvent.VK_DELETE, 0, "delete");
        mapKey(KeyEvent.VK_BACK_SPACE, 0, "backspace");
        mapKey(KeyEvent.VK_BACK_SPACE, SHIFT_MASK, "backspace");
        mapKey(KeyEvent.VK_BACK_SPACE, CTRL_MASK, "deleteWordLeft");
        mapKey(KeyEvent.VK_ENTER, 0, "newline");

        mapKey(KeyEvent.VK_ESCAPE, 0, "escape");

        mapKey(KeyEvent.VK_G, CTRL_MASK | SHIFT_MASK, "gotoFile");
        mapKey(KeyEvent.VK_B, CTRL_MASK | SHIFT_MASK, "browseFileAtDot");

        mapKey(KeyEvent.VK_D, CTRL_MASK, "dir");

        mapKey(KeyEvent.VK_F2, SHIFT_MASK, "stamp");

        mapKey(KeyEvent.VK_A, CTRL_MASK, "selectAll");

        mapKey(KeyEvent.VK_OPEN_BRACKET, ALT_MASK, "slideOut");
        mapKey(KeyEvent.VK_CLOSE_BRACKET, ALT_MASK, "slideIn");

        // Bookmarks MUST be mapped like this!
        mapKey(KeyEvent.VK_0, ALT_MASK, "dropBookmark");
        mapKey(KeyEvent.VK_1, ALT_MASK, "dropBookmark");
        mapKey(KeyEvent.VK_2, ALT_MASK, "dropBookmark");
        mapKey(KeyEvent.VK_3, ALT_MASK, "dropBookmark");
        mapKey(KeyEvent.VK_4, ALT_MASK, "dropBookmark");
        mapKey(KeyEvent.VK_5, ALT_MASK, "dropBookmark");
        mapKey(KeyEvent.VK_6, ALT_MASK, "dropBookmark");
        mapKey(KeyEvent.VK_7, ALT_MASK, "dropBookmark");
        mapKey(KeyEvent.VK_8, ALT_MASK, "dropBookmark");
        mapKey(KeyEvent.VK_9, ALT_MASK, "dropBookmark");
        mapKey(KeyEvent.VK_0, CTRL_MASK, "gotoBookmark");
        mapKey(KeyEvent.VK_1, CTRL_MASK, "gotoBookmark");
        mapKey(KeyEvent.VK_2, CTRL_MASK, "gotoBookmark");
        mapKey(KeyEvent.VK_3, CTRL_MASK, "gotoBookmark");
        mapKey(KeyEvent.VK_4, CTRL_MASK, "gotoBookmark");
        mapKey(KeyEvent.VK_5, CTRL_MASK, "gotoBookmark");
        mapKey(KeyEvent.VK_6, CTRL_MASK, "gotoBookmark");
        mapKey(KeyEvent.VK_7, CTRL_MASK, "gotoBookmark");
        mapKey(KeyEvent.VK_8, CTRL_MASK, "gotoBookmark");
        mapKey(KeyEvent.VK_9, CTRL_MASK, "gotoBookmark");

        mapKey(KeyEvent.VK_F11, 0, "commentRegion");
        mapKey(KeyEvent.VK_F11, SHIFT_MASK, "uncommentRegion");

        // Duplicate mappings to support IBM 1.3 for Linux.
        mapKey(0xffc8, 0, "commentRegion");
        mapKey(0xffc8, SHIFT_MASK, "uncommentRegion");

        mapKey(KeyEvent.VK_F12, 0, "wrapParagraph");
        mapKey(KeyEvent.VK_F12, SHIFT_MASK, "unwrapParagraph");
        mapKey(KeyEvent.VK_F12, CTRL_MASK, "toggleWrap");

        // Duplicate mappings to support IBM 1.3 for Linux.
        mapKey(0xffc9, 0, "wrapParagraph"); // F12
        mapKey(0xffc9, SHIFT_MASK, "unwrapParagraph"); // Shift F12
        mapKey(0xffc9, CTRL_MASK, "toggleWrap"); // Ctrl F12

        mapKey(KeyEvent.VK_T, CTRL_MASK | ALT_MASK, "visibleTabs");

        // Help menu.
        mapKey(KeyEvent.VK_F1, 0, "help");
        mapKey(KeyEvent.VK_K, ALT_MASK, "describeKey");

        mapKey(KeyEvent.VK_SLASH, ALT_MASK, "expand");
        mapKey(KeyEvent.VK_SPACE, ALT_MASK, "expand");

        mapKey(KeyEvent.VK_N, ALT_MASK, "nextFrame");

        mapKey(KeyEvent.VK_W, ALT_MASK, "selectWord");

        mapKey(VK_MOUSE_1, 0, "mouseMoveDotToPoint");
        mapKey(VK_MOUSE_1, SHIFT_MASK, "mouseSelect");
        mapKey(VK_MOUSE_1, CTRL_MASK | SHIFT_MASK, "mouseSelectColumn");
        mapKey(VK_DOUBLE_MOUSE_1, 0, "selectWord");

        if (Platform.isPlatformUnix() && Platform.isJava14())
            mapKey(VK_MOUSE_2, 0, "pastePrimarySelection");

        mapKey(VK_MOUSE_3, 0, "mouseShowContextMenu");

        mapKey(KeyEvent.VK_F7, 0, "recordMacro");
        mapKey(KeyEvent.VK_F8, 0, "playbackMacro");

        mapKey(KeyEvent.VK_W, CTRL_MASK | SHIFT_MASK, "killFrame");

        // Sidebar.
        mapKey(KeyEvent.VK_EQUALS, ALT_MASK, "toggleSidebar");
        mapKey(KeyEvent.VK_B, ALT_MASK, "sidebarListBuffers");
        mapKey(KeyEvent.VK_L, CTRL_MASK | SHIFT_MASK, "sidebarListTags");

        mapKey(KeyEvent.VK_F10, 0, "splitWindow");
        mapKey(KeyEvent.VK_F10, SHIFT_MASK, "unsplitWindow");
        mapKey(KeyEvent.VK_F10, CTRL_MASK | SHIFT_MASK, "killWindow");
        mapKey(KeyEvent.VK_O, ALT_MASK, "otherWindow");

        if (Editor.preferences().getBooleanProperty(Property.ENABLE_EXPERIMENTAL_FEATURES))
            mapKey(KeyEvent.VK_F9, ALT_MASK, "shell");

        // Map these globally so they're available in the compilation buffer too.
        mapKey(KeyEvent.VK_F4, 0, "nextError");
        mapKey(KeyEvent.VK_F4, SHIFT_MASK, "previousError");
        mapKey(KeyEvent.VK_M, ALT_MASK, "showMessage");

        // Windows VM seems to need this mapping for the tab key to work properly.
        // There's also code in Dispatcher.dispatchKeyTyped to handle the tab key.
        mapKey(KeyEvent.VK_TAB, 0, "insertTab");

        if (Platform.isPlatformLinux()) {
            // These mappings work with Blackdown 1.2.2 (and 1.2 pre-release v2).
            mapKey(0x2d, CTRL_MASK, "toCenter"); // Ctrl -
            mapKey(0x5f, CTRL_MASK | SHIFT_MASK, "toTop"); // Ctrl Shift -

            // IBM 1.3, Sun 1.4.0 beta 2.
            mapKey(0x2d, CTRL_MASK | SHIFT_MASK, "toTop"); // Ctrl Shift -
        } else if (Platform.isPlatformWindows()) {
            mapKey(0x2d, CTRL_MASK, "toCenter"); // Ctrl -
            mapKey(0x2d, CTRL_MASK | SHIFT_MASK, "toTop"); // Ctrl Shift -
        }
    }

    public synchronized final KeyMapping lookup(char keyChar, int keyCode,
        int modifiers)
    {
        if (keyCode == 0 && modifiers == 0) {
            // This is the keyTyped() case. Ignore keyCode and modifiers;
            // keyChar must match the mapping.
            for (int i = mappings.size()-1; i >= 0; i--) {
                KeyMapping mapping = (KeyMapping) mappings.get(i);
                if (keyChar == mapping.getKeyChar())
                    return mapping;
            }
        } else {
            for (int i = mappings.size()-1; i >= 0; i--) {
                // This is the keyPressed() case. keyCode and modifiers must
                // match the mapping. mapping.getKeyChar() must be zero, but
                // we ignore the keyChar argument.
                KeyMapping mapping = (KeyMapping) mappings.get(i);
                if (mapping.getKeyChar() == 0 &&
                    keyCode == mapping.getKeyCode() &&
                    modifiers == mapping.getModifiers())
                    return mapping;
            }
        }
        return null;
    }

    public synchronized final KeyMapping lookup(KeyStroke keyStroke)
    {
        return lookup(keyStroke.getKeyChar(), keyStroke.getKeyCode(),
            keyStroke.getModifiers());
    }

    public synchronized final KeyStroke getKeyStroke(String command)
    {
        command = command.intern();
        for (int i = mappings.size() - 1; i >= 0; i--) {
            KeyMapping mapping = (KeyMapping) mappings.get(i);
            if (command == mapping.getCommand())
                return KeyStroke.getKeyStroke(mapping.getKeyCode(),
                    mapping.getModifiers());
        }
        return null;
    }

    public synchronized List listKeys(String command)
    {
        command = command.intern();
        ArrayList list = new ArrayList();
        for (int i = mappings.size() - 1; i >= 0; i--) {
            KeyMapping mapping = (KeyMapping) mappings.get(i);
            if (command == mapping.getCommand())
                list.add(mapping.getKeyText());
        }
        return list;
    }

    // Add all mappings for command from source key map.
    public synchronized void addMappingsForCommand(String command, KeyMap source)
    {
        command = command.intern();
        final KeyMapping[] sourceMappings = source.getMappings();
        final int limit = sourceMappings.length;
        for (int i = 0; i < limit; i++) {
            KeyMapping mapping = sourceMappings[i];
            if (command == mapping.getCommand())
                mappings.add(mapping);
        }
    }

    // Only called from synchronized methods.
    public void mapKey(int keyCode, int modifiers, String command)
    {
        // See if we already have a mapping for this keystroke.
        for (int i = 0; i < mappings.size(); i++) {
            KeyMapping mapping = (KeyMapping) mappings.get(i);
            if (keyCode == mapping.getKeyCode() && modifiers == mapping.getModifiers()) {
                mappings.set(i, new KeyMapping(keyCode, modifiers, command));
                return;
            }
        }
        // No mapping found.
        mappings.add(new KeyMapping(keyCode, modifiers, command));
    }

    // Only called from synchronized methods.
    public void mapKey(char keyChar, String command)
    {
        // See if we already have a mapping for this keystroke.
        for (int i = 0; i < mappings.size(); i++) {
            KeyMapping mapping = (KeyMapping) mappings.get(i);
            if (keyChar == mapping.getKeyChar()) {
                mappings.set(i, new KeyMapping(keyChar, command));
                return;
            }
        }
        // No mapping found.
        mappings.add(new KeyMapping(keyChar, command));
    }

    // Only called from synchronized methods.
    public boolean mapKey(String s)
    {
        KeyMapping mapping = KeyMapping.createKeyMapping(s);
        if (mapping != null)         {
            mappings.add(mapping);
            return true;
        } else
            return false;
    }

    // Only called from synchronized methods.
    public boolean mapKey(String keyText, String command)
    {
        KeyMapping mapping = KeyMapping.createKeyMapping(keyText, command);
        if (mapping != null) {
            mappings.add(mapping);
            return true;
        } else
            return false;
    }

    // Only called from synchronized methods.
    public void unmapKey(char keyChar)
    {
        for (int i = 0; i < mappings.size(); i++) {
            KeyMapping mapping = (KeyMapping) mappings.get(i);
            if (keyChar == mapping.getKeyChar()) {
                mappings.remove(i);
                return;
            }
        }
    }

    // Only called from synchronized methods.
    public void unmapKey(int keyCode, int modifiers)
    {
        for (int i = 0; i < mappings.size(); i++) {
            KeyMapping mapping = (KeyMapping) mappings.get(i);
            if (keyCode == mapping.getKeyCode() && modifiers == mapping.getModifiers()) {
                mappings.remove(i);
                return;
            }
        }
    }

    public synchronized void writeKeyMap(File file)
    {
        try {
            PrintWriter out = new PrintWriter(file.getOutputStream());
            for (int i = 0; i < mappings.size(); i++)
                out.println(mappings.get(i).toString());
            out.close();
        }
        catch (IOException e) {
            Log.error(e);
        }
    }
}
