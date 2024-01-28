import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

val input = ZipFile("build\\libs\\Mindustry.jar")
val base = ZipFile("C:\\Users\\q1048\\Desktop\\PORTABLE-Portable\\Mindustry.jar")
val output = ZipOutputStream(File("C:\\Users\\q1048\\AppData\\Roaming\\Mindustry\\mods\\MindustryX.loader.jar").outputStream())

val baseMap = base.entries().asSequence().associateBy { it.name }

for (entry in input.entries()) {
    if (entry.name.startsWith("sprites") || entry.name == "version.properties") continue
    val baseEntry = baseMap[entry.name]
    if (baseEntry != null) {
        val a = input.getInputStream(entry).use { it.readAllBytes() }
        val b = base.getInputStream(baseEntry).use { it.readAllBytes() }
        val ext = entry.name.substringAfterLast('.', "")
        val eq = when (ext) {
            "", "frag", "vert", "js", "properties" -> a.filter { it != 10.toByte() && it != 13.toByte() } == a.filter { it != 10.toByte() && it != 13.toByte() }
            else -> a.contentEquals(b)
        }
        if (eq) continue
    }
    var outputEntry = entry
    //rename to mod.hjson
    if (entry.name == "MindustryX.hjson") {
        outputEntry = ZipEntry("mod.hjson")
    }
    output.putNextEntry(outputEntry)
    output.write(input.getInputStream(entry).use { it.readAllBytes() })
    output.closeEntry()
}
output.close()
