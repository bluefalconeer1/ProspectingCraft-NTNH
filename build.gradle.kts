
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

tasks.named<ProcessResources>("processResources") {
    from(rootProject.file("NOTICE.md"))
}
