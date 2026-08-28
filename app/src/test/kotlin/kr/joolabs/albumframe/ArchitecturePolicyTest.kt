package kr.joolabs.albumframe

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitecturePolicyTest {
    private val sourceRoot = listOf(
        File("app/src/main/kotlin"),
        File("src/main/kotlin"),
    ).first(File::isDirectory)

    @Test
    fun domainHasNoAndroidOrOuterLayerDependency() {
        val forbidden = listOf(
            "import android.",
            ".application.",
            ".data.",
            ".presentation.",
        )

        sourceFiles("kr/joolabs/albumframe/domain").forEach { file ->
            val source = file.readText()
            forbidden.forEach { token ->
                assertFalse("${file.path} imports $token", source.contains(token))
            }
        }
    }

    @Test
    fun presentationDependsOnContractsNotDataAdapters() {
        sourceFiles("kr/joolabs/albumframe/presentation").forEach { file ->
            assertFalse(
                "${file.path} imports a data adapter",
                file.readText().contains(".albumframe.data."),
            )
        }
    }

    @Test
    fun dreamServiceDoesNotOwnStorageMediaStoreOrShuffleImplementation() {
        val service = File(
            sourceRoot,
            "kr/joolabs/albumframe/dream/MomentFrameDreamService.kt",
        ).readText()
        listOf(
            "SharedPreferences",
            "MediaStore",
            "Manifest.permission",
            "ProcessCameraProvider",
            "CameraSelector",
            ".shuffle()",
        ).forEach {
            assertFalse("DreamService owns $it", service.contains(it))
        }
    }

    @Test
    fun flutterRuntimeIsAbsentFromNativeSources() {
        sourceRoot.walkTopDown().filter(File::isFile).forEach { file ->
            assertFalse(
                "${file.path} still references Flutter",
                file.readText().contains("io.flutter"),
            )
        }
    }

    @Test
    fun expectedSourceLayersExist() {
        listOf("domain", "application", "data", "presentation", "dream").forEach { layer ->
            assertTrue(
                "$layer layer is missing",
                File(sourceRoot, "kr/joolabs/albumframe/$layer").isDirectory,
            )
        }
    }

    private fun sourceFiles(relativePath: String): Sequence<File> =
        File(sourceRoot, relativePath).walkTopDown().filter {
            it.isFile && it.extension == "kt"
        }
}
