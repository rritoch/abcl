/*
 * LispShell.java
 *
 * Copyright (C) 2002-2004 Peter Graves
 * $Id: LispShell.java,v 1.71 2004-09-15 18:35:56 piso Exp $
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

import gnu.regexp.RE;
import gnu.regexp.REMatch;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.swing.SwingUtilities;
import org.armedbear.lisp.Site;

public class LispShell extends Shell
{
    private static final String DEFAULT_PROMPT_PATTERN =
        "^[^:>\\*\\]]*[:>\\*\\]] *";

    private static final String ALLEGRO_PROMPT_PATTERN =
        "^(\\[[0-9+][ci]?\\] )?[^ ]+\\([0-9]+\\): ";

    private static final String ARMEDBEAR_PROMPT_PATTERN =
        ALLEGRO_PROMPT_PATTERN;

    private static final String CLISP_PROMPT_PATTERN =
        "^[^>\\*\\]]*\\[[0-9]+\\]> ";

    private static final String CMUCL_PROMPT_PATTERN =
        "^\\* |^[0-9]+\\] ";

    private static final String SBCL_PROMPT_PATTERN =
        CMUCL_PROMPT_PATTERN + "|" + ALLEGRO_PROMPT_PATTERN;

    private final boolean slime;

    private String resetCommand = null;
    private String exitCommand = "(exit)";

    private Position posBeforeLastPrompt;
    private Position posEndOfInput;

    // For JLisp.java.
    protected LispShell()
    {
        setPromptRE(ARMEDBEAR_PROMPT_PATTERN);
        setResetCommand(":reset");
        slime = false;
    }

    private LispShell(String shellCommand, String title)
    {
        super(shellCommand, LispShellMode.getMode());
        this.title = title;
        formatter = mode.getFormatter(this);
        slime = title.startsWith("slime ");
    }

    public final boolean isLisp()
    {
        return true;
    }

    private void setResetCommand(String s)
    {
        resetCommand = s;
    }

    private void setExitCommand(String s)
    {
        exitCommand = s;
    }

    private static Shell createLispShell(String shellCommand, String title,
                                         boolean startSlime)
    {
        if (startSlime) {
            if (shellCommand.indexOf("sbcl") >= 0) {
                File lispHome = File.getInstance(Site.getLispHome());
                File swankLoader = File.getInstance(lispHome,
                                                    "swank-loader.lisp");
                shellCommand =
                    shellCommand + " --load " + swankLoader.canonicalPath();
            } else if (shellCommand.indexOf("abcl") >= 0 ||
                       shellCommand.indexOf("org.armedbear.lisp") >= 0) {
                shellCommand =
                    shellCommand.concat(" --load-system-file swank-loader.lisp");
            }
        }
        LispShell lisp = new LispShell(shellCommand, title);
        lisp.startProcess();
        if (lisp.getProcess() == null) {
            Editor.getBufferList().remove(lisp);
            String message;
            if (Utilities.haveJpty())
                message = "Unable to start process \"" + shellCommand + "\"";
            else
                message = JPTY_NOT_FOUND;
            MessageDialog.showMessageDialog(message, "Error");
            return null;
        }
        if (shellCommand.equals("alisp") || shellCommand.equals("/usr/bin/alisp")) {
            lisp.setPromptRE(ALLEGRO_PROMPT_PATTERN);
            lisp.setResetCommand(":reset");
        } else if (shellCommand.indexOf("clisp") >= 0) {
            // clisp -I
            lisp.setPromptRE(CLISP_PROMPT_PATTERN);
            lisp.setResetCommand("(sys::debug-unwind)");
        } else if (shellCommand.equals("/usr/bin/lisp")) {
            lisp.setPromptRE(CMUCL_PROMPT_PATTERN);
            lisp.setResetCommand(":q");
            lisp.setExitCommand("(quit)");
        } else if (shellCommand.indexOf("sbcl") >= 0) {
            lisp.setPromptRE(SBCL_PROMPT_PATTERN);
            lisp.setResetCommand(":abort");
            lisp.setExitCommand("(quit)");
        } else if (shellCommand.indexOf("org.armedbear.lisp") >= 0 ||
                   shellCommand.indexOf("abcl") >= 0)
        {
            lisp.setPromptRE(ARMEDBEAR_PROMPT_PATTERN);
            lisp.setResetCommand(":reset");
        } else {
            lisp.setPromptRE(DEFAULT_PROMPT_PATTERN);
            if (shellCommand.equals("rep") || shellCommand.equals("/usr/bin/rep"))
                lisp.setExitCommand(",quit");
        }
        lisp.needsRenumbering(true);
        if (startSlime) {
            try {
                JLisp.runLispCommand("(sys:load-system-file \"slime-loader.lisp\")");
                JLisp.runLispCommand("(setq slime::*repl-buffer-name* \"" + title + "\")");
            }
            catch (Throwable t) {
                Log.debug(t);
            }
        }
        if (Editor.isLispInitialized())
            LispAPI.invokeLispShellStartupHook(lisp, shellCommand);
        return lisp;
    }

    protected void initializeHistory()
    {
        history = new History("lisp.history", 30);
    }

    public void enter()
    {
        if (!checkProcess())
            return;
        final Editor editor = Editor.currentEditor();
        Position dot = editor.getDotCopy();
        if (dot == null)
            return;
        if (needsRenumbering)
            renumber();
        final Line dotLine = dot.getLine();
        final Position endOfOutput = getEndOfOutput();
        if (endOfOutput == null) {
            // Ignore input before first prompt is displayed.
            dotLine.setText("");
            return;
        }
        if (dot.isBefore(endOfOutput)) {
            editor.newlineAndIndent();
            return; // For now.
        }
        final Line promptLine = endOfOutput.getLine();
        Annotation a = new Annotation(endOfOutput.getOffset());
        promptLine.setAnnotation(a);
        promptLine.setFlags(STATE_PROMPT);
        Position end = getEnd();
        Position pos = LispMode.findContainingSexp(end);
        boolean isComplete = (pos == null || pos.isBefore(endOfOutput));
        if (isComplete) {
            // Complete sexp.
            editor.eob();
            editor.insertLineSeparator();
            editor.getDotLine().setFlags(0);
        } else {
            // Not complete; multiline input.
            editor.newline();
            editor.getDotLine().setFlags(STATE_INPUT);
        }
        if (needsRenumbering)
            renumber();
        editor.moveCaretToDotCol();
        editor.getDisplay().setReframe(-2);
        resetUndo();
        stripEcho = true;
        if (isComplete) {
            // No containing sexp. Send input to lisp process.
            Position begin = endOfOutput;
            end = editor.getDotCopy();
            end.setOffset(end.getLineLength());
            setEndOfOutput(end);
            Line lineBeforeLastPrompt = promptLine.previous();
            if (lineBeforeLastPrompt != null) {
                posBeforeLastPrompt =
                    new Position(lineBeforeLastPrompt,
                                 lineBeforeLastPrompt.length());
            }
            posEndOfInput = end.copy();
            String s = new Region(this, begin, end).toString();
            sendInputToLisp(s);
        } else
            indentLineAtDot(editor);
    }

    public void resetLisp()
    {
        if (resetCommand != null) {
            Position pos = getEnd();
            insertString(pos, resetCommand.concat("\n"));
            if (needsRenumbering())
                renumber();
            enforceOutputLimit(Property.SHELL_OUTPUT_LIMIT);
            posEndOfInput = pos.copy();
            send(resetCommand);
        }
    }

    protected void stdOutUpdate(final String s)
    {
        String prompt;
        int index = s.lastIndexOf('\n');
        if (index >= 0)
            prompt = s.substring(index + 1);
        else
            prompt = s;
        final REMatch match = promptRE.getMatch(prompt);
        if (match != null) {
            // Last line of output looks like a prompt.
            String m = match.toString();
            if (prompt.startsWith(m)) {
                if (prompt.substring(m.length()).startsWith(m)) {
                    // Double prompt. Remove one of them.
                    prompt = prompt.substring(m.length());
                }
            }
        }
        final String output;
        if (index >= 0)
            output = s.substring(0, index + 1) + prompt;
        else
            output = prompt;
        Runnable r = new Runnable() {
            public void run()
            {
                Position pos = getEnd();
                if (pos != null)
                    pos.getLine().setFlags(0); // This value will propagate.
                if (output.length() > 0) {
                    appendString(output);
                    if (match != null) {
                        Line lineBeforeLastPrompt =
                            getEnd().getLine().previous();
                        if (lineBeforeLastPrompt != null) {
                            posBeforeLastPrompt =
                                new Position(lineBeforeLastPrompt,
                                             lineBeforeLastPrompt.length());
                        }
                        if (isBusy())
                            setBusy(false);
                    }
                }
                updateDisplayInAllFrames();
                resetUndo();
            }
        };
        SwingUtilities.invokeLater(r);
    }

    protected void stdErrUpdate(final String s)
    {
        Runnable r = new Runnable() {
            public void run()
            {
                appendString(s);
                updateDisplayInAllFrames();
                resetUndo();
            }
        };
        SwingUtilities.invokeLater(r);
    }

    protected void appendString(String s)
    {
        try {
            lockWrite();
        }
        catch (InterruptedException e) {
            Log.error(e);
            return;
        }
        try {
            if (slime) {
                // Slime.
                if (posEndOfInput == null)
                    posEndOfInput = new Position(getFirstLine(), 0);
                final Position pos;
                if (posBeforeLastPrompt != null && posEndOfInput != null) {
                    if (posEndOfInput.isAfter(posBeforeLastPrompt)) {
                        // There has been user input since the last prompt.
                        pos = getEnd();
                    } else {
                        pos = posBeforeLastPrompt;
                    }
                } else {
                    pos = getEnd();
                }
                if (pos != null) {
                    if (pos == posBeforeLastPrompt) {
                        if (s.length() > 0) {
                            if (s.charAt(s.length() - 1) == '\n')
                                s = s.substring(0, s.length() - 1);
                        }
                        if (s.length() > 0 && s.charAt(0) != '\n')
                            insertLineSeparator(pos);
                    }
                    insertString(pos, s);
                    if (needsRenumbering())
                        renumber();
                    enforceOutputLimit(Property.SHELL_OUTPUT_LIMIT);
                    if (pos != posBeforeLastPrompt)
                        setEndOfOutput(pos.copy());
                } else {
                    // Empty buffer.
                    setText(s);
                    setEndOfOutput(getEnd().copy());
                }
            } else {
                // No slime.
                Position pos = getEnd();
                if (pos != null) {
                    insertString(pos, s);
                    if (needsRenumbering())
                        renumber();
                    enforceOutputLimit(Property.SHELL_OUTPUT_LIMIT);
                    setEndOfOutput(pos.copy());
                } else {
                    setText(s);
                    setEndOfOutput(getEnd().copy());
                }
            }
        }
        finally {
            unlockWrite();
        }
    }

    private void indentLineAtDot(Editor editor)
    {
        final Line dotLine = editor.getDotLine();
        if (dotLine.length() > 0)
            return;
        try {
            lockWrite();
        }
        catch (InterruptedException e) {
            Log.error(e);
            return;
        }
        try {
            getFormatter().parseBuffer();
            int indent = mode.getCorrectIndentation(dotLine, this);
            if (indent != getIndentation(dotLine)) {
                editor.addUndo(SimpleEdit.LINE_EDIT);
                setIndentation(dotLine, indent);
                dotLine.setFlags(STATE_INPUT);
                modified();
            }
            if (dotLine.length() > 0) {
                editor.moveDotToIndentation();
                editor.moveCaretToDotCol();
            } else {
                final Display display = editor.getDisplay();
                display.setCaretCol(indent - display.getShift());
                if (getBooleanProperty(Property.RESTRICT_CARET))
                    editor.fillToCaret();
            }
            resetUndo(); // Why?
        }
        finally {
            unlockWrite();
        }
    }

    private void sendInputToLisp(String input)
    {
        // Save history unless input is very short (e.g. ":q"). Ignore
        // whitespace at end of line.
        String trim = input.trim();
        if (trim.length() > 2) {
            history.append(trim);
            history.save();
        }
        send(input);
    }

    public void dispose()
    {
        if (!checkProcess()) {
            Log.debug("checkProcess returned false");
            return;
        }
        Thread t = new Thread("LispShell dispose") {
            public void run()
            {
                try {
                    stdin.write(3);
                    stdin.flush();
                    stdin.write(exitCommand);
                    stdin.write("\n");
                    stdin.flush();
                    stdin.close();
                    final Process p = getProcess();
                    if (p != null) {
                        p.destroy();
                        p.waitFor();
                    }
                }
                catch (IOException e) {
                    Log.error(e);
                }
                catch (InterruptedException e) {
                    Log.error(e);
                }
            }
        };
        t.setPriority(Thread.MIN_PRIORITY);
        t.setDaemon(true);
        t.start();
    }

    public String getFileNameForDisplay()
    {
        return title;
    }

    public String toString()
    {
        return title;
    }

    public static void slime()
    {
        lisp(getDefaultLispShellCommand(), "slime abcl", false, true);
    }

    public static void slime(String shellCommand)
    {
        // Require jpty on Unix platforms.
        lisp(shellCommand, "slime ".concat(shellCommand),
             Platform.isPlatformUnix(), true);
    }

    public static void lisp()
    {
        lisp(getDefaultLispShellCommand(), "abcl", false, false);
    }

    public static void lisp(String shellCommand)
    {
        // Require jpty on Unix platforms.
        lisp(shellCommand, shellCommand, Platform.isPlatformUnix(), false);
    }

    private static void lisp(String shellCommand, String title,
                             boolean requireJpty, boolean startSlime)
    {
        if (requireJpty && !Utilities.haveJpty()) {
            MessageDialog.showMessageDialog(JPTY_NOT_FOUND, "Error");
            return;
        }
        if (Platform.isPlatformWindows())
            if (!Platform.isPlatformWindows5())
                return;
        final Editor editor = Editor.currentEditor();
        // Look for an existing LispShell buffer with the same shell command.
        Buffer buf = findLisp(shellCommand);
        if (buf == null) {
            editor.setWaitCursor();
            buf = createLispShell(shellCommand, title, startSlime);
            if (buf != null)
                buf.setBusy(true);
            editor.setDefaultCursor();
        }
        if (buf != null) {
            editor.makeNext(buf);
            Buffer b = editor.getBuffer();
            if (b != null && b.isPaired())
                editor.switchToBuffer(buf);
            else
                editor.activateInOtherWindow(buf);
        }
    }

    private static String getDefaultLispShellCommand()
    {
        File java = null;
        File javaHome = File.getInstance(System.getProperty("java.home"));
        if (javaHome != null && javaHome.isDirectory()) {
            java = File.getInstance(javaHome,
                                    Platform.isPlatformWindows() ? "bin\\java.exe" : "bin/java");
            if (java != null && !java.isFile())
                java = null;
        }
        // If j was invoked via "java -jar j.jar", use the canonical path
        // of j.jar.
        String classPath = System.getProperty("java.class.path");
        if (classPath.equals("j.jar:.")) // IBM 1.4.0 on Linux
            classPath = "j.jar";
        if (classPath.indexOf(LocalFile.getPathSeparatorChar()) < 0) {
            // Only one component in classpath.
            String path = classPath;
            if (Platform.isPlatformWindows())
                path = path.toLowerCase();
            if (path.equals("j.jar") || path.endsWith("/j.jar") ||
                path.endsWith("\\j.jar")) {
                File dir = File.getInstance(System.getProperty("user.dir"));
                File file = File.getInstance(dir, path);
                if (file != null && file.isFile())
                    classPath = file.canonicalPath();
            }
        }
        FastStringBuffer sb = new FastStringBuffer();
        if (java != null) {
            sb.append(java.canonicalPath());
            String vendor = System.getProperty("java.vendor");
            if (vendor != null) {
                if (vendor.indexOf("Sun") >= 0 ||
                    vendor.indexOf("Blackdown") >= 0) {
                    sb.append(" -server");
                    sb.append(" -Xmx128M");
                    String lispHome = org.armedbear.lisp.Site.getLispHome();
                    if (lispHome != null) {
                        sb.append(" -Xrs -Djava.library.path=");
                        sb.append(lispHome);
                        sb.append(":/usr/local/lib/abcl");
                    }
                } else if (vendor.indexOf("IBM") >= 0) {
                    sb.append(" -Xss512K");
                    sb.append(" -Xmx128M");
                }
            }
        } else
            sb.append("java");
        sb.append(" -cp ");
        sb.append(classPath);
        sb.append(" org.armedbear.lisp.Main");
        return sb.toString();
    }

    public static CommandInterpreter findLisp(String shellCommand)
    {
        for (BufferIterator it = new BufferIterator(); it.hasNext();) {
            Buffer b = it.nextBuffer();
            if (b instanceof CommandInterpreter) {
                CommandInterpreter comint = (CommandInterpreter) b;
                if (comint.isLisp()) {
                    if (shellCommand == null ||
                        shellCommand.equals(comint.getShellCommand()))
                        return comint;
                }
            }
        }
        return null;
    }
}
