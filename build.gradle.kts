// Gemeinsame Konfiguration für alle JVM-Module.
allprojects {
    group = "net.frosthain"
    version = providers.gradleProperty("frosthain.version").get()
}

subprojects {
    repositories {
        mavenCentral()
    }
}
