package org.henry.hinject.utils

import com.android.build.gradle.BaseExtension
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.CtNewMethod
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.ConstPool
import javassist.bytecode.annotation.Annotation
import javassist.bytecode.annotation.StringMemberValue
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.henry.hinject.HInjectParam

import java.util.regex.Pattern

public class InjectUtil {
    private static Project project
    private static ClassPool pool = new ClassPool(true)

    public static void setProject(Project project) {
        InjectUtil.@project = project
    }

    public static Project getProject() {
        return project
    }

    public static BaseExtension getExtension() {
        return project.extensions.getByType(BaseExtension)
    }

    public static HInjectParam getHInject() {
        return project.hinject
    }

    public static boolean regMatch(String pattern, String target) {
        if (isEmpty(pattern) || isEmpty(target)) {
            return false
        }
        return Pattern.matches(pattern, target)
    }

    public static boolean isEmpty(String text) {
        return text == null || text.trim().length() < 1
    }

    /**
     * 添加classPath到ClassPool
     * @param libPath
     */
    public static void appendClassPath(String libPath) {
        pool.appendClassPath(libPath)
    }

    public static String getAndroidPath(def project) {
        def sdkDir
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
        return "${sdkDir}/platforms/android-23/android.jar"
    }

    /**
     * 对所有class文件进行过滤注入
     *
     * @param path
     */
    public static void injectDir(String path) {
        pool.appendClassPath(path)
        File dir = new File(path)
        if (dir.isDirectory()) {
            dir.eachFileRecurse { File file ->
                String filePath = file.absolutePath
                String packagePath = HInject.packageName.replace('.', File.separator)
                if (filePath.endsWith(".class") && filePath.contains(packagePath)) {
                    String className = filePath.substring(filePath.indexOf(packagePath), filePath.length() - 6).replace(File.separator, '.')
                    HInject.inject2Method.entrySet().each { Map.Entry<String, Object> entry ->
                        if (regMatch(entry.getKey(), className)) {
                            Logger.log(InjectUtil.class, "class rule is " + entry.getKey().toString())
                            injectClass(className, path, entry)
                        }
                    }
                }
            }
        }
    }

    /**
     * 将代码注入指定行
     * @param injectLine
     * @param method
     * @param injectCode
     */
    static void inject2Line(String injectLine, CtMethod method, String injectCode) {
        if (injectLine.isInteger()) {
            int line = Integer.parseInt(injectLine)
            method.insertAt(line, injectCode)
        } else if ("before".equals(injectLine)) {
            method.insertBefore(injectCode)
        } else {
            method.insertAfter(injectCode)
        }
        Logger.log(InjectUtil.class, "inject " + method.name + " with " + injectCode + " in line " + injectLine)
    }

    /**
     * 注入注解
     * @param clazz
     * @param method
     * @param entry
     */
    static void injectAnnotation(CtClass clazz, CtMethod method, List annotations) {
        if (annotations) {
            // 注入注解
            annotations.each { Map<String, Object> annotation ->
                String annotationClass = annotation.get(MapKey.KEY_ANNOTATION_CLASS)
                Map<String, String> annotationParam = annotation.get(MapKey.KEY_ANNOTATION_PARAM)
                ConstPool constPool = clazz.classFile.constPool
                AnnotationsAttribute methodAttr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
                Annotation anno = new Annotation(annotationClass, constPool);
                annotationParam.entrySet().each { Map.Entry<String, String> e ->
                    anno.addMemberValue(e.key, new StringMemberValue(e.value, constPool));
                }
                methodAttr.addAnnotation(anno);
                method.getMethodInfo().addAttribute(methodAttr);
                Logger.log(InjectUtil.class, "inject annotation " + annotationClass + " to method " + method.name)
            }
        }
    }

    /**
     * 针对类文件进行注入
     * @param className
     * @param path
     * @param entry
     */
    static void injectClass(String className, String path, Map.Entry<String, Object> entry) {
        Logger.log(InjectUtil.class, "inject class is :" + className)
        CtClass c = pool.getCtClass(className)
        if (c.isFrozen()) {
            c.defrost()
        }
        entry.getValue().each { Map rules ->
            String methodName = rules.get(MapKey.KEY_METHOD_NAME)
            String injectCode = rules.get(MapKey.KEY_INJECT_CODE)
            boolean hasSuper = rules.get(MapKey.KEY_CONTAIN_SUPER_METHOD)
            String injectLine = rules.get(MapKey.KEY_INJECT_LINE)

            CtMethod[] methods = c.declaredMethods
            if (hasSuper) {
                // 包含父类的方法
                c.methods.each { CtMethod method ->
                    if (!methods.contains(method) && regMatch(methodName, method.name)) {
                        CtMethod newMethod = CtNewMethod.delegator(method, c)
                        if (injectLine.isInteger()) throw IllegalArgumentException("Cannot set inject line with number when containSuperMethod is true")
                        inject2Line(injectLine, newMethod, injectCode)
                        injectAnnotation(c, newMethod, rules.get(MapKey.KEY_INJECT_ANNOTATION))
                        c.addMethod(newMethod)
                    }
                }
            }

            methods.each { CtMethod method ->
                if (regMatch(methodName, method.name)) {
                    inject2Line(injectLine, method, injectCode)
                    injectAnnotation(c, method, rules.get(MapKey.KEY_INJECT_ANNOTATION))
                }
            }
        }

        c.writeFile(path)
    }

    public static void injectJar(String path) {
    }
}




