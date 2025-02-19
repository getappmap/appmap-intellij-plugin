package appland

import com.intellij.openapi.progress.blockingContext
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Adapter to allow the use of ProjectActivity and its suspend function from plain Java code.
 */
abstract class ProjectActivityAdapter : ProjectActivity {
    abstract fun runActivity(project: Project)

    @Suppress("UnstableApiUsage")
    final override suspend fun execute(project: Project) {
        // Documentation of the old StartupActivity:
        // If activity implements {@link com.intellij.openapi.project.DumbAware}, it is executed after project is opened
        // on a background thread with no visible progress indicator. Otherwise, it is executed on EDT when indexes are ready.

        when (this) {
            is DumbAware -> blockingContext {
                runActivity(project)
            }

            else -> DumbService.getInstance(project).runWhenSmart {
                runActivity(project)
            }
        }

    }
}