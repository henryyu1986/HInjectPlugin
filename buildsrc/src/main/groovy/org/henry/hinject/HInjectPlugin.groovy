package org.henry.hinject

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.henry.hinject.utils.InjectUtil
import org.henry.hinject.utils.Logger

public class HInjectPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.extensions.create('hinject', HInjectParam)
        InjectUtil.setProject(project)
        // register transform
        project.extensions.findByType(BaseExtension).registerTransform(new HInjectTransform(project))
        Logger.setDebug(InjectUtil.HInject.debug)
        Logger.log(HInjectPlugin.class, "applied HInjectPlugin")
    }
}