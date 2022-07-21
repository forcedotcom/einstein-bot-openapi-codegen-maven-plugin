/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.openapi.codegen;

import static com.salesforce.einsteinbot.openapi.codegen.EinsteinBotCodeGenerator.PROPERTY_KEY_EXCLUDE_MODELS_IMPLEMENTS_POLYMORPHIC_INTERFACE;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

/**
 * EinsteinBotCodeGeneratorTest - Unit tests for EinsteinBotCodeGenerator.
 *
 * @author relango
 */
public class EinsteinBotCodeGeneratorTest {

  public static final String TEST_RESOURCES_DIR = "src/test/resources/";
  public static final String LIBRARY = "webclient";

  private ImmutableMap<String, String> typeMappings = ImmutableMap.of(
      "AnyOfBooleanVariableDateVariableDateTimeVariableMoneyVariableNumberVariableTextVariableObjectVariableRefVariableListVariable",
      "AnyVariable",
      "OneOfChoiceMessageTextMessageTransferSucceededRequestMessageTransferFailedRequestMessageRedirectMessage",
      "AnyRequestMessage",
      "OneOfSessionEndedResponseMessageTextResponseMessageChoicesResponseMessageEscalateResponseMessageStaticContentMessage",
      "AnyResponseMessage",
      "AnyOfStaticContentAttachmentsStaticContentText",
      "AnyStaticContentMessage"
  );

  private List<String> excludeModelsImplementsPolymorphicInterface = Lists.newArrayList("Attachment", "Status");

  private ImmutableMap<String, Object> additionalProperties = ImmutableMap.of(
      PROPERTY_KEY_EXCLUDE_MODELS_IMPLEMENTS_POLYMORPHIC_INTERFACE,
      excludeModelsImplementsPolymorphicInterface.stream().collect(Collectors.joining("|")));

  private boolean isModelFile(File file) {
    return !file.getName().endsWith("Test.java") && file.getName().endsWith(".java");
  }

  private boolean notEmpty(List<String> failures) {
    return !failures.isEmpty();
  }

  @Test
  public void testWorkingApi() throws Exception {
    File tempDir = Files.createTempDirectory("chatbot-generator").toFile();
    try {
      List<String> failureResults = generate(tempDir, "v5_0_0_api_specs.yaml")
          .stream()
          .filter(this::isModelFile)
          .map(this::findClassDeclarationLine)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .map(this::verifyClassInterface)
          .filter(this::notEmpty)
          .flatMap(List::stream)
          .collect(Collectors.toList());

      Assertions.assertTrue(failureResults.isEmpty(), " Failures Found : " + failureResults);
    } finally {
      FileUtils.deleteDirectory(tempDir);
    }
  }

  protected List<File> generate(File tempDir, String yaml) throws IOException {

    final CodegenConfigurator configurator = new CodegenConfigurator()
        .setAdditionalProperties(additionalProperties)
        .setGeneratorName(EinsteinBotCodeGenerator.CODE_GENERATOR_NAME)
        .setLanguageSpecificPrimitives(typeMappings.keySet())
        .setTypeMappings(typeMappings)
        .setLibrary(LIBRARY)
        .setInputSpec(TEST_RESOURCES_DIR + yaml)
        .setOutputDir(tempDir.getAbsolutePath().replace("\\", "/"));

    final ClientOptInput clientOptInput = configurator.toClientOptInput();
    return new DefaultGenerator().opts(clientOptInput).generate();
  }

  private Optional<Pair<String, String>> findClassDeclarationLine(File file) {
    String className = file.getName().replace(".java", "");
    try {
      List<String> lines = Files.readAllLines(file.toPath());
      return lines.stream()
          .filter(line -> line.contains("public class"))
          .findFirst()
          .map(line -> Pair.of(className, line));
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  private Optional<String> findInterfaceForModel(String name) {
    return this.typeMappings
        .entrySet()
        .stream()
        .filter(e -> isModelPresentInTypeMappingAndNotPresentInExcludeList(name, e))
        .findFirst()
        .map(Map.Entry::getValue);
  }

  private boolean isModelPresentInTypeMappingAndNotPresentInExcludeList(String modelName, Entry<String, String> typeMapping) {
    return checkIfTypeMappingContainName(typeMapping, modelName) &&
        !excludeModelsImplementsPolymorphicInterface.contains(modelName);
  }

  private boolean checkIfTypeMappingContainName(Map.Entry<String, String> typeMappingEntry,
      String name) {
    return typeMappingEntry.getKey().contains(name);
  }

  private List<String> verifyClassInterface(Pair<String, String> classNameAndDeclaration) {
    String className = classNameAndDeclaration.getLeft();
    String classDeclarationLine = classNameAndDeclaration.getRight();
    Optional<String> expectedInterface = findInterfaceForModel(className);
    List<String> failures = new ArrayList<>();

    if (expectedInterface.isPresent()) {
      verifyClassImplementInterface(className, classDeclarationLine, expectedInterface.get())
          .ifPresent(f -> failures.add(f));
    } else {
      verifyClassNotImplementInterface(className, classDeclarationLine)
          .ifPresent(f -> failures.add(f));
    }

    return failures;
  }

  private Optional<String> verifyClassNotImplementInterface(String className, String classDeclarationLine) {
    return typeMappings.values().stream()
        .filter(interfaceName -> classDeclarationLine.contains("implements " + interfaceName))
        .map(interfaceName -> className + " should not implement interface " + interfaceName)
        .findFirst();
  }

  private Optional<String> verifyClassImplementInterface(String className,
      String classDeclarationLine, String interfaceName) {
    if (!classDeclarationLine.contains("implements ") || !classDeclarationLine
        .contains(interfaceName)) {
      return Optional.of(className + " did not implement " + interfaceName);
    } else {
      return Optional.empty();
    }
  }
}
