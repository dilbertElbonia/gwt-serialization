/*
 * Copyright 2013 monkeyboy
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.googlecode.serialization;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.rebind.rpc.BlacklistFilter;
import com.google.gwt.user.rebind.rpc.SerializableTypeOracle;
import com.google.gwt.user.rebind.rpc.SerializableTypeOracleBuilder;
import com.google.gwt.user.rebind.rpc.TypeSerializerCreator;
import com.googlecode.gwt.serialization.ExtendWith;
import com.googlecode.gwt.serialization.JsonSerializationFactory;

import java.io.PrintWriter;

/**
 * User: monkeyboy
 */
public class JsonSerializationGenerator extends Generator {
    private static final String INTERFACE_NAME = JsonSerializationFactory.class.getCanonicalName();
    private static final String EXTEND_INTERFACE_NAME = ExtendWith.class.getCanonicalName();
    private JClassType factoryType;
    private JClassType type;
    private JClassType whiteListType;
    private JClassType[] typeParameters;

    @Override
    public String generate(
            final TreeLogger logger,
            final GeneratorContext context,
            final String typeName) throws UnableToCompleteException {
        factoryType = null;
        type = null;
        whiteListType = null;
        typeParameters = null;
        //logger.log(TreeLogger.Type.WARN, "typeName:" + typeName);
        final TypeOracle typeOracle = context.getTypeOracle();
        assert typeOracle != null;
        validateTypes(logger, typeOracle, typeName);
        final String packageName = factoryType.getPackage().getName();
        //logger.log(TreeLogger.Type.WARN, "packageName:" + packageName);

        final PropertyOracle propertyOracle = context.getPropertyOracle();

        // Debugging
//        logger.log(TreeLogger.Type.WARN, "Logging blacklist:");
//        try {
//            final ConfigurationProperty prop = propertyOracle.getConfigurationProperty("rpc.blacklist");
//            for (String value : prop.getValues()) {
//                logger.log(TreeLogger.Type.WARN, "Blacklist:" + value);
//            }
//        } catch (BadPropertyValueException e) {
//            logger.log(TreeLogger.Type.ERROR, "Could not find property rpc.blacklist");
//            throw new UnableToCompleteException();
//        }
        // end Debugging

        // Load the blacklist/whitelist
        final BlacklistFilter blacklistTypeFilter = new BlacklistFilter(logger, propertyOracle);

        final SerializableTypeOracleBuilder typesSentFromBrowserBuilder =
                new SerializableTypeOracleBuilder(logger, propertyOracle, context);
        typesSentFromBrowserBuilder.setTypeFilter(blacklistTypeFilter);
        final SerializableTypeOracleBuilder typesSentToBrowserBuilder =
                new SerializableTypeOracleBuilder(logger, propertyOracle, context);
        typesSentToBrowserBuilder.setTypeFilter(blacklistTypeFilter);

        addRoots(logger, typeOracle, typesSentFromBrowserBuilder, typesSentToBrowserBuilder);

        final SerializableTypeOracle typesSentFromBrowser = typesSentFromBrowserBuilder.build(logger);
        final SerializableTypeOracle typesSentToBrowser = typesSentToBrowserBuilder.build(logger);

        final String typeNameParam;
        if (typeParameters != null && typeParameters.length > 0) {
            final StringBuilder sb = new StringBuilder();
            sb.append(type.getName()).append("<");
            for (JClassType typeParameter : typeParameters) {
                sb.append(typeParameter.getName()).append(",");
            }
            sb.delete(sb.length() - 1, sb.length());
            sb.append(">");
            typeNameParam = sb.toString();
        } else {
            typeNameParam = type.getName();
        }
        final String typeNameParamInline = typeNameParam.replace(",", "_").replace("<", "_").replace(">", "");

        final String jsonTypeSerializer = typeNameParamInline + "_JsonTypeSerializer";
        final TypeSerializerCreator tsc =
                new TypeSerializerCreator(logger, typesSentFromBrowser, typesSentToBrowser, context,
                        packageName + "." + jsonTypeSerializer, jsonTypeSerializer);
        tsc.realize(logger);

        final String jsonFactoryName = factoryType.getName().replace('.', '_') + "_AutogeneratedImpl";
        final PrintWriter printWriter = context.tryCreate(logger, packageName, jsonFactoryName);

        if (printWriter != null) {
            printWriter.append("package ").append(packageName).append(";\n");
            printWriter.append("import com.google.gwt.user.client.rpc.impl.Serializer;\n");
            printWriter.append("import com.googlecode.gwt.serialization.JsonReader;\n");
            printWriter.append("import com.googlecode.gwt.serialization.JsonReaderImpl;\n");
            printWriter.append("import com.googlecode.gwt.serialization.JsonSerializationFactory;\n");
            printWriter.append("import com.googlecode.gwt.serialization.JsonWriter;\n");
            printWriter.append("import com.googlecode.gwt.serialization.JsonWriterImpl;\n");
            printWriter.append("import ").append(type.getQualifiedSourceName()).append(";\n");
            if (typeParameters != null && typeParameters.length > 0) {
                for (JClassType typeParameter : typeParameters) {
                    printWriter.append("import ").append(typeParameter.getQualifiedSourceName()).append(";\n");
                }
            }
            printWriter.append("import ").append(packageName).append(".").append(jsonTypeSerializer).append(";\n\n");

            printWriter.append("public class ").append(jsonFactoryName).append(" implements ").append(factoryType.getQualifiedSourceName()).append(" {\n");
            printWriter.append("  private final Serializer serializer = new ").append(jsonTypeSerializer).append("();\n");
            printWriter.append("  private final JsonReaderImpl<").append(typeNameParam).append("> reader = new JsonReaderImpl<").append(typeNameParam).append(">(serializer);\n");
            printWriter.append("  private final JsonWriterImpl<").append(typeNameParam).append("> writer = new JsonWriterImpl<").append(typeNameParam).append(">(serializer);\n\n");

            printWriter.append("  @Override\n");
            printWriter.append("  public JsonReader<").append(typeNameParam).append("> getReader() {\n");
            printWriter.append("    return reader;\n");
            printWriter.append("  }\n\n");

            printWriter.append("  @Override\n");
            printWriter.append("  public JsonWriter<").append(typeNameParam).append("> getWriter() {\n");
            printWriter.append("    return writer;\n");
            printWriter.append("  }\n");
            printWriter.append("}\n");

            context.commit(logger, printWriter);
        }
        return packageName + "." + jsonFactoryName;
    }

    private void validateTypes(
            final TreeLogger logger,
            final TypeOracle typeOracle,
            final String typeName) throws UnableToCompleteException {

        final JClassType interfaceType = typeOracle.findType(INTERFACE_NAME);
        if (interfaceType == null) {
            logger.log(TreeLogger.Type.ERROR, "Unable to find metadata for type " + INTERFACE_NAME);
            throw new UnableToCompleteException();
        }

        final JClassType extendInterfaceType = typeOracle.findType(EXTEND_INTERFACE_NAME);
        if (extendInterfaceType == null) {
            logger.log(TreeLogger.Type.ERROR, "Unable to find metadata for type " + EXTEND_INTERFACE_NAME);
            throw new UnableToCompleteException();
        }

        factoryType = typeOracle.findType(typeName);
        if (factoryType == null) {
            logger.log(TreeLogger.Type.ERROR, "Unable to find metadata for type " + typeName);
            throw new UnableToCompleteException();
        }

        if (interfaceType == factoryType) {
            logger.log(TreeLogger.Type.ERROR,
                    "You must use a subtype of " + interfaceType.getSimpleSourceName() + " in GWT.create(). E.g.,\n" +
                            "  interface ModelReader extends " + interfaceType.getSimpleSourceName() + "<Model> {}\n" +
                            "  ModelReader reader = GWT.create(ModelReader.class);");
            throw new UnableToCompleteException();
        }

        final JClassType[] implementedInterfaces = factoryType.getImplementedInterfaces();
        if (implementedInterfaces.length == 0) {
            logger.log(TreeLogger.Type.ERROR, "No implemented interfaces for " + factoryType.getSimpleSourceName());
        }

        // Check type parameter(s)
        for (JClassType t : implementedInterfaces) {
            //logger.log(TreeLogger.Type.WARN, "t:" + t.getQualifiedSourceName());
            //logger.log(TreeLogger.Type.WARN, "interfaceType:" + interfaceType.getQualifiedSourceName());
            if (t.getQualifiedSourceName().equals(interfaceType.getQualifiedSourceName())) {
                final JClassType[] typeArgs = t.isParameterized().getTypeArgs();
                if (typeArgs.length != 1) {
                    logger.log(TreeLogger.Type.WARN, "One type parameter is required for " + t.getName());
                    throw new UnableToCompleteException();
                }
                type = typeArgs[0];
            } else if (t.getQualifiedSourceName().equals(extendInterfaceType.getQualifiedSourceName())) {
                final JClassType[] typeArgs = t.isParameterized().getTypeArgs();
                if (typeArgs.length != 1) {
                    logger.log(TreeLogger.Type.WARN, "One type parameter is required for " + t.getName());
                    throw new UnableToCompleteException();
                }
                whiteListType = typeArgs[0];
            }
        }

        if (type == null) {
            logger.log(TreeLogger.Type.WARN, "No type parameter found in " + implementedInterfaces);
            throw new UnableToCompleteException();
        }

        final JParameterizedType parameterizedType = type.isParameterized();
        if (parameterizedType != null) {
            typeParameters = parameterizedType.getTypeArgs();
        }
    }

    private void addRoots(
            final TreeLogger logger,
            final TypeOracle typeOracle,
            final SerializableTypeOracleBuilder typesSentFromBrowserBuilder,
            final SerializableTypeOracleBuilder typesSentToBrowserBuilder) throws UnableToCompleteException {
        try {
            addRequiredRoots(logger, typeOracle, typesSentFromBrowserBuilder);
            addRequiredRoots(logger, typeOracle, typesSentToBrowserBuilder);

            typesSentFromBrowserBuilder.addRootType(logger, type);
            typesSentToBrowserBuilder.addRootType(logger, type);

            if (typeParameters != null && typeParameters.length > 0) {
                for (JClassType typeParameter : typeParameters) {
                    typesSentFromBrowserBuilder.addRootType(logger, typeParameter);
                    typesSentToBrowserBuilder.addRootType(logger, typeParameter);
                }
            }

            if (whiteListType != null) {
                typesSentFromBrowserBuilder.addRootType(logger, whiteListType);
                typesSentToBrowserBuilder.addRootType(logger, whiteListType);
            }
        } catch (NotFoundException e) {
            logger.log(TreeLogger.ERROR, "Unable to find type referenced from remote service", e);
            throw new UnableToCompleteException();
        }
    }

    private static void addRequiredRoots(
            TreeLogger logger,
            final TypeOracle typeOracle,
            final SerializableTypeOracleBuilder stob) throws NotFoundException {
        logger = logger.branch(TreeLogger.DEBUG, "Analyzing implicit types");

        // String is always instantiable.
        final JClassType stringType = typeOracle.getType(String.class.getName());
        stob.addRootType(logger, stringType);

        // IncompatibleRemoteServiceException is always serializable
        final JClassType icseType = typeOracle.getType(IncompatibleRemoteServiceException.class.getName());
        stob.addRootType(logger, icseType);
    }
}
