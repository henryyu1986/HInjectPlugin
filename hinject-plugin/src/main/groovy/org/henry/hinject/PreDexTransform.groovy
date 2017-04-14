package org.henry.hinject

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.henry.hinject.utils.Inject

public class PreDexTransform extends Transform {
    Project project

    public PreDexTransform(Project project) {
        this.project = project
    }

    // Transfrom在Task列表中的名字
    // TransfromClassesWithPreDexForXXXX
    @Override
    String getName() {
        return "preDex"
    }

    // 指定input的类型
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    // 指定Transfrom的作用范围
    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws IOException, TransformException, InterruptedException {
        Inject.appendClassPath(Inject.getAndroidPath(project))
        transformInvocation.getInputs().each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput directoryInput ->

                // 获取output目录
                def dest = transformInvocation.getOutputProvider().getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)

                Inject.inject(directoryInput.file.absolutePath)

                // 将input的目录复制到output指定目录
                FileUtils.copyDirectory(directoryInput.file, dest)
            }

            input.jarInputs.each { JarInput jarInput ->

                String jarPath = jarInput.file.absolutePath;
                String projectName = project.rootProject.name;
                if (jarPath.endsWith("classes.jar") && jarPath.contains("exploded-aar" + "\\" + projectName)) {
                    Inject.inject(jarPath)
                }

                // 重命名输出文件（同目录copyFile会冲突）
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                def dest = transformInvocation.getOutputProvider().getContentLocation(jarName + md5Name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                FileUtils.copyFile(jarInput.file, dest)
            }
        }
    }
}