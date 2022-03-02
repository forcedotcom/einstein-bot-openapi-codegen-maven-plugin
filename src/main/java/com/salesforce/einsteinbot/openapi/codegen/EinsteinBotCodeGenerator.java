
/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.openapi.codegen;

import io.swagger.v3.oas.models.media.Schema;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
  private Map<String, String> customTypeMapping;

  public EinsteinBotCodeGenerator() {
    super();
    //This dir contains only the customized template.
    //But the base class code generator many templates which are simply copied into the same path during compile time.
    embeddedTemplateDir = templateDir = EINSTEIN_BOT_TEMPLATE_DIR;
  }

  @Override
  public void processOpts() {
    super.processOpts();
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
    if (polymorphicInterface.isPresent()) {
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

  private boolean checkIfTypeMappingContainName(Map.Entry<String, String> typeMappingEntry,
      String name) {
    return typeMappingEntry.getKey().contains(name);
  }

  @Override
  public String getName() {
    return CODE_GENERATOR_NAME;
  }
}
