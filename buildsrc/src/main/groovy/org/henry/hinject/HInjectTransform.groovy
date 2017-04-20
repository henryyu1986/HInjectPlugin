package org.henry.hinject

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.henry.hinject.utils.InjectUtil

public class HInjectTransform extends Transform {
    Project project

    public HInjectTransform(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return "hinject"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

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
        InjectUtil.appendClassPath(InjectUtil.getAndroidPath(project))
        // 创建方法

        // 创建属性
        // 已有方法插入代码
        transformInvocation.getInputs().each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput directoryInput ->
                // 获取output目录
                def dest = transformInvocation.getOutputProvider().getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)

                InjectUtil.injectDir(directoryInput.file.absolutePath)

                // 将input的目录复制到output指定目录
                FileUtils.copyDirectory(directoryInput.file, dest)
            }

            // 对Jar文件进行操作
            input.jarInputs.each { JarInput jarInput ->
                String jarPath = jarInput.file.absolutePath;
                String projectName = project.rootProject.name;
                if (jarPath.contains("exploded-aar") && jarPath.contains("classes.jar")) {
                    // 添加全部依赖包
                    InjectUtil.appendClassPath(jarPath)
                }
                InjectUtil.injectJar(jarPath)

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