/*
 * Java.java
 *
 * Copyright (C) 2002-2003 Peter Graves
 * $Id: Java.java,v 1.3 2003-01-18 19:29:48 piso Exp $
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class Java extends Module
{
    // ### jclass
    private static final Primitive1 JCLASS =
        new Primitive1("jclass", PACKAGE_JAVA) {
        public LispObject execute(LispObject arg) throws LispException
        {
            try {
                return new JavaObject(Class.forName(LispString.getValue(arg)));
            }
            catch (ClassNotFoundException e) {
                throw new LispException("class not found: " + arg);
            }
            catch (Throwable t) {
                throw new LispException(getMessage(t));
            }
        }
    };

    // ### jconstructor
    // jconstructor class-name &rest parameter-class-names
    private static final Primitive JCONSTRUCTOR =
        new Primitive("jconstructor", PACKAGE_JAVA) {
        public LispObject execute(LispObject[] args) throws LispException
        {
            if (args.length < 1)
                throw new WrongNumberOfArgumentsException(this);
            String className = LispString.getValue(args[0]);
            try {
                final Class c = Class.forName(className);
                Class[] parameterTypes = new Class[args.length-1];
                for (int i = 1; i < args.length; i++) {
                    className = LispString.getValue(args[i]);
                    parameterTypes[i-1] = forName(className);
                }
                return new JavaObject(c.getConstructor(parameterTypes));
            }
            catch (ClassNotFoundException e) {
                throw new LispException("class not found: " + className);
            }
            catch (NoSuchMethodException e) {
                throw new LispException("no such constructor");
            }
            catch (Throwable t) {
                throw new LispException(getMessage(t));
            }
        }
    };

    // ### jmethod
    // jmethod class-ref name &rest parameter-class-names
    private static final Primitive JMETHOD =
        new Primitive("jmethod", PACKAGE_JAVA) {
        public LispObject execute(LispObject[] args) throws LispException
        {
            if (args.length < 2)
                throw new WrongNumberOfArgumentsException(this);
            String className = LispString.getValue(args[0]);
            String methodName = LispString.getValue(args[1]);
            try {
                final Class c = Class.forName(className);
                if (args.length > 2) {
                    Class[] parameterTypes = new Class[args.length-2];
                    for (int i = 2; i < args.length; i++) {
                        className = LispString.getValue(args[i]);
                        parameterTypes[i-2] = forName(className);
                    }
                    return new JavaObject(c.getMethod(methodName,
                        parameterTypes));
                } else {
                    // Parameter types not specified.
                    Method[] methods = c.getMethods();
                    for (int i = 0; i < methods.length; i++) {
                        Method method = methods[i];
                        if (method.getName().equals(methodName))
                            return new JavaObject(method);
                    }
                    throw new LispException("no such method");
                }
            }
            catch (ClassNotFoundException e) {
                throw new LispException("class not found: " + className);
            }
            catch (NoSuchMethodException e) {
                throw new LispException("no such method");
            }
            catch (Throwable t) {
                throw new LispException(getMessage(t));
            }
        }
    };

    // ### jstatic
    // jstatic method class &rest args
    private static final Primitive JSTATIC =
        new Primitive("jstatic", PACKAGE_JAVA) {
        public LispObject execute(LispObject[] args) throws LispException
        {
            if (args.length < 2)
                throw new WrongNumberOfArgumentsException(this);
            try {
                Method m = null;
                LispObject methodRef = args[0];
                if (methodRef instanceof JavaObject) {
                    Object obj = ((JavaObject)methodRef).getObject();
                    if (obj instanceof Method)
                        m = (Method) obj;
                } else if (methodRef instanceof LispString) {
                    Class c = null;
                    LispObject classRef = args[1];
                    if (classRef instanceof JavaObject) {
                        Object obj = ((JavaObject)classRef).getObject();
                        if (obj instanceof Class)
                            c = (Class) obj;
                    } else if (classRef instanceof LispString) {
                        c = Class.forName(LispString.getValue(classRef));
                    }
                    if (c != null) {
                        String methodName = LispString.getValue(methodRef);
                        Method[] methods = c.getMethods();
                        for (int i = 0; i < methods.length; i++) {
                            Method method = methods[i];
                            if (!Modifier.isStatic(method.getModifiers()))
                                continue;
                            if (method.getName().equals(methodName)) {
                                m = method;
                                break;
                            }
                        }
                        if (m == null)
                            throw new LispException("no such method");
                    }
                } else
                    throw new WrongTypeException(methodRef);
                Object[] methodArgs = new Object[args.length-2];
                for (int i = 2; i < args.length; i++) {
                    LispObject arg = args[i];
                    if (arg instanceof LispString)
                        methodArgs[i-2] = ((LispString)arg).getValue();
                    else if (arg instanceof LispCharacter)
                        methodArgs[i-2] = new Character(((LispCharacter)arg).getValue());
                    else if (arg instanceof JavaObject)
                        methodArgs[i-2] = ((JavaObject)arg).getObject();
                }
                Object result = m.invoke(null, methodArgs);
                return makeLispObject(result);
            }
            catch (Throwable t) {
                throw new LispException(getMessage(t));
            }
        }
    };

    // ### jnew
    // jnew constructor &rest args
    private static final Primitive JNEW = new Primitive("jnew", PACKAGE_JAVA) {
        public LispObject execute(LispObject[] args) throws LispException
        {
            if (args.length < 1)
                throw new WrongNumberOfArgumentsException(this);
            LispObject classRef = args[0];
            try {
                Constructor constructor = (Constructor) JavaObject.getObject(classRef);
                Object[] initargs = new Object[args.length-1];
                for (int i = 1; i < args.length; i++) {
                    LispObject arg = args[i];
                    if (arg instanceof LispString)
                        initargs[i-1] = ((LispString)arg).getValue();
                }
                return new JavaObject(constructor.newInstance(initargs));
            }
            catch (Throwable t) {
                throw new LispException(getMessage(t));
            }
        }
    };

    // ### jcall
    // jcall method instance &rest args
    private static final Primitive JCALL = new Primitive("jcall", PACKAGE_JAVA) {
        public LispObject execute(LispObject[] args) throws LispException
        {
            if (args.length < 2)
                throw new WrongNumberOfArgumentsException(this);
            try {
                Method method = (Method) JavaObject.getObject(args[0]);
                Object instance = JavaObject.getObject(args[1]);
                Object[] methodArgs = new Object[args.length-2];
                for (int i = 2; i < args.length; i++) {
                    LispObject arg = args[i];
                    if (arg instanceof LispString)
                        methodArgs[i-2] = ((LispString)arg).getValue();
                    else if (arg instanceof JavaObject)
                        methodArgs[i-2] = ((JavaObject)arg).getObject();
                    else if (arg instanceof LispCharacter)
                        methodArgs[i-2] = new Character(((LispCharacter)arg).getValue());
                }
                Object result = method.invoke(instance, methodArgs);
                return makeLispObject(result);
            }
            catch (Throwable t) {
                throw new LispException(getMessage(t));
            }
        }
    };

    // Supports Java primitive types too.
    private static Class forName(String className) throws ClassNotFoundException
    {
        if (className.equals("boolean"))
            return Boolean.TYPE;
        if (className.equals("byte"))
            return Byte.TYPE;
        if (className.equals("char"))
            return Character.TYPE;
        if (className.equals("short"))
            return Short.TYPE;
        if (className.equals("int"))
            return Integer.TYPE;
        if (className.equals("long"))
            return Long.TYPE;
        if (className.equals("float"))
            return Float.TYPE;
        if (className.equals("double"))
            return Double.TYPE;
        // Not a primitive Java type.
        return Class.forName(className);
    }

    private static final LispObject makeLispObject(Object obj)
    {
        if (obj == null)
            return NIL;
        if (obj instanceof Boolean)
            return ((Boolean)obj).booleanValue() ? T : NIL;
        if (obj instanceof Long)
            return new Fixnum(((Long)obj).longValue());
        if (obj instanceof Integer)
            return new Fixnum(((Integer)obj).longValue());
        if (obj instanceof String)
            return new LispString((String)obj);
        return new JavaObject(obj);
    }

    private static final String getMessage(Throwable t)
    {
        if (t instanceof InvocationTargetException) {
            try {
                Method method =
                    Throwable.class.getMethod("getCause", new Class[0]);
                if (method != null) {
                    Throwable cause = (Throwable) method.invoke(t,
                        new Object[0]);
                    if (cause != null)
                        t = cause;
                }
            }
            catch (NoSuchMethodException e) {
                Debug.trace(e);
            }
            catch (Exception e) {
                Debug.trace(e);
            }
        }
        String message = t.getMessage();
        if (message == null || message.length() == 0)
            message = t.getClass().getName();
        return message;
    }

    static {
        export("JCLASS", PACKAGE_JAVA);
        export("JCONSTRUCTOR", PACKAGE_JAVA);
        export("JMETHOD", PACKAGE_JAVA);
        export("JSTATIC", PACKAGE_JAVA);
        export("JNEW", PACKAGE_JAVA);
        export("JCALL", PACKAGE_JAVA);
    }
}
