# Chatbot Service OpenAPI Codegen

## Overview

This is an extension to the [openapi-generator-maven-plugin](https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator-maven-plugin) that customizes it for Einstein bot schema specific features like polymorphic types:

* Supports model class generation and serialization for polymorphic types like `anyOf`
* Uses Jackson for JSON serialization and Spring [Webclient](https://www.baeldung.com/spring-5-webclient) for HTTP Client library.



## Usage

This generator is used by the [Einstein Bot API SDK](https://github.com/forcedotcom/einstein-bot-sdk-java) and you should use the SDK. 

However, if you want to use your own custom configuration or want to use different HTTP Client library, you can add plugin to your `pom.xml` to generate code at build time.

Here is example usage in your pom.xml

```xml
<plugin>
                <groupId>org.openapitools</groupId>
                <artifactId>openapi-generator-maven-plugin</artifactId>
                <version>${openapi-generator-version}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>generate</goal>
                        </goals>
                        <configuration>
                            <inputSpec>${project.basedir}/src/main/resources/v4_0_0_api_specs.yaml</inputSpec>
                            <configOptions>
                                <sourceFolder>src/gen/java/main</sourceFolder>
                                <java8>true</java8>
                                <dateLibrary>java8</dateLibrary>
                            </configOptions>
                            <modelPackage>${base-sdk-package}.model</modelPackage>
                            <apiPackage>${base-sdk-package}.api</apiPackage>
                            <invokerPackage>${base-sdk-package}.handler</invokerPackage>
                            <generateApiDocumentation>true</generateApiDocumentation>
                            <generateModelDocumentation>true</generateModelDocumentation>
                            <generatorName>einsteinbot</generatorName>
                            <generateApiTests>false</generateApiTests>
                            <generateModelTests>false</generateModelTests>
                            <typeMappings>
                                AnyOfBooleanVariableDateVariableDateTimeVariableMoneyVariableNumberVariableTextVariableObjectVariableRefVariableListVariable=AnyVariable,AnyOfSessionEndedResponseMessageTextResponseMessageChoicesResponseMessageEscalateResponseMessageStaticContentMessage=AnyResponseMessage,ResponseEnvelopeMessagesOneOf=AnyResponseMessage
                            </typeMappings>
                            <languageSpecificPrimitives>
                                AnyOfBooleanVariableDateVariableDateTimeVariableMoneyVariableNumberVariableTextVariableObjectVariableRefVariableListVariable,AnyOfSessionEndedResponseMessageTextResponseMessageChoicesResponseMessageEscalateResponseMessageStaticContentMessage,ResponseEnvelopeMessagesOneOf
                            </languageSpecificPrimitives>
                            <library>webclient</library>
                        </configuration>
                    </execution>
                </executions>
               <dependencies>
                    <dependency>
                        <groupId>com.salesforce.einsteinbot</groupId>
                        <artifactId>einstein-bot-openapi-codegen-maven-plugin</artifactId>
                        <version>${einstein-bot-openapi-codegen-version}</version>
                    </dependency>
                </dependencies>
            </plugin>
```


* You can refer to [Maven Plugin documentation](https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator-maven-plugin) for all available configuration options.
* You can refer to [Java generator documentation](https://openapi-generator.tech/docs/generators/java/) for list of config options and supported libraries.




## Developer Guide

### Code Style

The project uses the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
Format settings definition files for importing into IDEs are available for [Eclipse](https://github.com/google/styleguide/blob/gh-pages/eclipse-java-google-style.xml)
and [IntelliJ IDEA](https://github.com/google/styleguide/blob/gh-pages/intellij-java-google-style.xml).

### Updating the library

### Versions

The first two fields in the version number match the major / minor version of the base OpenAPI generator. The last field is incremented for our own versioning.

### Templates

`pojo.mustache` file in the `src/main/resources/einsteinbot/Java` directory override the default OpenAPI templates. The remaining templates are copied into the same path at compile time (see pom.xml) . 

When updating the OpenAPI generator version, you need to update the template `pojo.mustache` as well. The directory contains the original pre-modified version of the templates to make it easier to re-apply the changes necessary in the template.

You can get the `pojo.mustache` template for the version you are upgrading by downloading source code from [releases page](https://github.com/OpenAPITools/openapi-generator/tags)  

As of version `5.4.0`, you will need to replace:

```
{{#vendorExtensions.x-implements}}
```
with

```
{{^vendorExtensions.x-implements}}{{#polymorphicInterface}} implements {{polymorphicInterface}} {{/polymorphicInterface}}{{/vendorExtensions.x-implements}} {{#vendorExtensions.x-implements}}
```
on the line that starts with `public class {{classname}}`