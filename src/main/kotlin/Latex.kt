package archives.tater.bot.mathwriter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory

val WORK_DIR: File = createTempDirectory("MathBot").toFile().apply {
    deleteOnExit()
}

val INPUT_FILE: File = Path(WORK_DIR.path, "math.tex").toFile()
val OUTPUT_FILE: File = Path(WORK_DIR.path, "math.png").toFile()

fun latexTemplate(latex: String) = """
    |\documentclass[border=0.50001bp,convert={convertexe={magick},outext=.png}]{standalone}
    |
    |\usepackage{xcolor}
    |
    |\usepackage{amsfonts}
    |
    |\usepackage{amsmath}
    |
    |\begin{document}
    |
    |\textcolor{white}{$\displaystyle $latex$}
    |
    |\end{document}
""".trimMargin()

sealed interface ImageResult {
    @JvmInline
    value class Success(val bytes: ByteArray) : ImageResult
    class Error(val message: String) : ImageResult
}

val mutex = Mutex()

suspend fun renderLatex(latex: String): ImageResult {
    return mutex.withLock {
        INPUT_FILE.writeText(latexTemplate(latex))

        // null means success
        val error: String? = withContext(Dispatchers.IO) {
            ProcessBuilder("lacheck", INPUT_FILE.name).apply {
                directory(WORK_DIR)
                redirectOutput(ProcessBuilder.Redirect.PIPE)
                redirectError(ProcessBuilder.Redirect.PIPE)
            }.start().apply {
                waitFor()
                inputReader().readText().run {
                    if (isNotEmpty()) {
                        return@withContext this
                    }
                }
            }
            ProcessBuilder(
                "pdflatex",
                "-shell-escape",
                "-interaction=batchmode",
                "-halt-on-error",
                INPUT_FILE.name
            ).apply {
                directory(WORK_DIR)
                redirectOutput(ProcessBuilder.Redirect.PIPE)
                redirectError(ProcessBuilder.Redirect.PIPE)
            }.start().apply {
                outputWriter().apply {
                    write("x\n")
                    flush()
                    close()
                }
                waitFor()
                if (exitValue() != 0) return@withContext inputReader().readText()
            }
            null
        }

        (if (error == null) ImageResult.Success(OUTPUT_FILE.readBytes()) else ImageResult.Error(error)).also {
            WORK_DIR.listFiles()?.forEach { it.delete() }
        }
    }
}
