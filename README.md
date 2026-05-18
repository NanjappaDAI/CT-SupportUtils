## Digital.ai Continuous Testing - Cloud Monitor

Simple straight line repo to check device health : 

Below XML checks devices health in Lisbon cloud before nightly runs using Appium OSS

```agsl
mvn clean test -DxmlFile=healthCheckTests.xml
```
Below XML executes Sanity Web Tests for Seetest Client on Global Demo devices.  
```agsl
mvn clean test -DxmlFile=SanityWebTests.xml
```
