/*
 * UnspecifiedTypeSpecifier.java
 *
 * Copyright (C) 2003 Peter Graves
 * $Id: UnspecifiedTypeSpecifier.java,v 1.1 2003-08-14 01:46:17 piso Exp $
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

package org.armedbear.lisp;

public final class UnspecifiedTypeSpecifier extends TypeSpecifier
{
    private static final UnspecifiedTypeSpecifier instance =
        new UnspecifiedTypeSpecifier();

    public static UnspecifiedTypeSpecifier getInstance()
    {
        return instance;
    }

    private UnspecifiedTypeSpecifier()
    {
    }

    public LispObject test(LispObject obj)
    {
        return T;
    }
}
