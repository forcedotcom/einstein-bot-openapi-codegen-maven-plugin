
/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.openapi.codegen;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import io.swagger.v3.oas.models.media.Schema;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.languages.JavaClientCodegen;

/**
 * EinsteinBotCodeGenerator - OpenApi Custom code generator for Chatbot Runtime OpenApi
 * Specification.
 * <p>
 * This extends standard Java code generator to Support polymorphic definitions used in Einstein Bot
 * Runtime Api.
 *
 * @author relango
 */
public class EinsteinBotCodeGenerator extends JavaClientCodegen implements CodegenConfig {

  public static final String POLYMORPHIC_INTERFACE = "polymorphicInterface";
  public static final String CODE_GENERATOR_NAME = "einsteinbot";
  public static final String ANY_OF_PREFIX = "AnyOf";
  public static final String ONE_OF_PREFIX = "OneOf";
  public static final String EINSTEIN_BOT_TEMPLATE_DIR = "einsteinbot/Java";
  private static final String PROPERTY_KEY_EXCLUDE_MODELS_IMPLEMENTS_POLYMORPHIC_INTERFACE = "ExcludeModelsImplementsPolymorphicInterface";
  private static final String PROPERTY_EXCLUDE_MODELS_IMPLEMENTS_POLYMORPHIC_INTERFACE_DELIMITER_REGEX = "\\|";

  private Map<String, String> customTypeMapping;
  private List<String> excludeModelsImplementsPolymorphicInterface = Collections.emptyList();

  public EinsteinBotCodeGenerator() {
    super();
    //This dir contains only the customized template.
    //But the base class code generator many templates which are simply copied into the same path during compile time.
    embeddedTemplateDir = templateDir = EINSTEIN_BOT_TEMPLATE_DIR;
  }

  @Override
  public void processOpts() {
    super.processOpts();

    File sourceFile = new File("/Users/relango/dev/chatbot/runtime/einstein-bot-sdk-java/src/main/java/com/salesforce/einsteinbot/sdk/model/AnyVariable.java");
    try {
      CompilationUnit cu = StaticJavaParser.parse(sourceFile);
      System.out.println("RAJA --> annotations " + cu.getType(0).getName());
      AnnotationExpr jsonSubtypeAnnotation = cu.getType(0)
          .getAnnotationByName("JsonSubTypes").get();
      ArrayInitializerExpr arrayInitializerExpr =  (ArrayInitializerExpr) jsonSubtypeAnnotation.getChildNodes().get(1);
      NormalAnnotationExpr firstValue = (NormalAnnotationExpr) arrayInitializerExpr.getValues().get(0);

      Optional<String> annovationValue = firstValue.getPairs()
          .stream()
          .filter(v -> v.getName().toString().equals("value"))
          .findFirst().map(v -> ((ClassExpr) v.getValue()).getType())
          .map(v -> v.toString());

      firstValue.getPairs().stream().forEach( v -> System.out.println(v.getName() + " ==> " + v.getValue() + " == " + v.getValue().getClass()));
      System.out.println("RAJA --> annotation value == " + annovationValue);



      Optional<AnnotationDeclaration> annotations = cu
          .getAnnotationDeclarationByName("JsonSubTypes");
      System.out.println("RAJA --> " + annotations);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

   /* JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    int result = compiler.run(null, null, null, sourceFile.getPath());
    System.out.println("RAJA compile result -> " + result);

    try {
      Class modelClass = Class.forName("com.salesforce.einsteinbot.sdk.model.AnyRequestMessage");
      System.out.println("RAJA -> " + modelClass.getAnnotations());
    } catch (ClassNotFoundException e) {
      //todo
      e.printStackTrace();
    }*/

    excludeModelsImplementsPolymorphicInterface = Arrays.asList(additionalProperties
        .getOrDefault(PROPERTY_KEY_EXCLUDE_MODELS_IMPLEMENTS_POLYMORPHIC_INTERFACE,"")
        .toString().split(PROPERTY_EXCLUDE_MODELS_IMPLEMENTS_POLYMORPHIC_INTERFACE_DELIMITER_REGEX));

    this.customTypeMapping = this.typeMapping
        .entrySet()
        .stream()
        .filter(e -> isSupportedTypeMapping(e.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private boolean isSupportedTypeMapping(String typeMappingKey) {
    return typeMappingKey.startsWith(ANY_OF_PREFIX) || typeMappingKey.startsWith(ONE_OF_PREFIX);
  }

  @Override
  public CodegenModel fromModel(String name, Schema model) {
    CodegenModel codegenModel = super.fromModel(name, model);

    Optional<String> polymorphicInterface = findInterfaceForModel(name);

    //If model name is present in Key of anyOfTypeMapping, then set property 'polymorphicInterface' to Value of anyOfTypeMapping
    if (polymorphicInterface.isPresent() && !isModelInImplementsExcludeList(name)) {
      additionalProperties.put(POLYMORPHIC_INTERFACE, polymorphicInterface.get());
    } else {
      additionalProperties.remove(POLYMORPHIC_INTERFACE);
    }

    return codegenModel;
  }

  private Optional<String> findInterfaceForModel(String name) {
    return this.customTypeMapping
        .entrySet()
        .stream()
        .filter(e -> checkIfTypeMappingContainName(e, name))
        .findFirst()
        .map(Map.Entry::getValue);
  }

  private boolean isModelInImplementsExcludeList(String name){
   //TODO System.out.println("RAJA EXCLUDING -> " + name + " ==> " + excludeModelsImplementsPolymorphicInterface.contains(name) + " ==? " + excludeModelsImplementsPolymorphicInterface );

    return excludeModelsImplementsPolymorphicInterface.contains(name);
  }

  private boolean checkIfTypeMappingContainName(Map.Entry<String, String> typeMappingEntry,
      String name) {
    return typeMappingEntry.getKey().contains(name);
  }

  @Override
  public String getName() {
    return CODE_GENERATOR_NAME;
  }
}
