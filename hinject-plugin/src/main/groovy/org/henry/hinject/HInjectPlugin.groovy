package org.henry.hinject

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

public class HInjectPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        def android = project.extensions.findByType(AppExtension)
        android.registerTransform(new PreDexTransform(project))
    }
}