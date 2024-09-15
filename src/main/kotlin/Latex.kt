package archives.tater.bot.mathwriter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory

val WORK_DIR: File = createTempDirectory("MathBot").toFile()

val INPUT_FILE: File = Path(WORK_DIR.path, "math.tex").toFile()
val OUTPUT_FILE: File = Path(WORK_DIR.path, "math.png").toFile()

fun latexTemplate(latex: String) = """
    \documentclass[border=0.50001bp,convert={convertexe={magick},outext=.png}]{standalone}
    
    \usepackage{xcolor}

    \usepackage{amsfonts}

    \usepackage{amsmath}

    \begin{document}

    \textcolor{white}{${'$'}${latex.replace("$", "\\$")}${'$'}}

    \end{document}
""".trimIndent()

val mutex = Mutex()

suspend fun renderLatex(latex: String): ByteArray? {
    return mutex.withLock {
        INPUT_FILE.writeText(latexTemplate(latex))
        val success = withContext(Dispatchers.IO) {
            val check = ProcessBuilder("lacheck", INPUT_FILE.name).apply {
                directory(WORK_DIR)
                redirectOutput(ProcessBuilder.Redirect.PIPE)
                redirectError(ProcessBuilder.Redirect.PIPE)
            }.start()
            check.waitFor(1, TimeUnit.SECONDS)
            if (check.inputStream.bufferedReader().readText().isNotEmpty()) {
                return@withContext false
            }
            ProcessBuilder(
                "pdflatex",
                "-shell-escape",
                "-interaction=nonstopmode",
                "-halt-on-error",
                INPUT_FILE.name
            ).apply {
                directory(WORK_DIR)
                redirectOutput(ProcessBuilder.Redirect.DISCARD)
                redirectError(ProcessBuilder.Redirect.DISCARD)
            }.start().waitFor(3, TimeUnit.SECONDS)
        }
        val bytes = if (success) OUTPUT_FILE.readBytes() else null
        WORK_DIR.listFiles()?.forEach { it.delete() }
        bytes
    }
}
