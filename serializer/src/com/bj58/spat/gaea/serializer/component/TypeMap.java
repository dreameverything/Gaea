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
package com.bj58.spat.gaea.serializer.component;

import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.IOException;
import java.math.BigDecimal;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.net.MalformedURLException;

import com.bj58.spat.gaea.serializer.classes.DBNull;
import com.bj58.spat.gaea.serializer.classes.GKeyValuePair;
import com.bj58.spat.gaea.serializer.component.annotation.GaeaSerializable;
import com.bj58.spat.gaea.serializer.component.exception.DisallowedSerializeException;
import com.bj58.spat.gaea.serializer.component.helper.StrHelper;
import org.apache.log4j.Logger;

/**
 * <pre>
 * 1.初始化可序列化的类型，类型包括系统默认的类型和自定义的类型。
 * 2.系统默认的类型包括以下类型：
 * | null | Object | String |
 * | Boolean boolean | Character char | Byte byte |
 * | Short short | Integer int | Long long |
 * | Float float | Double double | BigDecimal |
 * | util.Date sql.Date sql.Time sql.Timestamp |
 * | List | Array | Map | GKeyValuePair |
 *
 * 3.自定义的类型是包含GaeaSerializable注解的类。
 *
 * </pre>
 * 
 * @author Service Platform Architecture Team (spat@58.com)
 */
public final class TypeMap {
    private static Logger logger = Logger.getLogger(TypeMap.class);
    /**
     * <pre>
     * 类型与ClassItem的对应关系
     * 类型例如：Date.class, java.sql.Date.class
     * 多个类型可以对应同一个ClassItem，例如日期类型Date.class, java.sql.Date.class，对应同一个ClassItem。
     * </pre>
     */
    private static Map<Class, ClassItem> classToClassItemMap = new HashMap<Class, ClassItem>();
    /**
     * <pre>
     * 类型在ClassList列表中的索引index与ClassItem的对应关系
     * 索引ID如：1、2、3...
     * 如果是自定义类型的索引ID是根据name生成的哈希值
     * </pre>
     */
    private static Map<Integer, ClassItem> indexToClassItemMap = new HashMap<Integer, ClassItem>();

    /**
     * <pre>
     * 初始化序列化时涉及到的类型，包含两大类：系统默认和自定义的。
     * 系统默认的类型，如： DBNull.class、char.class、Map.class等。
     * 自定义的类型是类文件中包含GaeaSerializable注解的类。
     * </pre>
     */
    public static void InitTypeMap() {
        classToClassItemMap.clear();
        indexToClassItemMap.clear();
        ArrayList<ClassItem> ClassList = new ArrayList<ClassItem>();

        //初始化系统默认的类型
        ClassList.add(new ClassItem(1, DBNull.class));
        ClassList.add(new ClassItem(2, Object.class));
        ClassList.add(new ClassItem(3, Boolean.class, boolean.class));
        ClassList.add(new ClassItem(4, Character.class, char.class));
        ClassList.add(new ClassItem(5, Byte.class, byte.class));
        //6 之前的版本有6，这里空出来是为了兼容旧版本
        ClassList.add(new ClassItem(7, Short.class, short.class));
        ClassList.add(new ClassItem(9, Integer.class, int.class));
        ClassList.add(new ClassItem(11, Long.class, long.class));
        ClassList.add(new ClassItem(13, Float.class, float.class));
        ClassList.add(new ClassItem(14, Double.class, double.class));
        ClassList.add(new ClassItem(15, BigDecimal.class));
        ClassList.add(new ClassItem(16, Date.class, java.sql.Date.class, java.sql.Time.class, java.sql.Timestamp.class));
        ClassList.add(new ClassItem(18, String.class));
        ClassList.add(new ClassItem(19, List.class));
        //20 之前的版本有20，这里空出来是为了兼容旧版本
        ClassList.add(new ClassItem(22, GKeyValuePair.class));
        //21 之前的版本有21，这里空出来是为了兼容旧版本
        ClassList.add(new ClassItem(23, Array.class));
        ClassList.add(new ClassItem(24, Map.class));
        //25 之前的版本有25，这里空出来是为了兼容旧版本
        for (ClassItem item : ClassList) {
            int id = item.getTypeId();
            Class[] types = item.getTypes();
            for (Class c : types) {
                classToClassItemMap.put(c, item);
            }
            indexToClassItemMap.put(id, item);
        }

        //加载自定义的类型，类文件中包含GaeaSerializable注解的就是自定的类型
        String scanType = System.getProperty("gaea.serializer.scantype");
        //单独启动一个线程异步扫描类
        if (scanType != null && scanType.equals("asyn")) {
            Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                    logger.info("(异步)Scan jar files begin to find GaeaSerializable class!");
                    try {
                        LoadCustomType();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    logger.info("(异步)Scan jar files completed!");
                }
            });
            th.start();
        } else {//在主线程中扫描类
            logger.info("(同步)Scan jar files begin to find GaeaSerializable class!");
            try {
                LoadCustomType();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            logger.info("(同步)Scan jar files completed!");
        }
    }

    /**
     * <pre>
     * 加载包含GaeaSerializable的类文件
     * 1.首先检查gaea.serializer.basepakage是否配置了路径，多个路径以;分隔，
     * 如果gaea.serializer.basepakage中有路径，则只扫描这些路径。
     * 2.如果没有设置gaea.serializer.basepakage，则默认扫描当前线程的类加载器AppClassLoader加载的所有jar包或者
     * 目录中的类文件
     * </pre>
     * @throws URISyntaxException
     * @throws IOException
     * @throws MalformedURLException
     * @throws ClassNotFoundException
     */
    private static void LoadCustomType() throws URISyntaxException, IOException, MalformedURLException, ClassNotFoundException {
        ClassScaner cs = new ClassScaner();
        String basePakage = System.getProperty("gaea.serializer.basepakage");
        if (basePakage == null) {
            basePakage = StrHelper.EmptyString;
        }
        Set<Class> classes = cs.scanGaeaSerializableClass(basePakage.split(";"));
        for (Class c : classes) {
            logger.info( "[ " + Thread.currentThread().getName()+" ]"+"Scaning " + c.getPackage().getName() + "." + c.getName());
            try {
                GaeaSerializable ann = (GaeaSerializable) c.getAnnotation(GaeaSerializable.class);
                if (ann != null) {
                    String name = ann.name();
                    //如果注解GaeaSerializable没有设置name属性，则默认为类的名称
                    if (name.equals(StrHelper.EmptyString)) {
                        name = c.getSimpleName();
                    }
                    //TODO renjia 这里没有必要计算出哈希值吧，自增一的就可以
                    int typeId = StrHelper.GetHashcode(name);
                    classToClassItemMap.put(c, new ClassItem(typeId, c));
                    indexToClassItemMap.put(typeId, new ClassItem(typeId, c));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 根据typeId找到对应的类型ClassItem
     * @param typeId
     * @return
     */
    public static Class getClass(int typeId) {
        /*****************兼容之前版本***********************/
        if(typeId==6){
            typeId =5;
        }else if(typeId==20 || typeId == 21){
            typeId = 19;
        }else if(typeId == 25){
            typeId = 24;
        }
        /**************************************************/
        ClassItem ci = indexToClassItemMap.get(typeId);
        if (ci != null) {
            return ci.getType();
        }
        return null;
    }

    /**
     * <pre>
     * 根据Class找到对应的typeId
     * 如果type是集成自Map.class，则都算是Map.class
     * 如果type是继承自List.class，则都算是List.class
     * 如果type是包含注解GaeaSerializable，如果在缓存中找不到，则直接添加到缓存中。
     * </pre>
     * @param type
     * @return
     * @throws DisallowedSerializeException
     */
    public static int getTypeId(Class type) throws DisallowedSerializeException {
        int typeId = 0;
        if (type.isArray()) {
            type = Array.class;
        } else if (Map.class.isAssignableFrom(type)) {
            type = Map.class;
        } else if (List.class.isAssignableFrom(type)) {
            type = List.class;
        }
        ClassItem ci = classToClassItemMap.get(type);
        if (ci != null) {
            typeId = ci.getTypeId();
        } else {
            GaeaSerializable ann = (GaeaSerializable) type.getAnnotation(GaeaSerializable.class);
            if (ann == null) {
                throw new DisallowedSerializeException(type);
            }
            String name = ann.name();
            if (name.equals(StrHelper.EmptyString)) {
                name = type.getSimpleName();
            }
            typeId = StrHelper.GetHashcode(name);
            setTypeMap(type, typeId);
        }
        return typeId;
    }

    /**
     * 将包含GaeaSerializable注解的类直接添加到缓存中
     * @param type 包含GaeaSerializable注解
     * @param typeId 类型对应的唯一索引值
     */
    public static void setTypeMap(Class type, int typeId) {
        ClassItem ci = new ClassItem(typeId, type);
        classToClassItemMap.put(type, ci);
        indexToClassItemMap.put(typeId, ci);
    }
}

/**
 * 类型对象
 */
class ClassItem {

    private Class[] Types;
    private int TypeId;

    public ClassItem(int typeids, Class... types) {
        Types = types;
        TypeId = typeids;
    }

    public Class getType() {
        return Types[0];
    }

    public Class[] getTypes() {
        return Types;
    }

    public int getTypeId() {
        return TypeId;
    }
}
