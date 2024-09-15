package archives.tater.bot.mathwriter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path

val WORK_DIR = File("work")

val inputFile: File = Path(WORK_DIR.path, "math.tex").toFile()
val outputFile: File = Path(WORK_DIR.path, "math.png").toFile()

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
        inputFile.writeText(latexTemplate(latex))
        val success = withContext(Dispatchers.IO) {
            val check = ProcessBuilder("lacheck", inputFile.name).apply {
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
                inputFile.name
            ).apply {
                directory(WORK_DIR)
                redirectOutput(ProcessBuilder.Redirect.DISCARD)
                redirectError(ProcessBuilder.Redirect.DISCARD)
            }.start().waitFor(3, TimeUnit.SECONDS)
        }
        val bytes = if (success) outputFile.readBytes() else null
        WORK_DIR.listFiles()?.forEach { it.delete() }
        bytes
    }
}
