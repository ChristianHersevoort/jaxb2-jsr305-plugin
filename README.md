# jaxb2-jsr305-plugin

This jaxb2 plugin adds jsr305 annotations to generated classes.

`@Nonnull` annotation is applied to getters and setters where attribute or element marked "required" and not "nillable".

`@CheckForNull`/`@Nullable` annotations are applied to optional attributes/elements.

## Examples
Nillable string element will be annotated with `@CheckForNull`/`@Nullable` annotations.
```xml
<xsd:element name="optional_field" type="xsd:string" nillable="true"/>
```
```java
@CheckForNull
public String getOptionalField() {
    return optionalField;
}

public void setOptionalField(@Nullable String value) {
    this.optionalField = value;
}
```
Required string element will be annotated with `@Nonnull` annotation.
```xml
<xsd:element name="required_field" type="xsd:string"/>
```
```java
@Nonnull
public String getRequiredField() {
    return requiredField;
}

public void setRequiredField(@Nonnull String value) {
    this.requiredField = value;
}
```

## Dependencies
You should have jsr305 annotations in your project. The easiest way is put FindBugs annotations jar on classpath.
```xml
<dependency>
    <groupId>com.google.code.findbugs</groupId>
    <artifactId>jsr305</artifactId>
    <version>3.0.2</version>
</dependency>
```

## Using with Maven 
Add as XJC-plugin and activate `-Xjsr305` switch.

```xml
<plugin>
    <groupId>org.jvnet.jaxb2.maven2</groupId>
    <artifactId>maven-jaxb2-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <args>
                    <arg>-Xjsr305</arg>
                </args>
                <plugins>
                    <plugin>
                        <groupId>com.github.acmi</groupId>
                        <artifactId>jaxb2-jsr305-plugin</artifactId>
                        <version>1.0.0</version>
                    </plugin>
                </plugins>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>com.google.code.findbugs</groupId>
            <artifactId>jsr305</artifactId>
            <version>3.0.2</version>
        </dependency>
    </dependencies>
</plugin>
```