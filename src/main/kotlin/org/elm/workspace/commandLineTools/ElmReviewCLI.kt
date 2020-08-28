package org.elm.workspace.commandLineTools

import com.intellij.execution.ExecutionException
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.elm.lang.core.psi.ElmFile
import org.elm.openapiext.GeneralCommandLine
import org.elm.openapiext.Result
import org.elm.openapiext.execute
import org.elm.workspace.ElmApplicationProject
import org.elm.workspace.ElmPackageProject
import org.elm.workspace.ParseException
import org.elm.workspace.Version
import java.nio.file.Path

private val log = logger<ElmReviewCLI>()


/**
 * Interact with external `elm-review` process.
 */
class ElmReviewCLI(private val elmReviewExecutablePath: Path) {

    fun runReview(): ProcessOutput {
        val arguments: List<String> = listOf("--report=json")
        return GeneralCommandLine(elmReviewExecutablePath)
                .withParameters(arguments)
                .execute()
    }

    fun queryVersion(): Result<Version> {
        val firstLine = try {
            val arguments: List<String> = listOf("--version")
            GeneralCommandLine(elmReviewExecutablePath)
                    .withParameters(arguments)
                    .execute(timeoutInMilliseconds = 3000)
                    .stdoutLines
                    .firstOrNull()
        } catch (e: ExecutionException) {
            return Result.Err("failed to run elm-review: ${e.message}")
        } ?: return Result.Err("no output from elm-review")

        return try {
            Result.Ok(Version.parse(firstLine))
        } catch (e: ParseException) {
            Result.Err("invalid elm-review version: ${e.message}")
        }
    }

    companion object {
        fun getElmVersion(project: Project, file: VirtualFile): Version? {
            val psiFile = ElmFile.fromVirtualFile(file, project) ?: return null

            return when (val elmProject = psiFile.elmProject) {
                is ElmApplicationProject -> elmProject.elmVersion
                is ElmPackageProject -> elmProject.elmVersion.low
                else -> return null
            }
        }
    }
}