package net.lenni0451.classtransform.transformer;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.InjectionInfo;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.utils.Remapper;
import net.lenni0451.classtransform.utils.annotations.AnnotationParser;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ATransformer {

    private final AtomicInteger id = new AtomicInteger();

    /**
     * Transform the target class using the given transformer class
     *
     * @param transformerManager The transformer manager
     * @param classProvider      The class provider
     * @param injectionTargets   The available injection targets
     * @param transformedClass   The target {@link ClassNode}
     * @param transformer        The transformer {@link ClassNode}
     */
    public abstract void transform(final TransformerManager transformerManager, final IClassProvider classProvider, final Map<String, IInjectionTarget> injectionTargets, final ClassNode transformedClass, final ClassNode transformer);


    protected <T extends Annotation> T getAnnotation(final Class<T> annotationClass, final FieldNode field, final IClassProvider classProvider) {
        T annotation = this.getAnnotation(annotationClass, field.visibleAnnotations, classProvider);
        if (annotation == null) annotation = this.getAnnotation(annotationClass, field.invisibleAnnotations, classProvider);
        return annotation;
    }

    protected <T extends Annotation> T getAnnotation(final Class<T> annotationClass, final MethodNode method, final IClassProvider classProvider) {
        T annotation = this.getAnnotation(annotationClass, method.visibleAnnotations, classProvider);
        if (annotation == null) annotation = this.getAnnotation(annotationClass, method.invisibleAnnotations, classProvider);
        return annotation;
    }

    protected <T extends Annotation> T getAnnotation(final Class<T> annotationClass, final List<AnnotationNode> annotations, final IClassProvider classProvider) {
        if (annotations != null) {
            for (AnnotationNode annotation : annotations) {
                if (annotation.desc.equals(Type.getDescriptor(annotationClass))) {
                    return AnnotationParser.parse(annotationClass, classProvider, AnnotationParser.listToMap(annotation.values));
                }
            }
        }
        return null;
    }

    protected void prepareForCopy(final ClassNode transformer, final MethodNode method) {
        AnnotationNode injectionInfo = new AnnotationNode(Type.getDescriptor(InjectionInfo.class));
        injectionInfo.values = Arrays.asList(
                "transformer", transformer.name,
                "originalName", method.name + method.desc
        );
        if (method.invisibleAnnotations == null) method.invisibleAnnotations = new ArrayList<>();
        method.invisibleAnnotations.add(injectionInfo);
    }

    protected void renameAndCopy(final MethodNode injectionMethod, final MethodNode targetMethod, final ClassNode transformer, final ClassNode transformedClass, final String extra) {
        this.prepareForCopy(transformer, injectionMethod);
        injectionMethod.name = targetMethod.name.replace("<", "").replace(">", "") + "$" + extra + this.id.getAndIncrement();
        Remapper.remapAndAdd(transformer, transformedClass, injectionMethod);
    }

}
