package org.henry.hinject.utils

import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import org.apache.commons.io.FileUtils
import org.gradle.api.InvalidUserDataException

/**
 * Created by AItsuki on 2016/4/7.
 * 注入代码分为两种情况，一种是目录，需要遍历里面的class进行注入
 * 另外一种是jar包，需要先解压jar包，注入代码之后重新打包成jar
 */
public class Inject {
    private static ClassPool pool = new ClassPool(true)

    /**
     * 添加classPath到ClassPool
     * @param libPath
     */
    public static void appendClassPath(String libPath) {
        pool.appendClassPath(libPath)
    }

    public static void inject(String path) {
        if (path.endsWith(".jar")) {
            injectJar(path)
        } else {
            injectDir(path)
        }
    }

    public static String getAndroidPath(def project) {
        def sdkDir;
        Properties properties = new Properties()
        File localProps = project.rootProject.file("local.properties")
        if (localProps.exists()) {
            properties.load(localProps.newDataInputStream())
            sdkDir = properties.getProperty("sdk.dir")
        } else {
            sdkDir = System.getenv("ANDROID_HOME")
        }
        if (sdkDir) {

        } else {
            throw new InvalidUserDataException('$ANDROID_HOME is not defined')
        }
        return "${sdkDir}/platforms/android-23/android.jar";
    }

    /**
     * 遍历该目录下的所有class，对所有class进行代码注入。
     * 其中以下class是不需要注入代码的：
     * --- 1. R文件相关
     * --- 2. 配置文件相关（BuildConfig）
     * --- 3. Application
     * @param path 目录的路径
     */

    public static void injectDir(String path) {
        pool.appendClassPath(path)
        File dir = new File(path)
        if (dir.isDirectory()) {
            dir.eachFileRecurse { File file ->
                String filePath = file.absolutePath
                if (filePath.endsWith(".class") && filePath.contains("Activity")) {
                    int index = filePath.lastIndexOf("debug")
                    if (index != -1) {
                        int end = filePath.length() - 6 // .class = 6
                        String className = filePath.substring(index + 6, end).replace('\\', '.').replace('/', '.')
                        injectClass(className, path)
                    }
                }
            }
        }
    }

    /**
     * 这里需要将jar包先解压，注入代码后再重新生成jar包
     * @path jar包的绝对路径
     */
    public static void injectJar(String path) {
        File jarFile = new File(path)

        // jar包解压后的保存路径
        String jarZipDir = jarFile.getParent() + "/" + jarFile.getName().replace('.jar', '')

        // 解压jar包, 返回jar包中所有class的完整类名的集合（带.class后缀）
        List classNameList = JarZipUtil.unzipJar(path, jarZipDir)

        // 删除原来的jar包
        jarFile.delete()

        // 注入代码
        pool.appendClassPath(jarZipDir)
        for (String className : classNameList) {
            if (className.endsWith(".class")
                    && !className.contains('R$')
                    && !className.contains('R.class')
                    && !className.contains("BuildConfig.class")) {
                className = className.substring(0, className.length() - 6)
                injectClass(className, jarZipDir)
            }
        }

        // 从新打包jar
        JarZipUtil.zipJar(jarZipDir, path)

        // 删除目录
        FileUtils.deleteDirectory(new File(jarZipDir))
    }

    private static void injectClass(String className, String path) {
        CtClass c = pool.getCtClass(className)
        if (c.isFrozen()) {
            c.defrost()
        }
        CtMethod onCreate = c.getDeclaredMethod("onCreate")
        onCreate.insertAt(2, "System.out.println(\"成功\");")
        c.writeFile(path)
    }
}
