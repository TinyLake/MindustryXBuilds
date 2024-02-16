import javassist.ClassPool

buildscript {
    dependencies {
        classpath("org.javassist:javassist:3.30.2-GA")
    }
}
plugins {
    java
//    kotlin()
}

sourceSets {
    main {
        java.srcDirs("src/", layout.buildDirectory.dir("/generated/sources/annotationProcessor/java/main"))
        resources.srcDirs(layout.buildDirectory.dir("generated/patched"))
    }
}

val patchArc = task("patchArc") {
    group = "build"
    dependsOn("classes")
    val arcLib = configurations.compileClasspath.get().find { it.name.startsWith("arc-core-") }
    if (arcLib == null) {
        logger.error("can't find arc-core")
        return@task
    }
    inputs.file(arcLib)
    val outDir = layout.buildDirectory.dir("generated/patched")
    outputs.dir(outDir)

    doLast {
        val pool = ClassPool()
        pool.appendSystemPath()
        pool.appendClassPath(layout.buildDirectory.dir("classes/java/main/").get().toString())
        pool.appendClassPath(arcLib.absolutePath)

        val clz = pool.get("arc.util.Http\$HttpRequest")
        clz.getDeclaredMethod("block")
                .insertBefore("mindustryX.Hooks.onHttp($0);")

        clz.writeFile(outDir.get().toString())
    }
}
tasks.named("jar") {
    dependsOn(patchArc)
}