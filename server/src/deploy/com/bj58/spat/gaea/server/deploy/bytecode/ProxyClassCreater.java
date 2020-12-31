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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;

import com.bj58.spat.gaea.server.contract.log.ILog;
import com.bj58.spat.gaea.server.contract.log.LogFactory;
import com.bj58.spat.gaea.server.deploy.bytecode.ClassInfo.MethodInfo;
import com.bj58.spat.gaea.server.deploy.hotdeploy.DynamicClassLoader;
import com.bj58.spat.gaea.server.util.Util;

/**
 * TODO renjia 整个类的所有方法使用static更合适，不用new
 * @author Service Platform Architecture Team (spat@58.com)
 */
public class ProxyClassCreater {
	
	private static ILog logger = LogFactory.getLogger(ProxyClassCreater.class);
	
	@SuppressWarnings("rawtypes")
	public List<ClassFile> createProxy(DynamicClassLoader classLoader, 
									   ContractInfo serviceContract, 
									   long time) throws Exception {
		
		logger.info("Loading dynamic proxy v1...");
		List<ClassFile> clsList = new ArrayList<ClassFile>();
		//SessionBean中存放着接口和实现类的对应关系
		for (ContractInfo.SessionBean sessionBean : serviceContract.getSessionBeanList()) {
			ClassInfo interfaceClass = sessionBean.getInterfaceClass();
			if(interfaceClass != null) {
				Iterator it = sessionBean.getInstanceMap().entrySet().iterator(); // lookup --> implClassName
				while (it.hasNext()) {//循环每个实现类
					Map.Entry entry = (Map.Entry) it.next();
					String lookup = entry.getKey().toString();           //实现类的lookUP
					String implClassName = entry.getValue().toString();  //实现类的类名
					String proxyClassName = lookup + "ProxyStub" + time; //代理类的类名 = `lookUP` + ProxyStub + `time`
					logger.info("Loading ProxyClass Name: " + proxyClassName
							  + ",ServiceImplClass name:" + implClassName
							  + ",ServiceInterface name:" + interfaceClass.getCls().getName());
					//ClassPool对象是一个CtClass对象的容器
					ClassPool pool = ClassPool.getDefault();
					
					List<String> jarList = classLoader.getJarList();
					for(String jar : jarList) {
						pool.appendClassPath(jar);
					}
					//创建CtClass对象，它会被记录在ClassPool中,CtClass == Compile-Time Class
					//proxyClassName是完整包名的类名，例如：com.kongzhong.Test，如果该类已经存在，则会替换之前的类。
					//如果一个CtClass对象通过writeFile()，toClass()或者toBytecode()转换成了class文件，那么Javassist会冻结这个CtClass对象。
					//这样是为了警告开发者不要修改已经被JVM加载的class文件，因为JVM不允许重新加载一个类。
					//判断是否冻结ctClass.isFrozen()，解冻方法ctClass.defrost()，解冻之后可以再次被修改。
					CtClass ctProxyClass = pool.makeClass(proxyClassName, null);

					//如果ClassPool.doPruning被设置成true，那么Javassist会在冻结一个对象的时候对这个对象进行精简，
					//这是为了减少ClassPool的内存占用，精简的时候会丢弃class中不需要的属性。
					ctProxyClass.stopPruning(true);
					//加载已经存在的接口com.bj58.spat.gaea.server.contract.context.IProxyStub
					CtClass proxyStubInterface = pool.getCtClass(Constant.IPROXYSTUB_CLASS_NAME);
					ctProxyClass.addInterface(proxyStubInterface);
					
					//创建代理类的成员
					CtField proxyField = CtField.make("private static " +
														sessionBean.getInterfaceName() +
														" serviceProxy = new " +
														implClassName +
														"();", ctProxyClass);
					ctProxyClass.addField(proxyField);

					List<MethodInfo> methodList = interfaceClass.getMethodList();
					Method[] methodAry = new Method[methodList.size()];
					for(int i=0; i<methodList.size(); i++) {
						methodAry[i] = methodList.get(i).getMethod();
					}
					//TODO renjia 去重为什么不用Set?
					List<String> uniqueNameList = new ArrayList<String>();   //去重后的方法名列表
					List<Method> uniqueMethodList = new ArrayList<Method>(); //去重后的方法列表
					List<Method> allMethodList = new ArrayList<Method>();    //未去重的所有的方法列表
					for (Method m : methodAry) {
						if(Modifier.isPublic(m.getModifiers()) || Modifier.isProtected(m.getModifiers())){
							if(!uniqueNameList.contains(m.getName())){
								uniqueNameList.add(m.getName());
								uniqueMethodList.add(m);
							}
							allMethodList.add(m);
						}
					}
	
					//创建代理类的方法
                    //TODO renjia 这里用唯一的方法名称，那重载的方法就不支持了？
					for(Method m : uniqueMethodList) {
						logger.debug("Create service method of ProxyClass:" + m.getName());
						String methodStr = createMethods(proxyClassName, m.getName(), allMethodList, uniqueNameList);
						logger.debug("Service method name of ProxyClass :"+m.getName()+" , source code:"+methodStr);
						CtMethod methodItem = CtMethod.make(methodStr, ctProxyClass);
						ctProxyClass.addMethod(methodItem);
					}					
					
					//invoke
					String invokeMethod = createInvoke(proxyClassName, uniqueNameList);
					logger.debug("Create invoke method source code:" + invokeMethod);
					CtMethod invoke = CtMethod.make(invokeMethod, ctProxyClass);
					ctProxyClass.addMethod(invoke);
	
					clsList.add(new ClassFile(proxyClassName, ctProxyClass.toBytecode()));
				}
			}
		}
		logger.info("Load dynamic proxy v1 success!!!");
		return clsList;
	}
	

	/**
	 * ceate invoke method
	 * @param uniqueNameList
	 * @return
	 */
	private String createInvoke(String className, List<String> uniqueNameList) {
		StringBuilder sb = new StringBuilder();
		sb.append("public " + Constant.GAEARESPONSE_CLASS_NAME + " invoke(" + Constant.GAEACONTEXT_CLASS_NAME + " context) throws " + Constant.SERVICEFRAMEEXCEPTION_CLASS_NAME + " {");
		sb.append("String methodName = ((" + Constant.REQUEST_PROTOCOL_CLASS_NAME + ")context.getGaeaRequest().getProtocol().getSdpEntity()).getMethodName();");
		for (String methodName : uniqueNameList) {
			sb.append("if(methodName.equalsIgnoreCase(\"");
			sb.append(methodName);
			sb.append("\")){return ");
			sb.append(methodName);
			sb.append("(context);}");
		}
		sb.append("throw new " + Constant.SERVICEFRAMEEXCEPTION_CLASS_NAME + "(\"method:" + className + ".invoke--msg:not found method (\"+methodName+\")\", context.getChannel().getRemoteIP(), context.getChannel().getLocalIP(), context.getGaeaRequest().getProtocol().getSdpEntity(), " + Constant.ERRORSTATE_CLASS_NAME + ".NotFoundMethodException, null);");
		sb.append("}");
		return sb.toString();
	}
	
	
	/**
	 * create service method
	 * @param methodName
	 * @param methodList
	 * @param uniqueNameList
	 * @return
	 */
	public String createMethods(String className, String methodName, List<Method> methodList, List<String> uniqueNameList) {
		StringBuilder sb = new StringBuilder();
		sb.append("public " + Constant.GAEARESPONSE_CLASS_NAME + " "); //设置方法的返回值为：com.bj58.spat.gaea.server.contract.context.GaeaResponse
		sb.append(methodName);
		sb.append("(" + Constant.GAEACONTEXT_CLASS_NAME + " context) throws " + Constant.SERVICEFRAMEEXCEPTION_CLASS_NAME + " {"); //设置方法的参数为：com.bj58.spat.gaea.server.contract.context.GaeaContext
        //com.bj58.spat.gaea.server.core.convert.ConvertFacotry根据从context中获取到的Protocol，并
        //根据Protocol的SerializeType属性来得到com.bj58.spat.gaea.server.core.convert.IConvert
		sb.append(Constant.ICONVERT_CLASS_NAME + " convert = " + Constant.CONVERT_FACTORY_CLASS_NAME + ".getConvert(context.getGaeaRequest().getProtocol());");
		//从context中的Protocol中得到com.bj58.spat.gaea.protocol.sdp.RequestProtocol
		sb.append(Constant.REQUEST_PROTOCOL_CLASS_NAME + " request = (" + Constant.REQUEST_PROTOCOL_CLASS_NAME + ")context.getGaeaRequest().getProtocol().getSdpEntity();");
		//从request中到参数列表
		sb.append("java.util.List listKV = request.getParaKVList();");
		//TODO renjia 从方法列表中找到方法，这个不能提前找到吗？
		for(Method m : methodList){
			if(m.getName().equalsIgnoreCase(methodName)){
				
				Class<?>[] mType = m.getParameterTypes();
				Type[] mGenericType = m.getGenericParameterTypes();

				sb.append("if(listKV.size() == " + mGenericType.length);
				for(int i=0; i<mGenericType.length; i++) {
					String paraName = mGenericType[i].toString().replaceFirst("(class)|(interface) ", "");
					paraName = paraName.replaceAll("java.util.", "").replaceAll("java.lang.", "");
					//TODO renjia 这一段没看明白是要干啥？
					if(paraName.startsWith(Constant.OUT_PARAM)){
						paraName = paraName.replaceAll(Constant.OUT_PARAM+"<", "");
						paraName = paraName.substring(0, paraName.length() - 1);
						paraName = paraName.replaceAll("\\<.*\\>", "");
					}
					if(paraName.trim().startsWith("[")) {
						paraName = mType[i].getCanonicalName();
					}

					paraName = Util.getSimpleParaName(paraName);
					
					sb.append(" && (");
					sb.append("((" + Constant.KEYVALUEPAIR_PROTOCOL_CLASS_NAME + ")listKV.get(");
					sb.append(i);
					sb.append(")).getKey().toString().equalsIgnoreCase(\"");
					sb.append(paraName);
					sb.append("\")");

					if(paraName.indexOf("int")>=0) {
						sb.append("|| ((" + Constant.KEYVALUEPAIR_PROTOCOL_CLASS_NAME + ")listKV.get(");
						sb.append(i);
						sb.append(")).getKey().toString().equalsIgnoreCase(\"" + paraName.replaceAll("int", "Integer") + "\")");
					} else if(paraName.indexOf("Integer")>=0) {
						sb.append("|| ((" + Constant.KEYVALUEPAIR_PROTOCOL_CLASS_NAME + ")listKV.get(");
						sb.append(i);
						sb.append(")).getKey().toString().equalsIgnoreCase(\"" + paraName.replaceAll("Integer", "int") + "\")");
					} else if(paraName.indexOf("char")>=0) {
						sb.append("|| ((" + Constant.KEYVALUEPAIR_PROTOCOL_CLASS_NAME + ")listKV.get(");
						sb.append(i);
						sb.append(")).getKey().toString().equalsIgnoreCase(\"" + paraName.replaceAll("char", "Character") + "\")");
					} else if(paraName.indexOf("Character")>=0) {
						sb.append("|| ((" + Constant.KEYVALUEPAIR_PROTOCOL_CLASS_NAME + ")listKV.get(");
						sb.append(i);
						sb.append(")).getKey().toString().equalsIgnoreCase(\"" + paraName.replaceAll("Character", "char") + "\")");
					}
					
					
					sb.append(")");
					
				}
				sb.append("){");
				
				sb.append("java.util.List listOutPara = new java.util.ArrayList();");

				//define para
				for(int i=0; i<mGenericType.length;i++){
					String paraName = mGenericType[i].toString().replaceFirst("(class)|(interface) ", "");
					boolean isOutPara = false;
					if(paraName.startsWith(Constant.OUT_PARAM)){
						isOutPara = true;
					}
					
					if(paraName.trim().startsWith("[")){
						paraName = mType[i].getCanonicalName();
					}
					if(isOutPara){
						sb.append(Constant.OUT_PARAM + " arg" + i);
						sb.append("= new " + Constant.OUT_PARAM);
						sb.append("();");
					} else {
						sb.append(paraName.replaceAll("\\<.*\\>", ""));
						sb.append(" arg" + i);
					}
					
					if(!isOutPara) {
						paraName = paraName.replaceAll("java.util.", "").replaceAll("java.lang.", "");
						
						if(paraName.equals("long")){
							sb.append(" = 0L;");
						} else if(paraName.equals("float")){
							sb.append(" = 0F;");
						} else if(paraName.equals("double")){
							sb.append(" = 0D;");
						} else if(paraName.equals("int")){
							sb.append(" = 0;");
						} else if(paraName.equals("short")){
							sb.append(" = (short)0;");
						} else if(paraName.equals("byte")){
							sb.append(" = (byte)0;");
						} else if(paraName.equals("boolean")){
							sb.append(" = false;");
						} else if(paraName.equals("char")){
							sb.append(" = (char)'\\0';");
						}
						
						else if(paraName.equals("Long")){
							sb.append(" = new Long(\"0\");");
						} else if(paraName.equals("Float")){
							sb.append(" = new Float(\"0\");");
						} else if(paraName.equals("Double")){
							sb.append(" = new Double(\"0\");");
						} else if(paraName.equals("Integer")){
							sb.append(" = new Integer(\"0\");");
						} else if(paraName.equals("Short")){
							sb.append(" = new Short(\"0\");");
						} else if(paraName.equals("Byte")){
							sb.append(" = new Byte(\"0\");");
						} else if(paraName.equals("Boolean")){
							sb.append(" = new Boolean(\"false\");");
						} else if(paraName.equals("Character")){
							sb.append(" = new Character((char)'\\0');");
						} else{
							sb.append(" = null;");
						}
					}
					if(isOutPara){
						sb.append("listOutPara.add(arg"+i+");");
					}
				}
				
				//set value to para
				if(mGenericType.length > 0) {
					sb.append("try {");
				}
				for(int i=0; i<mGenericType.length;i++){
					String paraName = mGenericType[i].toString().replaceFirst("(class)|(interface) ", "");
					boolean isOutPara = false;
					if(paraName.startsWith(Constant.OUT_PARAM)){
						isOutPara = true;
					}
					
					if(paraName.trim().startsWith("[")){
						paraName = mType[i].getCanonicalName();
					}
					//去掉类型前的包名，例如：java.lang.Integer=>Integer，java.util.Date=>Date
					String pn = paraName.replaceAll("java.util.", "").replaceAll("java.lang.", "");
					if(!isOutPara){
						if (pn.equalsIgnoreCase("String")
								|| pn.equalsIgnoreCase("int") || pn.equalsIgnoreCase("Integer")
								|| pn.equalsIgnoreCase("long")
								|| pn.equalsIgnoreCase("short")
								|| pn.equalsIgnoreCase("float")
								|| pn.equalsIgnoreCase("boolean")
								|| pn.equalsIgnoreCase("double")
								|| pn.equalsIgnoreCase("char") || pn.equalsIgnoreCase("Character")
								|| pn.equalsIgnoreCase("byte")){
							
							sb.append("arg"+i);
							sb.append(" = convert.convertTo"+pn+"(((" + Constant.KEYVALUEPAIR_PROTOCOL_CLASS_NAME + ")listKV.get("+i+")).getValue());");
						} else {
							sb.append("arg"+i);
							sb.append(" = ("+paraName.replaceAll("\\<.*\\>", "")+")convert.convertToT(((" + Constant.KEYVALUEPAIR_PROTOCOL_CLASS_NAME + ")listKV.get("+i+")).getValue(), ");
							sb.append(paraName.replaceAll("<.*?>", "") + ".class");
							
							if(paraName.indexOf("java.util.List") >=0 ||
									paraName.indexOf("java.util.ArrayList") >=0 ||
									paraName.indexOf("java.util.Vector") >=0 ||
									paraName.indexOf("java.util.Set") >=0 ||
									paraName.indexOf("java.util.HashSet") >=0 ) {
								
								sb.append(", ");
								sb.append(paraName.replaceAll("java.util.List<", "")
												  .replaceAll("java.util.ArrayList<", "")
												  .replaceAll("java.util.Vector<", "")
												  .replaceAll("java.util.Set<", "")
												  .replaceAll("java.util.HashSet<", "")
												  .replaceAll(">", "")
								);
								sb.append(".class");
							}
							sb.append(");");
						}
					} else {
						String outType = paraName.replaceAll(Constant.OUT_PARAM + "<", "");
						outType = outType.substring(0, outType.length() - 1);
						
						String outpn = outType.replaceAll("java.util.", "")
						   					  .replaceAll("java.lang.", "");
						
						if (outpn.equalsIgnoreCase("String")
								|| outpn.equalsIgnoreCase("Integer")
								|| outpn.equalsIgnoreCase("Long")
								|| outpn.equalsIgnoreCase("Short")
								|| outpn.equalsIgnoreCase("Float")
								|| outpn.equalsIgnoreCase("Boolean")
								|| outpn.equalsIgnoreCase("Double")
								|| outpn.equalsIgnoreCase("Character")
								|| outpn.equalsIgnoreCase("Byte")){
							
							sb.append("arg"+i);
							sb.append(".setOutPara(convert.convertTo"+outpn+"(((" + Constant.KEYVALUEPAIR_PROTOCOL_CLASS_NAME + ")listKV.get("+i+")).getValue()));");
						} else {
							sb.append("arg"+i);
							sb.append(".setOutPara(("+outType+")convert.convertToT(((" + Constant.KEYVALUEPAIR_PROTOCOL_CLASS_NAME + ")listKV.get("+i+")).getValue(), "+outType+".class");
							
							if(outType.indexOf("java.util.List") >=0 ||
									outType.indexOf("java.util.ArrayList") >=0 ||
									outType.indexOf("java.util.Vector") >=0 ||
									outType.indexOf("java.util.Set") >=0 ||
									outType.indexOf("java.util.HashSet") >=0 ) {
								
								sb.append(", ");
								sb.append(outType.replaceAll("java.util.List<", "")
												 .replaceAll("java.util.ArrayList<", "")
												 .replaceAll("java.util.Vector<", "")
												 .replaceAll("java.util.Set<", "")
												 .replaceAll("java.util.HashSet<", "")
												 .replaceAll(">", "")
								);
								sb.append(".class");
							}
							sb.append("));");
						}
					}
				}
				
				if(mGenericType.length > 0) {
					sb.append("} catch (Exception e) {");
					sb.append("throw new " + Constant.SERVICEFRAMEEXCEPTION_CLASS_NAME + "(\"method:" + className + "." + methodName + "--msg:parse gaeaRequest error\", context.getChannel().getRemoteIP(), context.getChannel().getLocalIP(), context.getGaeaRequest().getProtocol().getSdpEntity(), " + Constant.ERRORSTATE_CLASS_NAME + ".ParaException, e);");
					sb.append("}");
				}
				
				//define returnValue
				Class<?> classReturn = m.getReturnType();
				Type typeReturn = m.getGenericReturnType();
				String returnValueType = typeReturn.toString().replaceFirst("(class)|(interface) ", "");
				if(returnValueType.trim().startsWith("[")){
					returnValueType = classReturn.getCanonicalName();
				}
				 
				if(!returnValueType.equalsIgnoreCase("void")) {
					sb.append(returnValueType.replaceAll("\\<.*\\>", "") + " returnValue = ");
				} 

				sb.append("serviceProxy.");
				sb.append(m.getName());
				sb.append("(");
				//method para
				for(int i=0; i<mGenericType.length;i++) {
					sb.append("arg");
					sb.append(i);
					if(i != mGenericType.length-1) {
						sb.append(", ");
					}
				}
				sb.append(");");
				
				if(!returnValueType.equalsIgnoreCase("void")) {
					sb.append("return new " + Constant.GAEARESPONSE_CLASS_NAME + "(returnValue");
				} else {
					sb.append("return new " + Constant.GAEARESPONSE_CLASS_NAME + "(null");
				}
				
				
				sb.append(", listOutPara);");
				sb.append("}");
				//end if
			}
		}
		sb.append("throw new " + Constant.SERVICEFRAMEEXCEPTION_CLASS_NAME + "(\"method:" + className + "." + methodName + "--msg:not fond method error\", context.getChannel().getRemoteIP(), context.getChannel().getLocalIP(), context.getGaeaRequest().getProtocol().getSdpEntity(), " + Constant.ERRORSTATE_CLASS_NAME + ".NotFoundMethodException, null);");
		sb.append("}");
		return sb.toString();
	}

}