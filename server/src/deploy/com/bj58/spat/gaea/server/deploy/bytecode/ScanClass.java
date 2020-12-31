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
package com.bj58.spat.gaea.server.deploy.bytecode;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bj58.spat.gaea.server.contract.annotation.AnnotationUtil;
import com.bj58.spat.gaea.server.contract.annotation.HttpPathParameter;
import com.bj58.spat.gaea.server.contract.annotation.HttpRequestMapping;
import com.bj58.spat.gaea.server.contract.annotation.OperationContract;
import com.bj58.spat.gaea.server.contract.annotation.ServiceBehavior;
import com.bj58.spat.gaea.server.contract.annotation.ServiceContract;
import com.bj58.spat.gaea.server.contract.log.ILog;
import com.bj58.spat.gaea.server.contract.log.LogFactory;
import com.bj58.spat.gaea.server.deploy.bytecode.ContractInfo.SessionBean;
import com.bj58.spat.gaea.server.deploy.hotdeploy.DynamicClassLoader;
import com.bj58.spat.gaea.server.util.ClassHelper;
import com.bj58.spat.gaea.server.util.FileHelper;

/**
 * 全局单例加载类
 * 
 * @author Service Platform Architecture Team (spat@58.com)
 */
public class ScanClass {
	
	/**
	 * logger
	 */
	private static ILog logger = LogFactory.getLogger(ScanClass.class);
	
	
	/**
	 * ContractInfo for create proxy
	 */
	private static ContractInfo contractInfo = null;
	
	/**
	 * 存放ServiceContract注解的类
	 */
	private static List<ClassInfo> contractClassInfos = null;
	
	/**
	 * 存放ServiceContract注解的类
	 */
	private static List<ClassInfo> behaviorClassInfos = null;
	
	private static Object lockHelper = new Object();
	
	
	/**
     * <pre>
	 * 找到注解ServiceContract标注的Service接口，以及ServiceBehavior标注的Service实现类。
     * 建立接口和实现类的对应关系
     * 只初始化一次
     * </pre>
	 * @param path
	 * @param classLoader
	 * @return
	 * @throws Exception
	 */
	public static ContractInfo getContractInfo(String path, DynamicClassLoader classLoader) throws Exception {
		if(contractInfo == null) {
			synchronized (lockHelper) {
				if(contractInfo == null) {
					scan(path, classLoader);
				}
			}
		}
		
		return contractInfo;
	}
	
	/**
	 * 
	 * @param path
	 * @param classLoader
	 * @return
	 * @throws Exception
	 */
	public static List<ClassInfo> getContractClassInfos(String path, DynamicClassLoader classLoader) throws Exception {
		if(contractInfo == null) {
			synchronized (lockHelper) {
				if(contractInfo == null) {
					scan(path, classLoader);
				}
			}
		}
		
		return contractClassInfos;
	}
	
	/**
	 * 
	 * @param path
	 * @param classLoader
	 * @return
	 * @throws Exception
	 */
	public static List<ClassInfo> getBehaviorClassInfos(String path, DynamicClassLoader classLoader) throws Exception {
		if(contractInfo == null) {
			synchronized (lockHelper) {
				if(contractInfo == null) {
					scan(path, classLoader);
				}
			}
		}
		
		return behaviorClassInfos;
	}
	
	
	/**
	 * <pre>
	 * 扫描jar包，创建ContractInfo
	 * 找到Service的接口和实现类
	 * Service的接口是使用注解ServiceContract标注
	 * Service的实现类是使用注解ServiceBehavior标注
	 * </pre>
	 * @param path
	 * @param classLoader
	 * @return
	 * @throws Exception
	 */
	private static void scan(String path, DynamicClassLoader classLoader) throws Exception {
		logger.info("begin scan jar to find ServiceContract & ServiceBehavior from path:" + path);

		List<String> jarPathList = FileHelper.getUniqueLibPath(path);

		if(jarPathList == null) {
		    throw new Exception("no jar found from path: " + path);
		}

		contractClassInfos = new ArrayList<ClassInfo>();
		behaviorClassInfos = new ArrayList<ClassInfo>();

		for (String jpath : jarPathList) {
		    Set<Class<?>> clsSet = null;
		    try {
		   	 	clsSet = ClassHelper.getClassFromJar(jpath, classLoader);
		    } catch (Exception ex) {
		   	 	throw ex;
		    }
		    
		    if (clsSet == null) {
		        continue;
		    }
		    
		    for (Class<?> cls : clsSet) {
		        try {
		       	 ServiceBehavior behavior = cls.getAnnotation(ServiceBehavior.class);
		       	 ServiceContract contract = cls.getAnnotation(ServiceContract.class);
		       	 if(behavior == null && contract == null) {
		       		 continue;
		       	 }
		       	 
		       	 if(contract != null) {
		       		 ClassInfo ci = contract(cls);
		       		 if(ci != null) {
		       			 contractClassInfos.add(ci);
		       		 }
		       	 } else if(behavior != null) {
		       		 ClassInfo ci = behavior(cls);
		       		 if(ci != null) {
		       			 behaviorClassInfos.add(ci);
		       		 }
		       	 }
		        } catch (Exception ex) {
		       	 throw ex;
		        }
		    }
		}

		contractInfo = createContractInfo(contractClassInfos, behaviorClassInfos);
		
		logger.info("finish scan jar to find ServiceContract & ServiceBehavior");
	}
	
	
	/**
	 * 
	 * @param cls
	 * @param ignoreAnnotation
	 * @return
	 */
	protected static ClassInfo contract(Class<?> cls, boolean ignoreAnnotation) {
		if(ignoreAnnotation) {
			ClassInfo ci = new ClassInfo();
			ci.setCls(cls);
			ci.setClassType(ClassInfo.ClassType.INTERFACE);

			Method[] methods = cls.getDeclaredMethods();
			List<ClassInfo.MethodInfo> methodInfos = new ArrayList<ClassInfo.MethodInfo>();
			
			 for(Method m : methods) {
				 if(Modifier.isPublic(m.getModifiers()) || Modifier.isProtected(m.getModifiers())) {
					 ClassInfo.MethodInfo mi = new ClassInfo.MethodInfo();
					 mi.setMethod(m);
					 methodInfos.add(mi);
				 }
			 }
			 ci.setMethodList(methodInfos);
			 
			 return ci;
		} else {
			return contract(cls);
		}
	}
	
	/**
	 * <pre>
	 *     1.如果接口的注解ServiceContract的属性defaultAll为true，则将该接口包括所有父接口的方法都算作对外开放的RPC方法
	 *     2.如果接口的注解ServiceContract的属性defaultAll为false，则找到该接口及所有父接口中包含OperationContract注解的方法
	 *     3.不管是哪种情况，都是只允许public和protect两种方法对外开放。
	 * </pre>
	 * @param cls
	 * @return
	 */
	protected static ClassInfo contract(Class<?> cls) {
		ServiceContract contractAnn = cls.getAnnotation(ServiceContract.class);
		
		ClassInfo ci = new ClassInfo();
		ci.setCls(cls);
		ci.setClassType(ClassInfo.ClassType.INTERFACE);

		List<Class<?>> interfaceList = getInterfaces(cls);
		List<ClassInfo.MethodInfo> methodInfos = new ArrayList<ClassInfo.MethodInfo>();

		for(Class<?> interfaceCls : interfaceList) {
			Method[] methods = interfaceCls.getDeclaredMethods();
			if(contractAnn != null && contractAnn.defaultAll()) {
				 for(Method m : methods) {
					 if(Modifier.isPublic(m.getModifiers()) || Modifier.isProtected(m.getModifiers())) {
						 ClassInfo.MethodInfo mi = new ClassInfo.MethodInfo();
						 mi.setMethod(m);
						 methodInfos.add(mi);
					 }
				 }
			} else {
				 for(Method m : methods) {
					 if(Modifier.isPublic(m.getModifiers()) || Modifier.isProtected(m.getModifiers())) {
						 OperationContract oc = m.getAnnotation(OperationContract.class);
						 if(oc != null) {
							 ClassInfo.MethodInfo mi = new ClassInfo.MethodInfo();
							 mi.setMethod(m);
							 methodInfos.add(mi);
						 }
					 }
				 }
			}
		}
		
		ci.setMethodList(methodInfos);

		return ci;
		
	}
	
	
	/**
	 * 
	 * @param cls
	 * @return
	 * @throws Exception
	 */
	protected static ClassInfo behavior(Class<?> cls) throws Exception {
		ServiceBehavior behaviorAnn = cls.getAnnotation(ServiceBehavior.class);

		ClassInfo ci = new ClassInfo();
		ci.setCls(cls);
		ci.setClassType(ClassInfo.ClassType.CLASS);

		if(behaviorAnn != null && !behaviorAnn.lookUP().equalsIgnoreCase(AnnotationUtil.DEFAULT_VALUE)) {
			ci.setLookUP(behaviorAnn.lookUP());
		} else {
			ci.setLookUP(cls.getSimpleName());
		}
		Method[] methods = cls.getDeclaredMethods();
		List<ClassInfo.MethodInfo> methodInfos = new ArrayList<ClassInfo.MethodInfo>();
		logger.info("ScanClass ServiceBehavior class:"+cls.getSimpleName());
		for(Method m : methods) {
			//only load public or protected method
			if(Modifier.isPublic(m.getModifiers()) || Modifier.isProtected(m.getModifiers())) {
				ClassInfo.MethodInfo mi = new ClassInfo.MethodInfo();
				mi.setMethod(m);
				
				HttpRequestMapping requestMappingAnn = m.getAnnotation(HttpRequestMapping.class);
				mi.setHttpRequestMapping(requestMappingAnn);
				//getParameterTypes()与getGenericParameterTypes()的区别是：
				//如果方法的参数不是泛型，则两个获取的数据是一样的，否则是不一样的，
				//与getGenericParameterTypes()方法会获取到泛型的具体类型，例如：
				//public void setIds(List<Integer> ids)
				//这个方法会打印出：java.util.List<java.lang.Integer>，而getParameterTypes()方法只会打印出java.util.List
				Class<?>[] paramAry = m.getParameterTypes();
				Type[] types = m.getGenericParameterTypes();
				String[] paramNames = ClassHelper.getParamNames(cls, m);
				String[] mapping = new String[paramAry.length]; 
				HttpPathParameter[] paramAnnAry = new HttpPathParameter[paramAry.length];

				//load RequestMapping
				if(requestMappingAnn != null) {
					Object[][] annotations = ClassHelper.getParamAnnotations(cls, m);
					for(int i=0; i<annotations.length; i++) {
						for(int j=0; j<annotations[i].length; j++) {
							HttpPathParameter paramAnn = null;
							try {
								paramAnn = (HttpPathParameter)annotations[i][j];
							} catch(Exception ex) {
								
							}
							
							paramAnnAry[i] = paramAnn;
							if(paramAnn != null) {
								mapping[i] = paramAnn.mapping();
								break;
							} else {
								mapping[i] = paramNames[i];
							}
						}
					}
					
					for(int i=0; i<paramAry.length; i++) {
						if(mapping[i] == null) {
							mapping[i] = paramNames[i];
						}
					}
				}
				
				ClassInfo.ParamInfo[] paramInfoAry = new ClassInfo.ParamInfo[paramAry.length];
				for(int i=0; i<paramAry.length; i++) {
					paramInfoAry[i] = new ClassInfo.ParamInfo(i, 
							paramAry[i],     // Class
							types[i],        // Type
							paramNames[i],   // 参数名称
							mapping[i],      // HttpRequestMapping注解
							paramAnnAry[i]); // HttpPathParameter注解
				}
				mi.setParamInfoAry(paramInfoAry);
				
				methodInfos.add(mi);
			}
		}
		ci.setMethodList(methodInfos);

		return ci;
	}
	
	/**
	 * <pre>
	 * create ContractInfo from contracts, behaviors
	 * 建立接口和实现类之间的对应关系，对应关系如下：
	 * --------------------------------------------
	 * Interface1......ServiceImpl1
	 *                 ServiceImpl2
	 * --------------------------------------------
	 * Interface2......ServiceImpl1
	 *                 ServiceImpl3
	 *                 ServiceImpl4
	 * --------------------------------------------
	 * </pre>
	 * @param contracts
	 * @param behaviors
	 * @return
	 */
	private static ContractInfo createContractInfo(List<ClassInfo> contracts,
												   List<ClassInfo> behaviors) {
		
		ContractInfo contractInfo = new ContractInfo();
		List<SessionBean> sessionBeanList = new ArrayList<SessionBean>();
		for(ClassInfo c : contracts) {
			SessionBean bean = new SessionBean();
			bean.setInterfaceClass(c);
			bean.setInterfaceName(c.getCls().getName());
			Map<String, String> implMap = new HashMap<String, String>();
			
			for(ClassInfo b : behaviors) {
				Class<?>[] interfaceAry = b.getCls().getInterfaces();
				for(Class<?> item : interfaceAry) {
					if(item == c.getCls()) {
						//TODO renjia 这里使用lookUP感觉会出现重复的情况
						implMap.put(b.getLookUP(), b.getCls().getName());
						break;
					}
				}
			}
			bean.setInstanceMap(implMap);
			sessionBeanList.add(bean);
		}
		
		contractInfo.setSessionBeanList(sessionBeanList);
		return contractInfo;
	}
	
	/**
	 * get all interfaces
	 * @param cls
	 * @return
	 */
	private static List<Class<?>> getInterfaces(Class<?> cls) {
		List<Class<?>> clsList = new ArrayList<Class<?>>();
		getInterfaces(cls, clsList);
		return clsList;
	}
	
	/**
	 * get all interfaces
	 * 递归查找所有的接口
	 * @param cls
	 * @param clsList
	 */
	private static void getInterfaces(Class<?> cls, List<Class<?>> clsList) {
		clsList.add(cls);
		Class<?>[] aryCls = cls.getInterfaces();
		
		if(aryCls != null && aryCls.length > 0) {
			for(Class<?> c : aryCls) {
				getInterfaces(c, clsList);
			}
		}
	}
}