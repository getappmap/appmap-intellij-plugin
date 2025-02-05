package appland

import com.intellij.openapi.progress.blockingContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Adapter to allow the use of ProjectActivity and its suspend function from plain Java code.
 */
abstract class ProjectActivityAdapter : ProjectActivity {
    abstract fun runActivity(project: Project)

    final override suspend fun execute(project: Project) {
        @Suppress("UnstableApiUsage")
        blockingContext {
            runActivity(project)
        }
    }
}