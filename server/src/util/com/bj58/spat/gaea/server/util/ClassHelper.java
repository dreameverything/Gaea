/*
 *  Copyright Beijing 58 Information Technology Co.,Ltd.
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package com.bj58.spat.gaea.server.util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

import com.bj58.spat.gaea.server.deploy.hotdeploy.DynamicClassLoader;

/**
 * 
 * @author Service Platform Architecture Team (spat@58.com)
 */
public class ClassHelper {

	/**
	 * 获取jar文件中的所有的Class类，并通过classLoader自定义的类加载器加载到JVM中
	 * @param jarPath
	 * @param classLoader
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
    public static Set<Class<?>> getClassFromJar(String jarPath, 
    											DynamicClassLoader classLoader) throws IOException, ClassNotFoundException {
        JarFile jarFile = new JarFile(jarPath);
        Enumeration<JarEntry> entries = jarFile.entries();
        Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            String name = jarEntry.getName();
            if (name.endsWith(".class")) {
                String className = name.replaceAll(".class", "").replaceAll("/", ".");
                Class<?> cls = null;
                try {
//    				cls = classLoader.findClass(className);
                	cls = classLoader.loadClass(className);
                } catch (Throwable ex) {
                	
                }
                if (cls != null) {
                    classes.add(cls);
                }
            }
        }
        return classes;
    }

    /**
     * check class is implements the interface
     * @param type
     * @param interfaceType
     * @return
     */
    public static boolean interfaceOf(Class<?> type, Class<?> interfaceType) {
        if (type == null) {
            return false;
        }
        Class<?>[] interfaces = type.getInterfaces();
        for (Class<?> c : interfaces) {
            if (c.equals(interfaceType)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * <pre>
     * get method parameter names
     * 根据javassist反射类字节码中的LocalVariableTable获取到方法中的参数对应到源码中的参数名称。
     * 但是要注意非静态方法和静态方法的区别，以下是非静态方法getNewsByID和静态方法getNewsByID1的字节码片段：
     *   // access flags 0x1
     *   public getNewsByID(I)Lentity/News; throws java/lang/Exception
     *    L0
     *     LINENUMBER 16 L0
     *     INVOKESTATIC components/NewsService.getNews ()Lentity/News;
     *     ARETURN
     *    L1
     *     LOCALVARIABLE this Lcomponents/NewsService; L0 L1 0
     *     LOCALVARIABLE newsID I L0 L1 1
     *     MAXSTACK = 1
     *     MAXLOCALS = 2
     *
     *   // access flags 0x9
     *   public static getNewsByID1(I)Lentity/News; throws java/lang/Exception
     *    L0
     *     LINENUMBER 20 L0
     *     INVOKESTATIC components/NewsService.getNews ()Lentity/News;
     *     ARETURN
     *    L1
     *     LOCALVARIABLE newsID I L0 L1 0
     *     MAXSTACK = 1
     *     MAXLOCALS = 1
     *
     *     从以上代码中可以看出来：非静态方法的LOCALVARIABLE表中的第一个参数是this；但是，
     *     静态方法的LOCALVARIABLE表中的第一个参数却没有this，直接就是第一个参数开始。
     * </pre>
     * @param cls
     * @param method
     * @return
     * @throws Exception
     */
    public static String[] getParamNames(Class<?> cls, Method method) throws Exception {
    	ClassPool pool = ClassPool.getDefault();  
    	CtClass cc = pool.get(cls.getName());

    	Class<?>[] paramAry = method.getParameterTypes();
    	String[] paramTypeNames = new String[paramAry.length];
    	for(int i=0; i<paramAry.length; i++) {
    		paramTypeNames[i] = paramAry[i].getName();
    	}
    	
    	CtMethod cm = cc.getDeclaredMethod(method.getName(), pool.get(paramTypeNames));

    	MethodInfo methodInfo = cm.getMethodInfo();  
    	CodeAttribute codeAttribute = methodInfo.getCodeAttribute();  
    	LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);  
    	if (attr == null)  {
    	    throw new Exception("class:"+cls.getName()+", have no LocalVariableTable, please use javac -g:{vars} to compile the source file");
    	}
    	String[] paramNames = new String[cm.getParameterTypes().length];
    	//非静态方法，LocalVariableTable中的第一个参数是this
    	int pos = Modifier.isStatic(cm.getModifiers()) ? 0 : 1;  
    	for (int i = 0; i < paramNames.length; i++) {
    	    paramNames[i] = attr.variableName(i + pos);
    	}
    	return paramNames;
    }
    
    
    /**
     * get method parameters annotations
     * @param cls
     * @param method
     * @return
     * @throws Exception
     */
    public static Object[][] getParamAnnotations(Class<?> cls, Method method) throws Exception {
    	ClassPool pool = ClassPool.getDefault();  
    	CtClass cc = pool.get(cls.getName());

    	Class<?>[] paramAry = method.getParameterTypes();
    	String[] paramTypeNames = new String[paramAry.length];
    	for(int i=0; i<paramAry.length; i++) {
    		paramTypeNames[i] = paramAry[i].getName();
    	}
    	
    	CtMethod cm = cc.getDeclaredMethod(method.getName(), pool.get(paramTypeNames));
    	return cm.getParameterAnnotations();
    }
    
}