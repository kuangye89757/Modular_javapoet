package com.diaochan.modular.compiler;

import com.diaochan.modular.annotation.ARouter;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;


@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.diaochan.modular.annotation.ARouter"})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedOptions("content")
public class ARouterProcessor extends AbstractProcessor {

    private Elements elementUtils;

    private Types typeUtils;

    private Messager messager;

    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();

        String content = processingEnv.getOptions().get("content");
        messager.printMessage(Diagnostic.Kind.NOTE, content);

    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (set.isEmpty()) return false;

        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(ARouter.class);
        for (Element element : elements) {

            String packageName = elementUtils.getPackageOf(element).getQualifiedName().toString();

            String className = element.getSimpleName().toString();
            
            String finalClassName = packageName + "." + className + "$$ARouter";

            messager.printMessage(Diagnostic.Kind.NOTE, "生成的文件：" + finalClassName);

            ARouter aRouter = element.getAnnotation(ARouter.class);

            //public static Class<?> findTargetClass(String path) {
            MethodSpec methodSpec = MethodSpec.methodBuilder("findTargetClass")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(Class.class)
                    .addParameter(String.class, "path")
                    /**
                     * if(path.equalsIgnoreCase("/app/MainActivity")){
                     *    return MainActivity.class;
                     * }
                     * return null;
                     */
                    .addStatement("if(path.equalsIgnoreCase($S)){ return $T.class", 
                            aRouter.path(),
                            ClassName.get((TypeElement) element))
                    .addStatement("} return null")
                    .build();

            // public class XActivity$$ARouter {        
            TypeSpec typeSpec = TypeSpec.classBuilder(className + "$$ARouter")
                    .addModifiers(Modifier.PUBLIC)
                    .addMethod(methodSpec)
                    .build();

            // package com.diaochan.modular.javapoet;
            JavaFile javaFile = JavaFile.builder(packageName, typeSpec)
                    .build();

            try {
                // 用来在控制台 查看生成的代码
//                javaFile.writeTo(System.out);
                javaFile.writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }
}
